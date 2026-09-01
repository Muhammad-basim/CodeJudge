Product Requirements Document (PRD): CodeJudge — Final State
1. Project Overview
•	Product Name: CodeJudge
•	Positioning: CodeJudge is not "an AI code reviewer" — any AI chat already does that, and no prompt makes CodeJudge's raw review quality exceed the underlying model. CodeJudge's identity is a personal code-quality tracker that uses AI as its judge: persistent memory, a consistent review standard, and visibility into whether code quality is actually improving over time — none of which a stateless chat window can offer.
•	One-line pitch: "A desktop app that remembers every review it's given you, holds every submission to the same strict standard, and shows you whether your code is actually getting better."
•	Status: Feature-complete. Originally built as a console app to practice Java fundamentals (Collections, JDBC), then rebuilt as a full JavaFX desktop application once the underlying logic was proven solid.
2. Design Principles
1.	Memory a chat thread doesn't have — every review persists in MySQL, tied to a file/snippet identity and timestamp.
2.	A consistent standard, not a re-typed prompt — every review runs through "the CodeJudge standard": strict role, fixed structure, per-issue severity tags, calibrated 1–10 scoring.
3.	Built for how developers actually work — full files, pasted snippets, and re-reviews of the same file over time (diff-aware).
4.	Trends, not snapshots — a visual quality trend and a recurring-issues report, both only possible because history persists.
5.	A real desktop tool — JavaFX, not a web wrapper; a distinct visual identity (custom theme, splash screen), not default OS styling.
3. Final Feature Set
Core Analysis
•	File mode — pick a file via native FileChooser; contents load into the code input.
•	Snippet mode — paste code directly; user provides a label and type since there's no file extension to derive one from.
•	Focus mode — General / Security / Performance / Readability; adjusts prompt emphasis while keeping the same structured output shape.
•	Diff-aware re-review — if a prior review exists for the same label, the AI is told what was previously found and explicitly notes what improved, regressed, or is unchanged (rendered in a distinct [IMPROVED] color).
•	The CodeJudge Standard prompt — strict reviewer role, fixed categories (bugs/performance/readability), per-issue severity tags ([CRITICAL]/[WARNING]/[SUGGESTION]/[IMPROVED]), explicit scoring anchors (9–10 production-ready, 7–8 minor issues only, 5–6 real problems, 3–4 significant risk, 1–2 broken), "no filler" rule, concrete-consequence requirement.
•	Async execution via JavaFX Task — all AI calls and DB queries run off the UI thread; button disables during processing to prevent duplicate requests.
•	Explicit HTTP status handling (429 rate-limit, other non-200 codes) surfaced as readable on-screen messages, not silent console failures.
History & Reporting
•	Sortable history table (TableView) — file name, type, date, score; click any column to sort.
•	Feedback detail panel — selecting a row shows its full stored feedback.
•	Delete — remove a selected review from MySQL.
•	Export — save a selected review to a .md file via FileChooser save dialog.
•	Recurring Issues Report — keyword frequency across all history (HashMap-based counting).
•	Quality Trend Chart — LineChart of scores over time, oldest to newest.
•	Dashboard — total reviews, average score, most common recurring issue, most recently analyzed file; auto-populates on launch and via manual refresh.
Configuration & Identity
•	Settings tab — API key stored in ~/.codejudge/config.properties, not hardcoded in source; loaded fresh on each analysis so changes apply without restarting.
•	Splash screen — branded loading screen with animated progress bar and status messages, fading into the main window.
•	Full custom dark theme — palette (#0B0C10, #1F2833, #C5C6C7, #66FCF1, #45A29E) applied via a dedicated stylesheet; card-based panel layout; monospace font for code/feedback content; styled tabs, tables, inputs, charts, and progress indicators.
4. Technical Architecture
•	Language: Java (JDK 26), GUI: JavaFX 26
•	Networking: java.net.http.HttpClient; JSON: Gson
•	Database: MySQL via plain JDBC (no ORM)
•	Concurrency: JavaFX Task for all blocking calls (network, database), keeping the UI thread free
Package structure:
com.codejudge
├── Main.java              — JavaFX Application: splash, main window, all UI wiring
├── model/                 — Review, ReviewResult, GeminiRequest/Response + parts
├── service/               — FileService, AIService, AIProvider (interface)
├── data/                  — DatabaseConnection, ReviewRepository
└── analysis/              — ReviewHistory, IssueTracker, TrendReport, Report (interface)
Interfaces in real use:
•	Report — implemented by IssueTracker and TrendReport, sharing printReport(List<Review>).
•	AIProvider — implemented by AIService; calling code depends on the interface, not the concrete class, so the AI backend is swappable.
reviews table:
CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20),
    review_timestamp DATETIME NOT NULL,
    feedback TEXT,
    quality_score INT
);
5. Non-Functional Requirements
•	Responsiveness: UI never freezes during AI calls or DB queries (verified: window remains draggable/resizable mid-analysis).
•	Reliability: validated across all input paths — blank snippets, blank labels, empty file paths, no row selected for delete/export, invalid/missing API key, rate-limit (429) and other API errors — all resolve to a clear on-screen message, never a silent crash or console-only failure.
•	Persistence: confirmed to survive full application restarts.
6. Known Limitations (Honest, Documented)
•	AI review quality depends entirely on the underlying model; it can occasionally state something confidently incorrect, especially on code fragments lacking full context. CodeJudge does not claim infallible review — its value is consistency and memory, not perfect judgment.
•	Free-tier Gemini rate limits apply; heavy rapid use will surface a clear rate-limit message rather than fail silently.
•	Single AI provider (Gemini) currently wired in; AIProvider makes adding another provider possible without touching calling code, but no second provider is implemented.
7. Not Built (Explicitly Deferred)
•	Packaging/distribution (jpackage installer)
•	A second AIProvider implementation
•	Connection pooling / ORM
•	Syntax highlighting in the code input
•	Batch/folder analysis
•	The original console version (superseded entirely by the GUI; not preserved)
8. Build History (Condensed)
Console MVP (menu, file I/O, Collections practice) → MySQL persistence → snippet mode → structured/scored AI prompt → recurring-issues + trend reports (console) → error-handling pass → package restructure → Report/AIProvider interfaces → JavaFX setup → GUI port of Analyze (async, file/snippet toggle) → history table → trend chart → severity color-coding → focus mode → diff-aware re-review → delete/export → dashboard/settings tabs → full theming, splash screen, and final polish.

