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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.stream.Stream;

import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class StripExtraSlashesExampleTest
{
    private Server server;

    @BeforeEach
    public void startServer() throws Exception
    {
        server = StripExtraSlashesExample.newServer(0);
        server.start();
    }

    @AfterEach
    public void stopServer()
    {
        LifeCycle.stop(server);
    }


    public static Stream<Arguments> rewriteCases()
    {
        return Stream.of(
            Arguments.of("/", "/"),
            Arguments.of("/dump", "/dump"),
            Arguments.of("/dump/", "/dump/"),
            Arguments.of("//", "/"),
            Arguments.of("//dump", "/dump"),
            Arguments.of("//dump/", "/dump/"),
            Arguments.of("//dump/b", "/dump/b"),
            Arguments.of("///toomany", "/toomany"),
            Arguments.of("//////toomany/", "/toomany/"),
            Arguments.of("//////toomany//deep", "/toomany//deep"),
            Arguments.of("/%2Fencoded", "/%2Fencoded"),
            Arguments.of("//%2Fencoded/", "/%2Fencoded/"),
            // path parameters in the mix
            Arguments.of("/;a=b/", "/;a=b/"),
            Arguments.of("/;a=b//", "/;a=b//"),
            Arguments.of("/;/dump", "/;/dump")
        );
    }

    @ParameterizedTest
    @MethodSource("rewriteCases")
    public void testRewrite(String inputPath, String expectedPath) throws IOException
    {
        String rawRequest = """
            GET %s HTTP/1.1
            Host: www.example.org
            Connection: close
            
            """.formatted(inputPath);

        URI uri = server.getURI();

        try (Socket socket = new Socket(uri.getHost(), uri.getPort());
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream())
        {
            out.write(rawRequest.getBytes(UTF_8));
            out.flush();

            HttpTester.Response response = HttpTester.parseResponse(in);
            assertThat(response.getStatus(), is(200));
            String responseBody = response.getContent();
            assertThat(responseBody, containsString(String.format("HttpURI.path: %s%n", expectedPath)));
        }
    }
}
