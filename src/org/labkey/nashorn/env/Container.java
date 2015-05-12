package org.labkey.nashorn.env;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.util.GUID;

/**
 * Created by matthew on 5/8/15.
 */
public class Container implements _Wrapper
{
    final org.labkey.api.data.Container _container;

    public Container(org.labkey.api.data.Container container)
    {
        this._container = container;
    }

    @NotNull
    public String getName()
    {
        return _container.getName();
    }

    public Container getParent()
    {
        if (null == _container.getParent())
            return null;
        return new Container(_container.getParent());
    }

    public String getPath()
    {
        return _container.getPath();
    }

    public String getId()
    {
        return _container.getId();
    }

    public int getRowId()
    {
        return _container.getRowId();
    }

    public boolean hasPermission(@NotNull User user, Object perm)
    {
        if (perm instanceof Class)
            return _container.hasPermission(user._user, (Class)perm);
        if (perm instanceof String)
        {
            try
            {
                Class clss = Class.forName("org.labkey.api.security.permissions." + (String)perm);
                return _container.hasPermission(user._user, clss);
            }
            catch (ClassNotFoundException x)
            {
                throw new RuntimeException("Permission not found: " + perm);
            }
        }
        throw new RuntimeException("Expected class or string permission");
    }


    @Override
    public Object unwrap(Class cls)
    {
        if (cls.isAssignableFrom(_container.getClass()))
            return _container;
        return null;
    }
}
