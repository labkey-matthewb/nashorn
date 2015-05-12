package org.labkey.nashorn.env;

import org.labkey.api.view.ViewContext;

/**
 * Created by matthew on 5/8/15.
 */
public class LABKEY
{
    final User _user;
    final Container _container;

    public LABKEY(ViewContext context)
    {
        _user = new User(context.getUser());
        _container = new Container(context.getContainer());
    }

    public User getUser()
    {
        return _user;
    }

    public Container getContainer()
    {
        return _container;
    }

    // How do I return capital Query for this object rather than 'query'?
    public Query getQuery()
    {
        return new Query(_container._container,_user._user);
    }
}
