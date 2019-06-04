pipeline {
    agent {
        docker {
            image 'maven:3.6.1-jdk-11'
            args '-v $HOME/.m2:/root/.m2'
        }
    }
    
    
    stages {
        stage('Build') {
            steps {
                echo 'Running now!'
                configFileProvider([configFile(fileId: '1875c4af-1841-47bf-b289-951a565458d1', variable: 'MAVEN_SETTINGS')]) {
                    sh 'mvn -B -s $MAVEN_SETTINGS clean deploy'
                }
                //sh 'mvn -B verify'
            }
        }
    }
    post {
        //always {
        //    echo 'I will do this 
        //          no matter what the status is'
        //}
        success {
            slackSend color: 'good', message: "Build succeeded: ${env.JOB_NAME} - ${env.BUILD_ID}"
        }
        failure {
            slackSend color: 'red', message: "Build failed: ${env.JOB_NAME} - ${env.BUILD_ID}"
        }
    }
}
