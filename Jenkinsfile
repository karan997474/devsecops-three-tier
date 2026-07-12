pipeline {
  agent { label 'docker-maven-kubectl-trivy' }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    booleanParam(name: 'PUBLISH', defaultValue: false, description: 'Publish artifacts to Nexus and images to a registry')
    booleanParam(name: 'DEPLOY', defaultValue: false, description: 'Deploy verified images to Kubernetes')
    choice(name: 'TARGET_ENV', choices: ['dev', 'staging'], description: 'Kubernetes namespace suffix')
  }

  environment {
    // Replace before running with PUBLISH=true. This local value permits image builds and scans.
    REGISTRY = 'devsecops'
    API_IMAGE = "${REGISTRY}/three-tier-api:${BUILD_NUMBER}"
    WEB_IMAGE = "${REGISTRY}/three-tier-web:${BUILD_NUMBER}"
    NEXUS_URL = 'https://nexus.example.com'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Secret and IaC scan') {
      steps {
        sh '''
          trivy fs --scanners secret,misconfig --severity HIGH,CRITICAL \
            --exit-code 1 --no-progress .
        '''
      }
    }

    stage('Build and unit test') {
      steps {
        dir('api') {
          sh 'mvn -B clean verify'
        }
        junit 'api/target/surefire-reports/*.xml'
      }
    }

    stage('SonarQube analysis') {
      steps {
        withSonarQubeEnv('sonarqube') {
          dir('api') {
            sh 'mvn -B sonar:sonar -Dsonar.projectKey=secure-three-tier-demo -Dsonar.projectName="Secure Three-Tier Demo"'
          }
        }
      }
    }

    stage('Quality gate') {
      steps {
        timeout(time: 5, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Publish immutable JAR to Nexus') {
      when { expression { return params.PUBLISH } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'nexus-user-password', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD')]) {
          sh '''
            curl --fail --silent --show-error --user "$NEXUS_USER:$NEXUS_PASSWORD" \
              --upload-file api/target/task-api-1.0.0.jar \
              "$NEXUS_URL/repository/build-artifacts/task-api/${BUILD_NUMBER}/task-api.jar"
          '''
        }
      }
    }

    stage('Build and scan container images') {
      steps {
        sh '''
          docker build --pull -t "$API_IMAGE" api
          docker build --pull -t "$WEB_IMAGE" frontend
          trivy image --severity HIGH,CRITICAL --exit-code 1 --no-progress "$API_IMAGE"
          trivy image --severity HIGH,CRITICAL --exit-code 1 --no-progress "$WEB_IMAGE"
        '''
        script {
          if (params.PUBLISH) {
            withCredentials([usernamePassword(credentialsId: 'dockerhub-user-password', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
              sh '''
                echo "$REGISTRY_PASSWORD" | docker login --username "$REGISTRY_USER" --password-stdin docker.io
                docker push "$API_IMAGE"
                docker push "$WEB_IMAGE"
                docker logout docker.io
              '''
            }
          }
        }
      }
    }

    stage('Deploy to Kubernetes') {
      when { expression { return params.PUBLISH && params.DEPLOY } }
      steps {
        withCredentials([
          file(credentialsId: 'eks-kubeconfig', variable: 'KUBECONFIG_FILE'),
          string(credentialsId: 'mongodb-atlas-uri', variable: 'MONGODB_URI')
        ]) {
          sh '''
            export KUBECONFIG="$KUBECONFIG_FILE"
            sed "s|devsecops-dev|devsecops-${TARGET_ENV}|g" k8s/namespace.yaml | kubectl apply -f -
            kubectl -n "devsecops-${TARGET_ENV}" create secret generic app-secrets \
              --from-literal=MONGODB_URI="$MONGODB_URI" \
              --dry-run=client -o yaml | kubectl apply -f -
            sed -e "s|__API_IMAGE__|$API_IMAGE|g" -e "s|__WEB_IMAGE__|$WEB_IMAGE|g" \
              k8s/workloads.yaml | sed "s|devsecops-dev|devsecops-${TARGET_ENV}|g" | kubectl apply -f -
            sed "s|devsecops-dev|devsecops-${TARGET_ENV}|g" k8s/network-policy.yaml | kubectl apply -f -
            kubectl -n "devsecops-${TARGET_ENV}" rollout status deployment/backend --timeout=180s
            kubectl -n "devsecops-${TARGET_ENV}" rollout status deployment/frontend --timeout=180s
          '''
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'api/target/*.jar', fingerprint: true, onlyIfSuccessful: true
      cleanWs(deleteDirs: true, notFailBuild: true)
    }
    success {
      echo "Pipeline succeeded: tested, quality-gated, scanned, published and optionally deployed."
    }
    failure {
      echo "Pipeline failed. Start with the first failed stage; do not bypass the quality or vulnerability gate."
    }
  }
}
