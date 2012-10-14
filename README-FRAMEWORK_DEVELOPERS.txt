README_FRAMEWORK_DEVELOPERS.txt (23-Sep-2012)

restSQL Framework Developer Guide

Project website is at http://restsql.org. Distributions at http://restsql.org/dist. Release history at http://restsql.org/doc/ReleaseHistory.html. Source code hosted at http://github.com/restsql.

-------------------------------------------------------------------------------
Prerequisites

* JDK 1.7
* Eclipse Helios SR2, but other versions probably work fine
* Ant 1.7.1, but newer versions probably work find
* Tomcat 7
* MySQL Server 5.5
* PostgreSQL 9.1
* Windows 7, but any OS should do


-------------------------------------------------------------------------------
Source Code 

restSQL source code is contained in three Eclipse projects available from github:
    1. restsql                      (service and core framework)
    2. restsql-sdk                  (documentation, HTTP API explorer, javadoc, examples)
    3. restsql-test                 (test framework, test cases)

In contributing to restSQL, you agree to release the code under the MIT license and copyright it to the restSQL project contributors. You must add your name to the CONTRIBUTORS.txt in the three projects. Each file must be prefaced with the following:
	
	/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */

(An Eclipse code template profile is available for you to import so that it is automatically added to your new files.)

Your changes will be checked into your fork off the main trunk. Send a pull request to 'restsql' to request the maintainer to incorporate your fork into the main line. Please describe the features added or defect fixed, test cases added, documentation added and any unresolved issues.


-------------------------------------------------------------------------------
JDK Config

The three Eclipse projects are set to use the workspace's default JDK/JR. Start Eclipse with JDK 1.7 and this will be automatic. The .classpath reference is currently set to the following:

	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>

Please do not change it to reference a specific JDK release, e.g. jdk1.7.0_07.


-------------------------------------------------------------------------------
Design

Please have a look at the architecture diagrams before starting on any extensions. See restsql-sdk/WebContent/doc/Architecture.html.

restSQL was designed from the ground up with extension in mind. SOLID principles are in effect. Interfaces are used widely. A central factory loads implementation from the restsql.properties. See http://en.wikipedia.org/wiki/SOLID_(object-oriented_design). Dependency Inversion is an important one to note. Please code to interfaces, not implementation. This allows others to add capability without modifying the core (Open-Closed Principle).

The framework is roughly divided into core, security and service. Most extensions should be to the core, but there could be new HTTP Service capability.


-------------------------------------------------------------------------------
Coding Style

Import the following into your Eclipse workspace:

	restsql/src/resources/eclipse
		formatter.xml 		[Formatter Profile]
		codetemplates.xml	[Code Templates]
		cleanup.xml			[Java Clean Up Profile]

	Please use these regularly! Orderly code is a must.

Let's keep a clean house -- do not leave commented out, dead code around spoiling readability. We have git to help us find old code if we ever need it.

Follow Sun's Java naming conventions and Javadoc style guidelines. Package overviews and class descriptions are a must. Method javadoc is strongly recommended, excepting simple getter/setters.


-------------------------------------------------------------------------------
Directories and Configuration Files

Source Root
Set up your Eclipse projects in /restsql/eclipse-workspace if possible. restSQL configuration uses absolute paths in most cases. The property values and default web.xml will work out of the box if you use /restsql/eclipse-workspace (or C:\restsql\eclipse-workspace) as your root.

Log Root
Logs by default are written to /var/log/restsql. Make it easier on yourself and use this directory.

Configuration Files
The main configuration file (restsql.properties) location is configured using a Java System property named org.restsql.properties. In a JAR deployment or development/Eclipse environment, the property can be injected into the JRE or overriden in an early loaded static block. Or just use the default, /resources/properties/default-restsql.properties, which requires you to use the default dev root.

