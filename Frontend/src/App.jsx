
import { useState } from "react";
import "./App.css";

const API_BASE = (
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8081"
).replace(/\/+$/, "");

export default function App() {
  const [repositoryId, setRepositoryId] = useState("1");
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState(null);
  const [review, setReview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState("ask");

  async function callApi(path, body) {
    const url = new URL(`${API_BASE}${path}`);

    const options = {
      method: "POST",
      headers: {
        Accept: "application/json",
      },
    };

    if (path === "/api/github/ask-repository") {
      // This endpoint expects request parameters, not a JSON body.
      url.searchParams.set("repository_id", String(body.repositoryId));
      url.searchParams.set("question", body.question);
    } else {
      // CodeReviewController expects a JSON request body.
      options.headers["Content-Type"] = "application/json";
      options.body = JSON.stringify(body);
    }

    const response = await fetch(url.toString(), options);
    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      throw new Error(
          data.message ||
          data.error ||
          `Request failed with HTTP ${response.status}`
      );
    }

    return data;
  }

  function validateRepositoryId() {
    const id = Number(repositoryId);

    if (!Number.isSafeInteger(id) || id <= 0) {
      setError("Enter a valid positive repository ID.");
      return null;
    }

    return id;
  }

  async function submitQuestion(event) {
    event.preventDefault();

    const id = validateRepositoryId();
    if (id === null) return;

    const trimmedQuestion = question.trim();

    if (!trimmedQuestion) {
      setError("Enter a question about your repository.");
      return;
    }

    setLoading(true);
    setError("");
    setAnswer(null);
    setReview(null);

    try {
      const data = await callApi("/api/github/ask-repository", {
        repositoryId: id,
        question: trimmedQuestion,
      });

      setAnswer(data);
    } catch (err) {
      setError(err.message || "Unable to ask the repository.");
    } finally {
      setLoading(false);
    }
  }

  async function runReview() {
    const id = validateRepositoryId();
    if (id === null) return;

    setLoading(true);
    setError("");
    setReview(null);
    setAnswer(null);

    try {
      const data = await callApi("/api/code-review/repository", {
        repositoryId: id,
      });

      setReview(data);
    } catch (err) {
      setError(err.message || "Unable to review the repository.");
    } finally {
      setLoading(false);
    }
  }

  function changeTab(tab) {
    setActiveTab(tab);
    setError("");
  }


  function renderSources(sources) {
    if (!Array.isArray(sources) || sources.length === 0) {
      return null;
    }

    return (
        <>
          <h3>Source references</h3>

          <div className="source-list">
            {sources.map((source, index) => {
              const path =
                  source.file ??
                  source.path ??
                  source.filePath ??
                  source.file_path ??
                  source.source ??
                  source.metadata?.path ??
                  source.metadata?.file ??
                  source.metadata?.filePath;

              const startLine =
                  source.start_line ??
                  source.startLine ??
                  source.start_line_number ??
                  source.metadata?.start_line;

              const endLine =
                  source.end_line ??
                  source.endLine ??
                  source.end_line_number ??
                  source.metadata?.end_line;

              return (
                  <div
                      className="source-item"
                      key={`${path || "source"}-${startLine ?? index}`}
                  >
                    <strong>{path || "Source path unavailable"}</strong>

                    <span>
                {startLine != null
                    ? `Lines ${startLine}–${endLine ?? startLine}`
                    : "Relevant retrieved code"}
              </span>
                  </div>
              );
            })}
          </div>
        </>
    );
  }


  return (
      <div className="app-shell">
        <aside className="sidebar">
          <div className="brand">
            <div className="brand-icon">⌘</div>
            <div>
              <strong>DevAgent</strong>
              <span>AI GitHub Developer</span>
            </div>
          </div>

          <div className="nav-label">WORKSPACE</div>

          <button
              type="button"
              className={activeTab === "ask" ? "nav-item active" : "nav-item"}
              onClick={() => changeTab("ask")}
          >
            <span>⌕</span> Repository Chat
          </button>

          <button
              type="button"
              className={activeTab === "review" ? "nav-item active" : "nav-item"}
              onClick={() => changeTab("review")}
          >
            <span>◈</span> AI Code Review
          </button>

          <div className="sidebar-bottom">
            <div className="status-dot" />
            <div>
              <strong>Developer workspace</strong>
              <span>Local development mode</span>
            </div>
          </div>
        </aside>

        <main className="main-content">
          <header className="topbar">
            <div className="breadcrumb">
              Workspace / <strong>Developer Agent</strong>
            </div>

            <div className="connection">
              <span /> API configured
            </div>
          </header>

          <section className="hero">
            <div className="eyebrow">AI-POWERED DEVELOPMENT</div>

            <h1>
              Your repository,
              <br />
              <span>understood by AI.</span>
            </h1>

            <p>
              Ask questions about your codebase, explore implementation details,
              and review saved source files with your AI developer assistant.
            </p>
          </section>

          <section className="workspace-card">
            <div className="card-heading">
              <div>
                <span className="section-kicker">ACTIVE WORKSPACE</span>
                <h2>Repository workspace</h2>
              </div>

              <span className="local-badge">● Local API</span>
            </div>

            <label htmlFor="repositoryId">Database repository ID</label>

            <input
                id="repositoryId"
                type="number"
                min="1"
                step="1"
                value={repositoryId}
                onChange={(event) => setRepositoryId(event.target.value)}
                required
            />

            <p className="hint">
              Enter the ID of a repository already saved in your backend
              database.
            </p>

            <div className="tabs">
              <button
                  type="button"
                  className={activeTab === "ask" ? "tab selected" : "tab"}
                  onClick={() => changeTab("ask")}
              >
                Ask your code
              </button>

              <button
                  type="button"
                  className={activeTab === "review" ? "tab selected" : "tab"}
                  onClick={() => changeTab("review")}
              >
                Review repository
              </button>
            </div>

            {activeTab === "ask" ? (
                <form onSubmit={submitQuestion}>
                  <label htmlFor="question">
                    What do you want to understand?
                  </label>

                  <textarea
                      id="question"
                      rows="4"
                      placeholder="How does authentication work in this repository?"
                      value={question}
                      onChange={(event) => setQuestion(event.target.value)}
                      required
                  />

                  <button
                      type="submit"
                      className="primary-button"
                      disabled={loading}
                  >
                    {loading ? "Analyzing repository..." : "Ask AI →"}
                  </button>
                </form>
            ) : (
                <div className="review-panel">
                  <h3>Static AI code review</h3>

                  <p>
                    Analyze saved source files for potential bugs, validation
                    issues, security concerns, and maintainability improvements.
                  </p>

                  <button
                      type="button"
                      className="primary-button"
                      onClick={runReview}
                      disabled={loading}
                  >
                    {loading ? "Reviewing source files..." : "Run code review →"}
                  </button>
                </div>
            )}
          </section>

          {loading && (
              <section className="result-card" aria-live="polite">
                <p>Processing your request. Please wait...</p>
              </section>
          )}

          {error && (
              <section className="result-card error-card" role="alert">
                <h3>Request failed</h3>
                <p>{error}</p>
                <p className="hint">
                  Check the backend, repository ID, request format, and CORS
                  configuration.
                </p>
              </section>
          )}

          {answer && (
              <section className="result-card">
                <span className="section-kicker">AI RESPONSE</span>
                <h2>Repository answer</h2>

                <div className="answer-text">
                  {answer.answer || "No answer was returned."}
                </div>

                {renderSources(answer.sources)}

                {answer.retrieved_chunks != null && (
                    <p className="hint">
                      Retrieved code chunks: {answer.retrieved_chunks}
                    </p>
                )}
              </section>
          )}

          {review && (
              <section className="result-card">
                <span className="section-kicker">REVIEW RESULTS</span>
                <h2>AI code review report</h2>

                <div className="answer-text">
                  {review.review_report || JSON.stringify(review, null, 2)}
                </div>

                <p className="hint">
                  AI-generated static analysis is advisory. Verify findings before
                  changing source code.
                </p>
              </section>
          )}

          <footer>
            DevAgent · AI-powered repository understanding and code review
          </footer>
        </main>
      </div>
  );
}
