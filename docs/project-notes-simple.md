# Simple Notes: Secure Three-Tier DevSecOps Project

These are beginner-friendly notes for explaining this project in an interview.

## 1. Project in one sentence

I built a small task-management application with a frontend, a Java backend, and a MongoDB database. I then created a DevSecOps pipeline that tests the code, checks code quality and security, builds Docker images, and validates the application on local Kubernetes.

## 2. Why I built this project

The application itself is deliberately small. The important learning is the delivery process:

```text
Write code -> save code in GitHub -> Jenkins checks it -> Docker packages it -> Kubernetes runs it
```

In a real company, developers should not manually copy files to a server. A pipeline makes the release repeatable, traceable, and safer.

## 3. What is a three-tier application?

The application is split into three separate layers.

| Layer | Technology used | Simple job |
| --- | --- | --- |
| Frontend | HTML, JavaScript, Nginx | Shows the website and accepts user input |
| Backend | Java, Spring Boot, Maven | Receives API requests and applies application logic |
| Database | MongoDB | Saves tasks permanently |

### Example: adding a task

Suppose the user types `Practice interview` and clicks **Add**.

1. The frontend sends a request to `/api/tasks`.
2. Nginx forwards that API request to the Spring Boot backend.
3. The backend validates the request.
4. The backend saves the task in MongoDB.
5. MongoDB returns the saved task.
6. The backend returns JSON to the frontend.
7. The frontend displays the new task.

Example request:

```json
POST /api/tasks
{
  "title": "Practice interview"
}
```

Example saved data:

```json
{
  "id": "generated-id",
  "title": "Practice interview",
  "completed": false
}
```

The backend also supports these operations:

| API | Meaning |
| --- | --- |
| `GET /api/tasks` | List all tasks |
| `POST /api/tasks` | Create a task |
| `PATCH /api/tasks/{id}/complete` | Mark a task as complete |

## 4. Repository structure

| Folder or file | Why it exists |
| --- | --- |
| `frontend/` | Browser UI and Nginx reverse-proxy configuration |
| `api/` | Spring Boot REST API, MongoDB code, and unit tests |
| `compose.yaml` | Runs frontend, backend, and MongoDB locally together |
| `Jenkinsfile` | Defines the automated CI/CD stages |
| `jenkins/` | Custom Jenkins image with Maven, Docker CLI, Trivy, and kubectl |
| `k8s/` | Kubernetes deployments, services, ingress, network policies, and local MongoDB |
| `docs/` | Setup instructions, validation checklist, interview notes, and these notes |

## 5. Git and GitHub

Git records every change to the code. GitHub stores that Git history online.

Simple flow:

```text
Change code -> git add -> git commit -> git push -> GitHub
```

Why this matters:

- Every Jenkins build can be connected to a specific Git commit.
- Team members can review changes.
- If a new change breaks the application, an earlier version is available.
- Secrets should never be committed to Git.

Example interview answer:

> I used GitHub as the source-code repository. Each pipeline build checks out a specific Git revision, which gives traceability between code, tests, images, and deployment.

## 6. Docker: packaging the application

Docker puts an application and the runtime it needs into a portable package called an **image**.

This project creates two images:

```text
Frontend image: Nginx + HTML + JavaScript
Backend image: Java runtime + Spring Boot JAR
```

### Why Docker is useful

Without Docker, one machine may have a different Java version, missing libraries, or different settings. With Docker, the same image can run on a laptop, a Jenkins worker, or Kubernetes.

### Backend Dockerfile

The backend uses a multi-stage build.

1. A Maven image compiles the Java code into a JAR.
2. A smaller Java runtime image runs only the JAR.

This is better than running Maven in the final image because the final image contains fewer unnecessary tools.

The backend container uses a non-root user, which reduces the impact if the container is compromised.

### Frontend Dockerfile

The frontend uses an unprivileged Nginx image. Nginx serves static website files and forwards `/api/` requests to the internal backend service.

## 7. Docker Compose: running locally

Docker Compose starts the three local services together:

```text
Browser -> frontend -> backend -> mongo
```

The local database address is:

```text
mongodb://mongo:27017/devsecops
```

The Compose file also contains health checks.

- MongoDB must answer a database ping.
- The backend must answer its Spring Boot health endpoint.
- The frontend waits until the backend is healthy.

This prevents the frontend from trying to use a backend that is still starting.

## 8. Jenkins: the automatic checker

Jenkins reads the `Jenkinsfile` and performs the same checks every time. It is like an automatic reviewer, tester, and release assistant.

The pipeline has these stages:

```text
1. Checkout
2. Secret and IaC scan
3. Build and unit test
4. SonarQube analysis
5. SonarQube Quality Gate
6. Optional Nexus artifact publishing
7. Build and scan Docker images
8. Optional Kubernetes deployment
```

### 8.1 Checkout

Jenkins downloads the source code from GitHub.

```groovy
checkout scm
```

`scm` means source-code management. It allows Jenkins to build the exact Git version selected for that build.

