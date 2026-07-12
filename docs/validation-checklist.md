# Validation checklist

Use this checklist before calling the project complete. Save the requested evidence in a private portfolio folder; redact hostnames, account IDs, and secrets.

- [ ] `docker compose up --build` starts all three local tiers and a task survives a refresh.
- [ ] `mvn clean verify` passes and Jenkins shows the published JUnit report.
- [ ] A GitHub push starts Jenkins through the webhook.
- [ ] SonarQube shows the project and a passing Quality Gate.
- [ ] Intentionally introduce a SonarQube issue and confirm Quality Gate prevents the next stage.
- [ ] Trivy filesystem scan completes; resolve or document every HIGH/CRITICAL result.
- [ ] The versioned JAR is visible in Nexus `build-artifacts`.
- [ ] API and frontend images are visible in the registry with the Jenkins build number tag.
- [ ] Trivy image scans pass before the push/deploy stage.
- [ ] Kubernetes rollout reports success, and pods are `Running` and `Ready`.
- [ ] The public app can create and complete a task using MongoDB Atlas.
- [ ] A deliberately broken deployment is detected by readiness/rollout checks and is rolled back.
- [ ] You captured Jenkins, SonarQube, Trivy, Nexus, Kubernetes, and application screenshots.

