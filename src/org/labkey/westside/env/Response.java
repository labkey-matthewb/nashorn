package org.labkey.westside.env;

import org.labkey.api.view.ActionURL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by matthew on 5/8/15.
 *
 * https://nodejs.org/api/http.html#http_http_incomingmessage
 */

@JSENV
public class Response implements Wrapped
{
    final HttpServletResponse _response;

    public Response(HttpServletResponse response)
    {
        _response = response;
    }

    public void setContentType(String contentType)
    {
        _response.setContentType(contentType);
    }

    public void sendError(int error, String message) throws IOException
    {
        _response.sendError(error, message);
    }

    public Writer getWriter() throws IOException
    {
        return _response.getWriter();
    }

    public void write(String s) throws IOException
    {
        _response.getWriter().print(s);
    }

    @Override
    public Object unwrap(Class cls)
    {
        if (null == cls && cls.isAssignableFrom(_response.getClass()))
            return _response;
        return null;
    }
}
