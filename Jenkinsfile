pipeline {
    agent any
    
    environment {
        SONAR_SCANNER_HOME = tool 'SonarScanner' // Tên phải khớp với Manage Jenkins -> Tools
        SNYK_TOKEN = credentials('snyk-token-id') 
    }

    stages {
        stage('Phase 1: Security Scan (Gitleaks)') {
            steps {
                // Quét bảo mật source code
                sh 'gitleaks detect --source=. -v'
            }
        }

        stage('Phase 2: Detect Changes') {
            steps {
                script {
                    // Lấy file thay đổi ở commit gần nhất
                    def changedFiles = sh(script: "git diff --name-only HEAD~1", returnStdout: true).trim()
                    env.TARGET_SERVICE = ""
                    
                    // Sửa lại tên cho đúng với thư mục project YAS của bạn
                    if (changedFiles.contains('media/')) env.TARGET_SERVICE = "media"
                    else if (changedFiles.contains('cart/')) env.TARGET_SERVICE = "cart"
                }
            }
        }

        stage('Phase 3: Test & Coverage') {
            when { expression { env.TARGET_SERVICE != "" } }
            steps {
                dir("${env.TARGET_SERVICE}") {
                    // Dùng ./mvnw để chạy test
                    sh 'chmod +x mvnw'
                    sh './mvnw clean test'
                }
            }
        }

        stage('Phase 4: Code Quality (SonarQube)') {
            when { expression { env.TARGET_SERVICE != "" } }
            steps {
                withSonarQubeEnv('LocalSonar') { 
                    dir("${env.TARGET_SERVICE}") {
                        // Dùng ./mvnw để đẩy dữ liệu lên SonarQube
                        sh "./mvnw sonar:sonar -Dsonar.projectKey=yas-${env.TARGET_SERVICE}"
                    }
                }
            }
        }

        stage('Phase 5: Snyk Scan') {
            steps {
                dir("${env.TARGET_SERVICE}") {
                    sh 'snyk test'
                }
            }
        }
    }
}