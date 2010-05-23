/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import com.google.code.morphia.mapping.MappedField;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EncoderNotFoundException extends RuntimeException
{

    public EncoderNotFoundException(final MappedField mf)
    {
        super(mf.getFullName());// FIXME us verbose
    }

}
