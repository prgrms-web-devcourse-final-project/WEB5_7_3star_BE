#!/bin/bash
# 배포 폴더가 없으면 생성하고 권한 부여
sudo mkdir -p /home/ec2-user/app
sudo chown -R ec2-user:ec2-user /home/ec2-user/app
