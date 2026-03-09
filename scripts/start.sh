#!/bin/bash
PROJECT_ROOT="/home/ec2-user/app"
JAR_NAME="train-us-0.0.1-SNAPSHOT.jar"
APP_LOG="$PROJECT_ROOT/application.log"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

echo "> .env 파일을 로드합니다." >> $DEPLOY_LOG
if [ -f "$PROJECT_ROOT/.env" ]; then
    while read -r line || [ -n "$line" ]; do
        [[ -z "$line" || "$line" =~ ^# ]] && continue
        clean_line=$(echo "$line" | tr -d '\r')
        export "$clean_line"
    done < "$PROJECT_ROOT/.env"
    echo "> .env 로드 완료" >> $DEPLOY_LOG
else
    echo "> .env 파일이 존재하지 않습니다." >> $DEPLOY_LOG
fi

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
