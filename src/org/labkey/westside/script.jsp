<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("Ext4"));
        resources.add(ClientDependency.fromPath("codemirror"));
        return resources;
    }
%>
<table>
<tr>
<td valign=top>
    <pre id="console" style="border:solid 1pt black; width:600pt; height:640pt; margin:0;"></pre>
    <input name=line id="line" style="border:solid 1pt black; width:600pt; margin:0;" onkeypress="line_onKeyPress()">
</td>
<td valign=top>
    <textarea name=script id="script" style="border:solid 1pt black; width:800pt; height:640pt;"></textarea>
    <br>
    <button onclick="script_onClick()">eval</button>
</td>
</tr>
</table>
<br>
<br>
<script type="text/javascript">

    function line_onKeyPress()
    {
        if (13!=event.keyCode)
            return;
        var script = Ext4.get("line").getValue();
        if (!script)
            return false;
        asyncEvalScript(script, onScriptSuccess, onScriptFailure);
        return false;
    }

    function script_onClick()
    {
        var script = Ext4.get("script").getValue();
        if (!script)
            return false;
        asyncEvalScript(script, onScriptSuccess, onScriptFailure);
        return false;
    }

    function onScriptSuccess(ret,script)
    {
        //Ext4.get("line").setValue("");
        Ext4.get("line").dom.value = "";

        var msg = [];
        if (script && script.indexOf("\n") == -1)
            msg.push("<span style=\"color:gray;\">&gt;&gt; " + Ext4.util.Format.htmlEncode(script) + "</span>\n");;
        msg.push(Ext4.util.Format.htmlEncode(ret.result));
        Ext4.DomHelper.append(Ext4.get("console"), {cn: msg});
    }

    function onScriptFailure(ret,script)
    {
        var msg = [];
        if (script && script.indexOf("\n") == -1)
            msg.push(">> " + script + "\n");;
        msg.push("<div class=labkey-error>");
        if (ret.message)
            msg.push(Ext4.util.Format.htmlEncode(ret.message) + "\n");
        if ('lineNumber' in ret && script.indexOf("\n") != -1)
            msg.push("line: " + Ext4.util.Format.htmlEncode(ret.lineNumber));
        if ('columnNumber' in ret && ret.columnNumber >= 0)
            msg.push("column: " + Ext4.util.Format.htmlEncode(ret.columnNumber));
        msg.push("</div>");
        Ext4.DomHelper.append(Ext4.get("console"), {cn: msg});
    }

    function onRequestFailure(response)
    {
        alert(JSON.stringify(response.responseText));
    }

    function asyncEvalScript(script,success,failure)
    {
        Ext4.Ajax.request(
                {
                    "url": window.location,
                    "method":"POST",
                    "params":
                    {
                        script:script
                    },
                    "success": function(response)
                    {
                        var json = JSON.parse(response.responseText);
                        if (json.success == true)
                            success(json,script);
                        else
                            failure(json,script);
                    },
                    "failure": onRequestFailure
                });

    }
</script>
