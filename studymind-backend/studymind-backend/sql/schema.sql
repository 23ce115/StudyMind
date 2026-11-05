-- -- StudyMind database schema. Run once in Supabase: SQL Editor -> New query -> paste -> Run.
-- -- The Java backend connects with the database password (a full Postgres role), so Row Level Security does not
-- -- get in its way. RLS is switched on at the bottom with NO policies as a safety net: the tables can't be read
-- -- through Supabase's public API even if an anon key ever leaks.

-- create table if not exists users (
--     id                 uuid primary key default gen_random_uuid(),
--     email              text not null unique,
--     password_hash      text not null,
--     name               text not null,
--     program            text,
--     avatar_url         text,
--     daily_goal_minutes integer not null default 120,
--     created_at         timestamptz not null default now()
-- );

-- create table if not exists subjects (
--     id         uuid primary key default gen_random_uuid(),
--     user_id    uuid not null references users(id) on delete cascade,
--     name       text not null,
--     color      text,
--     created_at timestamptz not null default now()
-- );
-- create unique index if not exists subjects_user_name on subjects (user_id, lower(name));

-- create table if not exists timetables (
--     user_id    uuid primary key references users(id) on delete cascade,
--     raw_text   text,
--     updated_at timestamptz not null default now()
-- );

-- create table if not exists timetable_entries (
--     id          uuid primary key default gen_random_uuid(),
--     user_id     uuid not null references users(id) on delete cascade,
--     subject_id  uuid not null references subjects(id) on delete cascade,
--     day_of_week integer not null check (day_of_week between 1 and 7),
--     start_time  time not null,
--     end_time    time not null,
--     location    text
-- );
-- create index if not exists timetable_entries_user on timetable_entries (user_id);

-- create table if not exists exams (
--     id             uuid primary key default gen_random_uuid(),
--     user_id        uuid not null references users(id) on delete cascade,
--     subject_id     uuid not null references subjects(id) on delete cascade,
--     title          text not null,
--     exam_date      date not null,
--     max_marks      integer not null default 100,
--     obtained_marks integer,
--     importance     integer not null default 3 check (importance between 1 and 5),
--     notes          text,
--     created_at     timestamptz not null default now()
-- );
-- create index if not exists exams_user_date on exams (user_id, exam_date);

-- create table if not exists documents (
--     id              uuid primary key default gen_random_uuid(),
--     user_id         uuid not null references users(id) on delete cascade,
--     subject_id      uuid references subjects(id) on delete set null,
--     name            text not null,
--     storage_path    text,
--     size_bytes      bigint,
--     page_count      integer,
--     status          text not null default 'processing',   -- ready | processing | low_confidence
--     summary         text,
--     last_page       integer,
--     last_studied_at timestamptz,
--     created_at      timestamptz not null default now()
-- );
-- create index if not exists documents_user on documents (user_id, created_at desc);

-- -- The searchable text of every uploaded document, split into ~900 character pieces that remember their page.
-- create table if not exists document_chunks (
--     id          uuid primary key default gen_random_uuid(),
--     document_id uuid not null references documents(id) on delete cascade,
--     user_id     uuid not null references users(id) on delete cascade,
--     page_number integer not null,
--     chunk_index integer not null,
--     content     text not null
-- );
-- create index if not exists document_chunks_doc on document_chunks (document_id, chunk_index);
-- create index if not exists document_chunks_fts on document_chunks using gin (to_tsvector('english', content));

-- create table if not exists chats (
--     id          uuid primary key default gen_random_uuid(),
--     user_id     uuid not null references users(id) on delete cascade,
--     title       text not null default 'New chat',
--     document_id uuid references documents(id) on delete set null,
--     subject_id  uuid references subjects(id) on delete set null,
--     created_at  timestamptz not null default now(),
--     updated_at  timestamptz not null default now()
-- );
-- create index if not exists chats_user on chats (user_id, updated_at desc);

-- create table if not exists messages (
--     id                  uuid primary key default gen_random_uuid(),
--     chat_id             uuid not null references chats(id) on delete cascade,
--     role                text not null,                 -- user | assistant
--     kind                text default 'plain',          -- plain | intro | synthesis
--     text                text,
--     heading             text,
--     body                text,
--     snippet_label       text,
--     snippet_code        text,
--     snippet_explanation text,
--     citation            text,
--     created_at          timestamptz not null default now()
-- );
-- create index if not exists messages_chat on messages (chat_id, created_at);

