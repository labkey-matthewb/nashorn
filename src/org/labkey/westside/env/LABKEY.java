package org.labkey.westside.env;

//import jdk.nashorn.api.scripting.AbstractJSObject;
import org.labkey.api.view.ViewContext;

import java.util.Collection;
import java.util.Set;

/**
 * Created by matthew on 5/8/15.
 */
@JSENV
public class LABKEY
{
    final User _user;
    final Container _container;

    public LABKEY(ViewContext context)
    {
        _user = new User(context.getUser());
        _container = new Container(context.getContainer());
    }

    @JSENV
    public User getUser()
    {
        return _user;
    }

    @JSENV
    public Container getContainer()
    {
        return _container;
    }

    @JSENV
    public QueryService getQueryService()
    {
        return new QueryService(_container._container,_user._user);
    }


//    @Override
//    public Object call(Object thiz, Object... args)
//    {
//        return super.call(thiz, args);
//    }
//
//    @Override
//    public Object newObject(Object... args)
//    {
//        return super.newObject(args);
//    }
//
//    @Override
//    public Object eval(String s)
//    {
//        return super.eval(s);
//    }
//
//    @Override
//    public Object getSlot(int index)
//    {
//        return super.getSlot(index);
//    }
//
//    @Override
//    public boolean hasMember(String name)
//    {
//        return super.hasMember(name);
//    }
//
//    @Override
//    public boolean hasSlot(int slot)
//    {
//        return super.hasSlot(slot);
//    }
//
//    @Override
//    public void removeMember(String name)
//    {
//        super.removeMember(name);
//    }
//
//    @Override
//    public void setMember(String name, Object value)
//    {
//        super.setMember(name, value);
//    }
//
//    @Override
//    public void setSlot(int index, Object value)
//    {
//        super.setSlot(index, value);
//    }
//
//    @Override
//    public Set<String> keySet()
//    {
//        return super.keySet();
//    }
//
//    @Override
//    public Collection<Object> values()
//    {
//        return super.values();
//    }
//
//    @Override
//    public boolean isInstance(Object instance)
//    {
//        return super.isInstance(instance);
//    }
//
//    @Override
//    public boolean isInstanceOf(Object clazz)
//    {
//        return super.isInstanceOf(clazz);
//    }
//
//    @Override
//    public String getClassName()
//    {
//        return super.getClassName();
//    }
//
//    @Override
//    public boolean isFunction()
//    {
//        return super.isFunction();
//    }
//
//    @Override
//    public boolean isStrictFunction()
//    {
//        return super.isStrictFunction();
//    }
//
//    @Override
//    public boolean isArray()
//    {
//        return super.isArray();
//    }
//
//    @Override
//    public double toNumber()
//    {
//        return super.toNumber();
//    }
//
//    @Override
//    public Object getMember(String name)
//    {
//        switch (name)
//        {
//            // instance data
//            case "container" : return getContainer();
//            case "user": return getUser();
//
//            // services
//            case "QueryService" : return getQueryService();
//        }
//        return super.getMember(name);
//    }
}
