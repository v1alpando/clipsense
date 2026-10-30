# Video Intel — Frontend

Web client for Video Intel: upload a video, get an AI analysis, chat about it.

## Stack

React + TypeScript + Vite, Tailwind CSS v4, shadcn/ui, react-router.

## Structure

```
src/
├── app/           # App shell: router, layout
├── pages/         # Route-level composition (HomePage, VideoPage, ...)
├── features/      # video / analysis / chat — each with api/components/hooks/types
├── components/ui/ # shadcn/ui primitives
└── lib/           # cross-feature helpers (env config, cn, ...)
```

## Setup

```bash
npm install
cp .env.example .env   # point VITE_API_BASE_URL at your backend
npm run dev
```
