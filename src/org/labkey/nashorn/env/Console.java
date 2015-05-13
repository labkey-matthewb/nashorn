package org.labkey.nashorn.env;

import org.apache.log4j.Logger;
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
}
