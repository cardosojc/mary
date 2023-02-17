FROM eclipse-temurin:11
RUN mkdir /opt/app
COPY target/scala-2.13/mary.jar /opt/app
EXPOSE 8081
CMD ["java", "-jar", "/opt/app/mary.jar"]