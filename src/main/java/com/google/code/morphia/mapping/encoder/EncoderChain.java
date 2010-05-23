/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.mapping.MappedField;
import com.mongodb.DBObject;

/**
 * implements chain of responsibility for encoders
 * 
 * @author doc
 */
public class EncoderChain
{

    private EncodingContext ctx;
    private List<TypeEncoder> knownEncoders = new LinkedList<TypeEncoder>();

    // constr. will change
    public EncoderChain()
    {
        // TODO replace by something serious.
        ctx = new EncodingContext()
        {

            @Override
            public Map getEntityCache()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Datastore getDatastore()
            {
                // TODO Auto-generated method stub
                return null;
            }
        };

        knownEncoders.add(new SerializedObjectEncoder());
        knownEncoders.add(new ByteArrayEncoder());
        knownEncoders.add(new MapOfValuesEncoder());
        knownEncoders.add(new CollectionOfValuesEncoder());

        // last resort
        knownEncoders.add(new ValueEncoder());
    }

    public void fromDBObject(final DBObject dbObj, final MappedField mf, final Object targetEntity)
    {
        // FIXME us

        Object object = dbObj.get(mf.getMappedFieldName());
        if (object == null)
        {
            handleAttributeNotPresentInDBObject(mf);
        }
        else
        {
            TypeEncoder enc = getEncoder(mf);
            mf.setFieldValue(targetEntity, enc.decode(ctx, mf, object));
            // FIXME handle uncaught
        }

    }

    private void handleAttributeNotPresentInDBObject(final MappedField mf)
    {

    }

    private TypeEncoder getEncoder(final MappedField mf)
    {
        for (TypeEncoder e : knownEncoders)
        {
            if (e.canHandle(mf))
            {
                return e;
            }
        }
        throw new EncoderNotFoundException("Cannot find encoder for " + mf.getType() + " as need for "
                + mf.getFullName());
    }

    public void toDBObject(final Object containingObject, final MappedField mf, final DBObject dbObj)
    {
        TypeEncoder enc = getEncoder(mf);
        Object fieldValue = mf.getFieldValue(containingObject);
        Object encoded = enc.encode(ctx, mf, fieldValue);
        if (encoded == null)
        {
            // TODO include null based on annotation?
        }
        else
        {
            dbObj.put(mf.getMappedFieldName(), encoded);
        }
    }
}
