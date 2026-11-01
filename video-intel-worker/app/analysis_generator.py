import json
import logging
from typing import Any, List, Optional

from pydantic import BaseModel, Field

from app import config

log = logging.getLogger(__name__)


class HookAnalysis(BaseModel):
    score: int = Field(description="Retention score from 1 to 10 for the first 3-5 seconds of the video.")
    type: str = Field(description="Hook category, e.g. 'Провокационный вопрос / Интрига / Проблема'")
    critique: str = Field(description="Assessment of viewer retention in the first 3-5 seconds.")
    suggestion: str = Field(description="Concrete actionable recommendation to make the hook more viral.")


class KeyMoment(BaseModel):
    timestamp: float = Field(description="Start time of the chapter in seconds.")
    title: str = Field(description="Short title of the chapter/key moment.")
    description: str = Field(description="Concise summary of what happens in this block.")


class DeepAnalysisResult(BaseModel):
    summary: str = Field(description="Concise 2-3 sentence summary of the entire video.")
    hook: HookAnalysis
    key_moments: List[KeyMoment]
    takeaways: List[str] = Field(description="List of 3-5 key takeaways from the video.")


SYSTEM_PROMPT = (
    "Ты — эксперт по аналитике и удержанию аудитории видеоконтента (AI Video Intelligence). "
    "Проанализируй транскрипт видео с таймкодами и верни глубокий семантический разбор строго в формате JSON: "
    "summary (краткая выжимка в 2-3 предложениях), "
    "hook (оценка первых 3-5 секунд по 10-балльной шкале, тип хука, критика удержания и конкретная рекомендация для виральности), "
    "key_moments (главы с таймкодами timestamp в секундах, названием и описанием), "
    "takeaways (3-5 главных смысловых тезисов). "
    "Отвечай на языке оригинала транскрипта (или на русском, если транскрипт русскоязычный/смешанный)."
)


def _format_segments_for_prompt(transcript_text: str, segments: list[dict[str, Any]]) -> str:
    if not segments:
        return f"Full Transcript:\n{transcript_text}"

    lines = ["Segments with timestamps:"]
    for seg in segments:
        start = float(seg.get("start", 0.0))
        end = float(seg.get("end", 0.0))
        text = str(seg.get("text", "")).strip()
        lines.append(f"[{start:.1f}s - {end:.1f}s]: {text}")
    return "\n".join(lines)


def _generate_with_gemini(prompt_input: str) -> dict[str, Any]:
    """Generate structured video analysis using Google GenAI Interactions API (gemini-3.8-flash)."""
    from google import genai

    client = genai.Client(api_key=config.GEMINI_API_KEY)
    model_name = config.ANALYSIS_MODEL or "gemini-3.8-flash"

    # Substitute deprecated 1.5/2.0/2.5 models with current gemini-3.8-flash
    if any(dep in model_name for dep in ("gemini-1.5", "gemini-2.0", "gemini-2.5")):
        log.info("Substituting deprecated model '%s' with 'gemini-3.8-flash'", model_name)
        model_name = "gemini-3.8-flash"

    log.info("Requesting deep video analysis from Google Gemini (%s)...", model_name)
    interaction = client.interactions.create(
        model=model_name,
        system_instruction=SYSTEM_PROMPT,
        input=prompt_input,
        response_format={
            "type": "text",
            "mime_type": "application/json",
            "schema": DeepAnalysisResult.model_json_schema(),
        },
    )

    parsed = DeepAnalysisResult.model_validate_json(interaction.output_text)
    return parsed.model_dump()


def _generate_with_openai(prompt_input: str) -> dict[str, Any]:
    """Generate structured video analysis using OpenAI GPT-4o-mini."""
    from openai import OpenAI

    client = OpenAI(api_key=config.OPENAI_API_KEY)
    log.info("Requesting deep video analysis from OpenAI (gpt-4o-mini)...")

    response = client.beta.chat.completions.parse(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt_input},
        ],
        response_format=DeepAnalysisResult,
    )
    parsed = response.choices[0].message.parsed
    if parsed is None:
        raise ValueError("OpenAI returned empty parsed response")
    return parsed.model_dump()


