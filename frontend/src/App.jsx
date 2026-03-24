import { useState } from "react";

const QUICK_CODES = [
  { label: "Debt", code: "49002010965", title: "Has debt" },
  { label: "S1", code: "49002010976", title: "Segment 1" },
  { label: "S2", code: "49002010987", title: "Segment 2" },
  { label: "S3", code: "49002010998", title: "Segment 3" }
];

const configuredApiBase =
  document.querySelector('meta[name="api-base-url"]')?.content?.trim() ||
  window.__API_BASE_URL__ ||
  "";

const API_URL = `${configuredApiBase.replace(/\/$/, "")}/api/decision`;

const formatCurrency = (value) => `€ ${Number(value).toLocaleString("en")}`;

export default function App() {
  const [personalCode, setPersonalCode] = useState("");
  const [loanAmount, setLoanAmount] = useState(4000);
  const [loanPeriod, setLoanPeriod] = useState(24);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const clearFeedback = () => {
    setResult(null);
    setError("");
  };

  const handleQuickCode = (code) => {
    setPersonalCode(code);
    clearFeedback();
  };

  const handleSubmit = async () => {
    clearFeedback();

    if (!personalCode.trim()) {
      setError("Please enter a personal code.");
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          personalCode: personalCode.trim(),
          loanAmount,
          loanPeriod
        })
      });

      const payload = await response.text();
      let data = {};
      if (payload) {
        try {
          data = JSON.parse(payload);
        } catch {
          data = {};
        }
      }

      if (!response.ok) {
        setError(data.message || `Request failed with status ${response.status}.`);
        return;
      }

      setResult(data);
    } catch {
      setError("Could not reach the backend. Make sure it is running on port 8080.");
    } finally {
      setLoading(false);
    }
  };

  const isPositive = result?.decision === "POSITIVE";

  return (
    <div className="page">
      <header className="header">
        <div className="logo">
          <span className="logo-bracket"></span>
          <em>Inbank</em>
          <span className="logo-bracket"></span>
        </div>
        <p className="header-sub">Loan Decision Engine</p>
      </header>

      <main className="main">
        <div className="card">
          <div className="card-label">Application</div>

          <div className="field">
            <label htmlFor="personalCode">Personal Code</label>
            <div className="input-row">
              <input
                id="personalCode"
                type="text"
                value={personalCode}
                placeholder="e.g. 49002010976"
                maxLength={20}
                autoComplete="off"
                onChange={(event) => setPersonalCode(event.target.value)}
              />

              <div className="quick-codes">
                <span className="quick-label">Quick fill:</span>
                {QUICK_CODES.map((item) => (
                  <button
                    key={item.code}
                    type="button"
                    className="chip"
                    title={item.title}
                    onClick={() => handleQuickCode(item.code)}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="field">
            <label htmlFor="loanAmount">
              Loan Amount <span className="field-value">{formatCurrency(loanAmount)}</span>
            </label>
            <input
              id="loanAmount"
              type="range"
              min="2000"
              max="10000"
              step="100"
              value={loanAmount}
              onChange={(event) => setLoanAmount(Number(event.target.value))}
            />
            <div className="range-labels">
              <span>€ 2,000</span>
              <span>€ 10,000</span>
            </div>
          </div>

          <div className="field">
            <label htmlFor="loanPeriod">
              Loan Period <span className="field-value">{loanPeriod} months</span>
            </label>
            <input
              id="loanPeriod"
              type="range"
              min="12"
              max="60"
              step="1"
              value={loanPeriod}
              onChange={(event) => setLoanPeriod(Number(event.target.value))}
            />
            <div className="range-labels">
              <span>12 months</span>
              <span>60 months</span>
            </div>
          </div>

          <button type="button" className="submit-btn" onClick={handleSubmit} disabled={loading}>
            <span className="btn-text" hidden={loading}>
              Calculate Decision
            </span>
            <span className="btn-spinner" hidden={!loading}>
              ···
            </span>
          </button>
        </div>

        {result ? (
          <div className="result-area">
            <div className="result-card">
              <div className={`result-badge ${isPositive ? "positive" : "negative"}`}>
                {isPositive ? "✓ Positive Decision" : "✗ Negative Decision"}
              </div>
              <div className="result-body">
                {isPositive ? (
                  <>
                    <div className="result-row">
                      <span className="result-label">Approved Amount</span>
                      <span className="result-val">{formatCurrency(result.approvedAmount)}</span>
                    </div>
                    <div className="result-row">
                      <span className="result-label">Approved Period</span>
                      <span className="result-val">{result.approvedPeriod} months</span>
                    </div>
                  </>
                ) : null}
                <p className="result-message">{result.message}</p>
              </div>
            </div>
          </div>
        ) : null}

        {error ? (
          <div className="error-area">
            <div className="error-card">
              <span className="error-icon">⚠</span>
              <p>{error}</p>
            </div>
          </div>
        ) : null}
      </main>

      <footer className="footer">
        <p>
          Decision engine · <code>POST /api/decision</code>
        </p>
      </footer>
    </div>
  );
}