### 8.2 Secret and infrastructure scan

Trivy scans the repository before the build continues.

```text
Trivy secret scan: finds accidentally committed passwords, tokens, and keys
Trivy config scan: finds unsafe Docker and Kubernetes configuration
```

The pipeline is configured to fail on HIGH or CRITICAL findings. This is important because the process stops rather than ignoring an important security problem.

Example:

```text
Bad: a database password is committed to Git
Result: Trivy can fail the pipeline
Correct action: remove the secret, rotate it, and store it in a credential manager
```

### 8.3 Maven build and unit tests

The backend runs:

```bash
mvn -B clean verify
```

This command compiles the Java application, runs its unit tests, and produces the JAR file.

Jenkins publishes the JUnit test results so a failed test is visible in the Jenkins build page.

Example:

```text
Developer breaks TaskService logic
-> unit test fails
-> Maven returns an error
-> Jenkins stops
-> bad code does not reach an image or deployment
```

### 8.4 SonarQube analysis and Quality Gate

SonarQube checks source-code quality. It can identify bugs, duplicated code, maintainability problems, and code-level security concerns.

Jenkins sends the backend code to SonarQube, then waits for the Quality Gate.

```text
Quality Gate passes -> pipeline may continue
Quality Gate fails -> Jenkins stops the pipeline
```

The Quality Gate is important because a scan is not useful if the pipeline ignores its result.

### 8.5 Building and scanning Docker images

After the quality checks pass, Jenkins builds frontend and backend Docker images.

Image names include the Jenkins build number, for example:

```text
devsecops/three-tier-api:14
devsecops/three-tier-web:14
```

The build number creates traceability. It is better than relying only on the `latest` tag because it identifies the exact pipeline build that produced an image.

Trivy then scans the images for HIGH or CRITICAL vulnerabilities.

Example:

```text
Java code is correct
but the Java base image has a critical vulnerability
-> Trivy image scan fails
-> release stops until the image or dependency is updated
```

## 9. SonarQube and Trivy: why both are needed

These tools check different risks.

| Tool | Main purpose |
| --- | --- |
| SonarQube | Code quality, maintainability, bugs, and code-level security rules |
| Trivy | Secrets, vulnerable dependencies/images, and infrastructure misconfiguration |

Simple explanation:

```text
SonarQube asks: “Is the source code written well?”
Trivy asks: “Does the project contain known security risks?”
```

## 10. Kubernetes: running the containers safely

Docker builds the application package. Kubernetes manages and runs that package.

This project uses the namespace:

```text
devsecops-dev
```

A namespace separates this application from other applications in the same Kubernetes cluster.

### Main Kubernetes resources

| Resource | Purpose |
| --- | --- |
| Deployment | Keeps the required number of application pods running |
| Pod | A running instance of a container |
| Service | Gives pods a stable internal network name |
| Ingress | Routes browser traffic to the frontend service |
| Secret | Stores sensitive configuration outside Git |
| NetworkPolicy | Restricts which pods can talk to each other |

### Replicas and rolling update

The backend is configured with two replicas. Kubernetes tries to keep two backend pods available.

The rolling-update settings are:

```text
maxUnavailable: 0
maxSurge: 1
```

Simple meaning:

- Do not make an existing backend pod unavailable before the new one is ready.
- Kubernetes may create one extra new pod during an update.
- This helps reduce downtime.

### Readiness and liveness probes

Kubernetes checks the backend health endpoints.

```text
Readiness probe: “Can this pod receive user traffic now?”
Liveness probe: “Is this pod still alive, or should Kubernetes restart it?”
```

A process can be running but still be unable to serve requests. Readiness probes prevent traffic from being sent too early.

## 11. Kubernetes security controls used

This project applies several layers of protection.

| Control | Simple meaning |
| --- | --- |
| `runAsNonRoot` | Containers do not run as the root Linux user |
| Drop all capabilities | Removes unnecessary privileged Linux actions |
| `allowPrivilegeEscalation: false` | Stops a process from gaining extra permissions |
| Read-only filesystem | Stops arbitrary writes inside the container filesystem |
| `emptyDir` at `/tmp` | Gives the app a small temporary writable location when required |
| `seccompProfile: RuntimeDefault` | Limits risky Linux system calls |
| Resource requests/limits | Prevents one pod using unlimited CPU or memory |
| `automountServiceAccountToken: false` | Avoids adding a Kubernetes API token when the app does not need it |
| NetworkPolicies | Allows only required pod-to-pod traffic |

### NetworkPolicy example

The project begins with a default-deny ingress policy.

Then it adds only the required paths:

```text
Frontend -> Backend on port 8080
Backend -> MongoDB on port 27017
```

This follows the least-privilege principle: allow only what is required.

## 12. MongoDB and secrets

For local Kubernetes learning, the project uses a local MongoDB pod with this internal address:

```text
mongodb://mongodb:27017/devsecops
```

