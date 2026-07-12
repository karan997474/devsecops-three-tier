# Your DevSecOps project: where to begin

I created a complete working reference project in this workspace. Start from the repository root and run:

```bash
docker compose up --build
```

Then open `http://localhost:8081`. This first proves that the three tiers work:

```text
Browser -> Nginx frontend -> Spring Boot API -> MongoDB
```

After that, follow this build sequence:

1. Push the project to GitHub.
2. Configure Jenkins to read the `Jenkinsfile` and trigger it with a GitHub webhook.
3. Add SonarQube and its webhook; a failed Quality Gate should stop the pipeline.
4. Add Nexus (`build-artifacts` raw hosted repository) and Docker Hub credentials to Jenkins.
5. Configure Trivy on the Jenkins agent. It scans source, secrets, IaC, and container images, blocking HIGH/CRITICAL findings.
6. Create MongoDB Atlas credentials, an EKS cluster, a Kubernetes ingress controller, and the four Jenkins credentials documented in `docs/implementation-guide.md`.
7. Run Jenkins with `DEPLOY=true` and `TARGET_ENV=dev`. It deploys images with the build number tag and waits for rollout validation.

For a resume, use only the tools you actually configured. A truthful concise entry is:

> Built a Jenkins-based DevSecOps pipeline for a three-tier Spring Boot application, integrating GitHub webhooks, Maven testing, SonarQube Quality Gates, Nexus artifact publishing, Docker/Trivy image scanning, and Kubernetes/EKS rolling deployments backed by MongoDB Atlas.

Keep evidence: a green Jenkins run, SonarQube Quality Gate, Trivy scan, Nexus artifact, Docker image tags, `kubectl rollout` output, deployed app screenshot, and one rollback demonstration. The workspace contains the detailed runbook, validation checklist, resume bullets, and interview script under `docs/`.

