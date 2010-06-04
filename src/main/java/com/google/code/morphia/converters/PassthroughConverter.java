/**
 * 
 */
package com.google.code.morphia.converters;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
@SuppressWarnings("unchecked")
public class PassthroughConverter extends TypeConverter {
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return true;
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
		return fromDBObject;
	}
	
}
