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

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

public class ParamRequestLogDemo
{
    public static void main(String[] args) throws Exception
    {
        Server server = ParamRequestLogDemo.newServer(8080);
        server.start();
        server.join();
    }

    public static Server newServer(int port)
    {
        Server server = new Server(port);

        Handler.Sequence handlers = new Handler.Sequence();
        server.setHandler(handlers);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        contexts.addHandler(new ContextHandler(new NoReadParamHandler(), "/no-read"));
        contexts.addHandler(new ContextHandler(new ReadParamHandler(), "/read"));

        Slf4jRequestLogWriter requestLoggingWriter = new Slf4jRequestLogWriter();
        requestLoggingWriter.setLoggerName("EXAMPLE.REQUESTLOG");
        RequestLog requestLog = new ParamRequestLog(requestLoggingWriter);
        server.setRequestLog(requestLog);

        return server;
    }

    public static class ReadParamHandler extends Handler.Abstract
    {
        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception
        {
            Fields params = Request.getParameters(request);
            StringBuilder reply = new StringBuilder("Params.size=%d%n".formatted(params.getSize()));
            for (Fields.Field field: params)
            {
                reply.append("Param[%s]=%s%n".formatted(field.getName(), field.getValue()));
            }
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain; charset=utf-8");
            Content.Sink.write(response, true, reply.toString(), callback);
            return true;
        }
    }

    public static class NoReadParamHandler extends Handler.Abstract
    {
        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception
        {
            long requestContentLength = request.getHeaders().getLongField(HttpHeader.CONTENT_LENGTH);
            String reply = "Didn't read params, Request.header[content-length]=%d%n".formatted(requestContentLength);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain; charset=utf-8");
            Content.Sink.write(response, true, reply, callback);
            return true;
        }
    }
}
