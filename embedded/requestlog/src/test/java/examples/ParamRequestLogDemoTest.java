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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ParamRequestLogDemoTest
{
    private static final Logger LOG = LoggerFactory.getLogger(ParamRequestLogDemoTest.class);
    private Server server;

    @BeforeEach
    public void startServer() throws Exception
    {
        LOG.warn("===== SEE Console Logging Output for \":INFO :EXAMPLE.REQUESTLOG:\" entries =====");

        server = ParamRequestLogDemo.newServer(0);
        server.start();
    }

    @AfterEach
    public void stopServer()
    {
        LifeCycle.stop(server);
    }

    /**
     * Demonstrate behavior with request that have no form sent (in request body).
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        # Path                         | Expected Response Status
        /read/                         | 200
        /read/?name=Bob                | 200
        /no-read/                      | 200
        /no-read/?name=Carl            | 200
        /no-read/?name=Anne&role=Chef  | 200
        /other/                        | 404
        """)
    public void testRequestNoForm(String path, int expectedStatus) throws IOException, InterruptedException
    {
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(server.getURI().resolve(path))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        assertThat(response.statusCode(), is(expectedStatus));
    }

    /**
     * Demonstrate behavior with request that has a form sent (as a request body).
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        # Path                         | Form Data            | Expected Response Status
        /read/                         | co=Canada            | 200
        /read/?name=Bruce              | co=Aussie            | 200
        /read/?name=Jack               | co=Aussie&region=NSW | 200
        /no-read/                      | co=Swiss             | 200
        /no-read/?name=Carl            | co=Aruba             | 200
        /no-read/?name=Anne&role=Chef  | co=Sweden            | 200
        /other/                        | co=France            | 404
        """)
    public void testRequestUrlForm(String path, String formdata, int expectedStatus) throws IOException, InterruptedException
    {
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(server.getURI().resolve(path))
            .version(HttpClient.Version.HTTP_1_1)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formdata))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        assertThat(response.statusCode(), is(expectedStatus));
    }
}
