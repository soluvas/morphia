/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.DBObject;

/**
 * @author doc
 * 
 */
public interface TypeEncoder<T> {

	// boolean canHandle(T t);
	boolean canHandle(MappedField f);
	
	void toDBObject(Datastore ds, MappedField f, Object entity, DBObject dbObject) throws MappingException;
	
	void fromDBObject(Datastore ds, MappedField f, Object entity, DBObject dbObject) throws MappingException;
}
