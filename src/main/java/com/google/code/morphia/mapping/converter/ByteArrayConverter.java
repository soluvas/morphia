/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ByteArrayConverter extends TypeConverter {
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, byte[].class); // TODO add Byte[]
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		return fromDBObject; // as it comes
	}
	
}
