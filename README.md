# AI GitHub Developer Agent

An AI-powered developer assistant that helps developers explore, understand, and review GitHub repository code using natural-language questions and locally hosted language models.

## Overview

The AI GitHub Developer Agent combines a React frontend, a Spring Boot backend, semantic code retrieval, and local AI inference to make repository analysis easier.

Developers can ask questions about indexed source code, inspect retrieved source references, and generate AI-assisted code review reports.

## Key Features

- **Repository Chat:** Ask natural-language questions about repository source code.
- **Semantic Code Search:** Retrieve relevant code chunks using vector similarity search.
- **Source References:** Display file paths and line ranges when available.
- **AI Code Review:** Generate concise reports containing potential issues, evidence, severity, and suggested fixes.
- **Local AI Inference:** Use Ollama to run language models locally.
- **Vector Storage:** Store and retrieve embeddings using PostgreSQL and pgvector.

## Architecture

```text
                  Developer
                      |
                      v
              React + Vite UI
                      |
                 REST APIs
                      |
                      v
             Spring Boot Backend
                /           \
               /             \
              v               v
      PostgreSQL + pgvector   Ollama
       Repository Data       Local LLM
       Code Chunks           Code Review
       Embeddings             Chat Answers
```

### Repository Chat Workflow

1. Repository source files are stored in the database and divided into chunks.
2. Code embeddings support semantic retrieval.
3. A developer submits a natural-language question.
4. The backend retrieves relevant code chunks.
5. The retrieved context is sent to the language model.
6. The frontend displays the answer and available source references.

### Code Review Workflow

1. The developer selects an indexed repository.
2. The backend gathers source files for review.
3. The source context is passed to the language model.
4. The model produces a concise review report.
5. The frontend displays the report for developer verification.

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | React, Vite, JavaScript, CSS |
| Backend | Java 21, Spring Boot, Spring AI |
| API | REST |
| AI runtime | Ollama |
| Chat model | Qwen 2.5 Coder 3B |
| Embedding model | Nomic Embed Text |
| Database | PostgreSQL |
| Vector search | pgvector |
| Build tool | Maven |

## Project Structure

```text
AI-GitHub-Developer-Agent/
├── Backend/
│   └── Backend/
│       ├── src/
│       ├── pom.xml
│       └── mvnw.cmd
├── Frontend/
│   ├── src/
│   │   ├── App.jsx
│   │   ├── App.css
│   │   └── main.jsx
│   ├── package.json
│   └── README.md
└── README.md
```

## Prerequisites

Install the following software:

- Java 21
- Node.js and npm
- PostgreSQL with pgvector support
- Ollama
- Git

Download the AI models:

```bash
ollama pull qwen2.5-coder:3b
ollama pull nomic-embed-text
```

Ensure Ollama is running and PostgreSQL is configured before starting the backend.

## Local Setup

### 1. Configure the database

Create and configure the PostgreSQL database required by the backend. Enable pgvector and set the database connection properties in the backend's local configuration.

Keep passwords, GitHub credentials, private keys, and JWT secrets out of source control.

### 2. Start the backend

Open a terminal in `Backend/Backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

The current local setup uses port `8081`.

### 3. Start the frontend

Open another terminal in `Frontend`:

```powershell
npm install
npm run dev
```

Open the URL printed by Vite. Depending on port availability, it may use `5173`, `5174`, or another port.

### 4. Verify the backend

With the backend running, open:

```text
http://localhost:8081/api/hello
```

The endpoint should return the configured backend health message.

## Main API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/hello` | Backend health check |
| GET | `/api/github/search-code` | Search indexed repository code |
| POST | `/api/github/ask-repository` | Ask questions about repository code |
| POST | `/api/code-review/repository` | Generate an AI code review |

Check the backend controller definitions for exact request parameters and response schemas.

## Responsible Use

AI-generated answers and review findings can be incorrect or incomplete. Verify findings against the actual source code before applying changes.

The application currently runs in a local development environment. Before public deployment, configure authentication and authorization, restrict CORS to trusted origins, secure credentials, and review all exposed endpoints.

## Known Limitations

- Review quality depends on the source context provided to the model.
- AI responses may contain false positives or miss defects.
- Response time depends on model loading, available hardware, and repository size.
- The database, backend, and Ollama runtime must be available for local operation.
- Production deployment and security hardening require additional work.

## Future Enhancements

- More reliable finding validation and source-level evidence.
- Improved source navigation and line references.
- Automated testing and performance monitoring.
- Repository indexing status and error handling.
- Secure deployment and production configuration.

## Project Status

The local development version supports repository question answering, semantic code retrieval, source references, and AI-generated code review reports.

---

Developed as an AI-powered developer productivity project.
