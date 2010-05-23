/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv.enc;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.mapper.conv.EncodingContext;

/**
 * @author doc
 * 
 */
public interface TypeEncoder {

	// boolean canHandle(T t);
	boolean canHandle(MappedField f);
	
	public Object decode(EncodingContext ctx, MappedField f, Object fromDBObject) throws MappingException;
	
	public Object encode(EncodingContext ctx, MappedField f, Object value) throws MappingException;
	
}
