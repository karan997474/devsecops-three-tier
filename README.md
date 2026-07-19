# Secure Three-Tier CI/CD Demo

A portfolio-ready reference project for a three-tier task application:

```text
Browser -> Nginx frontend -> Spring Boot API -> MongoDB Atlas
                         |                   |
                         +--- Kubernetes / EKS

GitHub -> Jenkins -> tests -> SonarQube -> Nexus -> Docker/Trivy -> EKS
```

The repository is deliberately small so that the delivery system—not application complexity—is the focus. It supports local Docker Compose development and an EKS/Kubernetes deployment. See [docs/implementation-guide.md](docs/implementation-guide.md) for the exact build order, prerequisites, Jenkins configuration, and troubleshooting. For beginner-friendly project and interview notes, see [docs/project-notes-simple.md](docs/project-notes-simple.md).

## Quick start (local)

1. Install Docker Desktop and Git.
2. Clone the repository and run `docker compose up --build`.
3. Open `http://localhost:8081`, add a task, and confirm it persists after a browser refresh.
4. Run `docker compose down` when finished. Use `docker compose down -v` to remove local MongoDB data.

The compose stack uses local MongoDB. Production Kubernetes/EKS uses a MongoDB Atlas connection string supplied at deployment time; never commit that value.

## Local Kubernetes option

For a Docker Desktop learning cluster, apply [`k8s/local-mongodb.yaml`](k8s/local-mongodb.yaml) and set `app-secrets` to `mongodb://mongodb:27017/devsecops`. This creates a disposable, cluster-internal MongoDB instance so you can validate the Kubernetes workload without cloud-database credentials. It is deliberately separate from the Atlas/EKS deployment path in the Jenkinsfile.

## Repository map

| Path | Purpose |
| --- | --- |
| `frontend/` | Static browser UI and Nginx reverse proxy |
| `api/` | Java 17 / Spring Boot / MongoDB REST API |
| `k8s/` | Hardened Kubernetes manifests for EKS or any cluster |
| `jenkins/` | Local Jenkins toolbox image for Maven, Docker, Trivy, and kubectl |
| `Jenkinsfile` | Build, quality, security, artifact, image, and deployment pipeline |
| `docs/` | Setup guide, validation evidence, resume and interview notes |

## Safety defaults

- Secrets are injected from Jenkins credentials or Kubernetes Secrets, never stored in Git.
- Trivy blocks the pipeline on HIGH or CRITICAL findings (tune the policy for your organisation).
- SonarQube Quality Gate blocks delivery when code-quality rules fail.
- Kubernetes workloads use non-root users, read-only filesystems where compatible, probes, and resource limits.

## Evidence to save for your portfolio

Save a successful Jenkins stage view, SonarQube Quality Gate, Trivy report, Nexus artifact version, `kubectl rollout status` output, and a screenshot of the deployed application. The checklist is in [docs/validation-checklist.md](docs/validation-checklist.md).
