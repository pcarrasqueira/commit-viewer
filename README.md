# Commit Viewer 

Commit-viewer is a Quarkus project that list commits for a given user and repository. 
It has one paginated endpoint exposed at
`http://localhost:8080/commit-viewer/{user}/{repo}`. 
You can check the open-api documentation of the endpoint using Swagger UI at `http://localhost:8080/api-docs/`

Commit Viewer uses by default the GitHub API in order to fetch the commits or, in case of failure, the git cli. 
You can force the use of the CLI with property :
```
commit-viewer.force-use-cli=true
```
Database support was added, now firs time we fetch commits for a repo, we will save the data to database. Subsequent requests
will fetch the data directly from database.


In order to increase resiliency of the application, retries, timeouts and fallbacks were implemented using [SmallRye Fault Tolerance](https://github.com/smallrye/smallrye-fault-tolerance/)
priveded by Quarkus. You can configure the fault tolerance values on application.properties, eg :
```
Timeout/value=5 // set timeout value to 5 globally
Timeout/unit=MINUTES // set MINUTES as the time unit globally
com.challenge.web.impl.CommitViewerResourceImpl/getCommitList/Timeout/value=5 // set timeout value to 5 for getCommitList method.
```

Checkstyle validation is active on build phase and is using a checker based on [sun_checks.xml](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/sun_checks.xml)

**_NOTE:_**  If you want to learn more about Quarkus, please visit its website: https://quarkus.io/.

## How can you run the application? 🏃🏻‍♂️💨

### Run locally on your machine

To run the application locally in dev mode run :
```shell script
make start-db
make dev-mode
```

Package and run the jar application :
```shell script
make start-db
make package
java -jar target/quarkus-app/quarkus-run.jar
```

**_NOTE1:_**  You need to have installed git, java and maven to build the solution.

**_NOTE2:_** Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

### Run on docker

You can run your application on docker and star using it right now :
```shell script
make start-app
```
To stop it jus run :
```shell script
make stop-app
```

### If you want to remove the docker containers just run 
```shell script
make remove-app
```

### Now you can test the commit viewer app 🚨🪲👀
```shell script
curl --location --request GET 'http://localhost:8080/commit-viewer/pcarrasqueira/commit-viewer-test?page=1&per_page=1'
```
## Makefile
A makefile is available with a bunch of helpful options. You can check it runnig :

```script
make help

Usage   :  make <target>
Targets :
   dev-mode ............Compiles the project and run application in dev mode
   install-quick .......Install the package into local repository after validate, compile and package the source code, skipping tests
   install .............Install the package into local repository after validate, compile, test and package the source code
   package .............Take the compiled code and package it in a JAR at target/quarkus-app/quarkus-run.jar
   run-tests ...........Run integration tests
   start-app ...........Start a docker container with commit-viewer app
   stop-app ............Stop commit-viewer app docker container
   remove-app ..........Stop and remove the docker container with commit-viewer app
   start-db ............Start a docker container with database
   stop-db .............Stop the docker container with database
   run-checkstyle ......Run checkstyle over code
   help ................Prints this help message
```
