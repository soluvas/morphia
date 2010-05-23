/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv;

import com.google.code.morphia.mapping.MappedField;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class EncoderNotFoundException extends RuntimeException {
	
	public EncoderNotFoundException(MappedField mf) {
		super(mf.getFullName());// FIXME us verbose
	}
	
}
