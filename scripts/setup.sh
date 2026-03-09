#!/bin/bash
set -e # 명령어 실패 시 즉시 중단

# 배포 폴더 생성
sudo mkdir -p /home/ec2-user/app

# S3에서 .env 파일 가져오기
aws s3 cp s3://trainus-deploy-bucket-2026/.env /home/ec2-user/app/.env

# 소유권 및 권한 정리
sudo chown -R ec2-user:ec2-user /home/ec2-user/app
sudo chmod 600 /home/ec2-user/app/.env
