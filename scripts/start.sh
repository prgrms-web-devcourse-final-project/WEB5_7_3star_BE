#!/bin/bash
PROJECT_ROOT="/home/ec2-user/app"
JAR_NAME="train-us-0.0.1-SNAPSHOT.jar"
APP_LOG="$PROJECT_ROOT/application.log"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

# 1. 시스템 전역 변수 파일 로드
if [ -f "/etc/profile.d/server_role.sh" ]; then
    echo "> 시스템 전역 SERVER_ROLE 설정을 로드합니다." >> $DEPLOY_LOG
    source /etc/profile.d/server_role.sh
fi

# 2. .env 파일 로드
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo "> .env 파일을 로드합니다." >> $DEPLOY_LOG
    while read -r line || [ -n "$line" ]; do
        [[ -z "$line" || "$line" =~ ^# ]] && continue
        clean_line=$(echo "$line" | tr -d '\r')
        export "$clean_line"
    done < "$PROJECT_ROOT/.env"
    echo "> .env 로드 완료" >> $DEPLOY_LOG
fi

# 3. 설정값이 없을 때 기본값 api 적용
if [ -z "$SERVER_ROLE" ]; then
    echo "> SERVER_ROLE이 설정되지 않아 기본값 api를 사용합니다." >> $DEPLOY_LOG
    export SERVER_ROLE="api"
fi

# 4. build/libs에 있는 최신 JAR 파일을 루트로 복사 (기존의 JAR를 Overwrite)
if [ -f "$PROJECT_ROOT/build/libs/$JAR_NAME" ]; then
    echo "> 최신 JAR 파일을 루트 경로로 복사합니다." >> $DEPLOY_LOG
    cp $PROJECT_ROOT/build/libs/$JAR_NAME $PROJECT_ROOT/$JAR_NAME
fi

# 5. 프로파일 설정 결정 (환경변수 우선순위 적용)
if [ -n "$SPRING_PROFILES_ACTIVE" ]; then
    ACTIVE_PROFILE=$SPRING_PROFILES_ACTIVE
    echo "> 시스템 환경변수 SPRING_PROFILES_ACTIVE($ACTIVE_PROFILE)를 사용합니다." >> $DEPLOY_LOG
else
    ACTIVE_PROFILE=$SERVER_ROLE
    echo "> SERVER_ROLE($ACTIVE_PROFILE)을 기본 프로파일로 사용합니다." >> $DEPLOY_LOG
fi

nohup java -jar \
    -Dspring.profiles.active=$ACTIVE_PROFILE \
    $PROJECT_ROOT/$JAR_NAME > $APP_LOG 2>&1 &

echo "> 애플리케이션 실행 완료" >> $DEPLOY_LOG
