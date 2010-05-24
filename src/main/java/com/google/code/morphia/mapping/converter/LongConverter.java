/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class LongConverter extends TypeConverter {
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, long.class, Long.class);
	}
	
	@Override
	Object decode(Class targetClass, Object val, MappedField optionalExtraInfo) throws MappingException {
		if (val instanceof String) {
			return Long.parseLong((String) val);
		} else {
			return ((Number) val).longValue();
		}
		
	}
	
}
