# Inbank Loan Decision Engine

A Spring Boot REST API and a small vanilla JavaScript frontend for evaluating loan applications against a mocked credit registry.

## Scope

The assignment supports the four requested hardcoded scenarios:

| Personal code | Scenario | Credit modifier |
| --- | --- | --- |
| `49002010965` | Has debt | N/A |
| `49002010976` | Segment 1 | `100` |
| `49002010987` | Segment 2 | `300` |
| `49002010998` | Segment 3 | `1000` |

## Features

- Single REST endpoint: `POST /api/decision`
- Hardcoded credit registry for the required scenarios
- Negative decisions for debt and impossible applications
- Automatic loan-period extension when the selected period cannot produce a valid amount
- Frontend form with quick-fill buttons for the four provided personal codes
- Input validation for personal code, amount, and period constraints
- Service tests for decision logic and MVC tests for endpoint behavior

## Project Structure

```text
inbank-decision-engine/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/inbank/decision/
│       │   ├── controller/
│       │   ├── exception/
│       │   ├── model/
│       │   ├── registry/
│       │   └── service/
│       └── test/java/com/inbank/decision/
│           ├── controller/
│           └── service/
└── frontend/
    ├── index.html
    ├── app.js
    └── style.css
```

## API

### Request

```http
POST /api/decision
Content-Type: application/json
```

```json
{
  "personalCode": "49002010976",
  "loanAmount": 4000,
  "loanPeriod": 24
}
```

### Positive response

```json
{
  "decision": "POSITIVE",
  "approvedAmount": 2400,
  "approvedPeriod": 24,
  "message": "Loan approved for 2400 € over 24 months."
}
```

### Negative response

Debt is treated as a normal supported decision scenario, not as an HTTP error:

```json
{
  "decision": "NEGATIVE",
  "approvedAmount": null,
  "approvedPeriod": null,
  "message": "Applicant has active debt. No loan can be approved."
}
```

### Validation and lookup errors

- Unknown personal code: `404 Not Found`
- Invalid request body (including malformed personal code): `400 Bad Request`

## Decision Logic

The scoring formula from the task is:

```text
credit score = (credit modifier / loan amount) * loan period
```

Rearranged, the largest approvable amount for a given period is:

```text
max approvable amount = credit modifier * loan period
```

The service applies the following flow:

1. Look up the applicant in the mocked registry.
2. If the applicant has debt, return a negative decision immediately.
3. Evaluate the requested input with the assignment formula (`score >= 1` means the requested amount is approvable).
4. Calculate the largest approvable amount for the selected period.
5. If that amount is below the minimum allowed output (`2000`), extend the period month by month until a valid amount is found or until `60` months is reached.
6. Cap any positive result at `10000`.
7. If no valid amount exists in the allowed period range, return a negative decision.

### Why the requested amount is not used directly

The requested amount is part of the input, but the engine is designed to return the best offer it can approve for the selected scenario. That means:

- If the applicant requests `4000` and the engine can approve `7200`, the response returns `7200`.
- If the applicant requests `4000` but only `2400` is possible for that period, the response returns `2400`.
- If no valid amount exists for the chosen period, the engine searches for the first longer period that yields a valid offer.

## Thought Process and Design Choices

- I kept the mocked registry isolated in its own class so the hardcoded assignment data can later be replaced with a real external integration.
- I treated debt as a normal negative decision because the task lists it as one of the supported business scenarios, not as an exceptional transport-level failure.
- I returned both `approvedAmount` and `approvedPeriod` so the frontend can explain whether the engine stayed on the requested period or had to extend it.
- I kept the frontend intentionally small and framework-free because the assignment only requires a working client, not a large frontend architecture.
- I replaced Mockito-based tests with a simple stub registry. That keeps the tests deterministic and avoids JDK-specific agent attachment issues on newer Java versions.

## One Thing I Would Improve About the Assignment

I would make the phrase "maximum sum" more explicit with concrete examples.

Right now, the wording can be read in two different ways:

- Return the maximum amount for the selected period, and only search a longer period if the selected one cannot produce any valid offer.
- Return the global maximum amount across all allowed periods, even when the selected period already produces a valid offer.

Both interpretations are defensible. I would improve the assignment by adding two or three example input/output cases that make the intended interpretation unambiguous.

## Getting Started

### Prerequisites

- Java 17 or newer
- Maven 3.9 or newer

The Maven build enforces Java 17+.

### Run the backend

```bash
cd backend
mvn clean spring-boot:run
```

The API runs at `http://localhost:8080`.

### Run the tests

```bash
cd backend
mvn test
```

### Run the frontend

Serve the frontend from the `frontend` directory:

```bash
cd frontend
python -m http.server 8000
```

Then open `http://localhost:8000`.

The frontend reads API base URL from:

- `<meta name="api-base-url" ...>` in `index.html`
- `window.__API_BASE_URL__` as an optional runtime override

## Testing

The test suite covers:

- Debt scenario returns a negative decision
- Unknown personal code handling
- Segment 1, 2, and 3 approval behavior
- Period extension when the initial period is too short
- Amount cap at `10000`
- Negative decisions when no valid amount exists
- Output range validation
- Controller status and payload checks for `200`, `400`, and `404`
