dev-mode:
	mvn clean compile quarkus:dev

install-quick:
	mvn compile install -DskipTests

install:
	mvn clean install

package:
	mvn clean package

run-tests:
	mvn clean verify
start-app:
	docker-compose -f infra/commit-viewer/docker-compose.yml up -d --build

stop-app:
	docker-compose -f infra/commit-viewer/docker-compose.yml stop

remove-app:
	docker-compose -f infra/commit-viewer/docker-compose.yml down

start-db:
	docker-compose -f infra/commit-viewer/docker-compose.yml up -d database

stop-db:
	docker-compose -f infra/commit-viewer/docker-compose.yml stop database

run-checkstyle:
	mvn checkstyle:check

help:
	@ echo "Usage   :  make <target>"
	@ echo "Targets :"
	@ echo "   dev-mode ............Compiles the project and run application in dev mode"
	@ echo "   install-quick .......Install the package into local repository after validate, compile and package the source code, skipping tests"
	@ echo "   install .............Install the package into local repository after validate, compile, test and package the source code"
	@ echo "   package .............Take the compiled code and package it in a JAR at target/quarkus-app/quarkus-run.jar"
	@ echo "   run-tests ...........Run integration tests"
	@ echo "   start-app ...........Start a docker container with commit-viewer app"
	@ echo "   stop-app ............Stop commit-viewer app docker container"
	@ echo "   remove-app ..........Stop and remove the docker container with commit-viewer app"
	@ echo "   start-db ............Start a docker container with database"
	@ echo "   stop-db .............Stop the docker container with database"
	@ echo "   run-checkstyle ......Run checkstyle over code"
	@ echo "   help ................Prints this help message"
