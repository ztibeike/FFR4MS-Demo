#!/usr/bin/env bash
set -eu

repo="registry.cn-beijing.aliyuncs.com/ffr4ms-demo"

echo
echo "Clean images, Repo: $repo"
echo
images=$(docker images | grep "$1"/ | awk 'BEGIN{OFS=":"}{print $1,$2}')

if [[ -n "$images" ]]; then
    echo "$images" | xargs -I {} docker rmi {}
fi