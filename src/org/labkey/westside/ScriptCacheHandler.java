package org.labkey.westside;

import org.graalvm.polyglot.Source;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleResourceCacheHandler;
import org.labkey.api.resource.Resource;
import org.labkey.api.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.Reader;

/**
 * Load and cache javascript controller definitions
 */

public class ScriptCacheHandler implements ModuleResourceCacheHandler<Map<String, ScriptCacheHandler.ScriptWrapper>>
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
                    return new ScriptWrapper(moduleName + "-" + baseName, resource);
                })
                .collect(Collectors.toUnmodifiableMap(sw -> sw.getControllerName().toLowerCase(), sw -> sw));
        return ret;
    }

    public boolean isResourceFile(String filename)
    {
        return filename.endsWith(".js");
    }

    // for now just wrap the resource.  Might be possible to pre-parse the script and cache that?
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
