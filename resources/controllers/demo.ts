//@ sourceURL=optionalModules/nashorn/resources/controllers/demo.js

var Permissions =
{
    READ:"org.labkey.api.security.permissions.ReadPermission",
    DELETE:"org.labkey.api.security.permissions.DeletePermission",
    UPDATE:"org.labkey.api.security.permissions.UpdatePermission",
    INSERT:"org.labkey.api.security.permissions.InsertPermission",
    ADMIN:"org.labkey.api.security.permissions.AdminPermission"
};
interface ValidationError
{
    message?:string;
    field?:string;           // TODO: field does not work with org.springframework.validation.BindException, may need custom implementation of Errors
    errorCode?:string;
    arguments?:any[];
}
interface Errors
{
    reject(error:ValidationError):void;
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
interface Query
{
}
interface Request
{
    url: string;
    contextPath: string;
    headers: Object;
}
interface User
{
    id:number;
    email:string;
    displayName:string;
}
class Action
{
    public validate(request:Request, json:any, errors:Errors) : void {}
    public execute(request:Request, json:any, errors:Errors) : Object
    {
        return {success:true};
    }
    public execute_POST(request:Request, json:any, errors:Errors) : Object
    {
        return this.execute(request,json,errors);
    }
    public execute_GET(request:Request, json:any, errors:Errors) : Object
    {
        return this.execute(request,json,errors);
    }
    methodsAllowed: string[];
    requiresPermission : string;
}
interface LabKey
{
    user:User;
    container:Container;
}

declare var LABKEY:LabKey;
declare var console:Console;





//---------------------------------------------------




class BeginAction extends Action
{
	public execute(request:Request,json:any, errors:Errors) : Object
	{
        console.log("name="+json.name);
        var name = json.name || LABKEY.user.displayName;
        var ret:any = {success:true};
        ret.message = 'Hello ' + name;
        ret.method = 'GET';
        ret.url = request.url.toString();
        ret['user-agent'] = request.headers['user-agent'];
        ret.contextPath = request.contextPath;
        ret.params = json;
        return ret;
	}

	methodsAllowed:string[] = ['POST','GET'];

    requiresPermission:string = Permissions.READ;
}


class SecondAction extends Action
{
    public validate(request:Request, json:any, errors:Errors) : void
    {
        console.log("json.name=" + json.name);
        if (json.name == 'Fred')
        {
            errors.reject({message:'not that guy'});
        }
    }
    public execute(request:Request,json:any, errors:Errors) : Object
    {
        return {success:true, answer:42};
    }

    methodsAllowed:string[] = ['GET'];

    requiresPermission:string = Permissions.READ;
}


// CONSIDER: "tsc --module" and "export var actions"?
// CONSIDER: advantages? disadvantages?

var actions:Object =
{
	begin: new BeginAction(),
	second: new SecondAction()
};
