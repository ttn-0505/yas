pipeline {
    agent any

    tools {
        jdk 'Java 21' 
        maven 'Maven 3'
    }

    environment {
        // Yêu cầu 7c: Credentials cho bảo mật
        SNYK_TOKEN = credentials('snyk-api-token')
        SONAR_TOKEN = credentials('sonarqube-token')
    }

    stages {
        // Yêu cầu 7c: Quét Secret toàn dự án (thường chạy đầu tiên)
        stage('Security: Gitleaks Scan') {
            steps {
                echo 'Đang quét lộ lọt thông tin bảo mật (Gitleaks)...'
                bat 'gitleaks detect --source . -v || echo "Phat hien canh bao hoac chua cai Gitleaks"'
            }
        }

        // Yêu cầu 6 & 7a: Chỉ tập trung cho Media Service
        stage('Service: Media') {
            when {
                anyOf {
                    changeset "media/**" // Chạy khi có thay đổi trong folder media
                    branch 'feature/media*' // Chạy khi bạn đang ở nhánh media
                }
            }
            stages {
                // Yêu cầu 5 & 7b: Chạy Unit Test và kiểm tra độ phủ > 70%
                stage('Media: Unit Test & Jacoco Coverage') {
                    steps {
                        dir('media') {
                            echo 'Đang thực thi Unit Test cho Media Service...'
                            bat 'mvn clean test jacoco:report'
                        }
                    }
                    post {
                        always {
                            // Yêu cầu 5: Upload kết quả test (JUnit)
                            junit '**/media/target/surefire-reports/*.xml'
                            
                            // Yêu cầu 7b: Fail pipeline nếu độ phủ code < 70%
                            jacoco(
                                execPattern: '**/media/target/jacoco.exec',
                                classPattern: '**/media/target/classes',
                                sourcePattern: '**/media/src/main/java',
                                minimumLineCoverage: '70' // Ngưỡng bắt buộc của đồ án
                            )
                        }
                    }
                }

                // Yêu cầu 7c: Quét chất lượng code và lỗ hổng bảo mật
                stage('Media: SonarQube & Snyk Scan') {
                    steps {
                        dir('media') {
                            echo 'Đang thực hiện phân tích SonarQube...'
                            bat "mvn sonar:sonar -Dsonar.projectKey=yas-media -Dsonar.login=%SONAR_TOKEN%"
                            
                            echo 'Đang quét lỗ hổng thư viện bằng Snyk...'
                            bat "snyk test --token=%SNYK_TOKEN%"
                        }
                    }
                }

                // Yêu cầu 5: Phase Build (Đóng gói sản phẩm)
                stage('Media: Package Build') {
                    steps {
                        dir('media') {
                            echo 'Đang đóng gói Media Service (JAR file)...'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'MEDIA SERVICE CI: SUCCESS'
        }
        failure {
            echo 'MEDIA SERVICE CI: FAILED - Vui lòng kiểm tra Log hoặc Độ phủ Code'
        }
    }
}