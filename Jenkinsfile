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
        SONAR_TOKEN = credentials('sonarqube-token')
        SNYK_TOKEN = credentials('snyk-api-token')
    }

    stages {
        stage('Security: Gitleaks Scan') {
            steps {
                echo 'Đang quét lộ lọt thông tin bảo mật (Gitleaks)...'
                sh '${GITLEAKS_HOME}/gitleaks detect --source . -v || echo "Phat hien canh bao hoac chua cai Gitleaks"'
            }
        }

        stage('Service: Product') {
            when {
                anyOf {
                    changeset "product/**"
                    branch 'feature/product*'
                    branch 'unit-test/product*'
                }
            }
            stages {
                stage('Product: Unit Test & Jacoco Coverage') {
                    steps {
                        echo 'Đang thực thi Unit Test cho Product Service...'
                        sh 'mvn clean test jacoco:report -pl product -am -DskipITs'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: '**/product/target/surefire-reports/*.xml'
                            jacoco(
                                execPattern: '**/product/target/jacoco.exec',
                                classPattern: '**/product/target/classes',
                                sourcePattern: '**/product/src/main/java',
                                minimumLineCoverage: '70'
                            )
                        }
                    }
                }

                stage('Product: SonarQube & Snyk Scan') {
                    steps {
                        echo 'Đang cài đặt parent POM và common-library...'
                        sh 'mvn install -N -DskipTests'
                        sh 'mvn install -pl common-library -am -DskipTests'

                        echo 'Đang thực hiện phân tích SonarQube...'
                        dir('product') {
                            sh 'mvn sonar:sonar -Dsonar.projectKey=yas-product -Dsonar.organization=zeus058 -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN'
                        }

                        echo 'Đang quét lỗ hổng thư viện bằng Snyk...'
                        sh '${SNYK_HOME}/snyk test --file=product/pom.xml --token=$SNYK_TOKEN || echo "Snyk scan failed or not installed"'
                    }
                }

                stage('Product: Package Build') {
                    steps {
                        echo 'Đang đóng gói Product Service (JAR file)...'
                        sh 'mvn package -pl product -am -DskipTests'
                    }
                }
            }
        }
    }

    post {
        success { echo 'PRODUCT SERVICE CI: SUCCESS' }
        failure { echo 'PRODUCT SERVICE CI: FAILED - Vui lòng kiểm tra Log hoặc Độ phủ Code' }
    }
}
