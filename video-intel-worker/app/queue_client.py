import json
from typing import Callable

import pika

from app import config
from app.models import AnalysisRequestedMessage, AnalysisResultMessage


def _connection() -> pika.BlockingConnection:
    credentials = pika.PlainCredentials(config.RABBITMQ_USER, config.RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=config.RABBITMQ_HOST, port=config.RABBITMQ_PORT, credentials=credentials
    )
    return pika.BlockingConnection(parameters)


def run_consumer(handle_requested: Callable[[AnalysisRequestedMessage], AnalysisResultMessage]) -> None:
    """handle_requested does the actual work and returns the result to publish.

    Exceptions raised by handle_requested are caught here and turned into a failure
    result instead of propagating - letting them propagate would nack the message and
    RabbitMQ would redeliver it forever with no backoff, exactly the infinite-retry bug
    we just fixed on the Java consumer side for a different reason. One attempt, one
    outcome (success or failure), always acked.
    """
    connection = _connection()
    channel = connection.channel()
    channel.queue_declare(queue=config.ANALYSIS_REQUESTED_QUEUE, durable=True)
    channel.queue_declare(queue=config.ANALYSIS_COMPLETED_QUEUE, durable=True)
    channel.basic_qos(prefetch_count=1)

    def _on_message(ch, method, _properties, body):
        requested = AnalysisRequestedMessage.from_dict(json.loads(body))
        try:
            result = handle_requested(requested)
        except Exception as e:  # noqa: BLE001 - any failure becomes a reported result, never a crash loop
            result = AnalysisResultMessage(job_id=requested.job_id, success=False, failure_reason=str(e))

        ch.basic_publish(
            exchange="",
            routing_key=config.ANALYSIS_COMPLETED_QUEUE,
            body=json.dumps(result.to_dict()),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )
        ch.basic_ack(delivery_tag=method.delivery_tag)

    channel.basic_consume(queue=config.ANALYSIS_REQUESTED_QUEUE, on_message_callback=_on_message)
    channel.start_consuming()
