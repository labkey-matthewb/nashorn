var __extends = this.__extends || function (d, b) {
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
    Action.prototype.validate = function (request, json, errors) {
    };
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
        console.log("name=" + json.name);
        var name = json.name || LABKEY.user.displayName;
        var ret = { success: true };
        ret.message = 'Hello ' + name;
        ret.method = 'GET';
        ret.url = request.url.toString();
        ret['user-agent'] = request.headers['user-agent'];
        ret.contextPath = request.contextPath;
        ret.params = json;
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
// use export.actions instead?
var actions = {
    begin: new BeginAction(),
    second: new SecondAction()
};
