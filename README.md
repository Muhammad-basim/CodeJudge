# CodeJudge

**A desktop app that remembers every review it's given you, holds every submission to the same strict standard, and shows you whether your code is actually getting better.**

CodeJudge isn't "an AI code reviewer" — pasting code into any AI chat already does that. CodeJudge's value is what a stateless chat window can never give you: **persistent memory, a consistent review standard, and visibility into whether you're actually improving over time.**

---

## Features

- **Analyze code** — from a local file or a pasted snippet
- **Focus mode** — bias the review toward Security, Performance, Readability, or General
- **Diff-aware re-review** — re-analyzing a previously reviewed file/snippet gets feedback that explicitly compares against the prior review (what improved, regressed, or stayed the same)
- **The CodeJudge Standard** — every review follows a strict, fixed format: severity-tagged issues (`CRITICAL` / `WARNING` / `SUGGESTION`), concrete consequences instead of vague advice, and a calibrated 1–10 quality score
- **Persisted history** — every review is saved to MySQL and survives restarts
- **Sortable history table** with full feedback detail, delete, and export to `.md`
- **Recurring Issues Report** — which problems keep showing up across your history
- **Quality Trend Chart** — a visual line chart of your scores over time
- **Dashboard** — total reviews, average score, most common issue, most recent file, at a glance
- **Settings** — your Gemini API key is stored locally, not hardcoded in source

## Tech Stack

- **Java 26**, **JavaFX 26** (GUI)
- **MySQL** via plain JDBC (no ORM)
- **Gson** for JSON (requests to and responses from the AI)
- **Google Gemini API** (`gemini-3.6-flash`) for code analysis
- No build tool (Maven/Gradle) — dependencies are managed as local `.jar` files in `lib/`

## Project Structure

```
com.codejudge
├── Main.java              — JavaFX app: splash screen, main window, all UI wiring
├── model/                 — Review, ReviewResult, Gemini request/response shapes
├── service/                — FileService, AIService, AIProvider (interface)
├── data/                   — DatabaseConnection, ReviewRepository
└── analysis/               — ReviewHistory, IssueTracker, TrendReport, Report (interface)
```

## Setup

### 1. Prerequisites
- JDK 26 (or adjust the JavaFX SDK version to match whatever JDK you use)
- MySQL Server running locally (phpMyAdmin or any MySQL client)
- A [Google Gemini API key](https://aistudio.google.com/app/apikey) (free tier is sufficient)

### 2. Database
Run in your MySQL client:
```sql
CREATE DATABASE codejudge;
USE codejudge;

CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20),
    review_timestamp DATETIME NOT NULL,
    feedback TEXT,
    quality_score INT
);
```
Update the connection details in `data/DatabaseConnection.java` if your MySQL username/password differ from the defaults.

### 3. Dependencies
Download and add these as project libraries (see `/lib` for what's expected):
- [Gson](https://search.maven.org/artifact/com.google.code.gson/gson)
- [MySQL Connector/J](https://search.maven.org/artifact/com.mysql/mysql-connector-j)
- [JavaFX SDK](https://gluonhq.com/products/javafx/) (version matching your JDK)

### 4. Run Configuration
CodeJudge requires JavaFX-specific VM options to run. Set these on your run configuration:
```
--module-path "<path-to-javafx-sdk>/lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics
```

### 5. API Key
Run the app once, open the **Settings** tab, paste your Gemini API key, and click **Save API Key**. It's stored at `~/.codejudge/config.properties` — outside the project folder, never committed to source control.

## Known Limitations

- AI review quality depends on the underlying model — it can occasionally state something confidently that isn't quite right, especially on isolated code fragments without full context. CodeJudge's value is consistency and memory, not infallible judgment.
- Gemini's free tier has rate limits; heavy rapid use will surface a clear on-screen message rather than fail silently.
- Only one AI provider (Gemini) is currently wired in. The `AIProvider` interface makes adding another provider possible without touching the rest of the app, but no second provider is implemented.

## Future Ideas

- Packaging as a standalone installer (`jpackage`)
- A second `AIProvider` implementation (e.g. Claude, OpenAI)
- Syntax highlighting in the code input
- Batch/folder analysis
