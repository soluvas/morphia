/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.converter.SimpleValueConverter;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MapOfValuesEncoder implements TypeEncoder
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

        return f.isMap();
    }

    @Override
    public Object decode(final EncodingContext ctx, final MappedField f, final Object fromDBObject)
            throws MappingException
    {
        Map<Object, Object> map = (Map<Object, Object>) fromDBObject;
        Map values = (Map) ReflectionUtils.tryConstructor(HashMap.class, f.getCTor());// FIXME
        for (Map.Entry<Object, Object> entry : map.entrySet())
        {
            Object objKey = SimpleValueConverter.objectFromValue(f.getMapKeyType(), entry.getKey());
            values.put(objKey, SimpleValueConverter.objectFromValue(f.getSubType(), entry.getValue()));
        }
        return values;
    }

    @Override
    public Object encode(final EncodingContext ctx, final MappedField f, final Object value) throws MappingException
    {
        Map<Object, Object> map = (Map<Object, Object>) value;
        if ((map != null) && (map.size() > 0))
        {
            Map mapForDb = new HashMap();
            for (Map.Entry<Object, Object> entry : map.entrySet())
            {
                String strKey = SimpleValueConverter.objectToValue(entry.getKey()).toString();
                mapForDb.put(strKey, SimpleValueConverter.objectToValue(entry.getValue()));
            }
            return mapForDb;
        }
        return null;
    }

}
