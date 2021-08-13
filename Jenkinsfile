// https://github.com/camunda/jenkins-global-shared-library
// https://github.com/camunda/cambpm-jenkins-shared-library
@Library(['camunda-ci', 'cambpm-jenkins-shared-library']) _

pipeline {
  agent {
    node {
      label 'jenkins-job-runner'
    }
  }
  environment {
    CAMBPM_LOGGER_LOG_LEVEL = 'DEBUG'
  }
  parameters {
    string name: 'RELEASE_BRANCH', defaultValue: 'test_master', description: 'The git repository branch to check out.'
    string name: 'RELEASE_VERSION', defaultValue: '7.16.99', description: 'The version to be released.'
    string name: 'NEXT_DEVELOPMENT_VERSION', defaultValue: '7.16.0-TEST', description: 'The next development version to set.'
    booleanParam name: 'PUST_TO_REMOTE', defaultValue: false, description: 'Push the changes back to remote repositories.'
    booleanParam name: 'SKIP_DEPLOY', defaultValue: true, description: 'When checked, job does NOT deploy maven artifacts to Nexus.'
    booleanParam name: 'SKIP_TESTS', defaultValue: true, description: 'Skip Unit tests.'
    choice name: 'RELEASE_TYPE', choices: ['ALPHA', 'FINAL'], description: 'In case of ALPHA release, all artifacts will be uploaded to nightly folders, otherwise to GA folder locations.'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
    throttleJobProperty(
            throttleEnabled: true,
            throttleOption: 'project',
            maxConcurrentTotal: 1
    )
  }
  stages {
//    stage('Prepare') {
//      agent {
//        node {
//          label ''
//        }
//      }
//      steps {
//        // parse versions
//      }
//    }
    stage('Create Version Tags') {
      agent {
        node {
          label 'centos-stable'
        }
      }
      steps {
        sh 'git --version'
        sh 'git status'
      }
      post {
        success {
          // TODO: push tags here
          echo 'push tags'
        }
      }
    }
    stage('Build CE Artifacts'){
      agent {
        node {
          label 'centos-stable'
        }
      }
      steps {
        // build with maven
        sh 'pwd'
      }
      post {
        success {
          // deploy to nexus/maven central
          echo 'deploy to nexus'
          // trigger EE pipeline?
        }
      }
    }
  }
  post {
    changed {
      script {
        if (!agentDisconnected()){
          cambpmSendEmailNotification()
        }
      }
    }
  }
}
