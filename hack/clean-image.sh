#!/usr/bin/env bash
set -eu

repo="$1"
tag="$2"

echo
echo "Clean images, Repo: $repo"
echo
images=$(docker images | grep "$tag"/ | awk 'BEGIN{OFS=":"}{print $1,$2}')

if [[ -n "$images" ]]; then
    echo "$images" | xargs -I {} docker rmi {}
fi