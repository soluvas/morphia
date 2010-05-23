/**
 * 
 */
package com.google.code.morphia.mapping;

import com.google.code.morphia.mapping.mapper.conv.EncoderChain;
import com.mongodb.BasicDBObject;

class ValueMapper {
	private final Mapper mapper;
	
	public ValueMapper(Mapper mapper) {
		this.mapper = mapper;
	}
	
	void mapValuesFromDBObject(final BasicDBObject dbObject, final MappedField mf, final Object entity) {
		try {
			// TODO wont stay that way, just to keep it compatible for now.
			new EncoderChain(mapper).fromDBObject(dbObject, mf, entity);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	void mapValuesToDBObject(final Object entity, final MappedField mf, final BasicDBObject dbObject) {
		try {
			
			// TODO wont stay that way, just to keep it compatible for now.
			new EncoderChain(mapper).toDBObject(entity, mf, dbObject);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
