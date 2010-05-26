/**
 * 
 */
package com.google.code.morphia.converters;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class KeyConverter extends TypeConverter {
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return oneOf(c, Key.class);
	}
	
	@Override
	Object decode(Class targetClass, Object o, MappedField optionalExtraInfo) throws MappingException {
		return new Key((DBRef) o);
	}
	
	@Override
	Object encode(Object t, MappedField optionalExtraInfo) {
		if (t == null)
			return null;
		return ((Key) t).toRef();
	}
	
}
