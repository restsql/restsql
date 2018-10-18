# restSQL Framework Developer Guide
Last Update: 0.8.13

This document describes framework developer requirements, framework structure, and the procedure to release a new version of restsql.

Project website is at http://restsql.org. Distributions at http://restsql.org/dist. Release history at http://restsql.org/doc/ReleaseHistory.html. Source code hosted at http://github.com/restsql.

# Development

## Requirements
You'll need the following tools:
* JDK 1.7
* Eclipse
* Ant 1.9
* Tomcat 7
* MySQL 5.7
* PostgreSQL 10.5
* Docker version 18.06 (tested on Amazon Linux and macOS 10.13.6)

Note that the current release version was developed on macOS High Sierra 10.13.6, but any OS should do.

## Source Code
restSQL source code and artifacts are contained in six projects available from http://github/restsql:
1. restsql (service and core framework)
2. restsql-sdk (documentation, HTTP API explorer, javadoc, examples)
3. restsql-test (test framework, test cases)
4. restsql-website (files specific to hosting restsql.org)
5. docker (container build and supporting files)
6. dist-archive (release binaries)

In contributing to restSQL, you agree to release the code under the MIT license and copyright it to the restSQL project contributors. You must add your name to the CONTRIBUTORS.txt in the three projects. Each file must be prefaced with the following:

    /* Copyright (c) restSQL Project Contributors. Licensed under MIT. */

(An Eclipse code template profile is available for you to import so that it is automatically added to your new files.)

Your changes will be checked into your fork off the main trunk. Send a pull request to 'restsql' to request the maintainer to incorporate your fork into the main line. Please describe the features added or defect fixed, test cases added, documentation added and any unresolved issues.

## Eclipse
The three Eclipse projects are set to use the workspace's default JDK/JR. Start Eclipse with JDK 1.7 and this will be automatic. The .classpath reference is currently set to the following:

    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>

Please do not change it to reference a specific JDK release, e.g. jdk1.7.0_07.

## Coding Style
Import the following into your Eclipse workspace:

    restsql/src/resources/eclipse
        formatter.xml 		[Formatter Profile]
        codetemplates.xml	[Code Templates]
        cleanup.xml			[Java Clean Up Profile]

Please use these regularly! Orderly code is a must.

Let's keep a clean house -- do not leave commented out, dead code around spoiling readability. We have git to help us find old code if we ever need it.

Follow Sun's Java naming conventions and Javadoc style guidelines. Package overviews and class descriptions are a must. Method javadoc is strongly recommended, excepting simple getter/setters.

## Design
Please have a look at the architecture diagrams before starting on any extensions. See restsql-sdk/WebContent/doc/Architecture.html.

restSQL was designed from the ground up with extension in mind. SOLID principles are in effect. Interfaces are used widely. A central factory loads implementation from the restsql.properties. See http://en.wikipedia.org/wiki/SOLID_(object-oriented_design). Dependency Inversion is an important one to note. Please code to interfaces, not implementation. This allows others to add capability without modifying the core (Open-Closed Principle).

The framework is roughly divided into core, security and service. Most extensions should be to the core, but there could be new HTTP Service capability.

## Directories and Configuration Files

### Source Root
Set up your Eclipse projects in /opt/restsql/code if possible. restSQL configuration uses absolute paths in most cases. The property values and default web.xml will work out of the box if you use /opt/restsql/code (or C:\opt\restsql\code on Windows) as your root.

### Configuration Files
The main configuration file (restsql.properties) location is configured using a Java System property named org.restsql.properties. In a JAR deployment or development/Eclipse environment, the property can be injected into the JRE or overriden in an early loaded static block. Or just use the default, /resources/properties/default-restsql.properties, which requires you to use the default dev root.

