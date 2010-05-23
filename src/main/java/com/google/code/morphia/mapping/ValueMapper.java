/**
 * 
 */
package com.google.code.morphia.mapping;

import com.google.code.morphia.mapping.mapper.encoder.EncoderChain;
import com.mongodb.BasicDBObject;

class ValueMapper {
	
	// TODO that should be made configurable
	private final EncoderChain encoderChain = new EncoderChain();
	
	void mapValuesFromDBObject(final BasicDBObject dbObject, final MappedField mf, final Object entity) {
		try {
			encoderChain.fromDBObject(dbObject, mf, entity);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	void mapValuesToDBObject(final Object entity, final MappedField mf, final BasicDBObject dbObject) {
		try {
			encoderChain.toDBObject(entity, mf, dbObject);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
