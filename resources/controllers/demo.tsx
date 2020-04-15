//@ sourceURL=optionalModules/nashorn/resources/controllers/demo.js
import React, {createElement} from "react";
import ReactDOMServer from "react-dom-server";

const PermissionClass =
{
    NONE:"",
    READ:"org.labkey.api.security.permissions.ReadPermission",
    DELETE:"org.labkey.api.security.permissions.DeletePermission",
    UPDATE:"org.labkey.api.security.permissions.UpdatePermission",
    INSERT:"org.labkey.api.security.permissions.InsertPermission",
    ADMIN:"org.labkey.api.security.permissions.AdminPermission"
};
const Methods =
{
    GET: "GET",
    HEAD: "HEAD",
    POST: "POST",
    PUT: "PUT",
    DELETE: "DELETE",
    OPTIONS: "OPTIONS",
    TRACE: "TRACE"
};
class ValidationError
{
    public message:string;
    public field?:string;
}
class Errors
{
    errors:ValidationError[];

    Errors()
    {
        this.errors = [];
    }
    public hasErrors() : boolean
    {
        return this.errors && 0!==this.errors.length;
    }
    public reject(message:string)
    {
        const error = new ValidationError();
        error.message = message;
        if (!this.errors)
            this.errors = [];
        this.errors.push(error)
    }
    public rejectValue(field:string, message:string)
    {
        const error = new ValidationError();
        error.message = message;
        error.field = field;
        if (!this.errors)
            this.errors = [];
        this.errors.push(error)
    }
}
interface Console
{
    debug(msg:string) : void;
    info(msg:string) : void;
    log(msg:string) : void;
    warn(msg:string) : void;
}
interface Container
{
    id:string;
    path:string;
    name:string;
    parent:Container;
}
interface FieldKey  // TODO
{
    parent() : FieldKey;
    name() : string;
}
interface Results
{
    next() : boolean;
    wasNull() : boolean;
    close() : void;
    // for scrollable results...
    beforeFirst() : boolean;
    afterLast() : void;
    absolute(row:number) ; boolean;
    // preferred getters
    getBoolean(field:number) : boolean;
    getString(field:number) : string;
    getNumber(field:number) : number;
    getTimestamp(field:number) : Date;  // date and time
    // more type specific getters
    getInteger(field:number) : number;
    getLong(field:number) : number;
    getDate(field:number) : Date;       // date only
    getInteger(field:number) : number;
}
interface QueryService
{
    select(config:any) : Results
}
interface ActionURL
{
    getParameter(key:string) : string|string[];
}
interface lkRequest
{
    getContentType() : string;
    getMethod() : string;
    getRequestURI(): string;
    getActionURL(): ActionURL;
    getContextPath(): string;
    getHeaders(): Object;
    getBodyAsString(): string;
    getParameterMap(): any;
    // TODO (seems kinda hacky?)
    getParameterMapJSON(): string;
}
interface lkResponse
{
    setContentType(contentType: string) : void;
    getWriter() : any;
    write(s:string) : void;
    sendError(status: number, message: string);
}
interface lkUser
{
    getId():number;
    getEmail():string;
    getDisplayName():string;
}
interface View
{
    render(request:lkRequest, response: lkResponse):void;
}
interface Action
{
    execute(request:lkRequest, response: lkResponse):void;
    methodsAllowed: string[];
    requiresPermission : string[];
}
class BaseReactView implements View
{
    render(request: lkRequest, response: lkResponse):void
    {
        const element = this.getMarkup();
        response.write(ReactDOMServer.renderToStaticMarkup(element));
    }
    getMarkup():any
    {
        return [];
    }
}
class JsonApiAction implements Action
{
    public JsonApiAction()
    {
        this.user = LABKEY.getUser();
        this.errors = new Errors();
    }

    public failResponse(message:string, errors:Errors) : any
    {
        return {success: false, message:message, errors: errors};
    }
    public successResponse(value:Object)
    {
        return {success: true, value: value};
    }

    public bind(request: lkRequest, errors:Errors): Object
    {
        let json = {};
        if (request.getContentType() === "text/json")
        {
            json = JSON.parse(request.getBodyAsString());
        }
        else
        {
            let params = JSON.parse(request.getParameterMapJSON());
            for (let key in params)
            {
                if (!params.hasOwnProperty(key))
                    continue;
                let value = params[key];
                console.log("key=" + key + " value=" + value);
                if (value.length===0)
                    json[key] = "";
                else if (value.length===1)
                    json[key] = value[0];
                else
                    json[key] = value;
            }
        }
        return json;
    }

