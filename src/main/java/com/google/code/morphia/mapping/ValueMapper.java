/**
 * 
 */
package com.google.code.morphia.mapping;

import com.google.code.morphia.mapping.converter.ConverterChain;
import com.mongodb.BasicDBObject;

class ValueMapper
{

	private final ConverterChain chain;
	
	public ValueMapper(ConverterChain chain) {
		this.chain = chain;
	}

	void mapValuesFromDBObject(final BasicDBObject dbObject, final MappedField mf, final Object entity)
    {
        try
        {
			chain.fromDBObject(dbObject, mf, entity);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    void mapValuesToDBObject(final Object entity, final MappedField mf, final BasicDBObject dbObject)
    {
        try
        {
			chain.toDBObject(entity, mf, dbObject);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
