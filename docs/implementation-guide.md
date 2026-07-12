# Step-by-step implementation guide

This project is a practical implementation of both project descriptions in your question. It uses one application and one delivery flow, so you can explain every stage honestly in an interview.

## 1. Understand the architecture first

```text
Developer push to GitHub
          |
          v
Jenkins webhook -> Trivy filesystem scan -> Maven tests -> SonarQube gate
          |                                               |
          +--> Nexus (versioned JAR)                       |
          v                                               v
Docker build -> Trivy image scan -> Docker Hub/ECR -> Kubernetes on AWS EKS
                                                        |
                                                        v
                                       Nginx frontend -> Spring Boot API -> MongoDB Atlas
```

**Frontend:** static HTML/JavaScript served by Nginx. Nginx forwards `/api` calls to the backend service.

**Backend:** Java 17 Spring Boot REST API. It stores tasks in MongoDB and exposes actuator health endpoints for Kubernetes probes.

**Database:** MongoDB Atlas in cloud deployments. Local Docker Compose starts MongoDB only for development.

## 2. Run the project locally before configuring CI/CD

Install Docker Desktop, then run this command from the repository root. The backend targets Java 17 and Spring Boot 3.5, a currently supported Spring Boot release line.

```bash
docker compose up --build
```

Open `http://localhost:8081`. Add a task, refresh the browser, and confirm it remains. This proves the frontend, API, reverse proxy, and database connections work. Stop it with:

```bash
docker compose down
```

Run backend tests separately whenever you change Java code:

```bash
cd api
mvn clean verify
```

## 3. Create the services (one-time setup)

Create accounts or local instances for GitHub, Docker Hub (or Amazon ECR), SonarQube, Nexus Repository, MongoDB Atlas, Jenkins, and AWS. For a student project, SonarQube Community Edition and Nexus OSS can run as Docker containers on the Jenkins machine; MongoDB Atlas can use its free tier.

In MongoDB Atlas:

1. Create a database user with access only to this application's database.
2. Allow network access from the EKS workload egress address; use your own IP temporarily for testing.
3. Copy the application connection string. Treat it as a password.

In Nexus, create a **raw hosted** repository named `build-artifacts`. The Jenkinsfile uploads the tested JAR there under the Jenkins build number. A raw repository keeps this demo straightforward; a Maven hosted repository is a good later enhancement.

In Docker Hub, create two repositories: `three-tier-api` and `three-tier-web`. If you use ECR instead, update `REGISTRY` and replace the Docker login command with your organisation's ECR login flow.

For AWS, use an EKS cluster with a Kubernetes ingress controller already installed. The manifests use the `nginx` ingress class. If you use the AWS Load Balancer Controller instead, adapt the Ingress class and annotations; the application workloads remain the same.

## 4. Configure Jenkins correctly

Run Jenkins on a machine or agent that has all of these available:

- Java 17 and Maven
- Docker CLI with permission to reach a Docker daemon
- Trivy, `kubectl`, and `curl`
- A Jenkins agent label named `docker-maven-kubectl-trivy`

Install these Jenkins plugins: Pipeline, Git, GitHub, Credentials Binding, SonarQube Scanner, Pipeline Maven Integration, JUnit, Workspace Cleanup, and Kubernetes CLI (or provide `kubectl` directly on the agent).

Add credentials using these exact IDs because the `Jenkinsfile` refers to them:

| Jenkins credential ID | Type | Value |
| --- | --- | --- |
| `nexus-user-password` | Username with password | Nexus deployment account |
| `dockerhub-user-password` | Username with password | Docker Hub account/token |
| `mongodb-atlas-uri` | Secret text | Full Atlas URI |
| `eks-kubeconfig` | Secret file | Kubeconfig limited to the target cluster/namespace |

Use a separate, least-privilege Kubernetes identity for Jenkins. It should deploy only the desired namespaces, not administer the cluster.

In **Manage Jenkins → System**, add the SonarQube server with the name `sonarqube`. Configure the SonarQube webhook to call:

```text
https://YOUR_JENKINS_URL/sonarqube-webhook/
```

Without this webhook, `waitForQualityGate` cannot receive the result reliably.

## 5. Connect GitHub to Jenkins

