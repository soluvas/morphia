/**
 * 
 */
package com.google.code.morphia.mapping.converter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class CharacterConverter implements ValueConverter<Character> {
	
	@Override
	public Character objectFromValue(Object o) {
		return o.toString().charAt(0);
	}
	
	@Override
	public Object valueFromObject(Character t) {
		return t;
	}
	
}
