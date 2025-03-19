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
import java.util.concurrent.ExecutionException;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.FormFields;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParamRequestLog extends ContainerLifeCycle implements RequestLog
{
    private static final Logger LOG = LoggerFactory.getLogger(ParamRequestLog.class);
    private final RequestLog.Writer logWriter;

    public ParamRequestLog(RequestLog.Writer requestLogWriter)
    {
        this.logWriter = requestLogWriter;
        installBean(this.logWriter);
    }

    @Override
    public void log(Request request, Response response)
    {
        try
        {
            StringBuilder entry = new StringBuilder();

            entry.append(request.getMethod());
            entry.append(" ");
            entry.append(request.getHttpURI().toString());
            entry.append(" ");

            long requestContentLength = request.getHeaders().getLongField(HttpHeader.CONTENT_LENGTH);
            if (requestContentLength != -1)
            {
                entry.append("(Length:%,d) ".formatted(requestContentLength));
            }

            entry.append(response.getStatus());
            entry.append(" ");

            Fields queryFields = getRequestQueryFields(request);
            appendFields(entry, "Query", queryFields);

            entry.append(" ");

            Fields formFields = getRequestFormFields(request);
            appendFields(entry, "Form", formFields);

            logWriter.write(entry.toString());
        }
        catch (Throwable e)
        {
            LOG.warn("Unable to log request", e);
        }
    }

    private void appendFields(Appendable appendable, String type, Fields fields) throws IOException
    {
        appendable.append("[");
        appendable.append(type);
        if (fields == null)
        {
            appendable.append(": <null>]");
            return;
        }

        appendable.append(".size=%d".formatted(fields.getSize()));
        for (Fields.Field field: fields)
        {
            appendable.append(", %s=%s".formatted(field.getName(), field.getValue()));
        }
        appendable.append("]");
    }

    private Fields getRequestFormFields(Request request)
    {
        Object attr = request.getAttribute(FormFields.class.getName());
        if (attr instanceof FormFields futureFormFields)
        {
            try
            {
                return futureFormFields.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                return null;
            }
        }
        else if (attr instanceof Fields fields)
        {
            return fields;
        }
        return null;
    }

    private Fields getRequestQueryFields(Request request)
    {
        return Request.extractQueryParameters(request);
    }
}
