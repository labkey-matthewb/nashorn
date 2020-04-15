package org.labkey.westside;

import org.graalvm.polyglot.Source;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleResourceCache;
import org.labkey.api.module.ModuleResourceCacheHandler;
import org.labkey.api.module.ModuleResourceCaches;
import org.labkey.api.module.ResourceRootProvider;
import org.labkey.api.resource.Resource;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Path;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ControllerScriptManager
{
    public static final Path CONTROLLERS_PATH = new Path("controllers");

    private static final ModuleResourceCache<Map<String, ScriptWrapper>> CONTROLLER_CACHE =
            ModuleResourceCaches.create("Javascript controllers cache", new ControllerScriptCacheHandler(), ResourceRootProvider.getStandard(CONTROLLERS_PATH));

    public static Controller getController(HttpServletRequest request, String name)
    {
        return new JavascriptActionController(name);
    }

    public static Controller getController(@Nullable HttpServletRequest request, Class<?> cls)
    {
        return new JavascriptActionController();
    }

    public static void addControllerAliases(Module me)
    {
        for (Module m : ModuleLoader.getInstance().getModules())
        {
            var map = CONTROLLER_CACHE.getResourceMap(m);
            for (ScriptWrapper sw : map.values())
                ModuleLoader.getInstance().addControllerAlias(me, sw.getControllerName(), JavascriptActionController.class);
        }
    }

    public static Source getControllerScriptSource(String controller) throws IOException
    {
        // consider only active modules
        var opt = CONTROLLER_CACHE.streamAllResourceMaps()
                .filter(map -> map.containsKey(controller))
                .findFirst();
        if (opt.isEmpty())
            return null;
        var map = opt.get();
        var sw = map.get(controller);
        return null==sw ? null : sw.getSource();
    }

    /**
     * Load and cache javascript controller definitions
     */

    public static class ControllerScriptCacheHandler implements ModuleResourceCacheHandler<Map<String, ScriptWrapper>>
    {
        // TODO aggressive reload on file change, or onModuleChange() so we can call
        // ModuleLoader.getInstance().addControllerAlias() for any new .js files

        @Override
        public Map<String, ScriptWrapper> load(Stream<? extends Resource> resources, Module module)
        {
            var ret = resources
                    .filter(resource -> isResourceFile(resource.getName()))
                    .map(resource -> {
                        String moduleName = module.getName();
                        String baseName = FileUtil.getBaseName(resource.getName());
                        return new ScriptWrapper(moduleName + '-' + baseName, resource);
                    })
                    .collect(Collectors.toUnmodifiableMap(sw -> sw.getControllerName().toLowerCase(), sw -> sw));
            return ret;
        }

        public boolean isResourceFile(String filename)
        {
            return filename.endsWith(".js");
        }
    }

    public static class ScriptWrapper
    {
        final String controllerName;
        final Resource resource;
        Source source;
        IOException buildException = null;

        ScriptWrapper(String controllerName, Resource r)
        {
            this.controllerName = controllerName;
            this.resource = r;
        }

        public String getControllerName()
        {
            return controllerName;
        }

        public Reader getReader() throws IOException
        {
            var is = resource.getInputStream();
            if (null == is)
                throw new IOException("Could not open " + resource.getPath());
            return new InputStreamReader(resource.getInputStream());
        }

        public synchronized Source getSource() throws IOException
        {
            if (null != source)
                return source;
            if (null != buildException)
                throw buildException;
            try
            {
                source = Source.newBuilder("js", getReader(), controllerName + ".js").build();
                return source;
            }
            catch (IOException x)
            {
                buildException = x;
                throw x;
            }
        }
    }
}
