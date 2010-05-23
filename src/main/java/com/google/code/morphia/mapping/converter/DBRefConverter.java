/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class DBRefConverter implements ValueConverter<DBRef>
{

    @Override
    public DBRef objectFromValue(final Object o)
    {
        return (DBRef) o;
    }

    @Override
    public Object valueFromObject(final DBRef t)
    {
        return t;
    }

}
