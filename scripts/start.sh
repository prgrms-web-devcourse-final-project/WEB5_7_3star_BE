#!/bin/bash
PROJECT_ROOT="/home/ec2-user/app"
JAR_NAME="train-us-0.0.1-SNAPSHOT.jar"
APP_LOG="$PROJECT_ROOT/application.log"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

# 자바 옵션을 담을 배열 선언
JAVA_OPTS=()

echo "> .env 파일을 읽어 Java 옵션으로 변환합니다." >> $DEPLOY_LOG
if [ -f "$PROJECT_ROOT/.env" ]; then
    while read -r line || [ -n "$line" ]; do
        [[ -z "$line" || "$line" =~ ^# ]] && continue
        
        # 윈도우 개행문자(\r) 제거
        clean_line=$(echo "$line" | tr -d '\r')
        
        # JAVA_OPTS 배열에 안전하게 추가 (공백 포함 값 처리)
        JAVA_OPTS+=("-D$clean_line")
    done < "$PROJECT_ROOT/.env"
    echo "> 환경 변수 주입 준비 완료" >> $DEPLOY_LOG
else
    echo "> .env 파일이 존재하지 않습니다. 기본 설정을 사용합니다." >> $DEPLOY_LOG
fi

echo "> Build 파일 복사" >> $DEPLOY_LOG
cp $PROJECT_ROOT/build/libs/$JAR_NAME $PROJECT_ROOT/

echo "> 애플리케이션 실행" >> $DEPLOY_LOG
if [ -z "$SERVER_ROLE" ]; then
    export SERVER_ROLE="api"
fi

echo "> SERVER_ROLE: $SERVER_ROLE 프로파일로 실행합니다." >> $DEPLOY_LOG

# 배열을 큰따옴표로 감싸서("${JAVA_OPTS[@]}") 공백이 있는 인자도 각각 하나로 전달
nohup java "${JAVA_OPTS[@]}" -jar \
    -Dspring.profiles.active=$SERVER_ROLE \
    $PROJECT_ROOT/$JAR_NAME > $APP_LOG 2>&1 &

echo "> 애플리케이션 실행 완료" >> $DEPLOY_LOG
