package org.labkey.nashorn.env;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.labkey.api.action.SpringActionController;

/**
 * Created by matthew on 5/19/15.
 */
public class Errors
{
    final org.springframework.validation.Errors _errors;

    public Errors(org.springframework.validation.Errors errors)
    {
        this._errors = errors;
    }

    public void reject(ScriptObjectMirror obj)
    {
        String field = getString(obj,"field");
        String message = getString(obj,"message");
        String errorCode = getString(obj,"errorCode");
        if (null == errorCode)
            errorCode = SpringActionController.ERROR_GENERIC;
        if (null == field)
        {
            _errors.reject(errorCode, message);
        }
        else
        {
            _errors.rejectValue(field, errorCode, message);
        }
    }


    private String getString(ScriptObjectMirror obj, String fieldName)
    {
        Object o = obj.get(fieldName);
        if (null == o)
            return null;
        return String.valueOf(o);
    }
}

