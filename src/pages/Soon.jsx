export default function Soon({ title }) {
  return (
    <>
      <h1>{title}</h1>
      <div className="panel" style={{ marginTop: 24, maxWidth: 520 }}>
        <h2>Coming soon</h2>
        <p className="muted" style={{ marginTop: 8 }}>
          {title} isn't built yet. Library, Chat and Flashcards are ready to
          use.
        </p>
      </div>
    </>
  );
}
