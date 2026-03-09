#!/bin/bash
PROJECT_ROOT="/home/ec2-user/app"
JAR_NAME="train-us-0.0.1-SNAPSHOT.jar"
APP_LOG="$PROJECT_ROOT/application.log"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

echo "> Build 파일 복사" >> $DEPLOY_LOG
cp $PROJECT_ROOT/build/libs/$JAR_NAME $PROJECT_ROOT/

echo "> 애플리케이션 실행" >> $DEPLOY_LOG
if [ -z "$SERVER_ROLE" ]; then
    export SERVER_ROLE="api"
fi

echo "> SERVER_ROLE: $SERVER_ROLE 프로파일로 실행합니다." >> $DEPLOY_LOG

nohup java -jar \
    -Dspring.profiles.active=$SERVER_ROLE \
    $PROJECT_ROOT/$JAR_NAME > $APP_LOG 2>&1 &

echo "> 애플리케이션 실행 완료" >> $DEPLOY_LOG
