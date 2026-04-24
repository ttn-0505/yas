pipeline {
    agent any

    tools {
        jdk 'Java 21'
        maven 'Maven 3'
        snyk 'snyk'
    }

    environment {
        // Đường dẫn cài đặt tool (cần cấu hình trong Jenkins Global Tool Configuration)
        SONAR_HOME = tool 'sonar-scanner'
        SNYK_HOME = tool 'snyk'
        GITLEAKS_HOME = tool 'gitleaks'
        // Token xác thực (cần tạo Credentials trong Jenkins)
        SONAR_TOKEN = credentials('sonar-qube-token')
        SNYK_TOKEN = credentials('snyk-api-token')
    }

    stages {
        stage('Security: Gitleaks Scan') {
            steps {
                echo 'Đang quét lộ lọt thông tin bảo mật (Gitleaks)...'
                sh '${GITLEAKS_HOME}/gitleaks detect --source . -v || echo "Phat hien canh bao hoac chua cai Gitleaks"'
            }
        }

        stage('Service: Media') {
            when {
                anyOf {
                    changeset "media/**"
                    branch 'feature/media*'
                    branch 'feature/unit-test-media*'
                }
            }
            stages {
                stage('Media: Unit Test & Jacoco Coverage') {
                    steps {
                        echo 'Đang thực thi Unit Test cho Media Service...'
                        sh 'mvn clean test jacoco:report -pl media -am -DskipITs'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: '**/media/target/surefire-reports/*.xml'
                            jacoco(
                                execPattern: '**/media/target/jacoco.exec',
                                classPattern: '**/media/target/classes',
                                sourcePattern: '**/media/src/main/java',
                                minimumLineCoverage: '70'
                            )
                        }
                    }
                }

                stage('Media: SonarQube & Snyk Scan') {
                    steps {
                        echo 'Đang cài đặt parent POM và common-library...'
                        sh 'mvn install -N -DskipTests'
                        sh 'mvn install -pl common-library -am -DskipTests'

                        echo 'Đang thực hiện phân tích SonarQube...'
                        dir('media') {
                            sh 'mvn sonar:sonar -Dsonar.projectKey=yas-media -Dsonar.organization=ttn-0505 -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN'
                        }

                        echo 'Đang quét lỗ hổng thư viện bằng Snyk...'
                        sh '${SNYK_HOME}/snyk test --file=media/pom.xml --token=$SNYK_TOKEN || echo "Snyk scan failed or not installed"'
                    }
                }

                stage('Media: Package Build') {
                    steps {
                        echo 'Đang đóng gói Media Service (JAR file)...'
                        sh 'mvn package -pl media -am -DskipTests'
                    }
                }
            }
        }
    }

    post {
        success { echo 'MEDIA SERVICE CI: SUCCESS' }
        failure { echo 'MEDIA SERVICE CI: FAILED - Vui lòng kiểm tra Log hoặc Độ phủ Code' }
    }
}
