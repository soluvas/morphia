/**
 * 
 */
package com.google.code.morphia.mapping.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class BooleanConverter implements ValueConverter<Boolean>
{

    @Override
    public Boolean objectFromValue(final Object val)
    {
        Object dbValue = val;
        if (dbValue instanceof Boolean)
        {
            return (Boolean) val;
        }
        String sVal = val.toString();
        return Boolean.parseBoolean(sVal);
    }

    @Override
    public Object valueFromObject(final Boolean t)
    {
        return t;
    }

}
