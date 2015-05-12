/*
 * Copyright (c) 2013 LabKey Corporation
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

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.ScriptObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiJsonWriter;
import org.labkey.api.action.FormApiAction;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.NullSafeBindException;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.reader.UTF8Reader;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.Pair;
import org.labkey.api.util.SessionHelper;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.nashorn.env.Console;
import org.labkey.nashorn.env.Request;
import org.springframework.beans.PropertyValue;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.Callable;

public class NashornController extends SpringActionController
{
    static final Logger _log = Logger.getLogger(NashornController.class);

    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(NashornController.class)
    {
        @Override
        public Controller resolveActionName(Controller actionController, String name)
        {
            Controller c = super.resolveActionName(actionController, name);
            if (null != c)
                return c;
            return super.resolveActionName(actionController, "action");
        }
    };

    public NashornController()
    {
        setActionResolver(_actionResolver);
    }


    @RequiresNoPermission
    @Marshal(Marshaller.Jackson)
    public class ActionAction extends ApiAction<JSONObject>
    {
        @NotNull
        @Override
        protected Pair<JSONObject, BindException> populateForm() throws Exception
        {
            // CONSIDER supporting strongly typed form objects
            if (StringUtils.contains(getViewContext().getRequest().getContentType(), ApiJsonWriter.CONTENT_TYPE_JSON))
                return populateJSONObjectForm();
            else
            {
                JSONObject json = new JSONObject();
                BindException ex = new NullSafeBindException(json,"form");
                for (PropertyValue pv : getViewContext().getBindPropertyValues().getPropertyValues())
                {
                    json.put(pv.getName(), pv.getValue());
                }
                return new Pair(json,ex);
            }
        }

        @Override
        public Object execute(JSONObject json, BindException errors) throws Exception
        {
            HttpServletRequest req = getViewContext().getRequest();


            String controllerName = getViewContext().getActionURL().getController();
            String scriptName = null;
            boolean useSessionEngine = true;
            if (controllerName.contains("-"))
            {
                scriptName = controllerName.substring(controllerName.indexOf("-")+1);
                useSessionEngine = false;
            }
            String actionName = getViewContext().getActionURL().getAction();
            if (StringUtils.equalsIgnoreCase("action",actionName) && null != req.getParameter("action"))
                actionName = StringUtils.stripToEmpty(req.getParameter("action"));
            String method=req.getMethod();


            Pair<ScriptEngine,ScriptContext> nashorn = getNashorn(useSessionEngine);

            // evaluate controller script
            if (null != scriptName)
            {
                Module m = ModuleLoader.getInstance().getModule("nashorn");
                try (InputStream is = m.getResourceStream("controllers/" + scriptName + ".js"))
                {
                    nashorn.first.eval(new UTF8Reader(is), nashorn.second);
                }
            }

            Bindings bindings = nashorn.second.getBindings(ScriptContext.ENGINE_SCOPE);
            if (!bindings.containsKey("actions"))
            {
                throw new NotFoundException("exports variable 'actions' not found");
            }

            ScriptObjectMirror actions = (ScriptObjectMirror)bindings.get("actions");

            if (null == actions || !actions.containsKey(actionName))
                throw new NotFoundException("action not found: nashorn." + actionName);

            ScriptObjectMirror action = (ScriptObjectMirror)actions.get(actionName);

            String validateFn = null;
            if (action.containsKey("validate"))
                validateFn = "validate";

            String executeFn = "execute";
            if (action.containsKey("execute_" + method))
                executeFn = "execute_" + method;

            if (null != validateFn)
                action.callMember(validateFn,json,errors);

            if (errors.hasErrors())
                return null;

            Object result = action.callMember(executeFn,json,errors);

            if (errors.hasErrors())
                return null;

            return result;
        }
    }



    public static class ScriptForm
    {
        String script;

        public String getScript()
        {
            return script;
        }

        public void setScript(String script)
        {
            this.script = script;
        }
    }


    @RequiresSiteAdmin
    public class ScriptAction extends FormApiAction<ScriptForm>
    {
        @Override
        public ModelAndView getView(ScriptForm scriptForm, BindException errors) throws Exception
        {
            if (!getUser().isDeveloper() || !AppProps.getInstance().isDevMode())
                throw new UnauthorizedException();
            JspView<Object> view = new JspView<>("/org/labkey/nashorn/script.jsp", null, errors);
            return view;
        }

        @Override
        public Object execute(ScriptForm scriptForm, BindException errors) throws Exception
        {
            if (!getUser().isDeveloper() || !AppProps.getInstance().isDevMode())
                throw new UnauthorizedException();

            JSONObject ret = new JSONObject();
            ret.put("success", true);

            String script = StringUtils.trimToNull(scriptForm.getScript());
            if (null == script)
            {
                return ret;
            }
            try
            {
                Pair<ScriptEngine,ScriptContext> nashorn = getNashorn(true);
                Object result = nashorn.first.eval(scriptForm.getScript(),nashorn.second);
                if (_log.isDebugEnabled())
                {
                    ScriptContext context = nashorn.second;
                    Bindings b = context.getBindings(ScriptContext.GLOBAL_SCOPE);
                    if (null != b)
                    {
                        _log.debug("globals:");
                        for (String s : b.keySet())
                            _log.debug("  " + s);
                    }
                     b = context.getBindings(ScriptContext.ENGINE_SCOPE);
                    _log.debug("engine:");
                     for (String s : b.keySet())
                     {
                         StringBuilder entry = new StringBuilder();
                         entry.append("  ").append(s);
                         if (_log.isTraceEnabled())
                         {
                             Object v = b.get(s);
                             entry.append(": ").append(String.valueOf(v));
                             if (v instanceof ScriptObjectMirror)
                             {
                                 ScriptObjectMirror so = (ScriptObjectMirror)v;
                                 for (String p : so.getOwnKeys(true))
                                 {
                                     entry.append("\n    ").append(p).append(": ").append(String.valueOf(so.get(p))).append(" ");
                                 }
                             }
                         }
                         _log.debug(entry);
                     }
                }
                ret.put("result", result);
            }
            catch (ScriptException x)
            {
                ret.put("success", false);
                ret.put("message", x.getMessage());
                ret.put("lineNumber", x.getLineNumber());
                ret.put("columnNumber", x.getColumnNumber());
            }
            return ret;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }

    }

    private Pair<ScriptEngine,ScriptContext> getNashorn(boolean useSession) throws Exception
    {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine;
        ScriptContext context;

        Callable<ScriptEngine> createContext =  new Callable<ScriptEngine>()
        {
            @Override @NotNull
            public ScriptEngine call() throws Exception
            {
                NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
                ScriptEngine engine = factory.getScriptEngine(new String[]{"--global-per-engine"});
                return engine;
            }
        };

        if (useSession)
        {
            HttpServletRequest req = getViewContext().getRequest();
            engine = SessionHelper.getAttribute(req, this.getClass().getName() + "#scriptEngine", createContext);
        }
        else
        {
            engine = createContext.call();
        }

        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        engineScope.put("LABKEY", new org.labkey.nashorn.env.LABKEY(getViewContext()));
        engineScope.put("user", new org.labkey.nashorn.env.User(getViewContext().getUser()));
        engineScope.put("container", new org.labkey.nashorn.env.Container(getViewContext().getContainer()));
        engineScope.put("request", new Request(getViewContext().getRequest(), getViewContext().getActionURL()));

        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        // null==engine.getBindings(ScriptContext.GLOBAL_SCOPE), because of --global-per-engine
        // some docs mention enginScope.get("nashorn.global"), but that is also null
        if (null == globalScope)
            globalScope = (Bindings)engineScope.get("nashorn.global");
        if (null == globalScope)
            globalScope = engineScope;
        globalScope.put("console",new Console());

        return new Pair<>(engine,engine.getContext());
    }
}



/*
// example script


(function(){

	var beginAction =
	{
	    validate:function(json,errors)                     // optional
	    {
	    },

	    execute:function(json,errors)                      // required (or execute_POST)
	    {
	    	var name = json.name || json.Name;
	    	if (name)
	        	return {message:'Hello ' + name};
	        else
	        	return {message:'Hello World'};
	    },

	    execute_POST:function(json,errors)                 // optional
	    {
	        var ret = this.execute(json,errors);
	        ret['method'] = 'POST';
	        return ret;
	    },

	    execute_GET:function(json,errors)                  // optional
	    {
	        var ret = this.execute(json,errors);
	        ret['method'] = 'GET';
	        return ret;
	    },

	    methodsAllowed:['POST','GET'],          // default = ['POST']

	    requiresPermission: "ReadPermission"    // required
	};

	return actions =
	{
	    begin: beginAction
	};

})();


 */
