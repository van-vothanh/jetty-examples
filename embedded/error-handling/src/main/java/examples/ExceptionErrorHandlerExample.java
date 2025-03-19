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
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

/**
 * Example of using Exceptions from Handlers in an ErrorHandler to customize the
 * response produced.
 */
public class ExceptionErrorHandlerExample
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(9090);
        server.start();
        server.join();
    }

    public static Server newServer(int port)
    {
        Server server = new Server(port);

        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        contextHandlerCollection.addHandler(new ContextHandler(new HelloHandler(), "/hello"));

        TooManyRequestsHandler tooManyRequestsHandler = new TooManyRequestsHandler();
        tooManyRequestsHandler.setHandler(contextHandlerCollection);

        server.setHandler(tooManyRequestsHandler);
        server.setErrorHandler(new CustomErrorHandler());

        return server;
    }

    public static class CustomErrorHandler extends ErrorHandler
    {
        @Override
        protected boolean generateAcceptableResponse(Request request, Response response, Callback callback, String contentType, List<Charset> charsets, int code, String message, Throwable cause) throws IOException
        {
            if (cause instanceof TooManyRequestsException)
            {
                response.getHeaders().put(HttpHeader.RETRY_AFTER, "120");
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS_429);
                // write nothing on response body for these kinds of requests, making sure to allow callback to succeed.
                response.write(true, BufferUtil.EMPTY_BUFFER, callback);
                return true;
            }
            else
            {
                // allow normal error handling to proceed for other kinds of error conditions (eg: 404, 500, 503, etc)
                return super.generateAcceptableResponse(request, response, callback, contentType, charsets, code, message, cause);
            }
        }
    }
}
