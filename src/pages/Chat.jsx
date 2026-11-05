import { useEffect, useRef, useState } from "react";
import { useLocation, useParams } from "react-router-dom";
import { ArrowUp, Copy, Quotes } from "@phosphor-icons/react";
import { api } from "../api";

function Answer({ m }) {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard?.writeText(m.body || m.text || "");
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };
  if (m.kind === "intro") return <p className="msg-ai muted">{m.text}</p>;
  return (
    <div className="msg-ai">
      {m.heading && <h2>{m.heading}</h2>}
      <p>{m.body}</p>
      {m.snippet && (
        <div className="snippet">
          <small className="muted">{m.snippet.label}</small>
          <code>{m.snippet.code}</code>
          <p>{m.snippet.explanation}</p>
        </div>
      )}
      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        {m.citation && (
          <button className="cite">
            <Quotes size={12} weight="bold" />
            {m.citation}
          </button>
        )}
        <button
          className="btn ghost"
          style={{ padding: "2px 10px", marginTop: 12 }}
          onClick={copy}
        >
          <Copy size={14} weight="bold" />
          {copied ? "Copied" : "Copy"}
        </button>
      </div>
    </div>
  );
}

export default function Chat() {
  const { chatId } = useParams();
  const { state } = useLocation();
  const [msgs, setMsgs] = useState([]);
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const end = useRef(null);

  useEffect(() => {
    api
      .messages(chatId)
      .then(setMsgs)
      .catch(() => setMsgs([]));
  }, [chatId]);
  useEffect(() => {
    end.current?.scrollIntoView({ behavior: "smooth" });
  }, [msgs]);

  const send = async (value) => {
    const t = (value ?? text).trim();
    if (!t || busy) return;
    setText("");
    setBusy(true);
    setMsgs((p) => [...p, { id: crypto.randomUUID(), role: "user", text: t }]);
    try {
      const reply = await api.send(chatId, t);
      setMsgs((p) => [...p, reply]);
    } catch (e) {
      setMsgs((p) => [
        ...p,
        {
          id: crypto.randomUUID(),
          role: "assistant",
          kind: "synthesis",
          body: `That didn't send: ${e.message}`,
        },
      ]);
    }
    setBusy(false);
  };

  useEffect(() => {
    if (state?.firstQuestion) send(state.firstQuestion);
  }, []); // eslint-disable-line

  return (
    <div className="chat">
      <div className="chat-head">
        <h2>DBMS normalization forms</h2>
        <p className="muted">
          Answers come only from your documents, with page citations.
        </p>
      </div>
      <div className="thread">
        {msgs.map((m) =>
          m.role === "user" ? (
            <div className="msg-user" key={m.id}>
              {m.text}
            </div>
          ) : (
            <Answer key={m.id} m={m} />
          ),
        )}
        {busy && <p className="muted">Reading your notes…</p>}
        <div ref={end} />
      </div>
      <div className="card compose">
        <input
          placeholder="Ask a follow-up question"
          value={text}
          aria-label="Message"
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
        />
        <button
          className="btn"
          onClick={() => send()}
          aria-label="Send"
          disabled={busy}
        >
          <ArrowUp size={16} weight="bold" />
        </button>
      </div>
    </div>
  );
}
