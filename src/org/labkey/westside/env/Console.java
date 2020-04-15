package org.labkey.westside.env;

import org.apache.log4j.Logger;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.labkey.api.script.ScriptService;

/**
 * Created by matthew on 5/8/15.
 */
public class Console
{
    final Logger _log;

    public Console()
    {
        _log = Logger.getLogger(ScriptService.Console.class);
    }

    public void debug(String msg)
    {
        _log.debug(msg);
    }
    public void info(String msg)
    {
        _log.info(msg);
    }
    public void log(String msg)
    {
        _log.info(msg);
    }
    public void warn(String msg)
    {
        _log.warn(msg);
    }
    public void error(String msg)
    {
        _log.error(msg);
    }

/*
    @Override
    public Object getMember(String key)
    {
        return null;
    }

    @Override
    public Object getMemberKeys()
    {
        return null;
    }

    @Override
    public boolean hasMember(String key)
    {
        return false;
    }

    @Override
    public void putMember(String key, Value value)
    {

    } */
}
