
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


        var secondAction = 
        {
	    execute:function(json,errors)
	    {
		return {success:true, answer:2};
	    },
	    requiresPermission:"ReadPermission"
	}

	return actions =
	{
	    begin: beginAction,
	    second: secondAction
	};

})();
