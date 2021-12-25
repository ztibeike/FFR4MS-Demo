FROM java:8-jre

RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

ADD ./target/eureka-registry-1.0-SNAPSHOT.jar /app/
CMD ["java", "-Xmx200m", "-jar", "/app/eureka-registry-1.0-SNAPSHOT.jar"]

EXPOSE 8030