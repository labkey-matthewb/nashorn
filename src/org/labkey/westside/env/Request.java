package org.labkey.westside.env;

import org.json.JSONObject;
import org.labkey.api.view.ActionURL;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by matthew on 5/8/15.
 */
@SuppressWarnings("unused")
@JSENV
public class Request implements Wrapped
{
    final HttpServletRequest _request;
    final ActionURL _url;

    public Request(HttpServletRequest request, ActionURL url)
    {
        _request = request;
        _url = url;
    }

    @JSENV
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

    public String getMethod()
    {
        return _request.getMethod();
    }

    public String getContextPath()
    {
        return _request.getContextPath();
    }

    public String getContentType()
    {
        return _request.getContentType();
    }

    public String getRequestURI()
    {
        return _request.getRequestURI();
    }

    public ActionURL getUrl()
    {
        return _url;
    }

    public Map<String,String[]> getParameterMap()
    {
        return _request.getParameterMap();
    }

    public String getParameterMapJSON()
    {
        return new JSONObject(_request.getParameterMap()).toString();
    }

    @Override
    public Object unwrap(Class cls)
    {
        if (cls.isAssignableFrom(_request.getClass()))
            return _request;
        return null;
    }
}
