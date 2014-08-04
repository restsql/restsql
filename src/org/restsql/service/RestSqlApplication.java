/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * Identifies JAX-RS resources. The Jersey scanner does not work with JBoss AS.
 * 
 * @author Mark Sawers
 */
public class RestSqlApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ConfResource.class);
        classes.add(LogResource.class);
        classes.add(ResResource.class);
        classes.add(StatsResource.class);
        classes.add(WadlResource.class);
        classes.add(ToolsResource.class);
        return classes;
    }

}
