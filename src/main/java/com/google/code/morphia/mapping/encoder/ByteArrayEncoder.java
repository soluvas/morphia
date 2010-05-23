/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ByteArrayEncoder implements TypeEncoder
{

    @Override
    public boolean canHandle(final MappedField f)
    {
        if (f.getAnnotation(Reference.class) != null)
        {
            return false;
        }
        if (f.getAnnotation(Serialized.class) != null)
        {
            return false;
        }
        if (f.getAnnotation(Embedded.class) != null)
        {
            return false;
        }

        return f.isMultipleValues() && (f.getSubType() == byte.class) && f.getType().isArray();
    }

    @Override
    public Object decode(final EncodingContext ctx, final MappedField f, final Object fromDBObject)
            throws MappingException
    {
        return fromDBObject; // as it comes
    }

    @Override
    public Object encode(final EncodingContext ctx, final MappedField f, final Object value) throws MappingException
    {
        return value;
    }

}
