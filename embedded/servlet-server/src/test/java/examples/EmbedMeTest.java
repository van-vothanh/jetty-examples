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

import java.net.URI;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class EmbedMeTest
{
    private Server server;
    private HttpClient client;

    @BeforeEach
    public void startServer() throws Exception
    {
        server = EmbedMe.newServer(0);
        server.start();
    }

    @BeforeEach
    public void startClient() throws Exception
    {
        client = new HttpClient();
        client.start();
    }

    @AfterEach
    public void stopAll()
    {
        LifeCycle.stop(client);
        LifeCycle.stop(server);
    }

    @Test
    public void testGetWelcome() throws InterruptedException, ExecutionException, TimeoutException
    {
        Request request = client.newRequest(server.getURI().resolve("/"))
            .method("GET");
        ContentResponse response = request.send();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getContentAsString(), containsString("<title>Welcome File</title>"));
    }

    public static Stream<Arguments> httpGets()
    {
        String expectedTime = new Date().toString();
        int idx = expectedTime.indexOf(" "); // Wed
        idx = expectedTime.indexOf(" ", idx+1); // Feb
        idx = expectedTime.indexOf(" ", idx+1); // 19
        expectedTime = expectedTime.substring(0, idx); // Wed Feb 19

        return Stream.of(
            Arguments.of("/", 200, "<title>Welcome File</title>"),
            // Arguments.of("//", 200, ""), // Uncomment if using UriCompliance.LEGACY
            Arguments.of("/test", 200, "This content from <code>{war}/WEB-INF/html/index.html</code>"),
            Arguments.of("//test", 200, "<title>Welcome File</title>"), // empty path segment
            Arguments.of("/test/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//test/", 200, "<title>Welcome File</title>"),
            Arguments.of("/time", 200, expectedTime),
            Arguments.of("//time", 200, "<title>Welcome File</title>"),
            Arguments.of("/time/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//time/", 200, "<title>Welcome File</title>") // empty path segment
        );
    }

    @ParameterizedTest
    @MethodSource("httpGets")
    public void testHttpGet(String path, int expectedStatus, String expectedText) throws InterruptedException, ExecutionException, TimeoutException
    {
        URI serverURI = server.getURI();
        Request request = client.newRequest(serverURI.getHost(), serverURI.getPort())
            .path(path)
            .method("GET");
        ContentResponse response = request.send();
        assertThat(response.getStatus(), is(expectedStatus));
        assertThat(response.getContentAsString(), containsString(expectedText));
    }
}
