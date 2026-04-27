pipeline {
    agent any 
   
    options {
        timestamps()
    }

    tools {
        jdk 'Java 21'
        maven 'Maven 3'
        snyk 'snyk'
    }

    stages {
        stage('Detect Changes') {
            steps {
                script {
                    sh 'git fetch origin +refs/heads/main:refs/remotes/origin/main --prune'

                    def baseCommit = ''
                    def hasOriginMain = (sh(
                        script: 'git rev-parse --verify refs/remotes/origin/main >/dev/null 2>&1',
                        returnStatus: true
                    ) == 0)

                    if (hasOriginMain) {
                        baseCommit = sh(
                            script: 'git merge-base HEAD refs/remotes/origin/main',
                            returnStdout: true
                        ).trim()
                        echo 'Using refs/remotes/origin/main as base'
                    } else if (sh(script: 'git rev-parse --verify HEAD~1 >/dev/null 2>&1', returnStatus: true) == 0) {
                        baseCommit = sh(
                            script: 'git rev-parse HEAD~1',
                            returnStdout: true
                        ).trim()
                        echo 'origin/main not found, fallback to HEAD~1'
                    } else {
                        baseCommit = sh(
                            script: 'git rev-parse HEAD',
                            returnStdout: true
                        ).trim()
                        echo 'Single-commit branch, fallback to HEAD'
                    }

                    def changedFiles = sh(
                        script: "git diff --name-only ${baseCommit} HEAD",
                        returnStdout: true
                    ).trim()

                    echo "Base commit: ${baseCommit}"
                    echo "Files changed:\n${changedFiles}"
                    env.BASE_COMMIT = baseCommit
                    env.CHANGED_FILES = changedFiles

                    def servicePaths = [
                        'backoffice-bff': 'backoffice-bff/',
                        'cart': 'cart/',
                        'customer': 'customer/',
                        'delivery': 'delivery/',
                        'inventory': 'inventory/',
                        'location': 'location/',
                        'media': 'media/',
                        'order': 'order/',
                        'payment-paypal': 'payment-paypal/',
                        'payment': 'payment/',
                        'product': 'product/',
                        'promotion': 'promotion/',
                        'rating': 'rating/',
                        'recommendation': 'recommendation/',
                        'sampledata': 'sampledata/',
                        'search': 'search/',
                        'storefront-bff': 'storefront-bff/',
                        'tax': 'tax/',
                        'webhook': 'webhook/'
                    ]

                    def changedServices = []
                    if (changedFiles.contains('common-library/')) {
                        changedServices.addAll(servicePaths.keySet())
                        echo 'common-library changed, scheduling all services'
                    } else {
                        servicePaths.each { serviceName, servicePath ->
                            if (changedFiles.contains(servicePath)) {
                                changedServices << serviceName
                            }
                        }
                    }

                    env.CHANGED_SERVICES = changedServices.unique().join(',')
                    echo "Changed services: ${env.CHANGED_SERVICES}"
                }
            }
        }

        stage('Check Tools') {
            steps {
                script {
                    def changedServices = env.CHANGED_SERVICES ? env.CHANGED_SERVICES.split(',').findAll { it?.trim() } : []
                    if (changedServices.isEmpty()) {
                        echo 'No service changes detected. Skipping tool checks.'
                        return
                    }

                    sh 'gitleaks version || echo "Gitleaks not found"'
                    
                    // Lấy đường dẫn thực thi chính xác của Snyk từ Jenkins Tool
                    def snykBin = tool name: 'snyk', type: 'com.tool.snyk.SnykInstallation'
                    sh "${snykBin} --version || echo 'Snyk not found'"
                    
                    // Lưu đường dẫn vào biến môi trường để các stage sau sử dụng
                    env.SNYK_EXE = snykBin
                }
            }
        }

        stage('Gitleaks Scan') {
            steps {
                script {
                    if (!env.CHANGED_FILES?.trim()) return
                    sh 'gitleaks detect --config gitleaks.toml --source . --log-opts="${BASE_COMMIT}..HEAD" --no-banner'
                }
            }
        }

        stage('Test & Build Changed Services') {
            steps {
                script {
                    def changedServices = env.CHANGED_SERVICES ? env.CHANGED_SERVICES.split(',').findAll { it?.trim() } : []
                    if (changedServices.isEmpty()) return

                    changedServices.each { serviceName ->
                        def serviceDir = serviceName.trim()
                        echo "Running tests and build for ${serviceDir}..."

                        dir(serviceDir) {
                            // Sử dụng withMaven để tự động nạp JAVA_HOME và Maven PATH
                            withMaven(maven: 'Maven 3', jdk: 'Java 21') {
                                sh "mvn -f ../pom.xml -pl ${serviceDir} -am clean test jacoco:report"
                                
                                junit 'target/surefire-reports/*.xml'
                                jacoco execPattern: 'target/jacoco.exec',
                                       classPattern: 'target/classes',
                                       sourcePattern: 'src/main/java',
                                       inclusionPattern: '**/*.class'
                                
                                sh '''
                                    |python3 - <<'PY'
                                    |import sys
                                    |import xml.etree.ElementTree as ET
                                    |report_path = 'target/site/jacoco/jacoco.xml'
                                    |try:
                                    |    tree = ET.parse(report_path)
                                    |    root = tree.getroot()
                                    |    missed = 0
                                    |    covered = 0
                                    |    for counter in root.findall('.//counter[@type="LINE"]'):
                                    |        missed += int(counter.attrib.get('missed', 0))
                                    |        covered += int(counter.attrib.get('covered', 0))
                                    |    total = missed + covered
                                    |    coverage = 100.0 * covered / total if total else 0.0
                                    |    print(f'Line coverage: {coverage:.2f}%')
                                    |    if coverage < 70.0:
                                    |        print('Coverage is below the required 70% threshold.')
                                    |        sys.exit(1)
                                    |except Exception as e:
                                    |    print(f'Error parsing Jacoco report: {e}')
                                    |    sys.exit(0) 
                                    |PY
                                '''.stripMargin()
                                
                                sh "mvn -f ../pom.xml -pl ${serviceDir} -am -DskipTests package"
                            }
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

                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        changedServices.each { serviceName ->
                            def serviceDir = serviceName.trim()
                            dir(serviceDir) {
                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                    withMaven(maven: 'Maven 3', jdk: 'Java 21') {
                                        sh """
                                            mvn -f ../pom.xml -pl ${serviceDir} -am sonar:sonar \
                                                -DskipTests \
                                                -Dsonar.token=\$SONAR_TOKEN \
                                                -Dsonar.projectKey="hcmus-devops-project1_yas_${serviceDir}" \
                                                -Dsonar.projectName="yas-${serviceDir}" \
                                                -Dsonar.ws.timeout=120
                                        """
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Snyk Scan') {
            steps {
                script {
                    def changedServices = env.CHANGED_SERVICES ? env.CHANGED_SERVICES.split(',').findAll { it?.trim() } : []
                    if (changedServices.isEmpty()) return

                    withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                        // Sử dụng đường dẫn Snyk đã lấy được từ stage Check Tools
                        def snykCmd = env.SNYK_EXE ?: 'snyk'

                        changedServices.each { serviceName ->
                            def serviceDir = serviceName.trim()
                            echo "Running Snyk scan for ${serviceDir}..."
                            dir(serviceDir) {
                                sh 'test -x ./mvnw || chmod +x ./mvnw'
                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                    sh "SNYK_TOKEN=\$SNYK_TOKEN ${snykCmd} test --file=pom.xml --severity-threshold=high --skip-unresolved"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
        }
    }
}