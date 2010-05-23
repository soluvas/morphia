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
@SuppressWarnings("unchecked")
public class ValueEncoder implements TypeEncoder {
	
	@Override
	public boolean canHandle(MappedField f) {
		return true; // TODO us: change to ValueConv.supportedValueType()...
	}
	
	@Override
	public Object decode(EncodingContext ctx, MappedField f, Object fromDBObject) throws MappingException {
		
		return ctx.getMapper().objectFromValue(f.getType(), fromDBObject);
	}
	
	@Override
	public Object encode(EncodingContext ctx, MappedField f, Object value) throws MappingException {
		return ctx.getMapper().objectToValue(value);
	}
	
}
