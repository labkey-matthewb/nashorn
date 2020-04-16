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
import org.labkey.api.action.PermissionCheckableAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresSiteAdmin;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.MemTracker;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.WebPartView;
import org.labkey.westside.env.JSENV;
import org.labkey.westside.env.Request;
import org.labkey.westside.env.Response;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class JavascriptActionController extends SpringActionController
{
    static final String NAME = "script";
    static final Logger _log = Logger.getLogger(JavascriptActionController.class);
    final String resolvedName;


    static final String executeFnName = "handleRequest";
    static final String renderFnName = "handleRequest";
    static final String requiresPermissionName = "requiresPermission";


    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(JavascriptActionController.class)
    {
        @Override
        public Controller resolveActionName(Controller actionController, String name)
        {
            JavascriptActionController me = (JavascriptActionController)actionController;
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


    public JavascriptActionController()
    {
        setActionResolver(_actionResolver);
        resolvedName = NAME;
    }


    public JavascriptActionController(String name)
    {
        setActionResolver(_actionResolver);
        this.resolvedName = name;
    }


    @RequiresNoPermission
    public class ActionAction extends PermissionCheckableAction
    {
        public ActionAction()
        {
            setUnauthorizedType(UnauthorizedException.Type.sendUnauthorized);
        }

        Value getActionInstance(Value exports, String actionName)
        {
//            var bindings = context.getBindings("js");
//            var exports = bindings.getMember("controller");
            if (!exports.hasMember("actions") && exports.hasMember("default"))
                exports = exports.getMember("default");
            if (!exports.hasMember("actions"))
                throw new NotFoundException("exports variable 'actions' not found");
            Value actions = exports.getMember("actions");

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


        Value getViewInstance(Value exports, String viewName)
        {
//            var bindings = context.getBindings("js");
//            var exports = bindings.getMember("controller");
            if (!exports.hasMember("views") && exports.hasMember("default"))
                exports = exports.getMember("default");
            if (!exports.hasMember("views"))
                throw new NotFoundException("exports variable 'views' not found");
            Value views = exports.getMember("views");

            // HANDLE both instantiated object as well as class?
            if (!views.hasMember(viewName))
                throw new NotFoundException("view not found: " + viewName);
            Value actionValue = views.getMember(viewName);
            Value actionImpl = actionValue;
            if (actionValue.canInstantiate())
                actionImpl = actionValue.newInstance();

            // validate object
            Value render = null;
            Value requiresPermission = null;

            if (actionImpl.hasMember(renderFnName))
                render = actionImpl.getMember(renderFnName);
            if (actionImpl.hasMember(requiresPermissionName))
                requiresPermission = actionImpl.getMember(requiresPermissionName);

            if (null == render || null == requiresPermission || !render.canExecute() || !requiresPermission.hasArrayElements())
            {
                throw new IllegalStateException("Action must have a '" + renderFnName +  "' function method and a '" + requiresPermissionName + "' array member.");
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
            response.setContentType("text/json");   // default to json instead of html

            String controllerName = getViewContext().getActionURL().getController();
            String actionName = getViewContext().getActionURL().getAction();
            if (StringUtils.equalsIgnoreCase("action",actionName) && null != request.getParameter("action"))
                actionName = StringUtils.stripToEmpty(request.getParameter("action"));

            Module module = ModuleLoader.getInstance().getModule(controllerName.substring(0,controllerName.indexOf('-')));
            Context context = getScriptContext(module);
            Source source  = ControllerScriptManager.getControllerScriptSource((controllerName).toLowerCase());
            if (null == source)
                throw new NotFoundException("Could not find script corresponding to " + controllerName);
            Value exports = context.eval("js", "require('controller://" + controllerName + "');");
            boolean isapi = getViewContext().getRequest().getRequestURI().endsWith(".api");

            if (isapi)
            {
                final Value actionImpl = getActionInstance(exports, actionName);
                final Request envRequest = new Request(getViewContext().getRequest(), getViewContext().getActionURL());
                final Response envResponse = new Response(getViewContext().getResponse());
                actionImpl.invokeMember(executeFnName, envRequest, envResponse);
                return null;
            }
            else
            {
                final Value actionImpl = getViewInstance(exports, actionName);
                return new WebPartView(WebPartView.FrameType.PORTAL)
                {
                    @Override
                    protected void renderView(Object model, HttpServletRequest request, HttpServletResponse response) throws Exception
                    {
                        final Request envRequest = new Request(getViewContext().getRequest(), getViewContext().getActionURL());
                        final Response envResponse = new Response(getViewContext().getResponse());
                        actionImpl.invokeMember(renderFnName, envRequest, envResponse);
                    }
                };
            }
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
                Context context = getScriptContext(ModuleLoader.getInstance().getModule("westside"));
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

    private Context getScriptContext(Module module)
    {
        if (null != _graalContext)
            return _graalContext;

        HostAccess access = HostAccess.newBuilder()
                .allowImplementationsAnnotatedBy(JSENV.class)
                .allowAccessAnnotatedBy(JSENV.class)
                .allowArrayAccess(true)
                .allowPublicAccess(true)
                .build();

        Context context = Context.newBuilder("js")
                .allowHostAccess(access).build();
        var scope = context.getBindings("js");
        // global native helper
        var labkey = new org.labkey.westside.env.LABKEY(context, getViewContext(),
                new File(((DefaultModule)module).getResourceDirectory(), "controllers"),
                new File[] {new File(((DefaultModule)ModuleLoader.getInstance().getModule("westside")).getResourceDirectory(),"node_modules")});
        scope.putMember("_labkey_native", labkey);
        context.eval("js",
            "var console = {" +
                    "assert:function(t,s){if (t) this.log(s);}," +
                    "debug:function(s){this.log(s);}," +
                    "error:function(s){this.log(s);}," +
                    "log:function(s){_labkey_native.log(typeof s === 'string' ? s : new String(s));}," +
                    "warn:function(s){this.log(s);}};\n"+
            "var _global_module = {\n" +
                "_cache:{},\n" +
                "filename: '_globals.js',\n" +
                "exports: {},\n" +
                "parent:null,\n"+
                "normalize: function(path) { return _labkey_native.normalizeModulePath(this.filename,path); },\n" +
                "load: function(path) { return _labkey_native.loadModule(path); },\n" +
                "require: function (moduleRef) {\n" +
                    "var path = (moduleRef=='stream') ? moduleRef : this.normalize(moduleRef);\n" +
                    "if (_global_module._cache[path]) return _global_module._cache[path].exports;\n" +
                    "var module = {parent:this, exports:{}, filename:path, normalize:this.normalize, load:this.load, require:this.require};\n" +
// UNDONE: need stream.Readable and stream.Writeable!
                    "if (path==='stream') module.exports.Readable = function(){};\n" +
                    "else this.load(path)(module);\n" +
                    "_global_module._cache[path] = module;\n" +
                    "return module.exports;\n" +
                "}};\n" +
            "var module = _global_module; var require=function(mod){return module.require(mod);}\n" +
            "var global = this;\n" +
            "var process = {browser:false, env:{NODE_ENV:'" + (AppProps.getInstance().isDevMode() ? "dev" : "production") + "'}};\n"
        );
        /*
        // TODO stupid hack, need to implement function require(){}
        try (InputStream react = new FileInputStream("/lk/develop/react.js");
             InputStream reactdom = new FileInputStream("/lk/develop/react-dom-server.js")) //ClassLoader.getSystemResourceAsStream("/org/labkey/westside/react.js"))
        {
            context.eval("js",
                "var global = this;\n" +
                "var process = {env:{NODE_ENV:'development'}};\n" +
                "var module =  {exports:{}};\n" +
                "var exports = module.exports;\n"
            );
            Source source = Source.newBuilder("js", new InputStreamReader(react), "react.js").build();
            context.eval(source);
            context.eval("js",
                "var React=module.exports; var ReactDOMServer=React.__SECRET_DOM_SERVER_DO_NOT_USE_OR_YOU_WILL_BE_FIRED;\n"+
                "function require(moduleName) { if (moduleName==='react') return React; if (moduleName==='react-dom-server') return ReactDOMServer; return null;}\n"+
                "var module =  {exports:{}};\n" +
                "var exports = module.exports;\n");
        }
        catch (IOException x)
        {
            throw new ConfigurationException("error loading react.js", x);
        }
*/
        _graalContext = context;
        assert MemTracker.get().put(context);
        return _graalContext;
    }
}

/* NOTES
https://github.com/graalvm/graaljs/blob/master/docs/user/NodeJSVSJavaScriptContext.md
 */

