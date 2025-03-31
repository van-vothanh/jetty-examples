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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ResourceServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Using a {@link ServletContextHandler} serve static file content from multiple locations.
 *
 * <p>
 *     You have 2 url-patterns that static content is served from.
 *     <ul>
 *         <li>{@code /*} - the root url-pattern, serving content from {@code static-root/} in classloader</li>
 *         <li>{@code /alt/*} - the url-pattern serving content from {@code webapps/alt-root/} in file system</li>
 *     </ul>
 * </p>
 */
public class ServletFileServerMultipleLocations
{
    private static final Logger LOG = LoggerFactory.getLogger(ServletFileServerMultipleLocations.class);

    public static void main(String[] args) throws Exception
    {
        Path altPath = Paths.get("webapps/alt-root").toRealPath();
        System.err.println("Alt Base Resource is " + altPath);

        Server server = ServletFileServerMultipleLocations.newServer(8080, altPath);
        server.start();
        server.join();
    }

    public static Server newServer(int port, Path altPath) throws FileNotFoundException
    {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ResourceFactory resourceFactory = ResourceFactory.of(context);
        Resource baseResource = resourceFactory.newClassLoaderResource("static-root");
        if (!Resources.isReadableDirectory(baseResource))
            throw new FileNotFoundException("Unable to find base-resource for [static-root]");
        context.setBaseResource(baseResource);
        context.setWelcomeFiles(new String[]{"index.html", "index.htm", "foo.htm"});
        server.setHandler(context);

        // add special pathspec of "/alt/" content mapped to the altPath
        ServletHolder holderAlt = new ServletHolder("static-alt", ResourceServlet.class);
        holderAlt.setInitParameter("baseResource", altPath.toUri().toASCIIString());
        holderAlt.setInitParameter("dirAllowed", "true");
        holderAlt.setInitParameter("pathInfoOnly", "true");
        context.addServlet(holderAlt, "/alt/*");

        // Lastly, the default servlet for root content (always needed, to satisfy servlet spec)
        // It is important that this is last.
        ServletHolder holderDef = new ServletHolder("default", DefaultServlet.class);
        holderDef.setInitParameter("dirAllowed", "true");
        context.addServlet(holderDef, "/");

        // Some filter to show that you can modify things on the response from a static resource.
        context.addFilter(new VaryFilter(), "/*", EnumSet.of(DispatcherType.REQUEST));

        return server;
    }

    public static class VaryFilter implements Filter
    {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
        {
            HttpServletResponse httpResponse = (HttpServletResponse)response;
            VaryResponseWrapper varyResponseWrapper = new VaryResponseWrapper(httpResponse);
            chain.doFilter(request, varyResponseWrapper);
        }
    }

    public static class VaryResponseWrapper extends HttpServletResponseWrapper
    {
        public VaryResponseWrapper(HttpServletResponse response)
        {
            super(response);
        }

        @Override
        public void addCookie(Cookie cookie)
        {
            if (isCommitted())
            {
                LOG.warn("addCookie after commit");
                return;
            }
            // only modify the "foo" named cookie
            if (cookie.getName().equals("foo"))
            {
                cookie.setValue("always-bar");
            }
            super.addCookie(cookie);
        }

        @Override
        public void addHeader(String name, String value)
        {
            if (isCommitted())
            {
                LOG.warn("addHeader after commit");
                return;
            }

            if (name.equalsIgnoreCase("accept-ranges"))
            {
                // filter it out, we don't want this to arrive in response.
                return;
            }
            else if (name.equalsIgnoreCase("set-cookie"))
            {
                if (value != null && value.contains("foo"))
                {
                    String newValue = "bar-always-bar";
                    super.addHeader(name, newValue);
                    return;
                }
            }
            else if (name.equalsIgnoreCase("vary"))
            {
                if (value != null && value.contains("accept-encoding"))
                {
                    String newValue = "x-accept-encoding";
                    super.addHeader(name, newValue);
                    return;
                }
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value)
        {
            // per servlet spec, a null name is a no-op
            if (name == null)
                return;

            if (isCommitted())
            {
                LOG.warn("setHeader after commit");
                return;
            }

            if (value == null)
            {
                // per servlet spec, a null value is a REMOVE of a header.
                setHeader(name, value);
                return;
            }

            if (name.equalsIgnoreCase("accept-ranges"))
            {
                // filter it out, we don't want this to arrive in response.
                return;
            }
            else if (name.equalsIgnoreCase("set-cookie"))
            {
                if (value.contains("foo"))
                {
                    String newValue = "bar-always-bar";
                    super.setHeader(name, newValue);
                    return;
                }
            }
            else if (name.equalsIgnoreCase("vary"))
            {
                if (value.contains("accept-encoding"))
                {
                    String newValue = "x-accept-encoding";
                    super.setHeader(name, newValue);
                    return;
                }
            }
            super.setHeader(name, value);
        }
    }
}
