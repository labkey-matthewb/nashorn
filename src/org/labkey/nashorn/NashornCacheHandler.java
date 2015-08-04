package org.labkey.nashorn;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.files.FileSystemDirectoryListener;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleResourceCacheHandler;
import org.labkey.api.resource.Resource;
import org.labkey.api.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Load and cache javascript controller definitions
 */

public class NashornCacheHandler implements ModuleResourceCacheHandler<String,NashornCacheHandler.ScriptWrapper>
{
    @Override
    public boolean isResourceFile(String filename)
    {
        return filename.endsWith(".js");
    }

    @Override
    public String getResourceName(Module module, String filename)
    {
        String baseName = FileUtil.getBaseName(filename);
        return module.getName().toLowerCase() + "-" + baseName.toLowerCase();
    }

    @Override
    public String createCacheKey(Module module, String resourceLocation)
    {
        return resourceLocation;
    }

    @Override
    public CacheLoader<String, ScriptWrapper> getResourceLoader()
    {
        return (String key, @Nullable Object argument) ->
        {
            String module = key.substring(0,key.indexOf("-"));
            String baseName = key.substring(key.indexOf("-")+1);
            Module m = ModuleLoader.getInstance().getModule(module);
            if (null == m)
                return null;
            String path = "controllers/" + baseName + ".js";
            Resource r = m.getModuleResource(path);
            if (null == r)
                return null;
            return new ScriptWrapper(key, r);
        };
    }

    @Nullable
    @Override
    public FileSystemDirectoryListener createChainedDirectoryListener(final Module module)
    {
        return new FileSystemDirectoryListener()
        {
            @Override
            public void entryCreated(java.nio.file.Path directory, java.nio.file.Path entry)
            {
                String key = getResourceName(module, entry.getFileName().toString());
                ModuleLoader.getInstance().addControllerAlias(module, key, NashornController.class);
            }

            @Override
            public void entryDeleted(java.nio.file.Path directory, java.nio.file.Path entry)
            {
                removeResource(entry);
            }

            @Override
            public void entryModified(java.nio.file.Path directory, java.nio.file.Path entry)
            {
                removeResource(entry);
            }

            @Override
            public void overflow()
            {
            }

            private void removeResource(java.nio.file.Path entry)
            {
            }
        };
    }

    // for now just wrap the resource.  Might be possible to pre-parse the script and cache that?
    public static class ScriptWrapper
    {
        final String controllerName;
        final Resource resource;
        ScriptWrapper(String controllerName, Resource r)
        {
            this.controllerName = controllerName;
            this.resource = r;
        }

        public String getControllerName()
        {
            return controllerName;
        }

        public InputStream getInputStream() throws IOException
        {
            return resource.getInputStream();
        }
    }
}
