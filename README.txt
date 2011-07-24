README.txt (23-Jul-2011)

restSQL Deployment Guide

Project website is at http://restsql.org. Distributions at http://restsql.org/dist. Source code hosted at http://github.com/restsql.

-------------------------------------------------------------------------------
Structure and Distributions

restSQL source code is contained in three eclipse projects:
    1. restsql                      (service and core framework)
    2. restsql-sdk                  (documentation, HTTP API explorer, javadoc, examples)
    3. restsql-test                 (test framework, test cases)

restSQL binary distributions contain three libraries:
    1. restsql-{version}.jar        (core framework only)
    2. restsql-{version}.war        (service and core framework, 3rd party dependencies)
    3. restsql-sdk-{version}.war    {sdk)

restSQL source distributions consist of one jar:
    1. restsql-{version}-src.jar    (service and core framework)
    

-------------------------------------------------------------------------------
Versions

The restsql and restsql-sdk versions are found in the jar and war's META-INF/MANIFEST.MF. It is also found in the the source tree in restsql/build.properties and restsql-sdk/build.properties in the property build.version. 


-------------------------------------------------------------------------------
Deployment Modes

restSQL may be deployed in two modes:
    1. WAR - web application
        Clients use an HTTP service (web app) directly
        
    2. JAR - java library
        Clients use your service which uses the restSQL Java API


-------------------------------------------------------------------------------
Configuring restSQL

restSQL uses two configuration files in both WAR and JAR modes:
    1. restsql.properties           (general framework settings)
    2. log4j.properties             (logging settings)

The files may be called anything you wish; these are only the suggested names.

The general restsql.properties is set through a System Property, "org.restsql.properties". The value is an absolute path to your properties file, e.g. /etc/opt/business/restsql/restsql.properties. The WAR mode should use a context-param in the web.xml to set this (See Installation section later for details). The JAR mode will default to default-restsql.properties (source location: restsql/src/resources/properties) that is included in the jar.

The general restsql.properties contains the following configurations:
    1. Logging                              (required)
    2. SQL Resource definition location     (required)
    3. Triggers                             (optional)
    4. XML                                  (optional)
    5. Database                             (required)
    6. Implementation classes               (optional)

Logging configuration example:

	# logging.facility=[log4j,java]
	# logging.config=relative/to/classpath
	# logging.dir=/absolute/path  - this is only used by the /log service method to find logs
	logging.facility=log4j
	logging.config=com/business/config/log4j.properties
	logging.dir=/var/log/restsql
 
Note that unlike all other locations, the logging.config location is RELATIVE to the classpath, not absolute. The default logging framework is log4j, using the default-log4j.properties included in the jar/war. This logs to the location /var/log/restsql. The logging levels are set for development, not production mode.

The location of SQL Resource definitions is critical. An example:

	# sqlresources.dir=/absolute/path
	sqlresources.dir=/opt/business/sqlresources

The XML configuration is optional. The defaults are:

    # request.useXmlSchema=[true, false]
    # response.useXmlDirective=[true, false]
    # response.useXmlSchema=[true, false]w
    request.useXmlSchema=false
    response.useXmlSchema=false
    response.useXmlDirective=false

The default settings indicate that request documents may be sent without schema references. Likewise response documents are sent without the xml directive (<?xml version="1.0" encoding="UTF-8"?>) and without schema references. 

The Triggers configuration is optional. Here is an example:

	# triggers.classpath=/absolute/path
	# triggers.definition=/absolute/path
	triggers.classpath=/opt/business/triggers
	triggers.definition=/opt/business/triggers.properties

Database configuration is required. Here is an example for a database with built-in support:

	# database.url=jdbc:etc:etc
	# database.user=userName
	# database.password=password
	database.url=jdbc:mysql://localhost:3306/
	database.user=restsql
	database.password=Rest00sql#
	
	# MetaData implemenation class - match the implementation to your database
	# For MySQL:
	#	org.restsql.core.SqlResourceMetaData=org.restsql.core.impl.SqlResourceMetaDataMySql
	# For PostgreSQL:
	#	org.restsql.core.SqlResourceMetaData=org.restsql.core.impl.SqlResourceMetaDataPostgreSql
	org.restsql.core.SqlResourceMetaData=com.business.restsql.SqlResourceMetaDataMySql

