pipeline {
    agent any

    tools {
        jdk 'Java 21' 
        maven 'Maven 3'
    }

    environment {
        SNYK_TOKEN = credentials('snyk-api-token')
        SONAR_TOKEN = credentials('sonarqube-token')
    }

    stages {
        stage('Security: Gitleaks Scan') {
            steps {
                echo 'Đang quét lộ lọt thông tin bảo mật (Gitleaks)...'
                // Đổi bat thành sh
                sh 'gitleaks detect --source . -v || echo "Phat hien canh bao hoac chua cai Gitleaks"'
            }
        }

        stage('Service: Media') {
            when {
                anyOf {
                    changeset "media/**"
                    branch 'feature/media*'
                }
            }
            stages {
                stage('Media: Unit Test & Jacoco Coverage') {
                    steps {
                        dir('media') {
                            echo 'Đang thực thi Unit Test cho Media Service...'
                            // Đổi bat thành sh
                            sh 'mvn clean test jacoco:report'
                        }
                    }
                    post {
                        always {
                            junit '**/media/target/surefire-reports/*.xml'
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
                        dir('media') {
                            echo 'Đang thực hiện phân tích SonarQube...'
                            // Trên Linux/Docker dùng ${TOKEN} thay vì %TOKEN%
                            sh "mvn sonar:sonar -Dsonar.projectKey=yas-media -Dsonar.login=${SONAR_TOKEN}"
                            
                            echo 'Đang quét lỗ hổng thư viện bằng Snyk...'
                            sh "snyk test --token=${SNYK_TOKEN}"
                        }
                    }
                }

                stage('Media: Package Build') {
                    steps {
                        dir('media') {
                            echo 'Đang đóng gói Media Service (JAR file)...'
                            sh 'mvn package -DskipTests'
                        }
                    }
                }
            }
        }
    }

    post {
        success { echo 'MEDIA SERVICE CI: SUCCESS' }
        failure { echo 'MEDIA SERVICE CI: FAILED - Vui lòng kiểm tra Log hoặc Độ phủ Code' }
    
}