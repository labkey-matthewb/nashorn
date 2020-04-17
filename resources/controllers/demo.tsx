import {Errors, JsonApiAction, Methods, PermissionClass, ReactWebPartView, WebPartView} from 'actions';
import React from "react";
import Combobox from 'react-widgets/lib/Combobox'

class MyApiAction extends JsonApiAction
{
    validate(json: any, errors: Errors): void
    {
        if (!json.name)
            errors.rejectValue("name", "Required");
        if (!("i" in json))
            errors.rejectValue("i", "Required");
        else if (isNaN(json.i = parseInt(json.i)))
            errors.rejectValue( "i", "Could not convert integer");
    }

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

	public handlePost(json:any, errors:Errors)
    {
        return this.handleGet(json,errors);
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
/*
        let rs:Results = ServiceManager.getQueryService().select(
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
 */

        console.log("</QueryAction.execute>");
        return {users:[{userid:'a',email:'a@acme.test'}]};
    }

    public handlePost(json:any, errors:Errors)
    {
        return this.handleGet(json,errors);
    }

    methodsAllowed:string[] = [ Methods.GET, Methods.POST ];
    requiresPermission:string[] = [PermissionClass.READ];
}


class HtmlView extends WebPartView
{
    render(params, writer)
    {
        writer.write("<div><h1>Hello World</h1></div>");
        return null;
    }
    requiresPermission:string[] = [PermissionClass.NONE];
}

function Hello(props)
{
    return <h1>Hello, {props.name}</h1>;
}

class ReactView extends ReactWebPartView
{
    getMarkup(json)
    {
        const names = ["matt","adam","kevin"];
        return(
            <div>
                <link href="/labkey/react-widgets.css" type="text/css" rel="stylesheet" />
                <div>
                    {names.map((name,index) => <Hello key={index} name={name}/>)}
                    <Combobox data={names}/>
                </div>
            </div>
        );
    }
    requiresPermission:string[] = [PermissionClass.NONE];
}

// differences between actions and views:
// a) wrapping of views in template
// b) default contentType
// c) rendering of unhandled exceptions

export default {
    actions:
    {
        myapi:  MyApiAction,            // class
        second: new SecondAction(),     // object
        query:  QueryAction
    },
    views:
    {
        html:   HtmlView,
        react:  ReactView
    }
};