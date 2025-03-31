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
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ServletFileServerMultipleLocationsTest
{
    private final long exampleSize = 2 * StaticFileGen.MB;
    private final long largeSize = 2 * StaticFileGen.GB;
    private Server server;
    private static String exampleSha;
    private static String largeSha;

    @BeforeEach
    public void startServer() throws Exception
    {
        Path resourcesRoot = StaticFileGen.tempDir("static-huge");
        FS.ensureDirExists(resourcesRoot);

        if (exampleSha == null)
            exampleSha = StaticFileGen.generate(resourcesRoot.resolve("example.png"), exampleSize);
        if (largeSha == null)
            largeSha = StaticFileGen.generate(resourcesRoot.resolve("large.mkv"), largeSize);

        Path altIndexTxt = resourcesRoot.resolve("index.txt");
        Files.writeString(altIndexTxt, "Alt Index TEXT", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        server = ServletFileServerMultipleLocations.newServer(0, resourcesRoot);
        server.start();
    }

    @AfterEach
    public void stopServer()
    {
        LifeCycle.stop(server);
    }

    /**
     * Get file
     */
    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, delimiterString = "|", textBlock = """
        requestPath     | expectedContents
        /index.html     | This is the index.html
        /               | This is the index.html
        /hello.html     | Hello from src/main/resources/static-root/
        /deeper/        | This is the foo.htm
        /deeper/foo.htm | This is the foo.htm
        /deeper/alt.txt | This is the alt.txt
        /alt/index.txt  | Alt Index TEXT
        """)
    public void testGet(String requestPath, String expectedContents) throws Exception
    {
        HttpURLConnection http = (HttpURLConnection)server.getURI().resolve(requestPath).toURL().openConnection();
        http.connect();
        dumpRequestResponse(http);
        assertEquals(HttpURLConnection.HTTP_OK, http.getResponseCode());
        try (InputStream in = http.getInputStream())
        {
            String responseBody = IO.toString(in, StandardCharsets.UTF_8);
            assertThat(responseBody, containsString(expectedContents));
        }
    }

    /**
     * Get small file
     */
    @Test
    public void testGetSmall() throws Exception
    {
        HttpURLConnection http = (HttpURLConnection)server.getURI().resolve("/alt/example.png").toURL().openConnection();
        http.connect();
        dumpRequestResponse(http);
        assertEquals(HttpURLConnection.HTTP_OK, http.getResponseCode());
        String contentLengthResponse = http.getHeaderField("Content-Length");
        assertNotNull(contentLengthResponse);
        long contentLengthLong = Long.parseLong(contentLengthResponse);
        Assertions.assertEquals(2 * StaticFileGen.MB, contentLengthLong);
        assertEquals("image/png", http.getHeaderField("Content-Type"));

        StaticFileGen.verify(http.getInputStream(), exampleSize, exampleSha);
    }

    /**
     * Get large file
     */
    @Test
    public void testGetLarge() throws Exception
    {
        HttpURLConnection http = (HttpURLConnection)server.getURI().resolve("/alt/large.mkv").toURL().openConnection();
        http.connect();
        dumpRequestResponse(http);
        assertEquals(HttpURLConnection.HTTP_OK, http.getResponseCode());
        String contentLengthResponse = http.getHeaderField("Content-Length");
        assertNotNull(contentLengthResponse);
        long contentLengthLong = Long.parseLong(contentLengthResponse);
        Assertions.assertEquals(2 * StaticFileGen.GB, contentLengthLong);
        assertNull(http.getHeaderField("Content-Type"), "Not a recognized mime-type by Jetty");

        StaticFileGen.verify(http.getInputStream(), largeSize, largeSha);
    }

    private static void dumpRequestResponse(HttpURLConnection http)
    {
        System.out.println();
        System.out.println("----");
        System.out.printf("%s %s HTTP/1.1%n", http.getRequestMethod(), http.getURL());
        System.out.println("----");
        System.out.printf("%s%n", http.getHeaderField(null));
        http.getHeaderFields().entrySet().stream()
            .filter(entry -> entry.getKey() != null)
            .forEach((entry) -> System.out.printf("%s: %s%n", entry.getKey(), http.getHeaderField(entry.getKey())));
    }
}
