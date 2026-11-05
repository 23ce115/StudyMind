import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  House,
  Books,
  ChatCircle,
  Cards,
  CheckSquare,
  CalendarCheck,
  CalendarBlank,
  ChartBar,
  Sun,
  Moon,
  Bell,
  MagnifyingGlass,
  CaretRight,
  GraduationCap,
} from "@phosphor-icons/react";
import { user } from "../data/mockData";

const links = [
  { to: "/", label: "Home", Icon: House, end: true },
  { to: "/library", label: "Library", Icon: Books },
  { to: "/chat", label: "Chat", Icon: ChatCircle },
  { to: "/flashcards", label: "Flashcards", Icon: Cards },
  { to: "/quizzes", label: "Quizzes", Icon: CheckSquare },
  { to: "/study-plan", label: "Study Plan", Icon: CalendarCheck },
  { to: "/calendar", label: "Calendar", Icon: CalendarBlank },
  { to: "/analytics", label: "Analytics", Icon: ChartBar },
];

export default function Layout({ children }) {
  const nav = useNavigate();
  const [q, setQ] = useState("");
  const box = useRef(null);
  const [theme, setTheme] = useState(
    () => document.documentElement.dataset.theme || "light",
  );
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem("theme", theme);
    } catch {
      /* private mode */
    }
  }, [theme]);
  useEffect(() => {
    const onKey = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        box.current?.focus();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
  const go = () => {
    if (q.trim()) {
      nav("/chat", { state: { firstQuestion: q.trim() } });
      setQ("");
    }
  };

  return (
    <div className="shell">
      <aside className="side">
        <Link to="/" className="brand">
          <GraduationCap size={30} weight="bold" />{" "}
          <span>
            Study<em>Mind</em>
          </span>
        </Link>
        <nav className="nav">
          {links.map(({ to, label, Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) => (isActive ? "active" : "")}
            >
              <Icon size={20} /> <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <Link to="/settings" className="me">
          <i className="avatar">{user.initials}</i>
          <span>
            <b>{user.full}</b>
            <small>{user.program}</small>
          </span>
          <CaretRight size={14} />
        </Link>
      </aside>
      <div className="body">
        <header className="top">
          <label className="search">
            <MagnifyingGlass size={18} />
            <input
              ref={box}
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && go()}
              placeholder="Search your documents, topics or ask anything..."
              aria-label="Search or ask"
            />
            <kbd>Ctrl K</kbd>
          </label>
          <button
            className="icon-btn"
            aria-label={
              theme === "dark" ? "Switch to light mode" : "Switch to dark mode"
            }
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          >
            {theme === "dark" ? <Sun size={20} /> : <Moon size={20} />}
          </button>
          <button className="icon-btn dot" aria-label="Notifications">
            <Bell size={20} />
          </button>
          <i className="avatar solid">{user.initials}</i>
        </header>
        <main className="main">{children}</main>
      </div>
    </div>
  );
}
