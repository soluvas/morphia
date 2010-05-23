/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public interface ValueConverter<T> {
	T objectFromValue(Object o);
	
	Object valueFromObject(T t);
}
