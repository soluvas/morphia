/**
 * 
 */
package com.google.code.morphia.mapping.encoder;

import java.io.IOException;

import org.bson.types.Binary;

import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.mapping.Serializer;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
@SuppressWarnings("unchecked")
public class SerializedObjectEncoder implements TypeEncoder
{
	
	@Override
	public boolean canHandle(final MappedField f)
	{
		return (f.hasAnnotation(Serialized.class));
	}
	
	@Override
	public Object decode(final EncodingContext ctx, final MappedField f, final Object fromDBObject)
	throws MappingException
	{
		
		if (!((fromDBObject instanceof Binary) || (fromDBObject instanceof byte[])))
		{
			throw new MappingException("The stored data is not a DBBinary or byte[] instance for " + f.getFullName()
					+ " ; it is a " + fromDBObject.getClass().getName());
		}
		
		try
		{
			boolean useCompression = !f.getAnnotation(Serialized.class).disableCompression();
			return Serializer.deserialize(fromDBObject, useCompression);
		}
		catch (IOException e)
		{
			throw new MappingException("While deserializing to " + f.getFullName(), e);
		}
		catch (ClassNotFoundException e)
		{
			throw new MappingException("While deserializing to " + f.getFullName(), e);
		}
	}
	
	@Override
	public Object encode(final EncodingContext ctx, final MappedField f, final Object value) throws MappingException
	{
		try
		{
			boolean useCompression = !f.getAnnotation(Serialized.class).disableCompression();
			return Serializer.serialize(value, useCompression);
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
}
