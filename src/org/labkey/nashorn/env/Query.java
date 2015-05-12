package org.labkey.nashorn.env;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONObject;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Results;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by matthew on 5/9/15.
 */

public class Query
{
    final org.labkey.api.data.Container _defaultContainer;
    final org.labkey.api.security.User _user;

    public Query(org.labkey.api.data.Container container, org.labkey.api.security.User user)
    {
        _defaultContainer = container;
        _user = user;
    }

    // CONSIDER
    // streaming v. non-streaming,
    // async v. sync
    // just get the data
    public ResultSet executeDirect(ScriptObjectMirror obj) throws SQLException
    {
        String containerPath = (String)obj.get("containerPath");
        String schemaName = (String)obj.get("schemaName");
        String sql = (String)obj.get("sql");
        Map parameters = (Map)obj.get("parameters");
        org.labkey.api.data.Container c = null==containerPath ? _defaultContainer : ContainerManager.getForPath(containerPath);
        DefaultSchema rootSchema = DefaultSchema.get(_user, c);
        QuerySchema schema = rootSchema.getSchema("schemaName");
        ResultSet rs = QueryService.get().select(schema, sql, null, true, true);
        return rs;
    }

    // like client side executeSql() or DataRegion
    public ResultSet executeForDisplay(ScriptObjectMirror obj) throws SQLException
    {
        String containerPath = (String)obj.get("containerPath");
        String schemaName = (String)obj.get("schemaName");
        String sql = (String)obj.get("sql");
        Map parameters = (Map)obj.get("parameters");
        org.labkey.api.data.Container c = null==containerPath ? _defaultContainer : ContainerManager.getForPath(containerPath);
        DefaultSchema rootSchema = DefaultSchema.get(_user, c);
        QuerySchema schema = rootSchema.getSchema("schemaName");
        ResultSet rs = QueryService.get().select(schema, sql, null, true, true);
        return rs;
    }
}



/*
Example, from the Reagent Request Confirmation Tutorial and Demo:
         // This snippet extracts a table of UserID, TotalRequests and
         // TotalQuantity from the "Reagent Requests" list.
         // Upon success, the writeTotals function (not included here) uses the
         // returned data object to display total requests and total quantities.

             LABKEY.Query.executeSql({
                     containerPath: 'home/Study/demo/guestaccess',
                     schemaName: 'lists',
                     sql: 'SELECT "Reagent Requests".UserID AS UserID, \
                         Count("Reagent Requests".UserID) AS TotalRequests, \
                         Sum("Reagent Requests".Quantity) AS TotalQuantity \
                         FROM "Reagent Requests" Group BY "Reagent Requests".UserID',
                     success: writeTotals
             });
 */
