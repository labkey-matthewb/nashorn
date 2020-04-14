//@ sourceURL=optionalModules/nashorn/resources/controllers/demo.js
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var PermissionClass = {
    READ: "org.labkey.api.security.permissions.ReadPermission",
    DELETE: "org.labkey.api.security.permissions.DeletePermission",
    UPDATE: "org.labkey.api.security.permissions.UpdatePermission",
    INSERT: "org.labkey.api.security.permissions.InsertPermission",
    ADMIN: "org.labkey.api.security.permissions.AdminPermission"
};
var Methods = {
    GET: "GET",
    HEAD: "HEAD",
    POST: "POST",
    PUT: "PUT",
    DELETE: "DELETE",
    OPTIONS: "OPTIONS",
    TRACE: "TRACE"
};
var ValidationError = /** @class */ (function () {
    function ValidationError() {
    }
    return ValidationError;
}());
var Errors = /** @class */ (function () {
    function Errors() {
    }
    Errors.prototype.Errors = function () {
        this.errors = [];
    };
    Errors.prototype.hasErrors = function () {
        return this.errors && 0 != this.errors.length;
    };
    Errors.prototype.reject = function (message) {
        var error = new ValidationError();
        error.message = message;
        if (!this.errors)
            this.errors = [];
        this.errors.push(error);
    };
    Errors.prototype.rejectValue = function (field, message) {
        var error = new ValidationError();
        error.message = message;
        error.field = field;
        if (!this.errors)
            this.errors = [];
        this.errors.push(error);
    };
    return Errors;
}());
var JsonApiAction = /** @class */ (function () {
    function JsonApiAction() {
        this.methodsAllowed = [Methods.POST];
        this.requiresPermission = [PermissionClass.READ];
    }
    JsonApiAction.prototype.JsonApiAction = function () {
        this.user = LABKEY.getUser();
        this.errors = new Errors();
    };
    JsonApiAction.prototype.failResponse = function (message, errors) {
        return { success: false, message: message, errors: errors };
    };
    JsonApiAction.prototype.successResponse = function (value) {
        return { success: true, value: value };
    };
    JsonApiAction.prototype.bind = function (request, errors) {
        var json = {};
        if (request.getContentType() === "text/json") {
            json = JSON.parse(request.getBodyAsString());
        }
        else {
            var params = JSON.parse(request.getParameterMapJSON());
            for (var key in params) {
                if (!params.hasOwnProperty(key))
                    continue;
                var value = params[key];
                console.log("key=" + key + " value=" + value);
                if (value.length === 0)
                    json[key] = "";
                else if (value.length === 1)
                    json[key] = value[0];
                else
                    json[key] = value;
            }
        }
        return json;
    };
    JsonApiAction.prototype.execute = function (request, response) {
        this.request = request;
        this.response = response;
        // HUH???
        this.errors = new Errors();
        this.user = LABKEY.getUser();
        var message = null;
        try {
            var json = this.bind(this.request, this.errors);
            if (!this.errors.hasErrors()) {
                this.validate(json, this.errors);
                if (!this.errors.hasErrors()) {
                    var method = this.request.getMethod();
                    var value = {};
                    if (method === Methods.GET)
                        value = this.handleGet(json, this.errors);
                    else if (method === Methods.POST)
                        value = this.handlePost(json, this.errors);
                    if (!this.errors.hasErrors()) {
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
        finally { }
        response.setContentType("text/json");
        response.write(JSON.stringify(this.failResponse(message, this.errors)));
    };
    JsonApiAction.prototype.validate = function (json, errors) { };
    JsonApiAction.prototype.handleGet = function (json, errors) {
    };
    JsonApiAction.prototype.handlePost = function (json, errors) {
    };
    return JsonApiAction;
}());
//---------------------------------------------------
var BeginAction = /** @class */ (function (_super) {
    __extends(BeginAction, _super);
    function BeginAction() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.methodsAllowed = ['POST', 'GET'];
        _this.requiresPermission = [PermissionClass.READ];
        return _this;
    }
    BeginAction.prototype.handleGet = function (json, errors) {
        console.log("<BeginAction.handleGet>");
        console.log("name=" + json.name);
        var ret = {
            message: 'Hello ' + this.user.getDisplayName(),
            method: 'GET',
            url: this.request.getRequestURI(),
            userAgent: this.request.getHeaders()['user-agent'],
            contextPath: this.request.getContextPath(),
            params: json
        };
        console.log("</BeginAction.handleGet>");
        return this.successResponse(ret);
    };
    return BeginAction;
}(JsonApiAction));
var SecondAction = /** @class */ (function (_super) {
    __extends(SecondAction, _super);
    function SecondAction() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.methodsAllowed = ['GET'];
        _this.requiresPermission = [PermissionClass.READ];
        return _this;
    }
    SecondAction.prototype.validate = function (json, errors) {
        console.log("json.name=" + json.name);
        if (!json.name)
            errors.rejectValue("name", "value is required");
        if (json.name == 'Fred')
            errors.rejectValue("name", "not that guy");
    };
    SecondAction.prototype.handleGet = function (json, errors) {
        return this.successResponse({ name: json.name, answer: 42 });
    };
    return SecondAction;
}(JsonApiAction));
var QueryAction = /** @class */ (function (_super) {
    __extends(QueryAction, _super);
    function QueryAction() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.methodsAllowed = [Methods.GET];
        _this.requiresPermission = [PermissionClass.READ];
        return _this;
    }
    QueryAction.prototype.validate = function (json, errors) {
    };
    QueryAction.prototype.handleGet = function (json, errors) {
        console.log("<QueryAction.execute>");
        var rs = LABKEY.getQueryService().select({
            "schemaName": "core",
            "sql": "SELECT userId, email FROM core.Users"
        });
        var arr = [];
        while (rs.next()) {
            console.log(rs.getString(2));
            arr.push({ "userid": rs.getNumber(1), "email": rs.getString(2) });
        }
        rs.close();
        console.log("</QueryAction.execute>");
        return { users: arr };
    };
    return QueryAction;
}(JsonApiAction));
// CONSIDER: "tsc --module" and "export var actions"?
// CONSIDER: advantages? disadvantages?
var actions = {
    begin: BeginAction,
    second: SecondAction,
    query: QueryAction
};
