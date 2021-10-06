####
# This Dockerfile is used in order to build a container that build and runs the Quarkus application in JVM mode
#
# Build the image with:
#
# docker build -f src/main/docker/Dockerfile -t quarkus/commit-viewer-jvm .
#
# Then run the container using:
#
# docker run -i --rm -p 8080:8080 quarkus/commit-viewer-jvm
#
# If you want to include the debug port into your docker image
#
# For debug run the container using :
#
# docker run -i --rm -p 8080:8080 -p 5005:5005 -e JAVA_ENABLE_DEBUG="true" quarkus/commit-viewer-jvm
#
###
FROM maven:3.6.0-jdk-11-slim AS build
COPY src /app/src/
COPY pom.xml /app/
COPY checkstyle-my-sun.xml /app/
WORKDIR /app
RUN mvn clean package

FROM registry.access.redhat.com/ubi8/ubi-minimal:8.4

ARG JAVA_PACKAGE=java-11-openjdk-headless
ARG RUN_JAVA_VERSION=1.3.8
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
# Install java and the run-java script
# Also set up permissions for user `1001`
RUN microdnf install curl ca-certificates ${JAVA_PACKAGE} \
    && microdnf update \
    && microdnf clean all \
    && mkdir /deployments \
    && chown 1001 /deployments \
    && chmod "g+rwX" /deployments \
    && chown 1001:root /deployments \
    && curl https://repo1.maven.org/maven2/io/fabric8/run-java-sh/${RUN_JAVA_VERSION}/run-java-sh-${RUN_JAVA_VERSION}-sh.sh -o /deployments/run-java.sh \
    && chown 1001 /deployments/run-java.sh \
    && chmod 540 /deployments/run-java.sh \
    && echo "securerandom.source=file:/dev/urandom" >> /etc/alternatives/jre/conf/security/java.security

RUN microdnf install git

# Configure the JAVA_OPTIONS, you can add -XshowSettings:vm to also display the heap size.
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --from=build --chown=1001 app/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=1001 app/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=1001 app/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=1001 app/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 1001

ENTRYPOINT [ "/deployments/run-java.sh" ]
