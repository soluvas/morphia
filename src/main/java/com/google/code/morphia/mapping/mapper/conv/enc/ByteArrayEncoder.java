/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv.enc;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.mapper.conv.EncodingContext;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class ByteArrayEncoder implements TypeEncoder {
	
	@Override
	public boolean canHandle(MappedField f) {
		return f.isMultipleValues() && f.getSubType() == byte.class && f.getType().isArray();
	}
	
	@Override
	public Object decode(EncodingContext ctx, MappedField f, Object fromDBObject) throws MappingException {
		return fromDBObject; // as it comes
	}
	
	@Override
	public Object encode(EncodingContext ctx, MappedField f, Object value) throws MappingException {
		return value;
	}
	
}
