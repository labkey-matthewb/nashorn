class Action
{
	public validate(request,json,errors) : void {}
    public execute(request,json,errors) : Object
    {
        return {success:true};
    }
    public execute_POST(request,json,errors) : Object
	{
		return this.execute(request,json,errors);
	}
    public execute_GET(request,json,errors) : Object
	{
		return this.execute(request,json,errors);
	}
	methodsAllowed: string[];
	requiresPermission : string;
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
interface LabKey
{
    user:User;
    container:Container;
}

declare var LABKEY:LabKey;





//---------------------------------------------------






class BeginAction extends Action
{
	public execute(request:Request,json,errors) : Object
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

	requiresPermission:string = "ReadPermission";
}


class SecondAction extends Action
{
    public execute(request,json,errors):Object
    {
        return {success:true, answer:42};
    }

    methodsAllowed:string[] = ['GET'];

    requiresPermission:string = "ReadPermission";
}

// use export.actions instead?
var actions:Object =
{
	begin: new BeginAction(),
	second: new SecondAction()
};
