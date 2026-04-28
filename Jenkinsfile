pipeline {
    agent any

    environment {
        SERVICE_NAME = "cart"
        // Đường dẫn đến service trong monorepo
        SERVICE_PATH = "cart" 
        // Cấu hình Tool IDs (khai báo trong Manage Jenkins > Tools)
        MAVEN_HOME = tool 'Maven 3'
        JDK_HOME = tool 'Java 21'
        SONAR_SCANNER = tool 'SonarScanner'
        
        // Credentials (khai báo trong Manage Jenkins > Credentials)
        SNYK_TOKEN = credentials('snyk-api-token')
    }

    stages {
        stage('Secret Scanning (Gitleaks)') {
            steps {
                echo "--- Scanning for secrets in ${SERVICE_NAME} ---"
                // Chạy quét riêng thư mục cart để nhanh hơn
                sh "docker run --rm -v \$(pwd):/path zricethezav/gitleaks:latest detect --source='/path/${SERVICE_PATH}' -v"
            }
        }

        stage('Security Scan (Snyk)') {
            steps {
                dir("${SERVICE_PATH}") {
                    echo "--- Checking dependencies vulnerabilities ---"
                    // Quét file pom.xml của Cart Service
                    sh "docker run --rm -v \$(pwd):/app -e SNYK_TOKEN=${SNYK_TOKEN} snyk/snyk:maven snyk test"
                }
            }
        }

        stage('Unit Test & Coverage') {
            steps {
                dir("${SERVICE_PATH}") {
                    echo "--- Running Tests for ${SERVICE_NAME} ---"
                    // Chạy test và tạo báo cáo JaCoCo
                    // Lưu ý: Nếu cấu hình Jacoco trong pom.xml < 70% thì lệnh này sẽ fail luôn stage này
                    sh "mvn clean test"
                }
            }
            post {
                always {
                    // Public kết quả test lên giao diện Jenkins
                    junit "${SERVICE_PATH}/target/surefire-reports/*.xml"
                    // Lưu trữ báo cáo độ phủ để xem lại
                    publishHTML([allowMissing: false, alwaysLinkName: true, keepAll: true, 
                                 reportDir: "${SERVICE_PATH}/target/site/jacoco", 
                                 reportFiles: 'index.html', reportName: 'JaCoCo Report'])
                }
            }
        }

        stage('Static Code Analysis (SonarQube)') {
            steps {
                dir("${SERVICE_PATH}") {
                    withSonarQubeEnv('SonarQubeServer') {
                        sh "mvn sonar:sonar \
                            -Dsonar.projectKey=yas-cart-service \
                            -Dsonar.projectName='YAS: Cart Service' \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
                    }
                }
            }
        }

        stage('Build Artifact') {
            // Chỉ build khi pass các bước trên
            steps {
                dir("${SERVICE_PATH}") {
                    echo "--- Packaging ${SERVICE_NAME} jar ---"
                    sh "mvn package -DskipTests"
                }
            }
        }

        stage('Dockerize') {
            when { branch 'main' } // Chỉ đóng gói image khi merge vào main
            steps {
                echo "--- Building Docker Image for Cart Service ---"
                sh "docker build -t yas-cart:latest ./${SERVICE_PATH}"
            }
        }
    }

    post {
        success {
            echo "Successfully completed CI for Cart Service!"
        }
        failure {
            echo "CI Pipeline for Cart Service failed. Please check Test Coverage or Security Scan logs."
        }
    }
}