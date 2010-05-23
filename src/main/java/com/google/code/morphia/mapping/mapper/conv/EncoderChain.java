/**
 * 
 */
package com.google.code.morphia.mapping.mapper.conv;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.mapper.conv.enc.ByteArrayEncoder;
import com.google.code.morphia.mapping.mapper.conv.enc.CollectionOfValuesEncoder;
import com.google.code.morphia.mapping.mapper.conv.enc.MapOfValuesEncoder;
import com.google.code.morphia.mapping.mapper.conv.enc.SerializedObjectEncoder;
import com.google.code.morphia.mapping.mapper.conv.enc.TypeEncoder;
import com.google.code.morphia.mapping.mapper.conv.enc.ValueEncoder;
import com.mongodb.DBObject;

/**
 * implements chain of responsibility for encoders
 * 
 * @author doc
 * 
 */
public class EncoderChain {
	
	private EncodingContext ctx;
	private List<TypeEncoder> knownEncoders = new LinkedList<TypeEncoder>();
	
	// constr. will change
	public EncoderChain() {
		// TODO replace by something serious.
		this.ctx = new EncodingContext() {
			
			@Override
			public Map getEntityCache() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Datastore getDatastore() {
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
	
	public void fromDBObject(DBObject dbObj, MappedField mf, Object targetEntity) {
		// FIXME us
		
		Object object = dbObj.get(mf.getMappedFieldName());
		if (object == null)
			handleAttributeNotPresentInDBObject(mf);
		else {
			TypeEncoder enc = getEncoder(mf);
			mf.setFieldValue(targetEntity, enc.decode(ctx, mf, object));
			// FIXME handle uncaught
		}

	}
	
	private void handleAttributeNotPresentInDBObject(MappedField mf) {
		
	}

	private TypeEncoder getEncoder(MappedField mf) {
		for (TypeEncoder e : knownEncoders) {
			if (e.canHandle(mf))
				return e;
		}
		throw new EncoderNotFoundException(mf); // policy about loggin only!?
	}

	public void toDBObject(Object containingObject, MappedField mf, DBObject dbObj) {
		TypeEncoder enc = getEncoder(mf);
		Object fieldValue = mf.getFieldValue(containingObject);
		Object encoded = enc.encode(ctx, mf, fieldValue);
		if (encoded == null) {
			// TODO include null based on annotation?
		} else {
			dbObj.put(mf.getMappedFieldName(), encoded);
		}
	}
}

