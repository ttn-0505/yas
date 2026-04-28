pipeline {
    agent any 

    options {
        timestamps()
    }

    tools {
        jdk 'Java 21'
        maven 'Maven 3'
    }

    stages {
        stage('Detect Changes') {
            steps {
                script {
                    sh 'git fetch origin +refs/heads/main:refs/remotes/origin/main --prune'
                    def baseCommit = ''
                    def hasOriginMain = (sh(script: 'git rev-parse --verify refs/remotes/origin/main >/dev/null 2>&1', returnStatus: true) == 0)

                    if (hasOriginMain) {
                        baseCommit = sh(script: 'git merge-base HEAD refs/remotes/origin/main', returnStdout: true).trim()
                        echo 'Using refs/remotes/origin/main as base'
                    } else if (sh(script: 'git rev-parse --verify HEAD~1 >/dev/null 2>&1', returnStatus: true) == 0) {
                        baseCommit = sh(script: 'git rev-parse HEAD~1', returnStdout: true).trim()
                        echo 'origin/main not found, fallback to HEAD~1'
                    } else {
                        baseCommit = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                        echo 'Single-commit branch, fallback to HEAD'
                    }

                    def changedFiles = sh(script: "git diff --name-only ${baseCommit} HEAD", returnStdout: true).trim()
                    env.BASE_COMMIT = baseCommit
                    env.CHANGED_FILES = changedFiles

                    def servicePaths = ['backoffice-bff': 'backoffice-bff/', 'cart': 'cart/', 'customer': 'customer/', 'delivery': 'delivery/', 'inventory': 'inventory/', 'location': 'location/', 'media': 'media/', 'order': 'order/', 'payment-paypal': 'payment-paypal/', 'payment': 'payment/', 'product': 'product/', 'promotion': 'promotion/', 'rating': 'rating/', 'recommendation': 'recommendation/', 'sampledata': 'sampledata/', 'search': 'search/', 'storefront-bff': 'storefront-bff/', 'tax': 'tax/', 'webhook': 'webhook/']
                    def changedServices = []
                    if (changedFiles.contains('common-library/')) {
                        changedServices.addAll(servicePaths.keySet())
                    } else {
                        servicePaths.each { name, path -> if (changedFiles.contains(path)) { changedServices << name } }
                    }
                    env.CHANGED_SERVICES = changedServices.unique().join(',')
                }
            }
        }

        stage('Check Tools') {
            steps {
                script {
                    if (env.CHANGED_SERVICES) { sh 'gitleaks version' }
                }
            }
        }

        stage('Gitleaks Scan') {
            steps {
                script {
                    if (env.CHANGED_FILES?.trim()) {
                        sh 'gitleaks detect --config gitleaks.toml --source . --log-opts="${BASE_COMMIT}..HEAD" --no-banner'
                    }
                }
            }
        }

        stage('Test & Build Changed Services') {
            steps {
                script {
                    def changedServices = env.CHANGED_SERVICES ? env.CHANGED_SERVICES.split(',').findAll { it?.trim() } : []
                    changedServices.each { serviceName ->
                        def serviceDir = serviceName.trim()
                        dir(serviceDir) {
                            sh "mvn -f ../pom.xml -pl ${serviceDir} -am clean test jacoco:report"
                            
                            // Fix đoạn Python Check Coverage
                            sh """
                                python3 -c "
import sys
import xml.etree.ElementTree as ET
try:
    tree = ET.parse('target/site/jacoco/jacoco.xml')
    root = tree.getroot()
    missed = 0
    covered = 0
    for counter in root.findall('.//counter[@type=\\'LINE\\']'):
        missed += int(counter.attrib.get('missed', 0))
        covered += int(counter.attrib.get('covered', 0))
    total = missed + covered
    coverage = 100.0 * covered / total if total else 0.0
    print(f'Line coverage for ${serviceDir}: {coverage:.2f}%')
    if coverage < 70.0:
        print('FAILED: Coverage below 70%')
        sys.exit(1)
except Exception as e:
    print(f'Error parsing Jacoco report: {e}')
    sys.exit(1)
"
                            """
                            sh "mvn -f ../pom.xml -pl ${serviceDir} -am -DskipTests package"
                        }
                    }
                }
            }
        }

        stage('SonarQube Scan') {
            steps {
                script {
                    def changedServices = env.CHANGED_SERVICES ? env.CHANGED_SERVICES.split(',').findAll { it?.trim() } : []
                    if (changedServices.isEmpty()) return
                    
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        changedServices.each { serviceDir ->
                            dir(serviceDir.trim()) {
                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                    sh "mvn -f ../pom.xml -pl . -am sonar:sonar -DskipTests -Dsonar.token=\$SONAR_TOKEN -Dsonar.projectKey=hcmus-devops-project1_yas_${serviceDir} -Dsonar.projectName=yas-${serviceDir}"
                                }
                            }
                        }
                    }
                }
            }
        }
    } // Đóng stages

    post {
        always {
            echo 'Pipeline finished.'
        }
    }
} // Đóng pipeline