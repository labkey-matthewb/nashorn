import React from "react";
import ReactDOMServer from "react-dom/server.js";

interface Console
{
    assert(test:boolean, msg:string);
    debug(msg:string) : void;
    error(msg:string) : void;
    info(msg:string) : void;
    log(msg:string) : void;
    warn(msg:string) : void;
}

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
    error(msg:string) : void;
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
    getAuthenticatedUser() : lkUser;
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

// This defines the interface the server expects
interface Action
{
    handleRequest(request:lkRequest, response: lkResponse):void;
    methodsAllowed: string[];
    requiresPermission : string[];
}

abstract class DefaultAction implements Action
{
    abstract handleRequest(request:lkRequest, response: lkResponse):void;
    methodsAllowed: string[];
    requiresPermission : string[];

    validate(json: any, errors:Errors): void
    {
    }

    bind(request: lkRequest, errors:Errors) : any
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
}


abstract class WebPartView extends DefaultAction
{
    handleRequest(request: lkRequest, response: lkResponse):void
    {
        let errors = new Errors();
        let json = this.bind(request,errors)
        if (!errors.hasErrors())
        {
            this.validate(json, errors);
            if (!errors.hasErrors())
            {
                this.render(json, response.getWriter());
                return;
            }
        }
        // TODO ErrorView
        response.write("mistakes were made");
    }
    abstract render(json:any, writer:any):any;
    methodsAllowed: string[];
    requiresPermission : string[];
}

abstract class ReactWebPartView extends WebPartView
{
    render(json:any, writer:any)
    {
        const element = this.getMarkup(json);
        writer.write(ReactDOMServer.renderToStaticMarkup(element));
    }
    abstract getMarkup(json:any):any;
    methodsAllowed: string[];
    requiresPermission : string[];
}
abstract class JsonApiAction extends DefaultAction
{
    public JsonApiAction()
    {
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

    handleRequest(request:lkRequest, response: lkResponse)
    {
        this.request = request;
        this.response = response;
        // HUH???
        this.errors = new Errors();
        this.user = request.getAuthenticatedUser();

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

export { Action, Console, Errors, PermissionClass, Methods, JsonApiAction, WebPartView, ReactWebPartView };
