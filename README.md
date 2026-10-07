# ⚡ ExamNexus - Enterprise Online Examination Portal

A modern, high-performance, and secure **Online Examination Portal** built with **Java 21**, **Spring Boot 3**, **Spring Data JPA**, **H2 Persistent Database**, and a responsive **Vanilla CSS/JS** frontend.

---

## 🌟 Key Features

### 👨‍🎓 Candidate / Student Experience
- **Catalog of Examinations**: Browse tests by category (Java, Algorithms, Web Architecture, Database Systems), view duration, passing criteria, and negative marking penalties.
- **Proctored Examination Room**:
  - **Live Countdown Timer**: Synchronized with server-side duration, visual urgency cues (< 2 mins).
  - **Interactive Question Palette**: Visual grid showing states in real time:
    - 🟩 **Answered**
    - 🟨 **Marked for Review**
    - 🟦 **Answered & Marked for Review**
    - ⬜ **Unvisited / Unanswered**
  - **Real-time Draft Auto-Save**: Selected options and review statuses are immediately saved asynchronously so nothing is lost if refreshed.
  - **Pre-Submission Summary Modal**: Review answered vs unanswered questions before final submission.
  - **Instant Scorecard & Detailed Review**: Total marks, percentage, pass/fail badge, question-by-question breakdown with chosen answer vs correct key and detailed solution explanations.
  - **Attempt History**: Audit past assessments, scores, and timestamps.

### 🛡️ Anti-Cheat & Proctoring Engine
- **Tab-Switch & Window Blur Detection**: Listens to `visibilitychange` and `blur` events.
- **Audible Warning System**: Synthesizes real-time alert beeps using Web Audio API on violation.
- **Visual Strike Tracker**: Real-time counter (0 to 3 strikes).
- **Auto-Disqualification**: On reaching 3 strikes, the assessment is instantly locked, marked `DISQUALIFIED_CHEATING`, and submitted automatically.

### 👨‍🏫 Administrator & Instructor Console
- **Analytical KPI Dashboard**: Real-time cards for total exams, total questions in bank, registered students, candidate pass percentage.
- **Exam Management (CRUD)**: Create, edit, publish/unpublish, configure time limits, passing marks, negative marks, and question shuffling.
- **Question Bank Builder**: Add/edit/delete multiple-choice questions with 4 options, difficulty levels (Easy, Medium, Hard), marks weight, and solution notes.
- **Proctor Audit & Submissions Log**: Review every candidate submission with scores, percentages, completion status, and recorded cheating strikes.

---

## 🚀 Default Credentials

| Role | Username | Password | Notes |
|---|---|---|---|
| **Administrator** | `admin` | `admin123` | Full access to exam builder, question bank, and audit logs |
| **Student** | `student` | `student123` | Can take proctored tests, view scorecards, and history |

*(You can also register new student or admin accounts anytime via the Registration tab).*

---

## 🛠️ Technology Stack

- **Backend**: Java 21 LTS, Spring Boot 3.3.3
- **Database**: H2 Database Engine (with file-based persistence at `./data/examportal`)
- **Security**: Spring Security 6 with BCrypt password hashing
- **Frontend**: HTML5, Vanilla CSS Design System (Glassmorphic dark/light accents, Outfit & Plus Jakarta Sans typography), Vanilla JavaScript SPA
- **Build Tool**: Apache Maven / Maven Wrapper (`mvnw.cmd`)

---

## 🏃 Running the Application

### Option 1: 1-Click Batch Runner (Windows)
Double-click `run.bat` or execute in PowerShell/CMD:
```powershell
.\run.bat
```

### Option 2: Using Maven Wrapper
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\mvnw.cmd spring-boot:run
```

Once running, navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🧪 Testing & Verification

Run tests with Maven:
```powershell
.\mvnw.cmd test
```
