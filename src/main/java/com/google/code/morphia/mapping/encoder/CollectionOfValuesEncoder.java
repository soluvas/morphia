/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.converter.SimpleValueConverter;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.DBRef;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class CollectionOfValuesEncoder implements TypeEncoder
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
        return f.isMultipleValues();
    }

    @Override
    public Object decode(final EncodingContext ctx, final MappedField mf, final Object fromDBObject)
            throws MappingException
    {
        List list = (List) fromDBObject; // TODO what if that fails? migration
        // issue?
        Class subtype = mf.getSubType();
        if (subtype != null)
        {
            // map back to the java datatype
            // (List/Set/Array[])
            Collection values = createCollection(ctx, mf);

            // FIXME move
            if (subtype == Locale.class)
            {
                for (Object o : list)
                {
                    values.add(SimpleValueConverter.parseLocale((String) o));
                }
            }
            else
                if (subtype == Key.class)
                {
                    for (Object o : list)
                    {
                        values.add(new Key((DBRef) o));
                    }
                }
                else
                    if (subtype.isEnum())
                    {
                        for (Object o : list)
                        {
                            values.add(Enum.valueOf(subtype, (String) o));
                        }
                    }
                    else
                    {
                        for (Object o : list)
                        {
                            values.add(o);
                        }
                    }

            if (mf.getType().isArray())
            {
                return SimpleValueConverter.convertToArray(subtype, values);
            }
            else
            {
                return values;
            }
        }
        else
        {
            // for types converted by driver?
            return list;
        }
    }

    private Collection createCollection(final EncodingContext ctx, final MappedField mf)
    {
        Collection values;

        if (!mf.isSet())
        {
            values = (List) ReflectionUtils.tryConstructor(ArrayList.class, mf.getCTor());
        }
        else
        {
            values = (Set) ReflectionUtils.tryConstructor(HashSet.class, mf.getCTor());
        }
        return values;
    }

    @Override
    public Object encode(final EncodingContext ctx, final MappedField f, final Object value) throws MappingException
    {
        if (value != null)
        {
            Iterable iterableValues = null;

            if (f.getType().isArray())
            {
                Object[] objects = null;
                try
                {
                    objects = (Object[]) value;
                    // TODO us see if possible without catching exception
                }
                catch (ClassCastException e)
                {
                    // store the primitive array without making
                    // it into a list.
                    if (Array.getLength(value) == 0)
                    {
                        return null;
                    }
                    return value;
                }
                // convert array into arraylist
                iterableValues = new ArrayList(objects.length);
                for (Object obj : objects)
                {
                    ((ArrayList) iterableValues).add(obj);
                }
            }
            else
            {
                // cast value to a common interface
                iterableValues = (Iterable) value;
            }

            List values = new ArrayList();

            if (f.getSubType() != null)
            {
                for (Object o : iterableValues)
                {
                    values.add(SimpleValueConverter.objectToValue(f.getSubType(), o));
                }
            }
            else
            {
                for (Object o : iterableValues)
                {
                    values.add(SimpleValueConverter.objectToValue(o));
                }
            }
            if (values.size() > 0)
            {
                return values;
            }
        }
        return null;
    }

}
