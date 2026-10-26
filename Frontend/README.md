# AI GitHub Developer Agent

An AI-powered developer assistant that helps developers understand GitHub repository code through natural-language questions and AI-generated code reviews.

## Features

- **Repository Chat:** Ask questions about source code and receive contextual answers.
- **Semantic Code Search:** Retrieve relevant code chunks using vector similarity search.
- **Source References:** Display retrieved source file paths and line ranges when available.
- **AI Code Review:** Generate concise, evidence-based code review reports.
- **Local AI Inference:** Use Ollama to run a local language model.
- **Persistent Storage:** Store repository data and code chunks in PostgreSQL with pgvector.

## Technology Stack

### Frontend
- React
- Vite
- JavaScript
- CSS

### Backend
- Java 21
- Spring Boot
- Spring AI
- Spring Security
- REST APIs
- Maven

### AI and Database
- Ollama
- Qwen 2.5 Coder 3B
- PostgreSQL
- pgvector
- Nomic Embed Text

## Architecture

1. Repository source files are stored and divided into chunks.
2. Embeddings represent code chunks for semantic retrieval.
3. PostgreSQL with pgvector stores and searches those embeddings.
4. Repository Chat retrieves relevant chunks and sends their context to the language model.
5. AI Code Review analyzes selected repository source files and generates a review report.
6. The React frontend displays responses and source references.

## Prerequisites

Install the following before running the application:

- Node.js and npm
- Java 21
- PostgreSQL with pgvector enabled
- Ollama
- Git

Pull the models used by the application:

```bash
ollama pull qwen2.5-coder:3b
ollama pull nomic-embed-text
```

Ensure Ollama is running locally.

## Run the Application

### 1. Start the backend

Open a terminal in the backend Maven project directory:

```powershell
cd Backend/Backend
.\mvnw.cmd spring-boot:run
```

The backend is configured to run on port `8081` in the current local development setup.

### 2. Start the frontend

Open another terminal in the frontend directory:

```powershell
cd Frontend
npm install
npm run dev
```

Open the local URL printed by Vite in the terminal. The port may vary depending on availability.

### 3. Configure the database

Configure PostgreSQL connection settings and required application properties before starting the backend. Ensure the database and pgvector extension are initialized according to the backend configuration.

**Security:** Do not commit database passwords, GitHub private keys, access tokens, or JWT secrets to Git.

## Using the Application

### Repository Chat
1. Open the application.
2. Select an indexed repository.
3. Ask a question about its code.
4. Review the AI answer and retrieved source references.

### AI Code Review
1. Open the AI Code Review section.
2. Select a repository.
3. Run the review.
4. Inspect the reported severity, evidence, problem description, and suggested fixes.

AI-generated findings are advisory and should be verified before changing source code.

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/github/ask-repository` | Ask a question about a repository |
| GET | `/api/github/search-code` | Search indexed code chunks |
| POST | `/api/code-review/repository` | Generate a repository code review |
| GET | `/api/hello` | Check backend availability |

Request formats and parameters should be checked against the current backend implementation.

## Limitations

- AI-generated answers and code reviews can contain mistakes.
- Results depend on the source files and code chunks available to retrieval.
- Local AI response time depends on hardware, model loading, and repository size.
- The application requires its configured backend, database, and Ollama models to be available.
- Production deployment requires appropriate authentication, authorization, secret management, and CORS configuration.

## Future Improvements

- More accurate code-review findings with validation against source code.
- Better source navigation and line-level references.
- Repository indexing progress and error reporting.
- Automated tests for backend APIs and frontend workflows.
- Secure deployment and improved observability.

## Project Status

The project currently supports repository question answering, semantic code retrieval, source references, and AI-generated code reviews in the local development environment.

---

Built as an AI-powered developer assistant project.
