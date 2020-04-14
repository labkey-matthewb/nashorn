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

package org.labkey.westside;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.json.JSONObject;
import org.labkey.api.action.FormApiAction;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.PermissionCheckableAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.westside.env.Console;
import org.labkey.westside.env.JSENV;
import org.labkey.westside.env.Request;
import org.labkey.westside.env.Response;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JavascriptDelegatingController extends SpringActionController
{
    static final String NAME = "script";
    static final Logger _log = Logger.getLogger(JavascriptDelegatingController.class);
    final String resolvedName;


    static final String executeFnName = "execute";
    static final String requiresPermissionName = "requiresPermission";


    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(JavascriptDelegatingController.class)
    {
        @Override
        public Controller resolveActionName(Controller actionController, String name)
        {
            JavascriptDelegatingController me = (JavascriptDelegatingController)actionController;
            // if controller is invoked with the default name "script" then act like a regular controller
            if (me.resolvedName.equals(NAME))
            {
                Controller c = super.resolveActionName(actionController, name);
                if (null != c)
                    return c;
            }
            // otherwise use the generic delegating action
            return super.resolveActionName(actionController, "action");
        }
    };


    public JavascriptDelegatingController()
    {
        setActionResolver(_actionResolver);
        resolvedName = NAME;
    }


    public JavascriptDelegatingController(String name)
    {
        setActionResolver(_actionResolver);
        this.resolvedName = name;
    }


    @RequiresNoPermission
    @Marshal(Marshaller.Jackson)
    public class ActionAction extends PermissionCheckableAction
    {
        public ActionAction()
        {
            setUnauthorizedType(UnauthorizedException.Type.sendUnauthorized);
        }

        Value getActionInstance(Context context, String actionName)
        {
            var bindings = context.getBindings("js");
            if (!bindings.hasMember("actions"))
                throw new NotFoundException("exports variable 'actions' not found");
            Value actions = bindings.getMember("actions");

            // HANDLE both instantiated object as well as class?
            if (!actions.hasMember(actionName))
                throw new NotFoundException("action not found: " + actionName);
            Value actionValue = actions.getMember(actionName);
            Value actionImpl = actionValue;
            if (actionValue.canInstantiate())
                actionImpl = actionValue.newInstance();

            // validate object
            Value execute = null;
            Value requiresPermission = null;

            if (actionImpl.hasMember(executeFnName))
                execute = actionImpl.getMember(executeFnName);
            if (actionImpl.hasMember(requiresPermissionName))
                requiresPermission = actionImpl.getMember(requiresPermissionName);

            if (null == execute || null == requiresPermission || !execute.canExecute() || !requiresPermission.hasArrayElements())
            {
                throw new IllegalStateException("Action must have an '" + executeFnName +  "' function method and a '" + requiresPermissionName + "' array member.");
            }

            return actionImpl;
        }



        @Override
        protected void checkPermissions(UnauthorizedException.Type unauthorizedType) throws UnauthorizedException
        {
            super.checkPermissions(unauthorizedType);
            // TODO check requiresPermission
        }


        @Override
        public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
        {
            response.setHeader("X-Robots-Tag", "noindex");

            String controllerName = getViewContext().getActionURL().getController();
            String actionName = getViewContext().getActionURL().getAction();
            if (StringUtils.equalsIgnoreCase("action",actionName) && null != request.getParameter("action"))
                actionName = StringUtils.stripToEmpty(request.getParameter("action"));

            Context context = getScriptContext();

            // evaluate controller script
            ScriptCacheHandler.ScriptWrapper w = ScriptControllerManager.getControllerScript((controllerName).toLowerCase());
            if (null == w)
                throw new NotFoundException("Could not find script corresponding to " + controllerName);
            Source source = w.getSource();
            context.eval(source);

            Value actionImpl = getActionInstance(context, actionName);

//            _dump(actionImpl);
//            _dump(actionImpl.getMetaObject());

            Request envRequest = new Request(getViewContext().getRequest(), getViewContext().getActionURL());
            Response envResponse = new Response(getViewContext().getResponse());

            if (!actionImpl.hasMember(executeFnName))
                throw new NotFoundException("method '" + executeFnName + "' not found for action '" + actionName + "'");
            Value executeFn = actionImpl.getMember(executeFnName);
            if (!executeFn.canExecute())
                throw new NotFoundException("method '" + executeFnName + "' not found for action '" + actionName + "'");
            actionImpl.invokeMember(executeFnName, envRequest, envResponse);
            return null;
        }
    }



    @SuppressWarnings("unused")
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


    @SuppressWarnings("unused")
    @RequiresSiteAdmin
    public class ScriptAction extends FormApiAction<ScriptForm>
    {
        @Override
        public ModelAndView getView(ScriptForm scriptForm, BindException errors)
        {
            if (!getUser().isPlatformDeveloper() || !AppProps.getInstance().isDevMode())
                throw new UnauthorizedException();
            return new JspView<>("/org/labkey/westside/script.jsp", null, errors);
        }

        @Override
        public Object execute(ScriptForm scriptForm, BindException errors)
        {
            if (!getUser().isPlatformDeveloper() || !AppProps.getInstance().isDevMode())
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
                Context context = getScriptContext();
                Source source = Source.newBuilder("js", scriptForm.getScript(), "script.js").build();
                Object result = context.eval(source);
                if (_log.isDebugEnabled())
                {
                    var b = context.getBindings("js");
                    if (null != b)
                    {
                        _log.debug("globals:");
                        for (String s : b.getMemberKeys())
                            _log.debug("  " + s);
                    }
                }
                ret.put("result", result);
            }
            catch (Exception x)
            {
                ret.put("success", false);
                ret.put("message", x.getMessage());
//                ret.put("lineNumber", x.getLineNumber());
//                ret.put("columnNumber", x.getColumnNumber());
            }
            return ret;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }

    }


    private Context _graalContext = null;

    private Context getScriptContext()
    {
        if (null != _graalContext)
            return _graalContext;

        HostAccess access = HostAccess.newBuilder()
                .allowImplementationsAnnotatedBy(JSENV.class)
                .allowAccessAnnotatedBy(JSENV.class)
                .allowPublicAccess(true)
                .build();

        Context context = Context.newBuilder("js")
                .allowHostAccess(access).build();
        var scope = context.getBindings("js");
        scope.putMember("LABKEY", new org.labkey.westside.env.LABKEY(getViewContext()));
        scope.putMember("console",new Console());
        _graalContext = context;
        return _graalContext;
    }
}


/*

 private void _dump(Value v)
 {
 try
 {
 System.out.println(String.valueOf(v.getMetaObject()));
 for (var k : v.getMemberKeys())
 {
 String value = "-";
 try
 {
 value = String.valueOf(v.getMember(k));
 }
 catch (Throwable t)
 {
 value = t.getMessage();
 }
 System.out.println(k + ": " + value);
 }
 if (v.hasMember("prototype"))
 System.out.println("prototype: " + v.getMember("prototype").getMetaObject().toString());
 if (v.hasMember("execute_GET"))
 System.out.println("execute_GET: " + v.getMember("execute_GET").canExecute());
 if (v.hasMember("execute_POST"))
 System.out.println("execute_POST: " + v.getMember("execute_POST").canExecute());
 if (v.hasMember("execute"))
 System.out.println("execute: " + v.getMember("execute").canExecute());
 }
 catch (Throwable t)
 {
 t.printStackTrace();
 }
 }
*/