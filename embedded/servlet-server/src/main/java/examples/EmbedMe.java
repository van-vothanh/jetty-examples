//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package examples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbedMe
{
    private static final Logger LOG = LoggerFactory.getLogger(EmbedMe.class);

    public static void main(String[] args) throws Exception
    {
        int port = 8080;
        Server server = newServer(port);
        server.start();
        server.join();
    }

    public static Server newServer(int port)
    {
        Server server = new Server();

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        // httpConfiguration.setUriCompliance(UriCompliance.LEGACY);
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);
        ServerConnector connector = new ServerConnector(server, httpConnectionFactory);
        connector.setPort(port);
        server.addConnector(connector);

        WebAppContext context = new WebAppContext();
        configureLocations(context);
        context.setContextPath("/");
        context.setWelcomeFiles(new String[]{"index.html", "welcome.html"});
        context.setParentLoaderPriority(false);
        server.setHandler(context);
        return server;
    }

    private static void configureLocations(WebAppContext context)
    {
        // Look for resource in common file system paths
        try
        {
            Path pwd = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            Path srcWebapp = pwd.resolve("src/main/webapp/");
            if (Files.exists(srcWebapp))
            {
                LOG.info("WebResourceBase (Using /src/main/webapp/ Path) {}", srcWebapp);
                context.setBaseResource(Resource.newResource(srcWebapp));
            }

            Path targetClasses = pwd.resolve("target/classes");
            if (Files.isDirectory(targetClasses))
            {
                context.setExtraClasspath(List.of(Resource.newResource(targetClasses)));
            }
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Unable to find web resource in file system", t);
        }

        if (context.getBaseResource() == null)
        {
            throw new RuntimeException("Unable to find Base Resource (src/main/webapp)");
        }

        if (context.getExtraClasspath() == null)
        {
            throw new RuntimeException("Unable to find Webapp Classpath (target/classes)");
        }
    }
}
