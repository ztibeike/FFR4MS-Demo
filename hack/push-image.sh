#!/usr/bin/env bash
set -eu

repo="registry.cn-beijing.aliyuncs.com/ffr4ms-demo"

echo
echo "Start pushing image"
echo
docker images | grep "$repo/" | awk 'BEGIN{OFS=":"}{print $1,$2}' | xargs -I {} docker push {}