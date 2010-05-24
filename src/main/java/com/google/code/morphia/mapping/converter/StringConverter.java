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
public class StringConverter extends TypeConverter {
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, String.class);
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		if (fromDBObject instanceof String) {
			return (String) fromDBObject;
		}
		return fromDBObject.toString();
	}
	
}