In a WAR deployment, the restsql.properties is loaded using the web.xml (or Parameter entry in Tomcat's context.xml). The default is: /etc/opt/restsql/restsql.properties.

The restsql-test project has additional properties files, one per database, which define non-default properties like triggers. Generally you use these restsql-test props files rather than the default.

The restsql-test project has prep-mysql or prep-pgsql targets to copy the restsql-test project files (conf files and sqlresources) out to /etc/opt/restsql.

restsql.properties has all of the properties that customize an installation to a database, file system and application. The deployment guide (SDK or README.txt) describes the properties. See org.restsql.core.Config to see all the possible properties and default values. Config creates the properties map from the properties file and sets up loggers. All restsql classes refer to property keys using the Config's public constants KEY_xxx and DEFAULT_xxx. Properties are located in Config.properties, an Immutable map. Properties must always be read with the default as backup, since the properties file may not have declared all of them. An example:

    String sqlResourcesDir = Config.properties.getProperty(Config.KEY_SQLRESOURCES_DIR, Config.DEFAULT_SQLRESOURCES_DIR);

Properties may be read once and cached. They are not expected to change during the JVM's lifetime.

If you would like to add a property, please add KEY and DEFAULT constants as well as the property in the default-restsql.properties along with a brief description.

### Logging
Logs by default are written to /var/log/restsql. Make it easier on yourself and use this directory. The logging conf is controlled by a property in the restsql.properties. The default is:

    logging.config=resources/properties/default-log4j.properties

To understand more about this topic, have a look at the Logging overview in the SDK: WebContent/doc/Logging.html.

Log4j is preferred and is used by default. The default logging properties for both the framework and the test context is in restsql/src/resources/properties/default-log4j.properties. You can temporarily edit this file or else create your own and make sure your restsql.properties refers to it (Remember this is the only location that is relative!).

You only need to change the bottom of the file. The defaults are:

    # Configure which loggers log to which appenders
    log4j.logger.org.restsql.internal=INFO, INTERNAL

    # Request loggers - set level to INFO to enable and FATAL to disable
    log4j.logger.org.restsql.access=INFO, ACCESS
    log4j.logger.org.restsql.error=INFO, ERROR
    log4j.logger.org.restsql.trace=INFO, TRACE

The internal log may be set to DEBUG to reveal more detail, which is generally just executed SQL. Logged SQL is useful when executing the JUnit test harness. The SQL is by default available in the trace and error logs using the http service as well as the service test harness.

Setting DEBUG on the access, error and trace will have no effect. Setting them to WARN or above would disable them. Changes require restart.

Logs by default are sent to /var/log/restsql and use a daily rolling file appender.

If you wish to add logging statements to the internal logger, use Config.logger.level(message), where level is debug, info, etc. The logger is an instance of the Apache Commons Logger, a facade to log4j, Java native and other logging subsystems. If your message requires any work, e.g. string manipulation, check if the level is enabled first, as in:

    if (Config.logger.isDebugEnabled()) {
    	Config.logger.debug("Loading meta data for " + this.resName + " - " + sql);
    }

The guard statement avoids unnecessary work. Please use them.

## Builds

The Eclipse projects will auto-compile however generation of documentation, source and binary distributions requires the Ant build scripts. These are called build.xml and are located in the root of each of the projects. The targets for each project are as follows:

	restsql Main targets:

	 clean           deletes output dir
	 compile         compiles sources
	 compile-schema  executes xjc on xml files
	 dist            generates distribution files
	 doc             generates javadoc
	 lib             generates binary libs and war
	 lib-doc         generates full javadoc jar
	Default target: dist


	restsql-test Main targets:

	 clean                       deletes output dir
	 compile-schema              compiles test case definition schema
	 compile-tests               compiles JUnit tests
	 exec-examples               executes sample programs
     prep-mysql                  copy conf files to /etc/opt/restsql (mysql)
     prep-pgsql                  copy conf files to /etc/opt/restsql (postgresql)
	 test-api                    exercises junit tests for java api (mysql)
     test-api-pgsql              exercises junit tests for java api (postgresql)
	 test-api-report             generates junit report for api tests
	 test-api-security           exercises security junit tests for java api
	 test-service-http           executes all test cases for the http interface (mysql)
     test-service-http-pgsql     executes all test cases for the http interface (postgresql)
	 test-service-http-security  executes security test cases for the http interface
	 test-service-http-subset    executes some of test cases for the http interface (mysql)
     test-service-http-subset-pgsql executes some of test cases for the http interface (postgresql)
	 test-service-java           executes all test cases for the service's java interface, excluding security (mysql)
     test-service-java-pgsql     executes all test cases for the service's java interface, excluding security (postgresql)
	 test-service-java-subset    executes some test cases for the service's java interface (mysql)
     test-service-java-subset-pgsql executes some test cases for the service's java interface (posgresql)
	Default target: test-all


	restsql-sdk Main targets:

	 clean          deletes output dir
	 dist           generates distribution files
	 exec-examples  executes example programs
	Default target: dist


The Ant view is rather handy in Eclipse. Drag the three build.xml files into the Ant view. This allows you to run any target without opening a command line. You can customize System properties as well.

Before creating any distribution, clean the output (obj) and then run the dist. This will ensure the contents are exactly what is required.

## Working with Tomcat

Development is faster using Tomcat from Eclipse rather than standalone. It allows you to start and stop the server and synchronize files with simple gestures in the Servers view. And best of all, you can set breakpoints and step through the server code.

Tomcat Server Configuration
1. Install Tomcat to your dev host if required
2. In Eclispe in the Servers view, add a new Apache Tomcat v7.0 Server configuration
3. Add restsql and restsql-sdk projects to it
4. Change published location from Eclipse's .metadata to your Tomcat 7 location's wtpwebapps
   (starting Tomcat from .metadata doesn't appear to work)
    * Right-click on the Server and view the General Properties
    * Click on Switch Location. It should change to /Servers/Tomcat v7.0 Server at localhost.server
    * Click Ok
    * Double click on /Servers/Tomcat v7.0 Server at localhost.server
    * Select Use Tomcat Installation (takes control of Tomcat installation). Deploy location says wtpwebapps.
5. Optionally add a Parameter to the context.xml for the server instance to facilitate switching between MySQL and PostgreSQL.
		<Parameter name="org.restsql.properties"
			value="/opt/restsql/code/restsql-test/src/resources/properties/restsql-postgresql.properties" override="false" />
   The default restsql properties will be /etc/opt/restsql.
   Uncomment the Parameter, publish and restart the server to start up with PostgreSQL.

   But you may prefer to use the default location, and copy files to it using restsql-test's ant build target prep-mysql and prep-pgsql.


Synchronizing Files
1. Right-click on the server instance in the Servers view
2. Select Publish

Starting the Server
1. Right-click on the server instance in the Servers view
2. Select Start

The console will show any stdout (System.out.printlns). You should see this:

    Loading restsql properties from /opt/restsql/code/restsql-test/src/resources/properties/restsql-mysql.properties

And then a few messages later:

	INFO: Instantiated the Application class org.restsql.service.RestSqlApplication

Stoping the Server
1. Right-click on the server instance in the Servers view
2. Select Stop

## Docker Builds
The restsql/docker project has three sub-projects:
* service (the core service)
* service-sdk (service + sdk)
* mysql-sakila (mysql + sakila + extensions)

Note: The mysql-sakila docker image is currently not building. The 5.7 minor release of mysql from apt-get repo, 5.7.23, does not start. If no updates are needed, you may simply create a new release tag off the old image, which used 5.17.17, and move the latest to it.


Both service and service-sdk have shell scripts, build.sh, to build the images. Both pull from restsql/build.properties for the version to use for war file name and for image tagging. service/build.sh expects restsql dist ant target to be run prior, and service-sdk/build.sh expects reststql-sdk dist ant target to be run as well.

To run the containers, use:
* `run-service[-sdk]-default.sh` (expects mysql, container-based /etc/opt/restsql, mapped /var/log/restsql)
* `run-service[-sdk]-mysql.sh` (expects mysql, mapped /etc/opt/restsql, mapped /var/log/restsql)
* `run-service[-sdk]-pgsql.sh ip-address` (expects pgsql running on ip-address, mapped /etc/opt/restsql, mapped /var/log/restsql)

Use the restsql-test ant build.xml prep-mysql and prep-pgsql for assistance with config files.

Note: The mapped host volumes are prefaced with /private, e.g. /private/var/log/restsql, for macOS interoperability.

# Functional Testing

All changes must be passed through restsql-test. Every new feature needs some form of coverage to prove correctness in the release it is introduced, and in all future releases. restsql-test is also a regression test bed that very quickly indicates unintended side-effects of changes.

restsql-test has four of categories of test suites:
1. Examples: these are small Java programs that demo use of the public java api
2. API: these are JUnit-based tests that exercise the public java api
3. Service-Java: these are XML-based tests that exercise the framework via the service's non-public java interface
4. Service-HTTP: these are the same XML-based tests as Service/Java, except it interacts with the service via the public http interface

The ant file build.xml in restsql-test has multiple public targets to exercise the above. All names are prefixed with 'test-'. There are generally two sets for each, one for mysql and one for postgresql. The mysql targets are named without mysql, since it is the default, while the postgresql targets are postfixed with -pgsql. Both databases need to be up and running on the host name 'mysql' or 'postgresql'.

Be sure you are running across the board with the same jdk. As of this writing, it is tested with with jdk1.7.0_21. Java versions have differences that end up with subtle differences. For example running tomcat in java 8 and restsql-test in java 7 will fail many test cases with subtle string differences, seemingly related to xml rendering changes in java 8.

## Prep - Switching Databases
Before running the first test target, and then when switching between databases, run one of the prep-mysql or prep-pgsql. These copy the config files (restsql.properties, trigger.properties, trigger obj files, and sqlresources to /etc/opt/restsql). It hard copies these because sym links will not work in docker - they will point to a source that isn't volumed in. Also run the prep- targets if the sources change.

For API testing, you may change org.restsql.core.BaseTestCase's static block temporarily to:
    /resources/properties/restsql-postgresql.properties

But it is simpler to run the test-api-pgsql target.

## Prep - Running Web app
The examples, api and service-java tests can be run without a running webapp. They should be run with both databases.

For HTTP, the restsql webapp must be running on localhost:8080. It can be run by:
1. deploying to a tomcat instance
2. launched inside of Eclipse
3. launched in a docker container

## New Feature or Coverage Expansion Tests
At a minimum new features are exercised through the service-http suite. You may introduce another folder in src/resources/xml/testcase oriented and named with the feature or add them to an existing folder with a good intention-revealing name. Copy an existing test case file from the SingleTable folder and edit it. The names follow the form Test{Operation}_{SomeCondition}.xml.

Couple of design points to follow:
* Test cases are responsible for deleting any test fixtures (data) that it creates in the setup. This enables the test case to be run repeatedly without manual developer intervention
* Test cases may not delete default sakila data. This ensures other test cases do not fail as a side effect of running another case.
* Test cases may not interact with others or be dependent on other test case fixtures, i.e. test cases must be able to run in any order, but themselves or in an arbitrary sequence.

You may target running of one by name, several arbitrary by name, or all contained in one or more folders by running test-xxx-subset[-pgsql]. Edit the _tests.txt filet to set the scope.

## Security Testing
By default, restSQL ships with disabled encyrption, authentication and authorization. The test harness does not support encryption however authentication and authorization may be enabled.

Security tests are generally not run in a release unless the developer judges a likely impact. The support is manual in the test infrastructure.

Testing with SQL resource authorization via the Java API is enabled by
1. Manually editing the src/resources/properties/restsql-mysql.properties, uncommenting this line:
    `security.privileges=/etc/opt/restsql/privileges.properties`
2. Run the prep-mysql target
3. Copy the src/resources/properties/privileges.properties to /etc/opt/restsql
4. Run the test-api-security target

All should pass.


Testing SQL and Administrative resource authentication and authorization via the HTTP API is enabled by:
1. Uncomment security.privileges property in the restsql properties file
2. Run the prep-mysql target
3. Copy the src/resources/properties/privileges.properties to /etc/opt/restsql
4. build restsql and copy obj/bin/war contents to $TOMCAT_HOME/webapps/reststql
5. Uncomment the security-roles, security-constraints and login-config elements in the restsql webapp's deployment descriptor (restsql/WebContent/WEB-INF/web.xml)
6. Add the following entries to server's tomcat-users.xml:
        <tomcat-users>
            <role rolename="all"/>
            <role rolename="limited"/>
            <role rolename="readonly"/>
            <role rolename="admin"/>
            <user username="all" password="all" roles="all"/>
            <user username="limited" password="limited" roles="limited"/>
            <user username="readonly" password="readonly" roles="readonly"/>
            <user username="admin" password="admin" roles="admin"/>
        </tomcat-users>
6. Run the test-service-http-security (there is no test-service-java-security)

All should pass.

## App Server Testing
The framework is designed to run on JEE compliant app servers. It was tested on Tomcat, JBoss and Weblogic. See [Architecture](http://restsql.org/doc/Architecture.html) for the versions. Every release should be at least tested with Tomcat, and this is also the bundled server in the docker image. Use your judgement if JBoss and Weblogic may be impacted for your new feature.

## Automated Regression Testing
There's a lot of stuff to run before we can be confident in release quality.

In addition to examples, The webapp needs to be exercised in multiple ways, as it is the ultimate product. The web app should be deployed to at least tomcat and tested with test-service-http. The docker container should be exercised with both databases and test-service-http.

Note that test-service-http pgsql does exclude some test cases that are mysql specific, and test-service-java (both db variants) excludes the security tests.

The result summary appears in the console. You will find detail on failures in per test case files in obj/test/service or per test class files in obj/test/api.

There is an additional test-service-java-default-properties that runs a service-java test with the restsql embedded properties file that ships with the war, in src/resources/properties/restsql-default.properties. You can expect to see one error because it does not define any triggers:

    [java] Failures and Errors:
    [java]    SingleTable/TestInsert_SingleRow_ByBody_TriggerException.xml`

If the system is misconfigured, the 'golden state' required by the test cases may be fouled up. Revert to the default data by running the src/resources/database/{database}/create-sakila.sh or .bat.

To summarize, every release needs to pass these suite/scenarios:
* exec-examples
* test-api
* test-api-pgsql
* test-service-java
* test-service-java-pgsql
* test-service-java-default-properties (one trigger failure expected)
* test-service-http on tomcat 7
* test-service-http with docker
* test-service-pgsql with docker

Optionally it must pass
* test-api-security
* test-service-http-security

There are no failures (with the one exception noted above) as of this writing in any of the four suites on any database. Please help us keep it clean.

## Manual Regression Testing
The automation http api coverage is good, however the service console and the sdk need manual testing. A light smoke test of each of the top-level console links, as well as any new or changed sdk doc is due.

# Performance Testing
A new feature could warrant a load or endurance test. There are no included test jigs. Free, simple options are
* Apache Bench: a command line tool like curl. Use this if you just want to send one request repeatedly.
* JMeter: a more sophisticated tool from the Apache org with nice graphing.

# Documentation
If infrastructure requirements and/or tested dependencies change, e.g. a new database, update restsql-sdk/doc/Architecture#Requirements and this document's Developer/Requirements.

You may need to update the deployment guides (restql/README.txt and restsql-sdk/doc/Deployment.html) if any changes impact deployment.

You may need to document your feature in the sdk. For example a new REST operation needs a new doc in restsql-sdk/doc/api and a reference from the index.html. If appropriate, a mention in the doc/Concepts.html may be warranted.

Add yourself to the CONTRIBUTORS.txt in each project.

# Pull Request
After making your changes to the framework, optionally the sdk if the feature has user impact and adding tests for coverage, you will submit a pull request to the project leads. You may be asked to make changes to comply with conventions, increase test coverage, etc., and then resubmit. When accepted, it may be released immediately or gathered later into a release.

# Release Procedure
The following procedure documents the how leads get a make a release public. The phases are:
1. documentation
2. build and smoke test
3. github updates
3. website deployment
4. community notification

## Documentation
* Check if the year range needs updates in LICENSE.txt in all projects.
* Check if new contributors need to be added to CONTRIBUTORS.txt.
* Update this document and the deployment guides (restql/README.txt and restsql-sdk/doc/Deployment.html) if necessary.
* Create a release overview in restsql-sdk/doc/release. Add a reference in doc/ReleaseHistory.html. Replace the recent news on the landing page doc/Overview.html and reference the release doc.

## Build and Test
* Update release version in restsql/build.properties, restsql-sdk/build.properties and restsql-website/build.properties.
* Build restsql and restsql-sdk (dist targets) and the three docker containers (build.sh).
* Run all functional tests and appropriate database, app server and docker variants

Note: The mysql-sakila docker image is currently not building. The 5.7 minor release of mysql from apt-get repo, 5.7.23, does not start. If no updates are needed, you may simply create a new release tag off the old image, which used 5.17.17, and move the latest to it.

## Github Updates
Copy the restsql/obj/lib and restsql-sdk/obj/lib outputs to dist-archive, totaling five files.

Check in, commit, push and tag all six projects. There is a single branch, master, in each of the six projects. Feature branches should be merged and deleted after use.

A new release is tagged in each of the five non archive branches. dist-archive is not tagged. All six projects are updated and the five non-archive are tagged together for a release.

## Website Deployment
1. Save the docker images as tars, then compress them with gzip and copy them to the target host.
2. There are non-public files that are shared with project leads. These need to be added to restsql-website:
    * WebContent/usr/local/tomcat/webapps/restsql-sdk/google-xxx.html   (for google site verification)
	* WebContent/usr/local/tomcat/conf/tomcat-users.xml   (tomcat admin user for protected areas)
2. Build restsql-website (dist target). This creates two tar balls in restsql-webiste/obj:
    * system.tar (archive of all files in /system)
    * WebContent.tar (compressed archive of all modified and new files to be added to the service-sdk container)
    * deploy-WebContent.sh (to deploy the WebContent.tar)
3. Copy these two target host.
4. Shell into the host. Expand the system.tar as root or sudo from the root directory. Make sure the /root/docker files are executable to root.
5. Load the images into the docker engine.
6. Start the mysql container, then the service-sdk container using the /root/docker scripts.
7. cd to the directory containing WebContent.tar and run deploy-WebContent.sh. This will docker cp the content into the running container and restart it.
8. Validate

## Docker Hub Updates
The README.md files must be updated with the tag list. The hub account owner can update the full descriptions for all three images on the [(]hub website](https://hub.docker.com/r/restsql/) with the new content manually.

The hub account owner will push new images to the central repo.


## Community Notification
The restSQL mailchimp account admin will send notification to the release notification list.

# Support
Use the issues forum on github (http://github.com/restsql/restsql/issues) or email the support@restsql.org.
