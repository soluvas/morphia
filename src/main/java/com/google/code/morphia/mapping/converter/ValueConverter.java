/**
 * 
 */
package com.google.code.morphia.mapping.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public interface ValueConverter<T> {
	T objectFromValue(Object o);
	
	Object valueFromObject(T t);
}
