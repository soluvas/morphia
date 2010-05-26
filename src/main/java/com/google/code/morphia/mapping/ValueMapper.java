/**
 * 
 */
package com.google.code.morphia.mapping;

import com.google.code.morphia.converters.DefaultConverters;
import com.mongodb.BasicDBObject;

class ValueMapper
{

	private final DefaultConverters converters;
	
	public ValueMapper(DefaultConverters converters) {
		this.converters = converters;
	}

	void fromDBObject(final BasicDBObject dbObject, final MappedField mf, final Object entity)
    {
        try
        {
			converters.fromDBObject(dbObject, mf, entity);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    void toDBObject(final Object entity, final MappedField mf, final BasicDBObject dbObject)
    {
        try
        {
			converters.toDBObject(entity, mf, dbObject);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
