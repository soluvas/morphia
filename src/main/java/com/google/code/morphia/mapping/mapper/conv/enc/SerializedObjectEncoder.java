/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv.enc;

import java.io.IOException;

import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.Serializer;
import com.google.code.morphia.mapping.mapper.conv.EncodingContext;
import com.mongodb.DBBinary;

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
	public Object decode(EncodingContext ctx, MappedField f, Object fromDBObject) throws MappingException {
		
		if (!((fromDBObject instanceof DBBinary) || (fromDBObject instanceof byte[]))) {
			throw new MappingException("The stored data is not a DBBinary or byte[] instance for " + f.getFullName()
					+ " ; it is a " + fromDBObject.getClass().getName());
		}
		
		try {
			return Serializer.deserialize(fromDBObject, f.getAnnotation(Serialized.class).compress());
		} catch (IOException e) {
			throw new MappingException("While deserializing to " + f.getFullName(), e);
		} catch (ClassNotFoundException e) {
			throw new MappingException("While deserializing to " + f.getFullName(), e);
		}
	}
	
	@Override
	public Object encode(EncodingContext ctx, MappedField f, Object value) throws MappingException {
		try {
			return Serializer.serialize(value, f.getAnnotation(Serialized.class).compress());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