This local database is disposable. It uses temporary `emptyDir` storage, so it is not intended for production data.

The production design supports MongoDB Atlas. In that design, the Atlas URI is provided from Jenkins Credentials and becomes a Kubernetes Secret at deployment time.

Correct security approach:

```text
Secret value -> Jenkins Credentials -> Kubernetes Secret -> backend environment variable
```

Incorrect security approach:

```text
Secret value -> GitHub repository or Dockerfile
```

## 13. Important local Docker/Jenkins troubleshooting lesson

Jenkins runs in a Docker container, but Jenkins still needs Docker access to build images.

The Docker CLI initially tried to reach a non-existent remote Docker host at `docker:2376`. It also needed permission to use Docker Desktop's Unix socket.

The solution was:

1. Mount the Docker socket into the Jenkins container.
2. Add the socket group permission to the Jenkins container.
3. Clear old remote-Docker environment settings.
4. Set `DOCKER_HOST` to the local Unix socket.

The pipeline now uses this idea:

```bash
unset DOCKER_CONTEXT DOCKER_TLS_VERIFY DOCKER_CERT_PATH
export DOCKER_HOST=unix:///var/run/docker.sock
```

Simple interview answer:

> Jenkins was containerized, so I had to give it controlled access to Docker Desktop through the Unix socket. I fixed stale remote-Docker settings and socket permissions, then verified that Jenkins could build and scan images.

## 14. What was successfully validated

The successful Jenkins validation build completed:

- Trivy secret scan
- Trivy Kubernetes/IaC configuration scan
- Maven build and unit tests
- SonarQube analysis and passing Quality Gate
- Docker frontend and backend image builds
- Trivy image scans

The application was also validated on local Docker Desktop Kubernetes with local MongoDB.

## 15. What is designed but not yet claimed as completed

The `Jenkinsfile` contains optional stages for Nexus publishing, container-registry push, MongoDB Atlas credentials, and Kubernetes/EKS deployment.

Do not say these are complete until you configure and run them successfully:

- Publishing JAR artifacts to Nexus
- Pushing images to Docker Hub or another registry
- Deploying to AWS EKS
- Using a production MongoDB Atlas connection

This is not a weakness. It is honest engineering: the pipeline design is present, while the local completed validation uses Docker Desktop Kubernetes and local MongoDB.

## 16. Interview explanation to practise

Use this answer in simple language:

> I built a three-tier task application. The frontend is JavaScript served by Nginx, the backend is a Java Spring Boot REST API, and MongoDB stores the tasks. I used Docker Compose to test all three layers locally. For DevSecOps, Jenkins checks out the GitHub code, runs Maven unit tests, sends the code to SonarQube, waits for the Quality Gate, scans the project and Docker images with Trivy, and then builds versioned Docker images. I validated the application in local Kubernetes with health probes, non-root containers, read-only filesystems, Secrets, resource limits, and network policies. The Jenkins validation build passed the configured quality and security checks.

## 17. Common interview questions

### Why use both SonarQube and Trivy?

> SonarQube checks source-code quality and maintainability. Trivy checks for secrets, vulnerable container images, and unsafe infrastructure configuration. They solve different problems.

### What happens when Trivy finds a critical vulnerability?

> The Trivy command returns a failure exit code, Jenkins stops the pipeline, and the image is not allowed to continue. I would update the vulnerable dependency or base image and run the pipeline again.

### Why use Docker?

> Docker packages the app with its required runtime, so it runs consistently on my laptop, Jenkins, and Kubernetes.

### Why use Kubernetes readiness probes?

> A container can be running but still not be ready to serve users. Kubernetes waits for readiness before sending traffic to that pod.

### Why do image tags use the Jenkins build number?

> A build-number tag helps trace an image back to a specific Jenkins build and Git source version. It is safer for tracking and rollback than only using `latest`.

### How are database credentials protected?

> They are not committed to Git. The production design injects them through Jenkins Credentials and Kubernetes Secrets.

### What would you improve for production?

> I would add a managed secrets solution, image signing, SBOM generation, GitOps deployment with Argo CD, monitoring and alerts, environment promotion rules, and workload identity for cloud access.

## 18. Key words to remember

| Word | Simple meaning |
| --- | --- |
| CI | Automatically build and test code after a change |
| CD | Automatically prepare or perform release/deployment steps |
| DevSecOps | Add security checks throughout development and delivery |
| Pipeline | Ordered automated steps in Jenkins |
| Docker image | Portable package containing an app and its runtime |
| Container | Running instance of a Docker image |
| Kubernetes pod | Smallest deployable unit; normally contains one app container here |
| Quality Gate | Pass/fail rule from SonarQube |
| Vulnerability scan | Search for known security risks |
| Secret | Sensitive value such as a password or database URI |
| Rolling update | Replace old application pods gradually instead of all at once |

## 19. Final memory sentence

```text
I write the code, GitHub saves it, Jenkins checks it, Docker packages it, and Kubernetes runs it safely.
```
