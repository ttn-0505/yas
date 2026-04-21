pipeline {
    agent any
    
    // Khai báo môi trường để dùng SonarQube
    environment {
        SCANNER_HOME = tool 'SonarScanner' // Tên này phải khớp với cấu hình trong Global Tool
    }

    stages {
        stage('1. Checkout Code') {
            steps {
                echo 'Downloading source code...'
                checkout scm
            }
        }

        stage('2. Build & Test with Coverage') {
            steps {
                echo 'Running Maven Build and Jacoco Test Report...'
                // Lệnh này vừa build vừa chạy test để tạo file báo cáo coverage
                sh './mvnw clean verify' 
            }
        }

        stage('3. SonarQube Analysis') {
            steps {
                // Đoạn này giúp Jenkins tự động đẩy kết quả lên SonarQube
                withSonarQubeEnv('SonarQube-Server') {
                    sh './mvnw sonar:sonar -Dsonar.projectKey=YAS-Project'
                }
            }
        }
    }
}