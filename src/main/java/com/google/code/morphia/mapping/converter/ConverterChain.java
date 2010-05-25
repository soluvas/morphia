/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import java.util.LinkedList;
import java.util.List;

import com.google.code.morphia.mapping.MappedField;
import com.mongodb.DBObject;

/**
 * implements chain of responsibility for encoders
 * 
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ConverterChain {
	
	private List<TypeConverter> knownEncoders = new LinkedList<TypeConverter>();
	
	// constr. will change
	public ConverterChain() {
		
		knownEncoders.add(new EnumSetConverter());
		knownEncoders.add(new ObjectIdConverter());
		knownEncoders.add(new EnumConverter());
		knownEncoders.add(new StringConverter());
		knownEncoders.add(new CharacterConverter());
		knownEncoders.add(new ByteConverter());
		knownEncoders.add(new BooleanConverter());
		knownEncoders.add(new DoubleConverter());
		knownEncoders.add(new FloatConverter());
		knownEncoders.add(new LongConverter());
		knownEncoders.add(new LocaleConverter());
		knownEncoders.add(new ShortConverter());
		knownEncoders.add(new IntegerConverter());
		knownEncoders.add(new SerializedObjectConverter());
		knownEncoders.add(new ByteArrayConverter());
		knownEncoders.add(new CharArrayConverter());
		knownEncoders.add(new DateConverter());
		knownEncoders.add(new KeyConverter());
		knownEncoders.add(new DBRefConverter());
		knownEncoders.add(new MapOfValuesConverter(this));
		knownEncoders.add(new CollectionOfValuesConverter(this));
		
		// last resort
		knownEncoders.add(new NullConverter());
		
	}
	
	public void fromDBObject(final DBObject dbObj, final MappedField mf, final Object targetEntity) {
		// FIXME us
		
		Object object = dbObj.get(mf.getMappedFieldName());
		if (object == null) {
			handleAttributeNotPresentInDBObject(mf);
		} else {
			TypeConverter enc = getEncoder(mf);
			mf.setFieldValue(targetEntity, enc.decode(mf.getType(), object, mf));
			// FIXME handle uncaught
		}
		
	}
	
	private void handleAttributeNotPresentInDBObject(final MappedField mf) {
		
	}
	
	private TypeConverter getEncoder(final MappedField mf) {
		for (TypeConverter e : knownEncoders) {
			if (e.canHandle(mf)) {
				return e;
			}
		}
		throw new ConverterNotFoundException("Cannot find encoder for " + mf.getType() + " as need for "
				+ mf.getFullName());
	}
	
	private TypeConverter getEncoder(final Class c) {
		for (TypeConverter e : knownEncoders) {
			if (e.canHandle(c)) {
				return e;
			}
		}
		throw new ConverterNotFoundException("Cannot find encoder for " + c.getName());
	}
	
	public void toDBObject(final Object containingObject, final MappedField mf, final DBObject dbObj) {
		TypeConverter enc = getEncoder(mf);
		Object fieldValue = mf.getFieldValue(containingObject);
		Object encoded = enc.encode(fieldValue, mf);
		if (encoded == null) {
			// TODO include null based on annotation?
		} else {
			dbObj.put(mf.getMappedFieldName(), encoded);
		}
	}
	
	public Object decode(Class c, Object fromDBObject) {
		if (c == null)
			c = fromDBObject.getClass();// FIXME find that rather weird
		return getEncoder(c).decode(c, fromDBObject);
	}
	
	public Object encode(Object o) {
		return encode(o.getClass(), o);
	}
	
	public Object encode(Class c, Object o) {
		return getEncoder(c).encode(o);
	}
}
