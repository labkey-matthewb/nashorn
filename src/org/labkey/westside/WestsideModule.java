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
import org.labkey.api.module.CodeOnlyModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.view.WebPartFactory;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;

/*
 * This is not like SimpleModule that other modules extends, instead it handles two new kinds of resources in other
 * modules and registers them.
 *
 * /controllers/{script}.js
 *      creates a new controller that defines actions in javascript e.g. /labkey/home/modulename-script-myaction.api
 *
 * /views/{view}.js
 *      creates a new view (and optionally webpart with .webpart.xml) e.g. /labkey/home/modulename-ws-view.view
 *
 * TODO have one controller name for both the html views and js views
 */
public class WestsideModule extends CodeOnlyModule
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
    public Controller getController(HttpServletRequest request, String name)
    {
        return ControllerScriptManager.getController(request, name);
    }

    @Override
    public Controller getController(@Nullable HttpServletRequest request, Class cls)
    {
        return ControllerScriptManager.getController(request, cls);
    }

    @Override
    protected void doStartup(ModuleContext moduleContext)
    {
        ControllerScriptManager.addControllerAliases(this);
    }
}