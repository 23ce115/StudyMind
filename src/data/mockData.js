export const user = {
  name: "Shrey",
  full: "Shrey Chauhan",
  initials: "SC",
  program: "BTech CSE · SVIT",
  subject: "CS 304: Database Systems",
};
export const usage = { questionsToday: 31, questionsLimit: 50 };
export const stats = {
  documents: 4,
  insights: 312,
  conversations: 7,
  flashcards: 12,
};

export const documents = [
  {
    id: "d1",
    name: "DBMS_Unit3_Notes.pdf",
    subject: "DBMS",
    meta: "Added 2 days ago · 4.2 MB · 42 pages",
    status: "ready",
    action: "Flashcards",
  },
  {
    id: "d2",
    name: "Operating_Systems_Concurrency.pdf",
    subject: "OS",
    meta: "Added yesterday · 1.8 MB · 18 pages",
    status: "ready",
    action: "Quiz",
  },
  {
    id: "d3",
    name: "Machine_Learning_QB.pdf",
    subject: "ML",
    meta: "Added 5 days ago · 3.1 MB · 36 pages",
    status: "ready",
    action: "Summary",
  },
  {
    id: "d4",
    name: "Cloud_Computing_Notes.pdf",
    subject: "Cloud",
    meta: "Added 10 minutes ago · 2.6 MB · 28 pages",
    status: "processing",
    action: "Flashcards",
  },
];

export const chats = [
  {
    id: "c1",
    title: "DBMS normalization forms",
    subtitle: "2 sources",
    when: "2h ago",
  },
  {
    id: "c2",
    title: "Deadlock avoidance vs detection",
    subtitle: "1 source",
    when: "Yesterday",
  },
  {
    id: "c3",
    title: "B-Tree vs B+ Tree indexing",
    subtitle: "DBMS_Unit3_Notes.pdf",
    when: "3 days ago",
  },
  {
    id: "c4",
    title: "Dijkstra's shortest path",
    subtitle: "Algorithms_Graph_Theory.pdf",
    when: "5 days ago",
  },
];

export const messages = [
  {
    id: "m1",
    role: "assistant",
    kind: "intro",
    text: "I've indexed DBMS_Unit3_Notes.pdf (42 pages). Ask me to explain a concept, compare normal forms, or quiz you on decomposition rules.",
  },
  {
    id: "m2",
    role: "user",
    text: "What's the main difference between 3NF and BCNF? Use an example from my notes.",
  },
  {
    id: "m3",
    role: "assistant",
    kind: "synthesis",
    heading: "BCNF closes the loophole 3NF leaves open",
    body: "A relation is in 3NF if every non-prime attribute depends on the candidate keys directly. 3NF still lets a prime attribute be determined by a non-superkey. BCNF removes that exception: for every dependency X → Y, X must be a superkey.",
    snippet: {
      label: "Textbook example, page 12",
      code: "R(Student, Subject, Teacher)",
      explanation:
        "(Student, Subject) is the candidate key, but Teacher → Subject holds. This satisfies 3NF because Subject is a prime attribute, yet violates BCNF because Teacher is not a superkey.",
    },
    citation: "DBMS_Unit3_Notes.pdf, p.12",
  },
];

export const flashcards = [
  {
    id: "f1",
    front: "When is a relation in BCNF?",
    back: "For every functional dependency X → Y, X is a superkey.",
  },
  {
    id: "f2",
    front: "Does BCNF decomposition always preserve dependencies?",
    back: "No. It is always lossless-join, but dependency preservation is not guaranteed.",
  },
  {
    id: "f3",
    front: "What is a prime attribute?",
    back: "An attribute that belongs to at least one candidate key.",
  },
];

export const resume = {
  title: "DBMS Unit 3 Notes",
  last: "Last studied 2 days ago",
  page: "Page 18 of 42",
  pct: 42,
};
export const streak = { days: 5, best: 12, done: [1, 1, 1, 1, 1, 0, 0] };
export const exams = [
  {
    id: "e1",
    name: "Cloud Computing",
    date: "25 Nov 2026",
    left: 49,
    tone: "blue",
  },
  {
    id: "e2",
    name: "Machine Learning",
    date: "28 Nov 2026",
    left: 52,
    tone: "green",
  },
  { id: "e3", name: "AI", date: "30 Nov 2026", left: 54, tone: "red" },
  { id: "e4", name: "MAD", date: "2 Dec 2026", left: 56, tone: "purple" },
];
export const tools = [
  { id: "t1", title: "Summarize PDF", sub: "Get concise notes", tone: "blue" },
  {
    id: "t2",
    title: "Generate Flashcards",
    sub: "Turn notes into cards",
    tone: "orange",
  },
  {
    id: "t3",
    title: "Create a Quiz",
    sub: "Test your understanding",
    tone: "purple",
  },
  { id: "t4", title: "Ask AI", sub: "Chat with your documents", tone: "green" },
];
export const activity = [
  {
    id: "a1",
    title: "DBMS normalization forms",
    sub: "You asked a question",
    when: "2h ago",
    tone: "gray",
  },
  {
    id: "a2",
    title: "Deadlock avoidance vs detection",
    sub: "AI gave an answer",
    when: "Yesterday",
    tone: "blue",
  },
  {
    id: "a3",
    title: "Cloud deployment models",
    sub: "Generated 12 flashcards",
    when: "2 days ago",
    tone: "orange",
  },
  {
    id: "a4",
    title: "Difference between RAG and fine-tuning",
    sub: "You asked a question",
    when: "3 days ago",
    tone: "green",
  },
];
export const sparks = {
  documents: [2, 3, 3, 5, 6, 8],
  insights: [3, 4, 6, 6, 8, 9],
  conversations: [2, 5, 3, 6, 4, 7],
  flashcards: [1, 2, 2, 3, 5, 4],
};

export const today = { minutes: 35, goalMinutes: 60 };
