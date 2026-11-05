import { useEffect, useState } from "react";
import { api } from "../api";

function Card({ c }) {
  const [open, setOpen] = useState(false);
  return (
    <button
      className={`fc ${open ? "is-open" : ""}`}
      onClick={() => setOpen(!open)}
      aria-pressed={open}
    >
      <span className="fc-inner">
        <span className="fc-face fc-front">
          <small>Question</small>
          {c.front}
        </span>
        <span className="fc-face fc-back">
          <small>Answer</small>
          {c.back}
        </span>
      </span>
    </button>
  );
}

export default function Flashcards() {
  const [cards, setCards] = useState([]);
  useEffect(() => {
    api
      .flashcards()
      .then(setCards)
      .catch(() => {});
  }, []);
  return (
    <>
      <h1>Flashcards</h1>
      <p className="muted">
        {cards.length} cards from your notes. Select a card to flip it.
      </p>
      {cards.length === 0 ? (
        <p className="muted" style={{ marginTop: 32 }}>
          No cards yet. Save an answer from a chat to turn it into a card.
        </p>
      ) : (
        <div className="deck">
          {cards.map((c) => (
            <Card key={c.id} c={c} />
          ))}
        </div>
      )}
    </>
  );
}
