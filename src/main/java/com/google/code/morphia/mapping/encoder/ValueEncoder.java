/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.converter.SimpleValueConverter;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class ValueEncoder implements TypeEncoder
{

    @Override
    public boolean canHandle(final MappedField f)
    {
        return f.getType().isEnum() || SimpleValueConverter.isSupportedType(f.getType());
    }

    @Override
    public Object decode(final EncodingContext ctx, final MappedField f, final Object fromDBObject)
            throws MappingException
    {
        return SimpleValueConverter.objectFromValue(f.getType(), fromDBObject);
    }

    @Override
    public Object encode(final EncodingContext ctx, final MappedField f, final Object value) throws MappingException
    {
        return SimpleValueConverter.objectToValue(value);
    }

}
