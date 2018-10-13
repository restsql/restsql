README.txt (24-Mar-2017)

restSQL Deployment Guide

Project website is at http://restsql.org. Distributions at http://restsql.org/dist. Release history at http://restsql.org/doc/ReleaseHistory.html. Source code hosted at http://github.com/restsql.

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
    
restSQL docker images:
	1. restsql/service - service and core framework
	2. restsql/service-sdk - service and core framework + sdk
	3. restsql/mysql-sakila - MySQL 5.7 + Sakila + restsql-test extensions

-------------------------------------------------------------------------------
Versions

The restsql and restsql-sdk versions are found in the jar and war's META-INF/MANIFEST.MF. It is also found in the the source tree in restsql/build.properties and restsql-sdk/build.properties in the property build.version. 


-------------------------------------------------------------------------------
Deployment Modes

restSQL may be deployed in two modes:
	1. Docker - containerized web application
		Clients use an HTTP service (docker container) directly

    2. WAR - web application
        Clients use an HTTP service (web app) directly
        
    3. JAR - java library
        Clients use your service which uses the restSQL Java API


-------------------------------------------------------------------------------
Configuring restSQL

restSQL uses two configuration files in both WAR and JAR modes:
    1. restsql.properties           (general framework settings)
    2. log4j.properties             (logging settings)

The files may be called anything you wish; these are only the suggested names.

The general restsql.properties is set through a System Property, "org.restsql.properties". The value is an absolute path to your properties file, e.g. /etc/opt/restsql/restsql.properties. The WAR mode should use a context-param in the web.xml to set this (See Installation section later for details). The JAR mode will default to default-restsql.properties (source location: restsql/src/resources/properties) that is included in the jar.

Note: All path separators must use the forward slash, even on Windows. To refer to a path on Windows, for example c:\tools\restsql, use the form /tools/restsql, or if the app server is on a different drive, use file:///c:/tools/restsql.

The general restsql.properties contains the following configurations:
    1. Logging                              (required)
    2. SQL Resource definition location     (required)
    3. Security								(optional)
    4. Triggers                             (optional)
    5. XML                                  (optional)
    6. HTTP									(optional)
    7. Database                             (required)
    8. Implementation classes               (optional)
    9. Monitoring							(optional)

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
	sqlresources.dir=/etc/opt/restsql/sqlresources

The Security configuration is optional. Here is an example:
	# security.privileges=/absolute/path
	security.privileges=/etc/opt/restsql/privileges.properties

The Triggers configuration is optional. Here is an example:

	# triggers.classpath=/absolute/path
	# triggers.definition=/absolute/path
	triggers.classpath=/etc/opt/restsql/triggers
	triggers.definition=/etc/opt/restsql/triggers.properties

The XML configuration is optional. The defaults are:

    # request.useXmlSchema=[true, false]
    # response.useXmlDirective=[true, false]
    # response.useXmlSchema=[true, false]w
    request.useXmlSchema=false
    response.useXmlSchema=false
    response.useXmlDirective=false

The default settings indicate that request documents may be sent without schema references. Likewise response documents are sent without the xml directive (<?xml version="1.0" encoding="UTF-8"?>) and without schema references. 

The HTTP configuration is optional. The defaults are:
	# http.response.cacheControl={cache-directive}, {cache-directive}, ...
	http.response.cacheControl=no-cache, no-transform

Database configuration is required. Here is an example for a database with built-in support:

	# database.driverClassName=x.x.x
	#	for MySQL use com.mysql.jdbc.Driver
	#    for PostgreSQL use org.postgresql.Driver
	# database.url=jdbc:etc:etc
	#    for MySQL use jdbc:mysql://hostname:3306/
	#    for PostgreSQL use jdbc:postgresql://hostname:5432/{database-name}
	# database.user=userName
	# database.password=password
	database.driverClassName=com.mysql.jdbc.Driver
	database.url=jdbc:mysql://localhost:3306/
	database.user=restsql
	database.password=Rest00sql#

	# DB-specific implementation classes - match the implementation to your database
	# For MySQL:
	# 	org.restsql.core.ColumnMetaData=org.restsql.core.impl.mysql.MySqlColumnMetaData
	#	org.restsql.core.SequenceManager=org.restsql.core.impl.mysql.MySqlSequenceManager
	#	org.restsql.core.SqlResourceMetaData=org.restsql.core.impl.mysql.MySqlSqlResourceMetaData
	#	org.restsql.core.SqlBuilder=org.restsql.core.impl.mysql.MySqlSqlBuilder
	#	org.restsql.tools.ResourceDefinitionGenerator=org.restsql.tools.impl.mysql.MySqlResourceDefinitionGenerator
	# For PostgreSQL:
	# 	org.restsql.core.ColumnMetaData=org.restsql.core.impl.ColumnMetaDataImpl
	#	org.restsql.core.SequenceManager=org.restsql.core.impl.postgresql.PostgreSqlSequenceManager
	#	org.restsql.core.SqlResourceMetaData=org.restsql.core.impl.postgresql.PostgreSqlSqlResourceMetaData
	#	org.restsql.core.SqlBuilder=org.restsql.core.impl.postgresql.PostgreSqlSqlBuilder
	#	org.restsql.tools.ResourceDefinitionGenerator=org.restsql.tools.impl.postgresql.PostgreSqlResourceDefinitionGenerator
	org.restsql.core.ColumnMetaData=org.restsql.core.impl.mysql.MySqlColumnMetaData
	org.restsql.core.SequenceManager=org.restsql.core.impl.mysql.MySqlSequenceManager
	org.restsql.core.SqlResourceMetaData=org.restsql.core.impl.mysql.MySqlSqlResourceMetaData
	org.restsql.core.SqlBuilder=org.restsql.core.impl.mysql.MySqlSqlBuilder
	org.restsql.tools.ResourceDefinitionGenerator=org.restsql.tools.impl.mysql.MySqlResourceDefinitionGenerator

