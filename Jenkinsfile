properties([
    buildDiscarder(logRotator(artifactDaysToKeepStr: '10', artifactNumToKeepStr: '20')),
    disableConcurrentBuilds(),
    pipelineTriggers([
        pollSCM('* * * * *')
    ])
])

node {
    timestamps {
        ansiColor('xterm') {

            deleteDir()

            stage('Test & Publish') {
                checkout scm
                sh "./ci/test_and_publish.sh"
            }
        }
    }
}
