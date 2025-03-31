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

import java.io.FileNotFoundException;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.Resources;

/**
 * Using a {@link ServletContextHandler} serve static file content from single location
 */
public class ServletFileServerSingleLocation
{
    public static void main(String[] args) throws Exception
    {
        Server server = ServletFileServerSingleLocation.newServer(8080);
        server.start();
        server.join();
    }

    public static Server newServer(int port) throws FileNotFoundException
    {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setWelcomeFiles(new String[]{"index.html", "index.htm", "foo.htm"});
        ResourceFactory resourceFactory = ResourceFactory.of(context);
        Resource baseResource = resourceFactory.newClassLoaderResource("static-root");
        if (!Resources.isReadableDirectory(baseResource))
            throw new FileNotFoundException("Unable to find base-resource for [static-root]");
        context.setBaseResource(baseResource);
        server.setHandler(context);

        // Default behavior
        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
        holderPwd.setInitParameter("dirAllowed", "true");
        context.addServlet(holderPwd, "/");

        return server;
    }
}
