import { useEffect, useState } from "react";
import { api } from "../api";
import LibraryPanel from "../components/LibraryPanel";

export default function Library() {
  const [documents, setDocuments] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => {
    api
      .documents()
      .then(setDocuments)
      .catch((e) => setError(e.message));
  }, []);
  return (
    <>
      <h1>Library</h1>
      <p className="muted lead">All your uploaded notes in one place.</p>
      {error && (
        <p className="pill low_confidence" role="alert">
          Could not load documents: {error}
        </p>
      )}
      <LibraryPanel documents={documents} />
    </>
  );
}
