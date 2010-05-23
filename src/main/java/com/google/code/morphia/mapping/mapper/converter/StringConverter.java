/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class StringConverter implements ValueConverter<String> {
	
	@Override
	public String objectFromValue(Object o) {
		return o.toString();
	}
	
	@Override
	public Object valueFromObject(String t) {
		return t.toString();
	}
	
}