def generate_deterministic_fallback(
    transcript_text: str,
    segments: list[dict[str, Any]],
) -> dict[str, Any]:
    """Deterministic timeline-based fallback when LLM keys/quotas are unavailable.

    Extracts the opening 3-5 seconds as the hook, groups segments into logical
    chapters (Key Moments), and builds summary & takeaways so the pipeline never fails.
    """
    clean_text = (transcript_text or "").strip()
    if not clean_text and not segments:
        return DeepAnalysisResult(
            summary="Видео не содержит распознанной речи (беззвучный ролик или фоновая музыка).",
            hook=HookAnalysis(
                score=5,
                type="Визуальный старт / Без речи",
                critique="В первые 3–5 секунд отсутствует голосовой хук. Удержание зависит исключительно от визуального ряда.",
                suggestion="Добавьте динамичный голосовой тезис или интригующий вопрос в первые 3 секунды, чтобы повысить удержание.",
            ),
            key_moments=[
                KeyMoment(
                    timestamp=0.0,
                    title="Начало видео",
                    description="Визуальный ряд без речевого сопровождения.",
                )
            ],
            takeaways=[
                "Речевая дорожка отсутствует или не содержит слов.",
                "Рекомендуется добавить субтитры или закадровую озвучку для повышения вовлеченности.",
            ],
        ).model_dump()

    # 1. Analyze opening hook (first 5 seconds or first segment)
    hook_segments = [s for s in segments if float(s.get("start", 0.0)) < 5.0]
    if not hook_segments and segments:
        hook_segments = [segments[0]]

    hook_text = " ".join(str(s.get("text", "")).strip() for s in hook_segments).strip() or clean_text[:120]
    hook_words = hook_text.split()
    has_question = "?" in hook_text

    if has_question:
        hook_type = "Провокационный вопрос / Интрига"
        hook_score = 8
        critique = f"Первые секунды начинаются с вопроса («{hook_text[:80]}»), что сразу вовлекает зрителя."
        suggestion = "Усильте визуальную динамику на первой секунде и покажите конечный результат сразу после вопроса."
    elif len(hook_words) >= 6:
        hook_type = "Прямое утверждение / Контекст"
        hook_score = 7
        critique = f"Старт видео задает тему («{hook_text[:80]}»), обеспечивая базовое удержание первых 3–5 секунд."
        suggestion = "Сократите подводку и начните с острой проблемы, цифры или неожиданного контраста в первые 3 секунды."
    else:
        hook_type = "Медленный разгон / Короткое вступление"
        hook_score = 6
        critique = f"В первые секунды мало речевой плотности («{hook_text[:80]}»), есть риск свайпа."
        suggestion = "Уберите паузы в начале и вынесите самый сильный тезис ролика в первые 2–3 секунды."

    # 2. Build Key Moments (group segments into chapters of ~15-30s or up to 5 chapters)
    key_moments: list[KeyMoment] = []
    if segments:
        total_segments = len(segments)
        chunk_size = max(1, (total_segments + 3) // 4)  # Aim for up to 4 chapters
        for idx, i in enumerate(range(0, total_segments, chunk_size), start=1):
            group = segments[i : i + chunk_size]
            start_ts = round(float(group[0].get("start", 0.0)), 1)
            group_text = " ".join(str(s.get("text", "")).strip() for s in group).strip()
            words = group_text.split()
            title_preview = " ".join(words[:5]).capitalize() if words else f"Часть {idx}"
            title = f"Глава {idx}: {title_preview}" if idx > 1 else f"Вступление: {title_preview}"
            description = group_text[:160] + ("..." if len(group_text) > 160 else "")
            key_moments.append(KeyMoment(timestamp=start_ts, title=title, description=description))
    else:
        key_moments.append(
            KeyMoment(
                timestamp=0.0,
                title="Основная часть",
                description=clean_text[:160] + ("..." if len(clean_text) > 160 else ""),
            )
        )

    # 3. Build Summary (2-3 sentences preview)
    sentences = [s.strip() for s in clean_text.replace("!", ".").replace("?", ".").split(".") if s.strip()]
    if len(sentences) >= 2:
        summary = ". ".join(sentences[:2]) + "."
    else:
        words = clean_text.split()
        summary = " ".join(words[:35]) + ("..." if len(words) > 35 else "")

    # 4. Build Takeaways (3 key points from distinct parts of the transcript)
    takeaways: list[str] = []
    if len(sentences) >= 3:
        step = max(1, len(sentences) // 3)
        for s in sentences[::step][:3]:
            takeaways.append(s + ".")
    else:
        for km in key_moments[:3]:
            if km.description:
                takeaways.append(km.description)
        while len(takeaways) < 3:
            if len(takeaways) == 1:
                takeaways.append(f"Основной фокус первой сцены: {hook_text[:100]}.")
            else:
                takeaways.append(f"Проанализировано сегментов речи: {len(segments) or 1}.")

    return DeepAnalysisResult(
        summary=summary,
        hook=HookAnalysis(
            score=hook_score,
            type=hook_type,
            critique=critique,
            suggestion=suggestion,
        ),
        key_moments=key_moments,
        takeaways=takeaways,
    ).model_dump()


def generate_analysis(
    transcript_text: str,
    segments: Optional[list[dict[str, Any]]] = None,
) -> dict[str, Any]:
    """Generate structured semantic video analysis (summary, hook, key_moments, takeaways).

    Priority:
    1. Google Gemini (`GEMINI_API_KEY` via `google-genai` SDK, `gemini-3.8-flash`)
    2. OpenAI (`OPENAI_API_KEY` via `gpt-4o-mini`, if `TRANSCRIPTION_PROVIDER != "local"` or key is active)
    3. Deterministic local timeline analyzer (100% offline/zero-cost fallback, never fails)
    """
    safe_segments = segments or []
    prompt_input = _format_segments_for_prompt(transcript_text or "", safe_segments)

    if config.GEMINI_API_KEY:
        try:
            return _generate_with_gemini(prompt_input)
        except Exception as exc:
            log.warning("Gemini analysis failed (%s). Falling back...", exc)

    if config.OPENAI_API_KEY and config.TRANSCRIPTION_PROVIDER == "openai":
        try:
            return _generate_with_openai(prompt_input)
        except Exception as exc:
            log.warning("OpenAI analysis failed (%s). Falling back to deterministic analyzer...", exc)

    log.info("Generating deterministic semantic video analysis from transcript & timestamps...")
    return generate_deterministic_fallback(transcript_text or "", safe_segments)
