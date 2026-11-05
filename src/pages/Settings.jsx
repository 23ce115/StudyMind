import { user } from "../data/mockData";

export default function Settings() {
  return (
    <>
      <h1>Settings</h1>
      <p className="muted">Signed in as {user.name}</p>
      <div
        className="card"
        style={{ padding: 24, marginTop: 32, maxWidth: 520 }}
      >
        <h2>Backend connection</h2>
        <p className="muted" style={{ marginTop: 8 }}>
          Currently using{" "}
          {import.meta.env.VITE_API_URL
            ? import.meta.env.VITE_API_URL
            : "built-in sample data"}
          . Set VITE_API_URL in your .env file to use the Java API.
        </p>
      </div>
    </>
  );
}
