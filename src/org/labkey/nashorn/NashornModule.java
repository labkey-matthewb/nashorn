/*
 * Copyright (c) 2013-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nashorn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleResourceCache;
import org.labkey.api.module.ModuleResourceCaches;
import org.labkey.api.module.ModuleResourceLoader;
import org.labkey.api.util.Path;
import org.labkey.api.view.WebPartFactory;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NashornModule extends DefaultModule
{
    public NashornModule()
    {
    }

    @Override
    public String getName()
    {
        return "Nashorn";
    }

    @Override
    public double getVersion()
    {
        return 15.10;
    }

    @Override
    public boolean hasScripts()
    {
        return false;
    }

    @NotNull
    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    @Override
    protected void init()
    {
        addController("nashorn", NashornController.class);
        CONTROLLER_CACHE = ModuleResourceCaches.create(new Path("controllers"), "Javascript controllers", new NashornCacheHandler());
    }


    @Override
    public void doStartup(ModuleContext moduleContext)
    {
        addControllerAliases();
    }


    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.emptySet();
    }


    @NotNull
    @Override
    public Set<? extends ModuleResourceLoader> getResourceLoaders()
    {
        return Collections.singleton(new NashornResourceLoader());
    }

    @Override
    public Controller getController(HttpServletRequest request, String name)
    {
        return new NashornController(name);
    }

    @Override
    public Controller getController(@Nullable HttpServletRequest request, Class cls)
    {
        return new NashornController();
    }

    private void addControllerAliases()
    {
        for (Module m : ModuleLoader.getInstance().getModules())
        {
            for (NashornCacheHandler.ScriptWrapper sw : CONTROLLER_CACHE.getResources(m))
                ModuleLoader.getInstance().addControllerAlias(this, sw.getControllerName(), NashornController.class);
        }
    }

    private static ModuleResourceCache<NashornCacheHandler.ScriptWrapper> CONTROLLER_CACHE = null;

    public static NashornCacheHandler.ScriptWrapper getControllerScript(String key)
    {
        return CONTROLLER_CACHE.getResource(key);
    }
}
