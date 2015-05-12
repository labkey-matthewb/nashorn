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
import org.labkey.api.data.Container;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.resource.Resource;
import org.labkey.api.view.WebPartFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NashornModule extends DefaultModule
{
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
        addNashornControllers();
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
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


    private void addNashornControllers()
    {
        // TODO iterate over all modules
        Resource dir = getModuleResource("controllers");
        if (!dir.isCollection())
            return;

        ArrayList<String> aliases = new ArrayList<>();
        for (Resource file : dir.list())
        {
            if (!file.getName().endsWith(".js"))
                continue;
            String controllerName = file.getName().substring(0,file.getName().length()-3);
            aliases.add("nashorn-" + controllerName);
        }

        addController("nashorn",NashornController.class,aliases.toArray(new String[aliases.size()]));
    }
}
