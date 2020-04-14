//@ sourceURL=optionalModules/nashorn/resources/controllers/demo.js

const PermissionClass =
{
    READ:"org.labkey.api.security.permissions.ReadPermission",
    DELETE:"org.labkey.api.security.permissions.DeletePermission",
    UPDATE:"org.labkey.api.security.permissions.UpdatePermission",
    INSERT:"org.labkey.api.security.permissions.InsertPermission",
    ADMIN:"org.labkey.api.security.permissions.AdminPermission"
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
        return this.errors && 0!=this.errors.length;
    }
    public reject(message:string)
    {
        const error = new ValidationError();
        error.message = message;
        this.errors.push(error)
    }
    public rejectValue(field:string, message:string)
    {
        const error = new ValidationError();
        error.message = message;
        error.field = field;
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
interface Action
{
    execute(request:lkRequest, response: lkResponse);
    methodsAllowed: string[];
    requiresPermission : string[];
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
            // TODO
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

        var message = null;

        try
        {
            var json = this.bind(this.request, this.errors);
            if (!this.errors.hasErrors())
            {
                this.validate(json, this.errors);
                if (!this.errors.hasErrors())
                {
                    var method = this.request.getMethod();
                    var value = {};
                    if (method === "GET")
                        value = this.handleGet(json, this.errors);
                    else if (method === "POST")
                        value = this.handlePost(json, this.errors);
                    if (!this.errors.hasErrors())
                    {
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
    methodsAllowed: string[] = ["POST"];
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
        if (json.name == 'Fred')
        {
            errors.reject("not that guy");
        }
    }
    public handleGET(json:any, errors:Errors) : Object
    {
        return {success:true, answer:42};
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
        var rs:Results = LABKEY.getQueryService().select(
            {
                "schemaName":"core",
                "sql":"SELECT userId, email FROM core.Users"
            });
        // TODO Jackson treats nashorn array as a generic Object e.g. {"0":"zero", "1","one"} instead of ["zero","one"]
        var arr = [];
        while (rs.next())
        {
            console.log(rs.getString(2));
            arr.push({"userid":rs.getNumber(1), "email":rs.getString(2)});
        }
        rs.close();

        console.log("</QueryAction.execute>");
        return {users:arr};
    }

    methodsAllowed:string[] = ['GET'];

    requiresPermission:string[] = [PermissionClass.READ];
}



// CONSIDER: "tsc --module" and "export var actions"?
// CONSIDER: advantages? disadvantages?

var actions:Object =
{
	begin:  BeginAction,
	second: SecondAction,
    query:  QueryAction
};
