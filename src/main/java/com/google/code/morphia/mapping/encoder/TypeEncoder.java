/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface TypeEncoder
{

    boolean canHandle(MappedField f);

    public Object decode(EncodingContext ctx, MappedField f, Object fromDBObject) throws MappingException;

    public Object encode(EncodingContext ctx, MappedField f, Object value) throws MappingException;

}
