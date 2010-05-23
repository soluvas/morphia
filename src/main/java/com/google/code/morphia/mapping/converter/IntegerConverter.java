/**
 * 
 */
package com.google.code.morphia.mapping.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class IntegerConverter implements ValueConverter<Integer> {
	
	@Override
	public Integer objectFromValue(Object val) {
		if (val instanceof String) {
			return Integer.parseInt((String) val);
		} else {
			return ((Number) val).intValue();
		}
	}
	
	@Override
	public Object valueFromObject(Integer t) {
		return t;
	}
	
}
