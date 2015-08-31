//@ sourceURL=optionalModules/nashorn/resources/controllers/demo.js
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    __.prototype = b.prototype;
    d.prototype = new __();
};
var Permissions = {
    READ: "org.labkey.api.security.permissions.ReadPermission",
    DELETE: "org.labkey.api.security.permissions.DeletePermission",
    UPDATE: "org.labkey.api.security.permissions.UpdatePermission",
    INSERT: "org.labkey.api.security.permissions.InsertPermission",
    ADMIN: "org.labkey.api.security.permissions.AdminPermission"
};
var Action = (function () {
    function Action() {
    }
    Action.prototype.validate = function (request, json, errors) { };
    Action.prototype.execute = function (request, json, errors) {
        return { success: true };
    };
    Action.prototype.execute_POST = function (request, json, errors) {
        return this.execute(request, json, errors);
    };
    Action.prototype.execute_GET = function (request, json, errors) {
        return this.execute(request, json, errors);
    };
    return Action;
})();
//---------------------------------------------------
var BeginAction = (function (_super) {
    __extends(BeginAction, _super);
    function BeginAction() {
        _super.apply(this, arguments);
        this.methodsAllowed = ['POST', 'GET'];
        this.requiresPermission = Permissions.READ;
    }
    BeginAction.prototype.execute = function (request, json, errors) {
        console.log("<BeginAction.execute>");
        console.log("name=" + json.name);
        var ret = {
            success: true,
            message: 'Hello ' + (json.name || LABKEY.user.displayName),
            method: 'GET',
            url: request.url.toString(),
            userAgent: request.headers['user-agent'],
            contextPath: request.contextPath,
            params: json
        };
        console.log("</BeginAction.execute>");
        return ret;
    };
    return BeginAction;
})(Action);
var SecondAction = (function (_super) {
    __extends(SecondAction, _super);
    function SecondAction() {
        _super.apply(this, arguments);
        this.methodsAllowed = ['GET'];
        this.requiresPermission = Permissions.READ;
    }
    SecondAction.prototype.validate = function (request, json, errors) {
        console.log("json.name=" + json.name);
        if (json.name == 'Fred') {
            errors.reject({ message: 'not that guy' });
        }
    };
    SecondAction.prototype.execute = function (request, json, errors) {
        return { success: true, answer: 42 };
    };
    return SecondAction;
})(Action);
var QueryAction = (function (_super) {
    __extends(QueryAction, _super);
    function QueryAction() {
        _super.apply(this, arguments);
        this.methodsAllowed = ['GET'];
        this.requiresPermission = Permissions.READ;
    }
    QueryAction.prototype.validate = function (request, json, errors) {
    };
    QueryAction.prototype.execute = function (request, json, errors) {
        console.log("<QueryAction.execute>");
        var rs = LABKEY.QueryService.select({
            "schemaName": "core",
            "sql": "SELECT userId, email FROM core.Users"
        });
        // TODO Jackson treats nashorn array as a generic Object e.g. {"0":"zero", "1","one"} instead of ["zero","one"]
        var arr = [];
        while (rs.next()) {
            console.log(rs.getString(2));
            arr.push({ "userid": rs.getNumber(1), "email": rs.getString(2) });
        }
        rs.close();
        console.log("</QueryAction.execute>");
        return { success: true, users: arr };
    };
    return QueryAction;
})(Action);
// CONSIDER: "tsc --module" and "export var actions"?
// CONSIDER: advantages? disadvantages?
var actions = {
    begin: new BeginAction(),
    second: new SecondAction(),
    query: new QueryAction()
};
