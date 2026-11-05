import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  HandWaving,
  ArrowRight,
  DotsThree,
  CaretRight,
  Fire,
  FileText,
  Lightbulb,
  ChatCircle,
  Cards,
  Cloud,
  ChartBar,
  Brain,
  DeviceMobile,
  Stack,
  Question,
  Sparkle,
  Check,
} from "@phosphor-icons/react";
import { api } from "../api";
import LibraryPanel from "../components/LibraryPanel";
import {
  user,
  resume,
  streak,
  exams,
  tools,
  activity,
  sparks,
  today,
} from "../data/mockData";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const EXAM_ICON = [Cloud, ChartBar, Brain, DeviceMobile];
const TOOL_ICON = [FileText, Stack, Question, Sparkle];
const ACT_ICON = [ChatCircle, FileText, Stack, Question];

export default function Home() {
  const nav = useNavigate();
  const [stats, setStats] = useState({});
  const [documents, setDocuments] = useState([]);
  const [usage, setUsage] = useState({ questionsToday: 0, questionsLimit: 1 });
  useEffect(() => {
    api
      .stats()
      .then(setStats)
      .catch(() => {});
    api
      .documents()
      .then(setDocuments)
      .catch(() => {});
    api
      .usage()
      .then(setUsage)
      .catch(() => {});
  }, []);

  const h = new Date().getHours();
  const hello =
    h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
  const cards = [
    { k: "documents", label: "Documents", Icon: FileText, tone: "blue" },
    {
      k: "insights",
      label: "Concepts learned",
      Icon: Lightbulb,
      tone: "yellow",
    },
    {
      k: "conversations",
      label: "AI conversations",
      Icon: ChatCircle,
      tone: "purple",
    },
    { k: "flashcards", label: "Flashcards reviewed", Icon: Cards, tone: "red" },
  ];

  return (
    <div className="home">
      <div className="home-main">
        <div className="greet">
          <div>
            <h1>
              {hello}, {user.name}{" "}
              <HandWaving
                size={40}
                weight="fill"
                style={{ color: "var(--yellow)" }}
              />
            </h1>
            <p className="muted lead">Stay consistent. You're doing great.</p>
          </div>
          <blockquote className="quote">
            "Discipline today, better results tomorrow."
          </blockquote>
        </div>

        <section className="hero">
          <small>
            <i /> Continue learning
          </small>
          <h2>{resume.title}</h2>
          <p className="muted">
            {resume.last} &nbsp;•&nbsp; {resume.page}
          </p>
          <div className="progress">
            <div className="bar">
              <i style={{ width: `${resume.pct}%` }} />
            </div>
            <span>{resume.pct}%</span>
          </div>
          <div className="hero-actions">
            <button className="btn dark" onClick={() => nav("/chat")}>
              Open chat <ArrowRight size={14} weight="bold" />
            </button>
            <button className="btn ghost" onClick={() => nav("/flashcards")}>
              <Cards size={16} /> Generate flashcards
            </button>
            <button className="icon-btn boxed" aria-label="More">
              <DotsThree size={20} weight="bold" />
            </button>
          </div>
        </section>

        <div className="stat-grid">
          {cards.map(({ k, label, Icon, tone }) => (
            <div className="stat" key={k}>
              <i className={`ico ${tone}`}>
                <Icon size={24} />
              </i>
              <div>
                <b>{stats[k] ?? 0}</b>
                <small>{label}</small>
                <div className="spark">
                  {sparks[k].map((v, i) => (
                    <u key={i} className={tone} style={{ height: v * 3 }} />
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>

        <LibraryPanel documents={documents} />
      </div>

      <aside className="rail">
        <section className="panel">
          <div className="panel-head">
            <h3>Today</h3>
            <small>Resets at midnight</small>
          </div>
          <div className="meter">
            <div>
              <b>{usage.questionsToday}</b>
              <span className="muted">
                of {usage.questionsLimit} questions asked
              </span>
            </div>
            <div className="bar">
              <i
                style={{
                  width: `${Math.min(100, (usage.questionsToday / usage.questionsLimit) * 100)}%`,
                }}
              />
            </div>
          </div>
          <div className="meter">
            <div>
              <b>{today.minutes}</b>
              <span className="muted">of {today.goalMinutes} min studied</span>
            </div>
            <div className="bar">
              <i
                style={{
                  width: `${Math.min(100, (today.minutes / today.goalMinutes) * 100)}%`,
                }}
              />
            </div>
          </div>
        </section>

        <section className="panel streak">
          <div className="streak-top">
            <Fire size={34} weight="fill" style={{ color: "var(--orange)" }} />
            <div>
              <small className="muted">Study streak</small>
              <b>{streak.days} days</b>
            </div>
            <CaretRight size={16} />
          </div>
          <div className="days">
            {DAYS.map((d, i) => (
              <span key={d}>
                <i className={streak.done[i] ? "done" : ""}>
                  {streak.done[i] ? <Check size={12} weight="bold" /> : null}
                </i>
                {d}
              </span>
            ))}
          </div>
          <small className="best">Longest streak: {streak.best} days</small>
        </section>

        <section className="panel">
          <div className="panel-head">
            <h3>Upcoming exams</h3>
            <a href="#" className="link">
              View all
            </a>
          </div>
          {exams.map((e, i) => {
            const Icon = EXAM_ICON[i % 4];
            return (
              <div className="line" key={e.id}>
                <i className={`ico ${e.tone}`}>
                  <Icon size={22} />
                </i>
                <div>
                  <b>{e.name}</b>
                  <small>{e.date}</small>
                </div>
                <span className="pill soft">{e.left} days</span>
              </div>
            );
          })}
        </section>

        <section className="panel">
          <div className="panel-head">
            <h3>Recent activity</h3>
            <Link to="/chat" className="link">
              View all
            </Link>
          </div>
          {activity.map((a, i) => {
            const Icon = ACT_ICON[i];
            return (
              <div className="line" key={a.id}>
                <i className={`ico ${a.tone}`}>
                  <Icon size={20} />
                </i>
                <div>
                  <b>{a.title}</b>
                  <small>{a.sub}</small>
                </div>
                <small className="when">{a.when}</small>
              </div>
            );
          })}
        </section>

        <section className="panel tools-panel">
          <div className="panel-head">
            <h3>Quick tools</h3>
            <a href="#" className="link">
              See all
            </a>
          </div>
          <div className="tool-grid">
            {tools.map((t, i) => {
              const Icon = TOOL_ICON[i];
              return (
                <button
                  className="tool"
                  key={t.id}
                  onClick={() => nav(i === 3 ? "/chat" : "/flashcards")}
                >
                  <i className={`ico ${t.tone}`}>
                    <Icon size={20} />
                  </i>
                  <span>
                    <b>{t.title}</b>
                    <small>{t.sub}</small>
                  </span>
                </button>
              );
            })}
          </div>
        </section>
      </aside>
    </div>
  );
}
