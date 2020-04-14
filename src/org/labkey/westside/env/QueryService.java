package org.labkey.westside.env;

import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Results;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by matthew on 5/9/15.
 *
 * Like LABKEY.Query, but without queryview, customview stuff.  This is just
 * straight LabKey SQL w/o display columns.
 */

@JSENV
public class QueryService
{
    final org.labkey.api.data.Container _defaultContainer;
    final org.labkey.api.security.User _user;

    public QueryService(org.labkey.api.data.Container container, org.labkey.api.security.User user)
    {
        _defaultContainer = container;
        _user = user;
    }

    @JSENV
    public Results select(Object obj) throws SQLException
    {
// GRAAL
//        String containerPath = (String)obj.get("containerPath");
//        String schemaName = (String)obj.get("schemaName");
//        String sql = (String)obj.get("sql");
//        Map parameters = (Map)obj.get("parameters");
//        org.labkey.api.data.Container c = null==containerPath ? _defaultContainer : ContainerManager.getForPath(containerPath);
//        DefaultSchema rootSchema = DefaultSchema.get(_user, c);
//        QuerySchema schema = rootSchema.getSchema(schemaName);
//        Map<String,TableInfo> tableMap = new HashMap<>();
//        Results rs = org.labkey.api.query.QueryService.get().selectResults(schema, sql, tableMap, parameters, true, true);
//        return rs;
        return null;
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
