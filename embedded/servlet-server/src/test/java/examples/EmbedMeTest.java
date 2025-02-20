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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class EmbedMeTest
{
    private Server server;

    @AfterEach
    public void stopAll()
    {
        LifeCycle.stop(server);
    }

    public static Stream<Arguments> httpGetsDefault()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd");
        String expectedTime = simpleDateFormat.format(new Date());

        return Stream.of(
            Arguments.of("/", 200, "<title>Welcome File</title>"),
            Arguments.of("/test", 200, "This content from <code>{war}/WEB-INF/html/index.html</code>"),
            Arguments.of("//test", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("/test/", 200, "This content from <code>{war}/WEB-INF/html/index.html</code>"),
            Arguments.of("//test/", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("//test/a", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("//test/a/b", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("//test/a/b/c", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("//test/a/", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("//test/a/b/", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("//test/a/b/c/", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("/time", 200, expectedTime),
            Arguments.of("//time", 400, "<h1>Bad Message 400</h1>"),
            Arguments.of("/time/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//time/", 400, "<h1>Bad Message 400</h1>")
        );
    }

    @ParameterizedTest
    @MethodSource("httpGetsDefault")
    public void testHttpGetDefault(String path, int expectedStatus, String expectedText) throws Exception
    {
        server = EmbedMe.newServer(0, UriCompliance.DEFAULT);
        server.start();

        testHttpGet(path, expectedStatus, expectedText);
    }

    public static Stream<Arguments> httpGetsLegacy()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd");
        String expectedTime = simpleDateFormat.format(new Date());

        return Stream.of(
            Arguments.of("/", 200, "<title>Welcome File</title>"),
            Arguments.of("/test", 200, "This content from <code>{war}/WEB-INF/html/index.html</code>"),
            Arguments.of("//test", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("/test/", 200, "This content from <code>{war}/WEB-INF/html/index.html</code>"),
            Arguments.of("//test/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//test/a", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//test/a/b", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//test/a/b/c", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//test/a/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//test/a/b/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//test/a/b/c/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("/time", 200, expectedTime),
            Arguments.of("//time", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("/time/", 404, "<h2>HTTP ERROR 404 Not Found</h2>"),
            Arguments.of("//time/", 404, "<h2>HTTP ERROR 404 Not Found</h2>")
        );
    }

    @ParameterizedTest
    @MethodSource("httpGetsLegacy")
    public void testHttpGetLegacy(String path, int expectedStatus, String expectedText) throws Exception
    {
        server = EmbedMe.newServer(0, UriCompliance.LEGACY);
        server.start();

        testHttpGet(path, expectedStatus, expectedText);
    }

    private void testHttpGet(String path, int expectedStatus, String expectedText) throws Exception
    {
        URI serverURI = server.getURI();
        try (Socket client = new Socket(serverURI.getHost(), serverURI.getPort());
             OutputStream out = client.getOutputStream();
             InputStream in = client.getInputStream())
        {
            String rawRequest = String.format("GET %s HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n", path, serverURI.getAuthority());

            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String rawResponse = IO.toString(in);
            HttpTester.Response response = HttpTester.parseResponse(rawResponse);

            assertThat(response.getStatus(), is(expectedStatus));
            assertThat(response.getContent(), containsString(expectedText));
        }
    }
}
