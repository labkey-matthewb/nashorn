package org.labkey.nashorn.env;

import org.labkey.api.view.ActionURL;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by matthew on 5/8/15.
 *
 * https://nodejs.org/api/http.html#http_http_incomingmessage
 */
public class Request implements _Wrapper
{
    final HttpServletRequest _request;
    final ActionURL _url;

    public Request(HttpServletRequest request, ActionURL url)
    {
        _request = request;
        _url = url;
    }

    public Map<String,String> getHeaders()
    {
        TreeMap<String,String> headers = new TreeMap<>();
        Enumeration e = _request.getHeaderNames();
        while (e.hasMoreElements())
        {
            String name = (String)e.nextElement();
            headers.put(name,_request.getHeader(name));
        }
        return headers;
    }

    public String getContextPath()
    {
        return _request.getContextPath();
    }

    public ActionURL getUrl()
    {
        return _url;
    }

    @Override
    public Object unwrap(Class cls)
    {
        if (cls.isAssignableFrom(_request.getClass()))
            return _request;
        return null;
    }
}
