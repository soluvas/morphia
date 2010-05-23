/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv;

import java.io.IOException;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.Serializer;
import com.mongodb.DBBinary;
import com.mongodb.DBObject;

/**
 * @author doc
 *
 */
@SuppressWarnings("unchecked")
public class SerializedObjectEncoder implements TypeEncoder {
	
	@Override
	public boolean canHandle(MappedField f) {
		return (f.hasAnnotation(Serialized.class));
	}
	
	@Override
	public void fromDBObject(Datastore ds, MappedField f, Object entity, DBObject dbObject) throws MappingException {
		Object data = dbObject.get(f.getMappedFieldName());
		if (!((data instanceof DBBinary) || (data instanceof byte[]))) {
			throw new MappingException("The stored data is not a DBBinary or byte[] instance for " + f.getFullName()
					+ " ; it is a " + data.getClass().getName());
		}
		
		try {
			f.setFieldValue(entity, Serializer.deserialize(data, f.getAnnotation(Serialized.class).compress()));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Unable to deserialize " + data + " on field " + f.getFullName(), ex);
		}
	}
	

	@Override
	public void toDBObject(Datastore ds, MappedField f, Object entity, DBObject dbObject) throws MappingException {
		try {
			dbObject.put(f.getMappedFieldName(), Serializer.serialize(f.getFieldValue(entity), f.getAnnotation(
					Serialized.class).compress()));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
