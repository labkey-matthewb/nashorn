package org.labkey.nashorn.env;

/**
 * Created by matthew on 5/8/15.
 */
public class User implements _Wrapper
{
    final org.labkey.api.security.User _user;

    public User(org.labkey.api.security.User user)
    {
        this._user = user;
    }

    public String getDisplayName()
    {
        return _user.getDisplayName(_user);
    }

    public int getId()
    {
        return _user.getUserId();
    }

    public String getEmail()
    {
        return _user.getEmail();
    }

    public boolean isGuest()
    {
        return _user.isGuest();
    }

    @Override
    public Object unwrap(Class cls)
    {
        if (cls.isAssignableFrom(_user.getClass()))
            return _user;
        return null;
    }
}