Implementation classes configuration is optional. The defaults are:

	# Implementation classes - use these to customize the framework
	# org.restsql.core.Factory.ConnectionFactory=fully.qualified.class.name
	# org.restsql.core.Factory.RequestFactory=fully.qualified.class.name
	# org.restsql.core.Factory.RequestDeserializerFactory=fully.qualified.class.name
	# org.restsql.core.Factory.ResponseSerializerFactory=fully.qualified.class.name
	# org.restsql.core.Factory.SqlResourceFactory=fully.qualified.class.name
	# org.restsql.core.HttpRequestAttributes=fully.qualified.class.name
	# org.restsql.core.RequestLogger=fully.qualified.class.name
	# org.restsql.core.TableMetaData=fully.qualified.class.name
	# org.restsql.security.Authorizer=fully.qualified.class.name
	org.restsql.core.Factory.ConnectionFactory=org.restsql.core.impl.ConnectionFactoryImpl
	org.restsql.core.Factory.RequestFactory=org.restsql.core.impl.RequestFactoryImpl
	org.restsql.core.Factory.RequestDeserializerFactory=org.restsql.core.impl.serial.RequestDeserializerFactoryImpl
	org.restsql.core.Factory.ResponseSerializerFactory=org.restsql.core.impl.serial.ResponseSerializerFactoryImpl
	org.restsql.core.Factory.SqlResourceFactory=org.restsql.core.impl.SqlResourceFactoryImpl
	org.restsql.core.HttpRequestAttributes=org.restsql.core.impl.HttpRequestAttributesImpl
	org.restsql.core.RequestLogger=org.restsql.core.impl.RequestLoggerImpl
	org.restsql.core.TableMetaData=org.restsql.core.impl.TableMetaDataImpl
	org.restsql.security.Authorizer=org.restsql.security.impl.AuthorizerImpl
	org.restsql.service.monitoring.MonitoringManager=org.restsql.service.monitoring.MonitoringManagerImpl

Monitoring configuration is optional:

	# Ganglia monitoring configuration
	monitoring.ganglia.host=hostName or ipAddress
	monitoring.ganglia.port=portNumber
	monitoring.ganglia.ttl=numberOfRouterHops
	monitoring.ganglia.udpMode=[unicast,multicast]
	monitoring.ganglia.frequency=seconds
	
	# Graphite monitoring configuration
	monitoring.graphite.host=hostName or ipAddress
	monitoring.graphite.port=portNumber
	monitoring.graphite.frequency=seconds

See the SDK for more detail on Logging, Security, Trigger and HTTP configuration.

Access http://yourhost:port/restsql for links to the effective runtime configuration.


-------------------------------------------------------------------------------
Installing restSQL Docker mode

See docker hub documentation at https://hub.docker.com/r/restsql/service/ or https://hub.docker.com/r/restsql/service-sdk/. 


-------------------------------------------------------------------------------
Installing restSQL WAR mode

Requirements: JEE Container, RDBMS, JAR tool

