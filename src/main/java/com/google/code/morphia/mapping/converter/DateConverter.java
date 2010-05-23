/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import java.util.Date;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class DateConverter implements ValueConverter<Date>
{

    @Override
    public Date objectFromValue(final Object o)
    {
        if (o instanceof Date)
        {
            Date d = (Date) o;
            return d;
        }
        return new Date(Date.parse(o.toString())); // good luck
    }

    @Override
    public Object valueFromObject(final Date t)
    {
        return t;
    }

}