-- create table if not exists flashcards (
--     id               uuid primary key default gen_random_uuid(),
--     user_id          uuid not null references users(id) on delete cascade,
--     document_id      uuid references documents(id) on delete set null,
--     chat_id          uuid references chats(id) on delete set null,
--     subject_id       uuid references subjects(id) on delete set null,
--     front            text not null,
--     back             text not null,
--     box              integer not null default 0,
--     next_review_at   timestamptz not null default now(),
--     review_count     integer not null default 0,
--     last_reviewed_at timestamptz,
--     created_at       timestamptz not null default now()
-- );
-- create index if not exists flashcards_user on flashcards (user_id, next_review_at);

-- create table if not exists quizzes (
--     id          uuid primary key default gen_random_uuid(),
--     user_id     uuid not null references users(id) on delete cascade,
--     document_id uuid references documents(id) on delete set null,
--     subject_id  uuid references subjects(id) on delete set null,
--     title       text not null,
--     difficulty  text not null default 'medium',
--     created_at  timestamptz not null default now()
-- );

-- create table if not exists quiz_questions (
--     id            uuid primary key default gen_random_uuid(),
--     quiz_id       uuid not null references quizzes(id) on delete cascade,
--     order_index   integer not null,
--     prompt        text not null,
--     options_json  text not null,
--     correct_index integer not null check (correct_index between 0 and 3),
--     explanation   text,
--     page          integer
-- );
-- create index if not exists quiz_questions_quiz on quiz_questions (quiz_id, order_index);

-- create table if not exists quiz_attempts (
--     id           uuid primary key default gen_random_uuid(),
--     quiz_id      uuid not null references quizzes(id) on delete cascade,
--     user_id      uuid not null references users(id) on delete cascade,
--     score        integer not null,
--     total        integer not null,
--     answers_json text,
--     taken_at     timestamptz not null default now()
-- );

-- create table if not exists study_plans (
--     id           uuid primary key default gen_random_uuid(),
--     user_id      uuid not null references users(id) on delete cascade,
--     plan_json    text not null,
--     generated_at timestamptz not null default now()
-- );
-- create index if not exists study_plans_user on study_plans (user_id, generated_at desc);

-- create table if not exists activity_log (
--     id         uuid primary key default gen_random_uuid(),
--     user_id    uuid not null references users(id) on delete cascade,
--     type       text not null,
--     title      text not null,
--     detail     text,
--     created_at timestamptz not null default now()
-- );
-- create index if not exists activity_user on activity_log (user_id, created_at desc);

-- create table if not exists study_sessions (
--     id         uuid primary key default gen_random_uuid(),
--     user_id    uuid not null references users(id) on delete cascade,
--     minutes    integer not null,
--     created_at timestamptz not null default now()
-- );
-- create index if not exists study_sessions_user on study_sessions (user_id, created_at desc);

-- create table if not exists usage_daily (
--     id              uuid primary key default gen_random_uuid(),
--     user_id         uuid not null references users(id) on delete cascade,
--     usage_date      date not null,
--     questions_asked integer not null default 0,
--     unique (user_id, usage_date)
-- );

-- -- Safety net (see the note at the top): RLS on, no policies.
-- alter table users             enable row level security;
-- alter table subjects          enable row level security;
-- alter table timetables        enable row level security;
-- alter table timetable_entries enable row level security;
-- alter table exams             enable row level security;
-- alter table documents         enable row level security;
-- alter table document_chunks   enable row level security;
-- alter table chats             enable row level security;
-- alter table messages          enable row level security;
-- alter table flashcards        enable row level security;
-- alter table quizzes           enable row level security;
-- alter table quiz_questions    enable row level security;
-- alter table quiz_attempts     enable row level security;
-- alter table study_plans       enable row level security;
-- alter table activity_log      enable row level security;
-- alter table study_sessions    enable row level security;
-- alter table usage_daily       enable row level security;


-- ============================================================
-- STUDYMIND DATABASE SCHEMA
-- Supabase PostgreSQL
-- Safe to run on existing databases
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 1. USERS
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    name TEXT NOT NULL,
    program TEXT,
    avatar_url TEXT,
    daily_goal_minutes INTEGER NOT NULL DEFAULT 120,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 2. SUBJECTS
-- ============================================================

CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    color TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS subjects_user_name
ON subjects (user_id, LOWER(name));

-- ============================================================
-- 3. TIMETABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS timetables (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    raw_text TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS timetable_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location TEXT
);

CREATE INDEX IF NOT EXISTS timetable_entries_user
ON timetable_entries(user_id);

-- ============================================================
-- 4. EXAMS
-- ============================================================

