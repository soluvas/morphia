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
public class CharacterConverter extends TypeConverter {
	// FIXME us needs test
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, Character.class, char.class);
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		return fromDBObject.toString().charAt(0);
	}
	
}
