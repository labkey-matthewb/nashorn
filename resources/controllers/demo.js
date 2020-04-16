"use strict";
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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
// TODO There must be a shorter syntax for this
var actions_1 = require("actions");
var react_1 = __importDefault(require("react"));
var BeginAction = /** @class */ (function (_super) {
    __extends(BeginAction, _super);
    function BeginAction() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.methodsAllowed = ['POST', 'GET'];
        _this.requiresPermission = [actions_1.PermissionClass.READ];
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
    BeginAction.prototype.handlePost = function (json, errors) {
        return this.handleGet(json, errors);
    };
    return BeginAction;
}(actions_1.JsonApiAction));
var SecondAction = /** @class */ (function (_super) {
    __extends(SecondAction, _super);
    function SecondAction() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.methodsAllowed = ['GET'];
        _this.requiresPermission = [actions_1.PermissionClass.READ];
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
}(actions_1.JsonApiAction));
var QueryAction = /** @class */ (function (_super) {
    __extends(QueryAction, _super);
    function QueryAction() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.methodsAllowed = [actions_1.Methods.GET, actions_1.Methods.POST];
        _this.requiresPermission = [actions_1.PermissionClass.READ];
        return _this;
    }
    QueryAction.prototype.validate = function (json, errors) {
    };
    QueryAction.prototype.handleGet = function (json, errors) {
        console.log("<QueryAction.execute>");
        // let rs:Results = ServiceManager.getQueryService().select(
        //     {
        //         "schemaName":"core",
        //         "sql":"SELECT userId, email FROM core.Users"
        //     });
        // let arr = [];
        // while (rs.next())
        // {
        //     console.log(rs.getString(2));
        //     arr.push({"userid":rs.getNumber(1), "email":rs.getString(2)});
        // }
        // rs.close();
        console.log("</QueryAction.execute>");
        return { users: [{ userid: 'a', email: 'a@acme.test' }] };
    };
    QueryAction.prototype.handlePost = function (json, errors) {
        return this.handleGet(json, errors);
    };
    return QueryAction;
}(actions_1.JsonApiAction));
var HtmlView = /** @class */ (function (_super) {
    __extends(HtmlView, _super);
    function HtmlView() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.requiresPermission = [actions_1.PermissionClass.NONE];
        return _this;
    }
    HtmlView.prototype.render = function (params, writer) {
        writer.write("<div><h1>Hello World</h1></div>");
        return null;
    };
    return HtmlView;
}(actions_1.WebPartView));
var ReactView = /** @class */ (function (_super) {
    __extends(ReactView, _super);
    function ReactView() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.requiresPermission = [actions_1.PermissionClass.NONE];
        return _this;
    }
    ReactView.prototype.getMarkup = function (params) {
        return react_1.default.createElement("div", null,
            react_1.default.createElement("h1", null, "Hello React"));
    };
    return ReactView;
}(actions_1.ReactWebPartView));
function Hello(props) {
    return react_1.default.createElement("h1", null,
        "Hello, ",
        props.name);
}
var React2View = /** @class */ (function (_super) {
    __extends(React2View, _super);
    function React2View() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.requiresPermission = [actions_1.PermissionClass.NONE];
        return _this;
    }
    React2View.prototype.getMarkup = function (json) {
        var names = ["matt", "adam", "kevin"];
        return (react_1.default.createElement("div", null, names.map(function (name, index) { return react_1.default.createElement(Hello, { key: index, name: name }); })));
    };
    return React2View;
}(actions_1.ReactWebPartView));
// differences between actions and views:
// a) wrapping of views in template
// b) default contentType
// c) rendering of unhandled exceptions
exports.default = {
    actions: {
        begin: BeginAction,
        second: new SecondAction(),
        query: QueryAction
    },
    views: {
        html: HtmlView,
        react: React2View
    }
};
//# sourceMappingURL=demo.js.map