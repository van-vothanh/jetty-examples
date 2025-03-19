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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.StringUtil;

public class TooManyRequestsHandler extends Handler.Wrapper
{
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception
    {
        // simulating some criteria to trigger the failed request handling
        if (StringUtil.isNotBlank(request.getHeaders().get("X-Overdoing-It-1")))
        {
            // Option 1: just fail the callback
            callback.failed(new TooManyRequestsException());
            // Indicate to Jetty that this Handler is handling this request
            // not allowing further handlers to handle this request.
            return true;
        }
        else if (StringUtil.isNotBlank(request.getHeaders().get("X-Overdoing-It-2")))
        {
            // Option 2: write the error
            Response.writeError(request, response, callback, new TooManyRequestsException());
            // Indicate to Jetty that this Handler is handling this request
            // not allowing further handlers to handle this request.
            return true;
        }

        // process the child handlers
        return super.handle(request, response, callback);
    }
}