In a WAR deployment, the restsql.properties is loaded using the web.xml (or Parameter entry in Tomcat's context.xml). The default is:
	/restsql/eclipse-workspace/restsql-test/src/resources/properties/restsql-mysql.properties

This facilitates an easier dev/test cycle when using Tomcat execution within Eclipse. The restsql-test JUnit tests and Ant launched test suites use it as well.

There is also a postgresql config file for restsql-test. You may change org.restsql.core.BaseTestCase's static block temporarily to:
	/resources/properties/restsql-postgresql.properties

But it is generally simpler to launch ant from an Eclipse Ant config or command line and a property override, as in:
	ant -Dorg.restsql.properties=/resources/properties/restsql-postgresql.properties target

	(target = test-api, test-service-java, etc.)
	
restsql.properties has all of the properties that customize an installation to a database, file system and application. The deployment guide (SDK or README.txt) describes the properties. See org.restsql.core.Config to see all the possible properties and default values. Config creates the properties map from the properties file and sets up loggers. All restsql classes refer to property keys using the Config's public constants KEY_xxx and DEFAULT_xxx. Properties are located in Config.properties, an Immutable map. Properties must always be read with the default as backup, since the properties file may not have declared all of them. An example:

		String sqlResourcesDir = Config.properties.getProperty(Config.KEY_SQLRESOURCES_DIR, Config.DEFAULT_SQLRESOURCES_DIR);

Properties may be read once and cached. They are not expected to change during the JVM's lifetime.

If you would like to add a property, please add KEY and DEFAULT constants as well as the property in the default-restsql.properties along with a brief description.


-------------------------------------------------------------------------------
Builds

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
	 test-api                    exercises junit tests for java api
	 test-api-report             generates junit report for api tests
	 test-api-security           exercises security junit tests for java api
	 test-service-http           executes all test cases for the http interface
	 test-service-http-security  executes security test cases for the http interface
	 test-service-http-subset    executes some of test cases for the http interface
	 test-service-java           executes all test cases for the service's java interface, excluding security
	 test-service-java-subset    executes some test cases for the service's java interface
	Default target: test-all
	
	
	restsql-sdk Main targets:
	
	 clean          deletes output dir
	 dist           generates distribution files
	 exec-examples  executes example programs
	Default target: dist


The Ant view is rather handy in Eclipse. Drag the three build.xml files into the Ant view. This allows you to run any target without opening a command line. You can customize System properties as well.

Before creating any distribution, clean the output (obj) and then run the dist. This will ensure the contents are exactly what is required. 


-------------------------------------------------------------------------------
Working with Tomcat

Development is faster using Tomcat from Eclipse rather than standalone. It allows you to start and stop the server and synchronize files with simple gestures in the Servers view. And best of all, you can set breakpoints and step through the server code.

Tomcat Server Configuration
	1. Install Tomcat to your dev host if required
	2. In Eclispe in the Servers view, add a new Apache Tomcat v7.0 Server configuration
	3. Add restsql and restsql-sdk projects to it
	4. Change published location from Eclipse's .metadata to your Tomcat 7 location's wtpwebapps
	   (starting Tomcat from .metadata doesn't appear to work)
	   a. Right-click on the Server and view the General Properties
	   b. Click on Switch Location. It should change to /Servers/Tomcat v7.0 Server at localhost.server
	   c. Click Ok
	   d. Double click on /Servers/Tomcat v7.0 Server at localhost.server
	   e. Select Use Tomcat Installation (takes control of Tomcat installation). Deploy location says wtpwebapps.
	5. Add a Parameter to the context.xml for th server instance to facilitate switching between MySQL and PostgreSQL.
			<!-- 
			<Parameter name="org.restsql.properties"
				value="/restsql/eclipse-workspace/restsql-test/src/resources/properties/restsql-postgresql.properties" override="false" />
			 -->
	   The default restsql properties will be restsql-test/src/resources/properties/restsql-mysql.properties.
	   Uncomment the Parameter, publish and restart the server to start up with PostgreSQL. 
	   

Synchronizing Files
	 1. Right-click on the server instance in the Servers view
	 2. Select Publish
	 
Starting the Server
 	1. Right-click on the server instance in the Servers view
 	2. Select Start
 	
	The console will show any stdout (System.out.printlns). You should see this:
	
		Loading restsql properties from /restsql/eclipse-workspace/restsql-test/src/resources/properties/restsql-mysql.properties
		
	And then something close to this:
	
		Sep 22, 2012 4:18:49 PM com.sun.jersey.api.core.PackagesResourceConfig init
		INFO: Scanning for root resource and provider classes in the packages:
		  org.restsql.service
		Sep 22, 2012 4:18:49 PM com.sun.jersey.api.core.ScanningResourceConfig logClasses
		INFO: Root resource classes found:
		  class org.restsql.service.WadlResource
		  class org.restsql.service.ResResource
		  class org.restsql.service.ConfResource
		  class org.restsql.service.StatsResource
		  class org.restsql.service.LogResource
		Sep 22, 2012 4:18:49 PM com.sun.jersey.api.core.ScanningResourceConfig init
		INFO: No provider classes found.
	
Stoping the Server
 	1. Right-click on the server instance in the Servers view
 	2. Select Stop


-------------------------------------------------------------------------------
Logging

Have a look at the Logging overview in the SDK first: WebContent/doc/Logging.html.

Log4j is preferred and is used by default. The default logging properties for both the framework and the test context is in restsql/srsc/resources/properties/default-log4j.properties. You can temporarily edit this file or else create your own and make sure your restsql.properties refers to it (Remember this is the only location that is relative!).

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


-------------------------------------------------------------------------------
Testing

Have a look at the README in the restsql-test project first. 

Since this is supposed to be rock-solid infrastructure code, it is essential that:
	* new feature code is delivered with comprehensive (postive and negative) tests
	* for all changes (enhancements and fixes) 
		* the entire test suite is run on both MySQL and PostgreSQL
			- 3 parts: test-api, test-service-java and test-service-http
		* standalone deployment to Tomcat (not through Eclipse)
		* deployment to one other JEE container for any HTTP-related changes
		
API tests are straight JUnit tests. These are optional. 

Service test cases are not optional. Service-java is a subset of the service-http tests, except they are run without a container.

Service test cases are declared in XML. They include SQL for fixture setup, requests, expected responses and SQL for fixture teardown. The test case root is src/resources/xml/service/testcase and are put in a dozen category folders, and usually named with the resource or special feature it tests. To start from scratch, find the schema in the root and use Eclipse's Generate wizard. But it's usually simpler to copy and paste from an example.

You can run one, several or a group of tests using the test-service-java-subset or test-service-http-subset targets. The scope is driven by your text entries in a special file: src/resources/xml/service/testcase/_tests.txt.

You can run a subset in debug mode by creating a debug configuration for the class org.restsql.service.ServiceTestRunner with
	Program arguments: 	java src/resources/xml/service/testcase/_tests.txt none
	VM arguments:				-Dorg.restsql.properties=/resources/properties/restsql-mysql.properties

Launch the ServiceTestRunner in debug mode.

There is currently no built-in coverage analysis nor are there any performance/load test suites.


-------------------------------------------------------------------------------
Enabling Security

By default, restSQL ships with disabled encyrption, authentication and authorization. The test harness does not support encryption however authentication and authorization may be enabled.

Testing with SQL resource authorization via the Java API is enabled by uncommenting the security.privileges property in the restsql properties file.

Testing SQL and Administrative resource authentication and authorization via the HTTP API is enabled by:
	1. Uncomment security.privileges property in the restsql properties file
	2. Uncomment the security-roles, security-constraints and login-config elements in the restsql webapp's deployment descriptor (restsql/WebContent/WEB-INF/web.xml)
	3. Add the following entries to server's tomcat-users.xml:
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
	

-------------------------------------------------------------------------------
Support

Use the issues forum on github (http://github.com/restsql/restsql/issues) or email the project lead directly at mark.sawers@restsql.org.