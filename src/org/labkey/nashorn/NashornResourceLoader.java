package org.labkey.nashorn;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleResourceLoadException;
import org.labkey.api.module.ModuleResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Created by matthew on 8/4/15.
 */
public class NashornResourceLoader implements ModuleResourceLoader
{
    @NotNull
    @Override
    public Set<String> getModuleDependencies(Module module, File explodedModuleDir)
    {
        return Collections.emptySet();
    }

    @Override
    public void registerResources(Module module) throws IOException, ModuleResourceLoadException
    {

    }
}
