# StudyMind backend

A NotebookLM-style study assistant for students. **Spring Boot 3 (Java 21) + Supabase Postgres**, talking to the React frontend over a JSON REST API.

## What it does

| Feature | How it works |
|---|---|
| **Accounts** | Email + password, JWT login (`Authorization: Bearer <token>`). Every row is scoped to the signed-in student. |
| **Timetable** | The student *writes* it in plain words. The AI (or a built-in rule reader when no AI key is set) turns it into subjects and class slots, and reports how many subjects there are and the weekly hours for each. |
| **Exams** | Add exams with date, maximum marks and importance (1-5); later enter the marks obtained. `/api/exams/summary` gives how many are coming, how finished ones went and the average per subject. |
| **Study plan** | A day-by-day schedule up to the last exam. Each day's study time (the student's daily goal, reduced on heavy lecture days) is split across exams by **how close** each exam is, **how important** it is and **how weak** the student is in that subject (from past marks). The maths is deterministic, so it always works; the AI only adds the coaching summary and tips. |
| **Documents** | Upload PDF / TXT / MD per subject. Text is extracted page by page (PDFBox), split into searchable chunks and stored in Postgres. Scanned PDFs without a text layer are flagged `low_confidence` ("needs review"). |
| **Chat with your PDFs** | Question -> keyword search over *your* chunks (Postgres full-text search) -> the best passages go to the AI with the instruction to answer **only** from them. Page citations are attached by the server from the passages the model used, so they can't be invented. Without an AI key, chat still works and returns the best-matching passages. |
| **Summaries, flashcards, quizzes** | Generated from the document by the AI. Flashcards use a simple spaced-repetition schedule (Leitner boxes). Quizzes are graded server-side; correct answers are only revealed after submitting. |
| **Dashboard** | One call (`/api/dashboard`) returns everything on the Home screen: stats, streak, today's questions and minutes, upcoming exams, recent activity and the "continue learning" card. |

## Setup (about 10 minutes)

1. **Supabase**: create a project. SQL Editor -> paste `sql/schema.sql` -> Run.
2. **Connection details**: Project Settings -> Database -> Connection string -> **Session pooler** (works on IPv4). Turn it into the three `SUPABASE_DB_*` values (see `.env.example`).
3. **JWT secret**: generate your own random string of 32+ characters (`openssl rand -base64 48`). Do not reuse one that has ever been pasted into a chat or an issue.
4. **AI key (free)**: https://aistudio.google.com/apikey -> create a key -> `AI_PROVIDER=gemini`, `AI_API_KEY=...`.
   Free-tier limits are small (a few requests per minute, a few hundred per day) and change over time, so the app makes as few AI calls as it can and caches summaries. Google's free tier may use prompts to improve its models, so check its current terms before uploading private material. Anthropic and OpenAI also work (`AI_PROVIDER=anthropic|openai`, set `AI_MODEL`).
5. **Run**
   - **Eclipse**: File -> Import -> Existing Maven Projects. Then Run -> Run Configurations -> `BackendApplication` -> Environment tab: add each variable from `.env.example`. Run as Java Application. It listens on `http://localhost:8080`.
   - **Terminal**: set the same variables, then `mvn spring-boot:run`.
6. **Check**: open `http://localhost:8080/api/health` (`{"status":"UP","ai":"gemini",...}`), then walk through `docs/api.http`.

> Spring Boot does not read `.env` files by itself: the values must be real environment variables (Run Configuration or terminal).

## API

All routes except `register`, `login` and `health` need `Authorization: Bearer <token>`. Errors are always `{"message": "..."}`; an expired token gives `401`.

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register` `{email,password,name,program?}` · `POST /api/auth/login` · `GET/PUT /api/auth/me` (name, program, `dailyGoalMinutes`) |
| Home | `GET /api/dashboard` · `POST /api/study-sessions` `{minutes}` |
| Subjects | `GET/POST /api/subjects` · `PUT/DELETE /api/subjects/{id}` |
| Timetable | `GET /api/timetable` · `PUT /api/timetable` `{text}` -> entries, subjects, weekly hours |
| Exams | `GET/POST /api/exams` · `PUT/DELETE /api/exams/{id}` · `GET /api/exams/summary` |
| Study plan | `POST /api/study-plan/generate` · `GET /api/study-plan` (204 if none yet) |
| Documents | `GET /api/documents` · `POST /api/documents` (multipart `file`, optional `subjectId`; leave `subjectId` out rather than empty) · `PATCH /api/documents/{id}` `{subjectId}` · `DELETE` · `POST /api/documents/{id}/summary` · `POST /api/documents/{id}/flashcards/generate` `{count}` · `POST /api/documents/{id}/quizzes/generate` `{count,difficulty}` |
| Chat | `GET/POST /api/chats` (`{documentId?|subjectId?}` limits the scope) · `GET /api/chats/{id}/messages` · `POST /api/chats/{id}/messages` `{text}` -> answer · `DELETE /api/chats/{id}` |
| Flashcards | `GET /api/flashcards?documentId=` · `GET /api/flashcards/due` · `POST` · `POST /api/flashcards/{id}/review` `{rating: again|good|easy}` · `DELETE` |
| Quizzes | `GET /api/quizzes` · `GET /api/quizzes/{id}` · `POST /api/quizzes/{id}/attempts` `{answers:[..]}` · `DELETE` |

## Connecting the React app

Set the backend address in the frontend's `.env` (`VITE_API_URL=http://localhost:8080`), send the token on every request, and show `message` from error bodies. Add the frontend's address to `ALLOWED_ORIGINS` (comma separated) before deploying.

## Deploy

The `Dockerfile` runs anywhere that runs containers (Railway, Render, Fly.io; `Dockerfile.vercel` is the same file for Vercel's container deploys). Set the variables from `.env.example` in the host's dashboard. `APP_TIMEZONE` decides what "today" and a streak day mean (default `Asia/Kolkata`).

## Putting it on GitHub

Suggested layout: one repo with `frontend/` (your React app) and `backend/` (this folder).

```powershell
git init
git add .
git status          # check: no .env, no target/ folder, no keys
git commit -m "StudyMind: React frontend + Spring Boot backend"
git branch -M main
git remote add origin https://github.com/<you>/studymind.git
git push -u origin main
```

`.gitignore` already excludes `.env` and build output. Never commit a database password, JWT secret, AI key or the Supabase `service_role` key. If one ever gets committed, rotate it, because deleting the file doesn't remove it from history.

## Tests

`mvn test` runs the unit tests for the pure logic: PDF chunking, search-query building, the timetable reader, the study-plan scheduler and the streak counter.

## Not included yet

- OCR for scanned PDFs (they upload fine but are flagged `low_confidence` and can't be searched).
- `.docx` / `.pptx` uploads.
- Login throttling and password reset by email.
- Embedding-based (semantic) search. Keyword search is used now; it's the first thing to upgrade if answers miss relevant passages.
