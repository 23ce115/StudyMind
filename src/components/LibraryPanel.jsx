import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  FilePdf,
  MagnifyingGlass,
  UploadSimple,
  ChatCircle,
  Cards,
  CheckCircle,
  DotsThree,
  CaretDown,
} from "@phosphor-icons/react";

const LABEL = {
  ready: "Ready",
  processing: "Processing",
  low_confidence: "Needs review",
};
const TONES = ["red", "blue", "purple", "orange"];

export default function LibraryPanel({ documents }) {
  const nav = useNavigate();
  const [subject, setSubject] = useState("All");
  const [q, setQ] = useState("");
  const [sort, setSort] = useState("recent");

  const subjects = [
    "All",
    ...new Set(documents.map((d) => d.subject ?? "Others")),
  ];
  const shown = useMemo(() => {
    const list = documents.filter(
      (d) =>
        (subject === "All" || (d.subject ?? "Others") === subject) &&
        d.name.toLowerCase().includes(q.toLowerCase()),
    );
    return sort === "name"
      ? [...list].sort((a, b) => a.name.localeCompare(b.name))
      : list;
  }, [documents, subject, q, sort]);

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Your library</h2>
        <button className="btn dark">
          <UploadSimple size={16} weight="bold" /> Upload
        </button>
      </div>
      <div className="chips">
        {subjects.map((s) => (
          <button
            key={s}
            className={`chip ${s === subject ? "on" : ""}`}
            onClick={() => setSubject(s)}
          >
            {s}
          </button>
        ))}
      </div>
      <div className="tools-row">
        <label className="field">
          <MagnifyingGlass size={16} />
          <input
            placeholder="Search documents..."
            value={q}
            onChange={(e) => setQ(e.target.value)}
            aria-label="Search documents"
          />
        </label>
        <label className="field select">
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            aria-label="Sort"
          >
            <option value="recent">Recently added</option>
            <option value="name">Name</option>
          </select>
          <CaretDown size={14} />
        </label>
      </div>
      {shown.length === 0 && (
        <p className="muted empty">
          No documents match. Try another subject or upload a PDF.
        </p>
      )}
      {shown.map((d, i) => (
        <div className="doc" key={d.id}>
          <i className={`pdf ${TONES[i % 4]}`}>
            <FilePdf size={20} weight="fill" />
          </i>
          <div className="doc-t">
            <b>{d.name}</b>
            <small>{d.meta}</small>
          </div>
          <span className={`pill ${d.status}`}>
            {d.status === "ready" && <CheckCircle size={14} weight="fill" />}
            {LABEL[d.status]}
          </span>
          <button className="btn ghost sm" onClick={() => nav("/chat")}>
            <ChatCircle size={16} /> Chat
          </button>
          <button className="btn ghost sm" onClick={() => nav("/flashcards")}>
            <Cards size={16} /> {d.action ?? "Flashcards"}
          </button>
          <button className="icon-btn" aria-label="More">
            <DotsThree size={20} weight="bold" />
          </button>
        </div>
      ))}
    </section>
  );
}
