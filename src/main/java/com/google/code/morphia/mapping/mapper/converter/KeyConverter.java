/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

import com.google.code.morphia.Key;
import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class KeyConverter implements ValueConverter<Key<?>> {
	
	@Override
	public Key<?> objectFromValue(Object o) {
		return new Key((DBRef) o);
	}
	
	@Override
	public Object valueFromObject(Key<?> t) {
		return t.toRef();
	}
	
}
