package org.labkey.westside.env;

//import jdk.nashorn.api.scripting.AbstractJSObject;
import org.labkey.api.view.ViewContext;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * Created by matthew on 5/8/15.
 */
@JSENV
public class LABKEY
{
    final Context _context;
    final User _user;
    final Container _container;
    final File _localPath;
    final File[] _paths;
    final Logger _log = Logger.getLogger(LABKEY.class);

    public LABKEY(Context scriptContext, ViewContext viewContext, File localPath, File[] libraryPaths)
    {
        _context = scriptContext;
        _user = new User(viewContext.getUser());
        _container = new Container(viewContext.getContainer());
        _localPath = localPath;
        _paths = libraryPaths.clone();
    }

    @JSENV
    public void log(String msg)
    {
        _log.info(msg);
    }

    @JSENV
    public User getUser()
    {
        return _user;
    }

    @JSENV
    public Container getContainer()
    {
        return _container;
    }


    /* TODO figure out the actual conventions for require()
     *   https://nodejs.org/api/modules.html#modules_the_module_scope
     */
    @JSENV
    public String normalizeModulePath(String current, String moduleReference) throws IOException
    {
        String ret = _normalizePath(current, moduleReference);
        _log.trace(current + " + " + moduleReference + " = " + ret);
        return ret;
    }

    private String _normalizePath(String current, String moduleReference) throws IOException
    {
        // built in and special...
        if (moduleReference.contains("://"))
            return moduleReference;
        if (current.contains("://"))
            current = "/";

        current = defaultString(current, "/");
        if (moduleReference.startsWith("/"))
            current = "/";

        Path currentPath = Path.parse(current);
        Path currentDir = currentPath;
        if (currentPath.size()>0)
            currentDir = currentPath.getParent();

        String normalized;
        Path modulePath = Path.parse(moduleReference);
        if (moduleReference.startsWith("./") || moduleReference.startsWith("/") || moduleReference.startsWith("../"))
        {
            if (modulePath.isAbsolute())
                return modulePath.toString();
            normalized = currentDir.append(modulePath).normalize().toString();
        }
        else
        {
            normalized = modulePath.normalize().toString();
        }
        try
        {
            var resolve = resolveModuleReference(normalized);
            if (null != resolve)
                normalized = resolve.getValue();
        }
        catch (IOException io) {/*pass*/}
        return normalized;
    }

    @JSENV
    public Value loadModule(String moduleName) throws Exception
    {
        var resolve = resolveModuleReference(moduleName);
        if (null == resolve)
            throw new Exception("module not resolved: " + moduleName);
        File scriptFile = new File(resolve.getKey(), resolve.getValue());
        _log.trace("load(" + moduleName + ") --> " + scriptFile);
        String script = IOUtils.toString(new FileReader(scriptFile));
        String decoratedScript = decorateScript(moduleName, scriptFile, script);
        Source source = Source.newBuilder("js", decoratedScript, scriptFile.getPath()).build();
        return _context.eval(source);
    }

    private String decorateScript(String moduleName, File scriptFile, String script)
    {
        // NOTE purposefully no new-lines in prolog
        return "(function(module){ " +
                "var exports=module.exports; " +
                "var require=function(mod){return module.require(mod);};  " +
                script + "\n" +
                "})\n";
    }

    HashMap<String,Map.Entry<File, String>> resolutionCache = new HashMap<>();

    private Map.Entry<File, String> resolveModuleReference(String module) throws IOException
    {
        var ret = resolutionCache.get(module);
        if (null == ret)
        {
            ret = _resolveModuleReference(module);
            resolutionCache.put(module, ret);
        }
        return ret;
    }

    private Map.Entry<File, String> _resolveModuleReference(String module) throws IOException
    {
        List<File> roots = new ArrayList<>();
        if (null != _localPath && _localPath.isDirectory())
            roots.add(_localPath);
        roots.addAll(Arrays.asList(_paths));

        if (module.startsWith("controller://"))
        {
            module = module.substring("controller//:".length());
            File file = ControllerScriptManager.getControllerScriptFile(module);
            if (null == file)
                return null;
            return new AbstractMap.SimpleEntry<>(file.getParentFile(), file.getName());
        }
        for (File root : roots)
        {
            if (!module.endsWith("/"))
            {
                String relative = module.endsWith(".js") ? module : module + ".js";
                if (new File(root,relative).isFile())
                    return new AbstractMap.SimpleEntry<>(root, relative);
            }
            File packageFile = new File(root, module + "/package.json");
            if (packageFile.isFile())
            {
                try (var r = new FileReader(packageFile))
                {
                    var main = (String) new JSONObject(IOUtils.toString(r)).get("main");
                    String relative;
                    if (new File(root, relative=(module + "/" + defaultString(main, "index.js"))).isFile())
                        return new AbstractMap.SimpleEntry<>(root, relative);
                }
            }
        }
        return null;
    }

    @JSENV
    public QueryService getQueryService()
    {
        return new QueryService(_container._container,_user._user);
    }
}
