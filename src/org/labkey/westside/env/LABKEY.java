package org.labkey.westside.env;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.json.JSONObject;
import org.labkey.api.util.Path;
import org.labkey.api.view.ViewContext;
import org.labkey.westside.ControllerScriptManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.log4j.Level.INFO;

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
    final Category _log = Category.getInstance(LABKEY.class);

    public LABKEY(Context scriptContext, ViewContext viewContext, File localPath, File[] libraryPaths)
    {
        _context = scriptContext;
        _user = new User(viewContext.getUser());
        _container = new Container(viewContext.getContainer());
        _localPath = localPath;
        _paths = libraryPaths.clone();
    }

    @JSENV
    public void log(int pri, String msg)
    {
        _log.log(Level.toLevel(pri, INFO), msg);
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
    private static  class ResolvedModule
    {
        final String modulePath;      // normalized/resolved path for use on the JS side
        Path packagePath;     // path to containing package if any (substring of modulePath)
        final File root;              // directory on search path where module was found
        final Path resolvedPath;    // javascript file is new File(root,resolvedSource);
        ResolvedModule(String modulePath, Path packagePath, File root, String resolvedPath)
        {
            this.modulePath = modulePath;
            this.packagePath = packagePath;
            this.root = root;
            this.resolvedPath = Path.parse(!resolvedPath.startsWith("/") ? "/" + resolvedPath : resolvedPath);
        }
        File getScriptFile()
        {
            return new File(root,resolvedPath.toString());
        }
        // TODO: actually set packagePath at construction
        private Path findPackagePath()
        {
            Path dir = resolvedPath.getParent();
            do
            {
                if (new File(root, dir.toString() + "/package.json").isFile())
                    return dir;
                dir = dir.getParent();
            }
            while (null != dir);
            return null;
        }
        Path getPackagePath()
        {
            if (null == packagePath)
                packagePath = findPackagePath();
            return packagePath;
        }
    }

    @JSENV
    public String normalizeModulePath(String current, String requireReference) throws IOException
    {
        String ret = _normalizePath(current, requireReference);
        _log.debug(current + " + " + requireReference + " = " + ret);
        return ret;
    }

    private String _normalizePath(String current, String requireReference) throws IOException
    {
        // built in and special...
        if (requireReference.contains("://"))
            return requireReference;
        if (current.contains("://"))
            current = "/";

        current = defaultIfBlank(current, "/");
        Path currentFile = Path.parse(current).normalize();
        Path currentDir  = 0==currentFile.size() ? currentFile : currentFile.getParent();

        // find the current resolved module if any
        ResolvedModule currentResolved = null;
        if (0!=currentFile.size())
        {
            currentResolved = resolveModuleReferenceNoThrow(null, currentFile);
            if (null == currentResolved)
                throw new IllegalStateException("could not find current module: " + current);
        }

        Path normalizedPath = null;
        Path modulePath = Path.parse(requireReference);
        ResolvedModule resolved = null;
        if (requireReference.startsWith("/"))
        {
            normalizedPath = modulePath.normalize();
            resolved = resolveModuleReferenceNoThrow(null, normalizedPath);
        }
        else if (requireReference.startsWith("./") || requireReference.startsWith("../"))
        {
            normalizedPath = currentDir.append(modulePath).normalize();
            resolved = resolveModuleReferenceNoThrow(currentResolved, normalizedPath);
        }
        else
        {
            if (null != currentResolved)
            {
                Path packagePath = currentResolved.findPackagePath();
                if (null != packagePath)
                {
                    normalizedPath = packagePath.append("node_modules").append(modulePath.normalize());
                    resolved = resolveModuleReferenceNoThrow(currentResolved, normalizedPath);
                }
            }
            if (null == resolved)
            {
                normalizedPath = modulePath.normalize();
                resolved = resolveModuleReferenceNoThrow(null, normalizedPath);
            }
        }
        if (null == normalizedPath)
            throw new IllegalStateException("illegal path: " + requireReference);
        return  null != resolved ? resolved.resolvedPath.toString() : normalizedPath.toString();
    }

    @JSENV
    public Value loadModule(String moduleName) throws Exception
    {
        var resolved = resolveModuleReference(moduleName);
        if (null == resolved)
            throw new Exception("module not resolved: " + moduleName);
        File scriptFile = resolved.getScriptFile();
        _log.debug("load(" + moduleName + ") --> " + scriptFile);
        String script = IOUtils.toString(new FileReader(scriptFile));
        String decoratedScript = decorateScript(resolved, script);
        Source source = Source.newBuilder("js", decoratedScript, scriptFile.getPath()).build();
        return _context.eval(source);
    }

    private String decorateScript(ResolvedModule module, String script)
    {
        // NOTE purposefully no new-lines in prolog
        return "(function(module){ " +
                "var exports=module.exports; " +
                "var require=function(mod){return module.require(mod);};  " +
                script + "\n" +
                "})\n";
    }

    HashMap<String, ResolvedModule> resolutionCache = new HashMap<>();

    private ResolvedModule resolveModuleReferenceNoThrow(ResolvedModule resolvedModule, Path path)
    {
        try
        {
            if (null == resolvedModule)
                return resolveModuleReference(path.toString());
            return _resolveModuleReference(resolvedModule.root, path.toString());
        }
        catch (IOException x)
        {
            return null;
        }
    }

    private ResolvedModule resolveModuleReference(String module) throws IOException
    {
        var ret = resolutionCache.get(module);
        if (null == ret)
        {
            ret = _resolveModuleReference(null, module);
            resolutionCache.put(module, ret);
        }
        return ret;
    }

    private ResolvedModule _resolveModuleReference(File searchRoot, String module) throws IOException
    {
        List<File> roots = new ArrayList<>();
        if (null != searchRoot)
        {
            roots.add(searchRoot);
        }
        else
        {
            if (null != _localPath && _localPath.isDirectory())
                roots.add(_localPath);
            roots.addAll(Arrays.asList(_paths));
        }

        if (module.startsWith("controller://"))
        {
            module = module.substring("controller//:".length());
            File file = ControllerScriptManager.getControllerScriptFile(module);
            if (null == file)
                return null;
            return new ResolvedModule(module, null, file.getParentFile(), file.getName());
        }
        for (File root : roots)
        {
            if (!module.endsWith("/"))
            {
                String relative = module.endsWith(".js") ? module : module + ".js";
                if (new File(root,relative).isFile())
                    return new ResolvedModule(module, null, root, relative);
            }
            String main = "index.js";
            File packageFile = new File(root, module + "/package.json");
            if (packageFile.isFile())
            {
                try (var r = new FileReader(packageFile))
                {
                    String s = (String) new JSONObject(IOUtils.toString(r)).get("main");
                    if (!isBlank(s))
                        main = s;
                }
            }
            String relative;
            if (new File(root, relative=(module + "/" + defaultString(main, "index.js"))).isFile())
                return new ResolvedModule(module, null, root, relative);
        }
        return null;
    }

    @JSENV
    public QueryService getQueryService()
    {
        return new QueryService(_container._container,_user._user);
    }
}
