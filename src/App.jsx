import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import Home from "./pages/Home";
import Library from "./pages/Library";
import Chat from "./pages/Chat";
import Flashcards from "./pages/Flashcards";
import Settings from "./pages/Settings";
import Soon from "./pages/Soon";

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/library" element={<Library />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/chat/:chatId" element={<Chat />} />
        <Route path="/flashcards" element={<Flashcards />} />
        <Route path="/quizzes" element={<Soon title="Quizzes" />} />
        <Route path="/study-plan" element={<Soon title="Study Plan" />} />
        <Route path="/calendar" element={<Soon title="Calendar" />} />
        <Route path="/analytics" element={<Soon title="Analytics" />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