CREATE TABLE IF NOT EXISTS exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    exam_date DATE NOT NULL,
    max_marks INTEGER NOT NULL DEFAULT 100,
    obtained_marks INTEGER,
    importance INTEGER NOT NULL DEFAULT 3
        CHECK (importance BETWEEN 1 AND 5),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS exams_user_date
ON exams(user_id, exam_date);

-- ============================================================
-- 5. DOCUMENTS
-- ============================================================

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    storage_path TEXT,
    size_bytes BIGINT,
    page_count INTEGER,
    status TEXT NOT NULL DEFAULT 'processing',
    summary TEXT,
    last_page INTEGER,
    last_studied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS documents_user
ON documents(user_id, created_at DESC);

-- ============================================================
-- 6. DOCUMENT CHUNKS
-- ============================================================

CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS document_chunks_doc
ON document_chunks(document_id, chunk_index);

CREATE INDEX IF NOT EXISTS document_chunks_fts
ON document_chunks
USING GIN (to_tsvector('english', content));

-- ============================================================
-- 7. CHATS
-- ============================================================

CREATE TABLE IF NOT EXISTS chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL DEFAULT 'New chat',
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS chats_user
ON chats(user_id, updated_at DESC);

-- ============================================================
-- 8. MESSAGES
-- ============================================================

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    kind TEXT DEFAULT 'plain',
    text TEXT,
    heading TEXT,
    body TEXT,
    snippet_label TEXT,
    snippet_code TEXT,
    snippet_explanation TEXT,
    citation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS messages_chat
ON messages(chat_id, created_at);

-- ============================================================
-- 9. FLASHCARDS
-- ============================================================

CREATE TABLE IF NOT EXISTS flashcards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    chat_id UUID REFERENCES chats(id) ON DELETE SET NULL,
    subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    box INTEGER NOT NULL DEFAULT 0,
    next_review_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    review_count INTEGER NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- IMPORTANT: Migrate existing flashcards tables.
-- CREATE TABLE IF NOT EXISTS does not add missing columns.

ALTER TABLE flashcards
ADD COLUMN IF NOT EXISTS box INTEGER NOT NULL DEFAULT 0;

ALTER TABLE flashcards
ADD COLUMN IF NOT EXISTS next_review_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE flashcards
ADD COLUMN IF NOT EXISTS review_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE flashcards
ADD COLUMN IF NOT EXISTS last_reviewed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS flashcards_user
ON flashcards(user_id, next_review_at);

-- ============================================================
-- 10. QUIZZES
-- ============================================================

CREATE TABLE IF NOT EXISTS quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    difficulty TEXT NOT NULL DEFAULT 'medium',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 11. QUIZ QUESTIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS quiz_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL,
    prompt TEXT NOT NULL,
    options_json TEXT NOT NULL,
    correct_index INTEGER NOT NULL CHECK (correct_index BETWEEN 0 AND 3),
    explanation TEXT,
    page INTEGER
);

CREATE INDEX IF NOT EXISTS quiz_questions_quiz
ON quiz_questions(quiz_id, order_index);

-- ============================================================
-- 12. QUIZ ATTEMPTS
-- ============================================================

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score INTEGER NOT NULL,
    total INTEGER NOT NULL,
    answers_json TEXT,
    taken_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 13. STUDY PLANS
-- ============================================================

CREATE TABLE IF NOT EXISTS study_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_json TEXT NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS study_plans_user
ON study_plans(user_id, generated_at DESC);

-- ============================================================
-- 14. ACTIVITY LOG
-- ============================================================

CREATE TABLE IF NOT EXISTS activity_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    detail TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS activity_user
ON activity_log(user_id, created_at DESC);

-- ============================================================
-- 15. STUDY SESSIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS study_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    minutes INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS study_sessions_user
ON study_sessions(user_id, created_at DESC);

-- ============================================================
-- 16. DAILY USAGE
-- ============================================================

CREATE TABLE IF NOT EXISTS usage_daily (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    questions_asked INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, usage_date)
);

-- ============================================================
-- 17. ENABLE ROW LEVEL SECURITY
-- ============================================================

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE timetables ENABLE ROW LEVEL SECURITY;
ALTER TABLE timetable_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE exams ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE flashcards ENABLE ROW LEVEL SECURITY;
ALTER TABLE quizzes ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE study_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE study_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_daily ENABLE ROW LEVEL SECURITY;

-- No public RLS policies.
-- Backend should connect using a dedicated database role
-- with the required permissions.