Implementation classes configuration is optional. The defaults are:

	# Implementation classes - use these to customize the framework
	# org.restsql.core.SqlBuilder=full.qualified.class.name
	# org.restsql.core.Factory.ConnectionFactory=fully.qualified.class.name
	# org.restsql.core.Factory.RequestFactory=fully.qualified.class.name
	# org.restsql.core.Factory.RequestLoggerFactory=fully.qualified.class.name
	# org.restsql.core.Factory.SqlResourceFactory=fully.qualified.class.name
	org.restsql.core.SqlBuilder=org.restsql.core.impl.SqlBuilderImpl
	org.restsql.core.Factory.ConnectionFactory=org.restsql.core.impl.ConnectionFactoryImpl
	org.restsql.core.Factory.RequestFactory=org.restsql.core.impl.RequestFactoryImpl
	org.restsql.core.Factory.RequestLoggerFactory=org.restsql.core.impl.RequestLoggerFactoryImpl
	org.restsql.core.Factory.SqlResourceFactory=org.restsql.core.impl.SqlResourceFactoryImpl

See the SDK for more detail on Logging and Trigger configuration.

Access http://yourhost:port/restsql for links to the effective runtime configuration.


-------------------------------------------------------------------------------
Installing restSQL WAR mode

Requirements: JEE Container, JAR tool, RDBMS

The restsql-{version}.war contains the service and framework classes as well as dependencies. Extract it's contents to some temp area, e.g. /tmp/restsql. Use the standard jar tool that comes with your JRE/JDK. The command is jar -xf war-file-name. It extracts all contents in the current directory. The contents looks like:
    restsql/
        META-INF/
        wadl/
        WEB-INF/
        index.html

Properties Files: Create your restsql.properties and log4j.properties (or logging.properties) as above. The restsql.properties can exist outside the restSQL webapp, however the log4j.properties/logging.properties must exist within the classpath in WEB-INF/classes. Note that it will not load properly if you put the logging properties in WEB-INF/lib. You do not have to create the logging directory or directories, e.g. /var/log/restsql. The logging frameworks will do this automatically.

web.xml: Change the restSQL WEB-INF/web.xml. The LifecycleManager needs to know where to load your restsql.properties. Here's an example:

    <context-param>
        <param-name>org.restsql.properties</param-name>
        <param-value>/etc/opt/business/restsql/restsql.properties</param-value>
    </context-param>

Naming: You may deploy this as a single file or exploded war to your JEE container. Rename it to restsql.war or webapps/restsql if you want the path to be http://yourhost:port/restsql. Containers generally use the war file name instead of the web.xml's id to name the web app. Additionally, the SDK's HTTP API Explorer will work without any customization.

Deploy: Copy your exploded war or war to your container's webapps dir and restart the container, or deploy the webapp in your preferred style. All third party dependencies are included in the war distribution in the WEB-INF/lib.

Container Specific Issues:
	JBoss - The jdbc library will not load from restsql's WAR file. It must be placed in a server lib. In JBoss Enterprise Web Platform 5.1, the only location that works is jboss-as-web/common/lib. If that does not work on your JBoss products/version variant, try server/lib or server/default/lib. If you receive a 500 response to any res query with the text "No suitable driver found", then JBoss cannot find your jdbc driver. The very first request on EAP 5.1 will fail with the previous message but all subsequent queries will succeed.


-------------------------------------------------------------------------------
Installing restSQL JAR mode

Properties: Follow the instructions for WAR mode.

Deploy: Copy jar to the classpath of your web app, e.g. WEB-INF/lib. The following third party dependencies also need to be in your classpath:
	* commons-logging.jar  (tested with version 1.1.1)
	* log4j.jar (tested with 1.2.16)

log4j may be excluded if you are using Java native logging.

Additionally one of the following jdbc drivers is necessary for databases with built-in support:
	* mysql-connector-java.jar (tested with MySQL version 5.5)
	* postgresql-9.0-801.jdbc4.jar (tested with PostgreSQL version 9.0)


-------------------------------------------------------------------------------
License

restSQL is licensed under the standard MIT license. Refer to the LICENSE.txt and CONTRIBUTORS.txt in the distribution. 