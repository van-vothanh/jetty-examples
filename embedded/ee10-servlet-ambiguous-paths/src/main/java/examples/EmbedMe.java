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

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class EmbedMe
{
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

        HttpConfiguration httpConfig = new HttpConfiguration();
        UriCompliance uriCompliance = UriCompliance.LEGACY.with("custom", UriCompliance.Violation.SUSPICIOUS_PATH_CHARACTERS);
        httpConfig.setUriCompliance(uriCompliance);

        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);
        ServerConnector connector = new ServerConnector(server, httpConnectionFactory);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        // Allow ServletContext to accept ambiguous uris.
        context.getServletHandler().setDecodeAmbiguousURIs(true);
        context.addServlet(AmbiguousPathServlet.class, "/*");

        server.setHandler(context);
        //server.setDumpAfterStart(true);
        return server;
    }
}
