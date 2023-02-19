repo=registry.cn-beijing.aliyuncs.com/ffr4ms-demo
tag=0.0.1

.PHONY: build
build:
	clean-image package build-image

.PHONY: package
package:
	@mvn clean package -DskipTests

.PHONY: build-image
build-image:
	@bash hack/build-image.sh $(repo) $(tag)

.PHONY: clean-image
clean-image:
	@bash hack/clean-image.sh $(repo) $(tag)

.PHONY: push-image
push-image:
	@bash hack/push-image.sh $(repo)

.PHONY: deploy
deploy:
	@bash hack/deploy.sh