Properties Files: Create your two required properties files (restsql.properties and log4j.properties (or logging.properties), as above. Create your two optional privileges and triggers definitions if required. The restsql.properties can exist outside the restSQL webapp, however the log4j.properties/logging.properties must exist within the classpath in WEB-INF/classes. Note that it will not load properly if you put the logging properties in WEB-INF/lib. You do not have to create the logging directory or directories, e.g. /var/log/restsql. The logging frameworks will do this automatically, assuming the user running the java container can create the folder. In the previous case that would be write privilege on /var/log. Better to create it by hand and ensure it's writable to the user running the java container.

Abbreviated Deployment for Tomcat:
You can use this shortcut if you are using Tomcat, do not want restSQL Authentication or Authorization and Java Security Manager is disabled (the default for Tomcat). Add a Parameter entry that indicates your absolute path to your restsql.properties in your $TOMCAT_HOME/conf/context.xml, as in:
	
	<Parameter name="org.restsql.properties" value="/etc/opt/restsql/restsql.properties" override="false" />

That will override the default properties location in the web.xml of the WAR.

Place the unmodified WAR in the $TOMCAT/webapps directory and bounce the server, or deploy the webapp using your favorite method.

Complete Deployment:
The restsql-{version}.war contains the service and framework classes as well as dependencies. Extract it's contents to some temp area, e.g. /tmp/restsql. Use the standard jar tool that comes with your JRE/JDK. The command is jar -xf war-file-name. It extracts all contents in the current directory. The contents looks like:
    restsql/
        META-INF/
        wadl/
        WEB-INF/
        index.html

web.xml: Change the restSQL WEB-INF/web.xml. The LifecycleManager needs to know where to load your restsql.properties. Here's an example:

    <context-param>
        <param-name>org.restsql.properties</param-name>
        <param-value>/etc/opt/restsql/restsql.properties</param-value>
    </context-param>

The default deployment descriptor (web.xml) contains login config (authentication method) and security constraints (authorization declarations). See the restSQL SDK's /restsql-sdk/default/xml/web.xml for the default deployment descriptor. 

Disabling Authentication and Authorization: To disable authentication/authorization, simply remove or comment out the security-constraint and login-config elements in the web.xml. Security is disabled by default.

Enabling Authentication and Authorization: You may use the default security constraints and login config or change it to conform to your specific roles, realm and other requirements. More information on Web Application Security using deployment descriptors is available at http://java.sun.com/javaee/6/docs/tutorial/doc/bncbe.html. Or consult your container's documentation. Authentication mechanisms (credential management, user to role assigment) are typically container-specific/proprietary. You will also need to configure a privileges properties file and reference it in the restsql properties file. See the SDK's Security configuration for instructions. 

Naming: You may deploy this as a single file or exploded war to your JEE container. Rename it to restsql.war or webapps/restsql if you want the path to be http://yourhost:port/restsql. Containers generally use the war file name instead of the web.xml's id to name the web app. Additionally, the SDK's HTTP API Explorer will work without any customization.

Deploy: Copy your exploded war or war to your container's webapps dir and restart the container, or deploy the webapp in your preferred style. All third party dependencies are included in the war distribution in the WEB-INF/lib.

Java Security Manager: If Java Security is enabled in your container, permissions must be added to your container's policy file. restSQL requires:
	* read/write access (java.util.PropertyPermission) to the following system properties: org.restsql.properties, org.apache.commons.logging.Log and either log4j.configuration for log4j logging or java.util.logging.config.file for Java Native logging
	* read access (java.io.FilePermission) to the various properties files, and SQL Resources and triggers directories

However, restsql uses other libraries (jersey, jdbc, logging) which need some unknown combination of access permissions. The only configuration that has been demonstrated to work is to grant all permissions to restsql. For example for Tomcat, add this to the end of the ${TOMCAT_HOME}/conf/catalina.policy file:

	grant codeBase "file:${catalina.base}/webapps/restsql/-" {
	    permission java.security.AllPermission;
	};

Note: If you receive a 500 response to any res query with the text "No suitable driver found", then the container cannot find your jdbc driver. This can usually be fixed by placing the database driver in some common server library location. This also occurs after deploying the restSQL.war to WebLogic using the console when the container is running. After a container restart, the driver is found.

-------------------------------------------------------------------------------
Installing restSQL JAR mode

Properties: Follow the instructions for Configuring restSQL.

Deploy: Copy jar to the classpath of your web app, e.g. WEB-INF/lib. The following third party dependencies also need to be in your classpath:
	* commons-lang.jar - tested with version 2.6
	* commons-logging.jar  (tested with version 1.1.1)
	* json_simple - tested with version 1.1
	* log4j.jar (tested with 1.2.16)

log4j is not necessary if your app uses Java Native Logging. restSQL has been tested with JRE 1.6 and Java Native Logging.

Additionally one of the following jdbc drivers is necessary for databases with built-in support:
	* mysql-connector-java-#.jar (tested with MySQL version 5.7)
	* postgresql-#.jdbc4.jar (tested with PostgreSQL version 10.5)

Enabling Authentication and Authorization: restSQL will authorize SQL Resource operations. Your app will authenticate users and associate users with roles. You must provide a priviliges properties file and reference it in the restsql.properties. Your app will call restSQL's Authorizer and provide a SecurityContextimplementation. See the SDK's Security configuration for more instructions.


-------------------------------------------------------------------------------
Resource Definition Generation Tool
The tool creates resource definition templates for all columns for each table in a database schema. The definitions are created in the user-provided subfolder. These resources can be immediately used, or modified and reloaded and moved as necessary.

The tool is accessed after deploying restsql from the Tools link on the home page, or from http://host:port/restsql/tools. Enter the subfolder name (default is 'auto') and the database name. The definitions are created in the configured SQL Resources directory (in the restsql.properties file). These are viewable from the SQL Resources browser, i.e. http://host:port/restsql/res.



-------------------------------------------------------------------------------
License

restSQL is licensed under the standard MIT license. Refer to the LICENSE.txt and CONTRIBUTORS.txt in the distribution. 