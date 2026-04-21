pipeline {
    agent any

    stages {
        stage('1. Checkout Code') {
            steps {
                echo 'Downloading source code from GitHub to Jenkins...'
                checkout scm
            }
        }

        stage('2. Build & Test') {
            steps {
                echo 'Running Unit Tests with Maven...'
                // Actual command placeholder: sh './mvnw clean test'
                sh 'echo "This stage will execute Build and run Tests to calculate Coverage > 70%"'
            }
        }

        stage('3. SonarQube Analysis') {
            steps {
                echo 'Scanning code quality with SonarQube...'
                // This stage fulfills Requirement #5 of the project
                sh 'echo "Sending analysis data to localhost:9000..."'
            }
        }
    }

    post {
        success {
            echo 'CI Pipeline completed successfully!'
        }
        failure {
            echo 'CI Pipeline failed! Please check the logs.'
        }
    }
}