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

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class StripExtraSlashesExample
{
    public static void main(String[] args) throws Exception
    {
        Server server = newServer(8080);
        server.start();
        server.join();
    }

    public static Server newServer(int port)
    {
        Server server = new Server();

        // Setup a UriCompliance that allows AMBIGUOUS_EMPTY_SEGMENT
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setUriCompliance(UriCompliance.LEGACY);

        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);

        ServerConnector connector = new ServerConnector(server, httpConnectionFactory);
        connector.setPort(port);
        server.addConnector(connector);

        // Setup Rewrite Handler
        RewriteHandler rewriteHandler = new RewriteHandler();
        RewriteRegexRule stripSlashesRule = new RewriteRegexRule();
        stripSlashesRule.setRegex("^/{2,}(.*)$");
        stripSlashesRule.setReplacement("/$1");

        /* Use this one instead if you only want to strip the first `//` and not things like `//////`
        RewritePatternRule stripSlashesRule = new RewritePatternRule();
        stripSlashesRule.setPattern("//*");
        stripSlashesRule.setReplacement("/");
        */

        rewriteHandler.addRule(stripSlashesRule);

        // Setup Dump Handler to respond on all requests (raw handler, no context-path)
        DumpHandler dumpHandler = new DumpHandler();
        rewriteHandler.setHandler(dumpHandler);

        server.setHandler(rewriteHandler);

        return server;
    }
}
