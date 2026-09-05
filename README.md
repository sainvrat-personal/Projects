# Projects

Personal portfolio of software projects, organized by category. Each category folder contains individual project subfolders (or modules) with their own README and setup instructions.

## Repository structure

```
Projects/
├── AI-Assignments/              # Course assignments and learning projects
│   ├── Ecommerce-Inventory-Mgmt-Service/
│   ├── Orders-And-Returns-Mgmt-Service/
│   ├── calorie-tracker/
│   └── realTime-foodDelivery-service/
├── Collage/                     # College-era projects
│   ├── desktop-app/
│   └── sellers.com/
├── Hackathons/                  # Hackathon submissions
│   ├── ai-agent-project/
│   └── Code-Sherlock/
├── Har-Ghar-Aaurved-Trust/      # Trust / non-profit website work
│   ├── backend/                 # Spring Boot API
│   └── frontend/trustsite/      # React + Vite site
└── README.md
```

## Project catalog

### AI-Assignments

| Project | Description | Stack |
|---------|-------------|-------|
| [Ecommerce-Inventory-Mgmt-Service](AI-Assignments/Ecommerce-Inventory-Mgmt-Service/) | RESTful inventory management for e-commerce (categories, products, SKUs) | Spring Boot, PostgreSQL, Redis |
| [Orders-And-Returns-Mgmt-Service](AI-Assignments/Orders-And-Returns-Mgmt-Service/) | Order lifecycle, returns, refunds, and audit trails | Spring Boot |
| [calorie-tracker](AI-Assignments/calorie-tracker/) | Calorie tracking application | Docker, backend + frontend |
| [realTime-foodDelivery-service](AI-Assignments/realTime-foodDelivery-service/) | SwiftEats — real-time food delivery platform (microservices) | Spring Boot, Redis, Kafka, RabbitMQ |

**Status:** All four projects are tracked in this repo as regular folders.

---

### Collage

| Project | Description | Stack |
|---------|-------------|-------|
| [desktop-app](Collage/desktop-app/) | Online quiz desktop application | Java (Eclipse) |
| [sellers.com](Collage/sellers.com/) | E-commerce product listing site | HTML, Bootstrap, JavaScript |

**Status:** Both projects are tracked in this repo as regular folders.

---

### Hackathons

| Project | Description | Stack |
|---------|-------------|-------|
| [ai-agent-project](Hackathons/ai-agent-project/) | Multi-agent AI customer support system | LangGraph, FAISS/Chroma RAG, Mem0, Langfuse, NeMo Guardrails |
| [Code-Sherlock](Hackathons/Code-Sherlock/) | RAG-powered incident analysis and escalation resolution | Python, RAG, embeddings |

**Status:** Present on disk; currently referenced as nested git repos (submodule/gitlink entries). Convert to regular tracked folders to match the AI-Assignments layout.

---

### Har-Ghar-Aaurved-Trust

Trust website for Har Ghar Aaurved, organized as a flat monorepo under the category folder (the former `Har-Ghar-Aaurved-master/` wrapper has been removed).

| Module | Path | Description | Stack |
|--------|------|-------------|-------|
| Backend | [backend/](Har-Ghar-Aaurved-Trust/backend/) | REST API for departments, users, and roles | Spring Boot, Maven |
| Frontend | [frontend/trustsite/](Har-Ghar-Aaurved-Trust/frontend/trustsite/) | Public trust site with auth and landing pages | React, TypeScript, Vite |

**Status:** Restructured and tracked in this repo. `backend/` and `frontend/` live directly under `Har-Ghar-Aaurved-Trust/` (staged rename from the previous nested layout).

---

## Summary

| Category | Projects / modules | Repo integration |
|----------|-------------------|------------------|
| AI-Assignments | 4 | Fully integrated |
| Collage | 2 | Fully integrated |
| Hackathons | 2 | Nested git repos (pending integration) |
| Har-Ghar-Aaurved-Trust | 1 (backend + frontend) | Fully integrated |

**Total:** 9 projects across 4 categories.

## Getting started

Each project has its own README with run instructions. Navigate to the project folder and follow the local README:

```bash
# Assignment project
cd AI-Assignments/realTime-foodDelivery-service

# Trust project backend
cd Har-Ghar-Aaurved-Trust/backend

# Trust project frontend
cd Har-Ghar-Aaurved-Trust/frontend/trustsite
```
