/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv;

/**
 * 
 * @author doc
 * 
 */
public interface TypeConverter {
	
	boolean canHandle(Class<?> targetClass, Object value);
	
	<V> V convert(Class<V> targetClass, Object value) throws TypeConversionException;
}
