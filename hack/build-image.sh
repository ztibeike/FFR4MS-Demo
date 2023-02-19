#!/usr/bin/env bash

repo="$tag"
tag="$2"

echo
echo "Start build images, Tag: $tag"
echo

echo "build eureka-registry"
docker build -t "$repo"/eureka-registry eureka-registry
docker tag "$repo"/eureka-registry:latest "$repo"/eureka-registry:"$tag"

echo "build service-a"
docker build -t "$repo"/service-a service-a
docker tag "$repo"/service-a:latest "$repo"/service-a:"$tag"

echo "build service-b"
docker build -t "$repo"/service-b service-b
docker tag "$repo"/service-b:latest "$repo"/service-b:"$tag"

echo "build zuul-gateway-a"
docker build -f zuul-gateway/Dockerfile-a -t "$repo"/zuul-gateway-a zuul-gateway
docker tag "$repo"/zuul-gateway-a:latest "$repo"/zuul-gateway-a:"$tag"

echo "build zuul-gateway-b"
docker build -f zuul-gateway/Dockerfile-b -t "$repo"/zuul-gateway-b zuul-gateway
docker tag "$repo"/zuul-gateway-b:latest "$repo"/zuul-gateway-b:"$tag"
