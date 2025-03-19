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

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ExceptionErrorHandlerExampleTest
{
    private Server server;

    @BeforeEach
    public void startServer() throws Exception
    {
        server = ExceptionErrorHandlerExample.newServer(0);
        server.start();
    }

    @AfterEach
    public void stopServer()
    {
        LifeCycle.stop(server);
    }

    @Test
    public void testNormalRequest() throws IOException, InterruptedException
    {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(server.getURI().resolve("/hello/"))
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode(), is(200));
        assertThat(response.body(), is("Hello from " + HelloHandler.class.getName()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "X-Overdoing-It-1",
        "X-Overdoing-It-2"
    })
    public void testFailedRequest(String triggerTechniqueHeaderName) throws IOException, InterruptedException
    {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(server.getURI().resolve("/hello/"))
            .header(triggerTechniqueHeaderName, "oops")
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat("Status code", response.statusCode(), is(429));
        String retryAfter = String.join(", ", response.headers().allValues("Retry-After"));
        assertThat("Response.header", retryAfter, is("120"));
        assertThat("Expecting empty response body", response.body(), is(""));
    }
}