    execute(request:lkRequest, response: lkResponse)
    {
        this.request = request;
        this.response = response;
        // HUH???
        this.errors = new Errors();
        this.user = LABKEY.getUser();

        let message = null;

        try
        {
            let json = this.bind(this.request, this.errors);
            if (!this.errors.hasErrors())
            {
                this.validate(json, this.errors);
                if (!this.errors.hasErrors())
                {
                    let method = this.request.getMethod();
                    let value = {};
                    if (method === Methods.GET)
                        value = this.handleGet(json, this.errors);
                    else if (method === Methods.POST)
                        value = this.handlePost(json, this.errors);
                    if (!this.errors.hasErrors())
                    {
                        response.setContentType("text/json");
                        response.write(JSON.stringify(value));
                        return;
                    }
                }
            }
        }
        // catch (ex)
        // {
        //     message = ex;
        // }
        finally {}
        response.setContentType("text/json");
        response.write(JSON.stringify(this.failResponse(message,this.errors)));
    }

    public validate(json:any, errors:Errors) : void {}

    public handleGet(json : Object, errors:Errors) : any
    {
    }
    public handlePost(json:any, errors:Errors) : any
    {
    }

    user: lkUser;
    errors: Errors;
    request: lkRequest;
    response: lkResponse;
    methodsAllowed: string[] = [Methods.POST];
    requiresPermission : string[] = [PermissionClass.READ];
}


interface LabKey
{
    getUser():lkUser;
    getContainer():Container;
    getQueryService():QueryService;
}

declare var LABKEY:LabKey;
declare var console:Console;







//---------------------------------------------------






class BeginAction extends JsonApiAction
{
    public handleGet(json:any, errors:Errors) : Object
    {
        console.log("<BeginAction.handleGet>");
        console.log("name="+json.name);
  
    	const ret:any =
		{
			message: 'Hello ' + this.user.getDisplayName(),
			method : 'GET',
			url : this.request.getRequestURI(),
            userAgent : this.request.getHeaders()['user-agent'],
			contextPath : this.request.getContextPath(),
			params : json
		};

        console.log("</BeginAction.handleGet>");
        return this.successResponse(ret);
	}

	methodsAllowed:string[] = ['POST','GET'];
    requiresPermission:string[] = [PermissionClass.READ];
}


class SecondAction extends JsonApiAction
{
    public validate(json:any, errors:Errors) : void
    {
        console.log("json.name=" + json.name);
        if (!json.name)
            errors.rejectValue("name", "value is required");
        if (json.name == 'Fred')
            errors.rejectValue("name", "not that guy");
    }
    public handleGet(json:any, errors:Errors) : Object
    {
        return this.successResponse({name:json.name, answer:42});
    }

    methodsAllowed:string[] = ['GET'];
    requiresPermission:string[] = [PermissionClass.READ];
}



class QueryAction extends JsonApiAction
{
    public validate(json:any, errors:Errors) : void
    {
    }

    public handleGet(json:any, errors:Errors) : Object
    {
        console.log("<QueryAction.execute>");
        let rs:Results = LABKEY.getQueryService().select(
            {
                "schemaName":"core",
                "sql":"SELECT userId, email FROM core.Users"
            });
        let arr = [];
        while (rs.next())
        {
            console.log(rs.getString(2));
            arr.push({"userid":rs.getNumber(1), "email":rs.getString(2)});
        }
        rs.close();

        console.log("</QueryAction.execute>");
        return {users:arr};
    }

    methodsAllowed:string[] = [ Methods.GET ];
    requiresPermission:string[] = [PermissionClass.READ];
}


class HtmlView implements View
{
    render(request: lkRequest, response: lkResponse)
    {
        response.write("<div><h1>Hello World</h1></div>");
        return null;
    }
    requiresPermission:string[] = [PermissionClass.NONE];
}


class ReactView extends BaseReactView
{
    getMarkup()
    {
        return <div><h1>Hello React</h1></div>;
    }
    requiresPermission:string[] = [PermissionClass.NONE];
}

exports.actions =
{
	begin:  BeginAction,
	second: SecondAction,
    query:  QueryAction
};

exports.views =
{
    html:   HtmlView,
    react:  ReactView
};
