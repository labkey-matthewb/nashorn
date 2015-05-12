class Action
{
	validate(json,errors) : void {}
    public execute(json,errors) : Object
    {
        return {success:true};
    }
    public execute_POST(json,errors) : Object
	{
		return this.execute(json,errors);
	}
    public execute_GET(json,errors) : Object
	{
		return this.execute(json,errors);
	}
	methodsAllowed: string[];
	requiresPermission : string;
}


class BeginAction extends Action
{
	execute(json,errors)
	{
        console.log("name="+json.name);
        var ret:Object = {success:true};
		if (json.name)
			ret['message'] = 'Hello ' + json.name;
		else
            ret['message'] = 'Hello World';
        ret['method='] = 'GET';
        ret['url'] = request.url.toString();
        ret['user-agent'] = request.headers['user-agent'];
        ret['contextPath'] = request.contextPath;
	}

	methodsAllowed:string[] = ['POST','GET'];

	requiresPermission:string = "ReadPermission";
}


class SecondAction extends Action
{
    public execute(json,errors):Object
    {
        return {success:true, answer:42};
    }

    methodsAllowed:string[] = ['GET'];

    requiresPermission:string = "ReadPermission";
}


var actions:Object =
{
	begin: new BeginAction(),
	second: new SecondAction()
};
