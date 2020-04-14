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

package org.labkey.westside;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.SimpleModule;
import org.labkey.api.view.WebPartFactory;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;


public class WestsideModule extends SimpleModule
{
    public WestsideModule()
    {
    }

    @Override
    public String getName()
    {
        return "Nashorn";
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
        addController("nashorn", JavascriptDelegatingController.class);
    }


    @Override
    public void startupAfterSpringConfig(ModuleContext moduleContext)
    {
        ScriptControllerManager.addControllerAliases(this);
    }

    @Override
    public Controller getController(HttpServletRequest request, String name)
    {
        return ScriptControllerManager.getController(request, name);
    }


    @Override
    public Controller getController(@Nullable HttpServletRequest request, Class cls)
    {
        return ScriptControllerManager.getController(request, cls);
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
}
