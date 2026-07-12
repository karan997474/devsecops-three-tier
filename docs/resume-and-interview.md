# Resume bullets and interview explanation

## Resume entry (use after you build it)

**End-to-End DevSecOps CI/CD Pipeline for a Three-Tier Application**  
*GitHub, Jenkins, Java, Maven, SonarQube, Nexus Repository, Docker, Trivy, Kubernetes (EKS), MongoDB Atlas*

- Built a Jenkins pipeline triggered by GitHub webhooks to compile and test a Spring Boot API, enforce SonarQube Quality Gates, and publish versioned JAR artifacts to Nexus Repository.
- Implemented shift-left security gates with Trivy filesystem, secret, IaC, and container-image scans; configured the pipeline to block releases on HIGH/CRITICAL findings.
- Containerized Nginx frontend and Spring Boot backend services, published immutable build-number images, and deployed rolling releases to Kubernetes/EKS with readiness and liveness validation.
- Integrated MongoDB Atlas through credential-managed Kubernetes Secrets and applied least-privilege container settings, resource limits, NetworkPolicies, and rollout monitoring.

If you have real measurements, replace a bullet with a measured result such as “reduced manual deployment steps from 12 to 2” or “cut deployment time from 20 minutes to 6 minutes.” State the measurement period in an interview.

## 60-second explanation

“I built a small three-tier task application to focus on the delivery system. The frontend is served by Nginx, which proxies requests to a Spring Boot API; the API persists data in MongoDB Atlas. A GitHub push triggers Jenkins. Jenkins runs unit tests, sends the code to SonarQube and waits for the Quality Gate, then uses Trivy to scan the repository and container images. When all gates pass, it publishes the JAR to Nexus, pushes build-number-tagged Docker images, and deploys them to EKS. MongoDB credentials are injected at deploy time rather than stored in Git. Kubernetes uses rolling updates and readiness probes, so Jenkins verifies the rollout before reporting success.”

## Interview questions to practise

**Why Nexus and a container registry?** Nexus stores the tested Java artifact; the registry stores the runnable container image. They provide different parts of the supply chain and both are traceable to the Jenkins build.

**What happens when a vulnerability is found?** The relevant Trivy stage exits non-zero and stops the release. I identify whether an updated base image/dependency resolves it. If not, a real team would document a time-bound risk exception rather than silently bypassing the gate.

**Why readiness probes?** A process can be running but unable to serve requests. Kubernetes waits for readiness before routing traffic, so rolling deployments avoid sending traffic to an unready pod.

**How are credentials protected?** They are stored in Jenkins Credentials and injected only into the process that needs them. The Atlas URI becomes a Kubernetes Secret at deployment; it is never committed or baked into an image.

**How would you improve this for production?** Use GitOps (Argo CD), an image-signing/attestation policy, a secrets manager with External Secrets, SBOM generation, environment-specific promotion, alerting/metrics, and separate AWS IAM roles through workload identity.

