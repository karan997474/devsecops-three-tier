# Local Jenkins toolbox

`Dockerfile` extends the official Jenkins LTS image with the tools used by this learning project: Git, Maven, Docker CLI, Trivy, and kubectl. It is for a local portfolio lab only. A production controller should remain minimal and delegate builds to isolated, least-privilege agents.

The Docker-in-Docker setup in the main implementation guide uses TLS to connect the Jenkins controller to a separate Docker daemon. Jenkins configuration remains in the existing `jenkins_home` Docker volume when the controller is rebuilt.