1. Create a GitHub repository and push this source code.
2. In Jenkins, create a **Pipeline from SCM** job that reads `Jenkinsfile` from the main branch.
3. Add a GitHub webhook pointing to `https://YOUR_JENKINS_URL/github-webhook/` for push events.
4. For the first local run, leave `PUBLISH` and `DEPLOY` unchecked. Before enabling `PUBLISH`, change `REGISTRY` and `NEXUS_URL` at the top of `Jenkinsfile` to real values; do not put passwords there.
5. Push a small README change and confirm that the job begins automatically.

For a public portfolio project, use a Multibranch Pipeline. It demonstrates feature-branch checks and protects `main` with required Jenkins/Sonar status checks.

## 6. What every Jenkins stage does

| Stage | Why it is there | What proves success |
| --- | --- | --- |
| Checkout | Retrieves the exact Git revision | Jenkins shows commit SHA |
| Trivy filesystem scan | Finds secrets, vulnerable dependencies, and IaC issues before a build | No HIGH/CRITICAL blocking findings |
| Maven test | Compiles the API and runs unit tests | JUnit report is published |
| SonarQube + Quality Gate | Measures bugs, duplication, coverage, and code smells | Quality Gate is green |
| Nexus publication | Stores the tested JAR independently of Jenkins workspace | Versioned JAR appears in Nexus |
| Docker build/Trivy | Creates deployable images and blocks serious image CVEs | Image scan passes |
| Docker push | Makes immutable build-number images available to EKS | Two tags visible in registry |
| Kubernetes deployment | Injects secrets, applies manifests, then waits for rollout | Both rollouts report success |

The pipeline intentionally fails on a bad Quality Gate or HIGH/CRITICAL Trivy finding. Do not simply remove `--exit-code 1`; fix, update, or formally accept the risk with a documented exception.

## 7. First Kubernetes deployment

Before the first deployment, replace `tasks.example.com` in `k8s/workloads.yaml` with a real DNS name. Point that DNS record to your ingress controller's external address. Start a Jenkins build with:

- `DEPLOY = true`
- `TARGET_ENV = dev`

The pipeline creates `devsecops-dev`, writes the Atlas URI to the `app-secrets` Kubernetes Secret, substitutes immutable image tags into the manifest, and waits for the API and frontend deployments to become ready.

Verify it from a machine with Kubernetes access:

```bash
kubectl -n devsecops-dev get deploy,pods,svc,ingress
kubectl -n devsecops-dev rollout status deployment/backend
kubectl -n devsecops-dev get events --sort-by=.lastTimestamp
```

Test a rollback by deploying a deliberately bad image tag, observing the failed readiness check, then restoring the previously successful image with `kubectl rollout undo deployment/backend -n devsecops-dev`. Take a screenshot of both the failed release and successful rollback for your project evidence.

## 8. Security decisions you should be able to explain

- **Shift left:** Trivy scans source, secrets, dependencies, Dockerfiles, and Kubernetes YAML before an image is pushed.
- **Quality gate:** SonarQube prevents release after code-quality policy failures.
- **Artifact traceability:** Jenkins build number connects Git commit, JAR in Nexus, container tag, and Kubernetes deployment.
- **Secret handling:** Atlas URI stays in Jenkins credentials and a Kubernetes Secret. It is not in source code, image layers, console output, or a `.env` file committed to Git.
- **Container hardening:** API runs as user `10001`; workloads disallow privilege escalation, drop Linux capabilities, use seccomp, and use read-only filesystems with a temporary writable volume.
- **Reliable releases:** Readiness probes ensure Kubernetes only sends traffic to usable pods; rolling deployments keep an existing replica available during update.

## 9. A good development order (do this over 5–7 days)

1. **Day 1:** Run Compose locally, read the API and test one endpoint with `curl`.
2. **Day 2:** Push to GitHub and make Jenkins run the build/test stage from a webhook.
3. **Day 3:** Add SonarQube, create a deliberately poor-code change, and see the Quality Gate fail.
4. **Day 4:** Add Trivy, scan the filesystem and image, and save a report/screenshot.
5. **Day 5:** Configure Nexus and registry publishing; confirm each build produces unique artifacts/tags.
6. **Day 6:** Deploy to EKS, configure MongoDB Atlas network access, and test the public URL.
7. **Day 7:** Demonstrate a failed deployment, a rollback, and collect portfolio evidence.

## 10. Be accurate on your resume

Only claim tools you configured and can explain. Do not claim percentage improvements unless you measured a baseline. Use the template in `docs/resume-and-interview.md` after you have completed the evidence checklist.
