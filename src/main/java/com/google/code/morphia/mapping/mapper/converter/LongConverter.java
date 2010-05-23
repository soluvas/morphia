/**
 * 
 */
package com.google.code.morphia.mapping.mapper.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class LongConverter implements ValueConverter<Long> {
	
	@Override
	public Long objectFromValue(Object val) {
		if (val instanceof String) {
			return Long.parseLong((String) val);
		} else {
			return ((Number) val).longValue();
		}
	}
	
	@Override
	public Object valueFromObject(Long t) {
		return t;
	}
	
}
