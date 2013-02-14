package com.google.code.morphia.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.morphia.annotations.AlsoLoad;
import com.google.code.morphia.annotations.ConstructorArgs;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.NotSaved;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.DBObject;

/**
 * Represents the mapping of this field to/from mongodb (name, list<annotation>)
 * 
 * @author Scott Hernandez
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MappedField {
	private static final Logr log = MorphiaLoggerFactory.get(MappedField.class);
	// The Annotations to look for when reflecting on the field (stored in the mappingAnnotations)
	public static List<Class<? extends Annotation>> interestingAnnotations = new ArrayList<Class<? extends Annotation>>(Arrays.asList(
			Serialized.class, 
			Indexed.class, 
			Property.class, 
			Reference.class, 
			Embedded.class, 
			Id.class,
			Version.class, 
			ConstructorArgs.class, 
			AlsoLoad.class, 
			NotSaved.class));
	
	protected Class persistedClass;
	protected Field field; // the field :)
	protected Class realType; // the real type
	protected Constructor ctor; // the constructor for the type
	// Annotations that have been found relevant to mapping
	protected Map<Class<? extends Annotation>, Annotation> foundAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
	protected Type subType = null; // the type (T) for the Collection<T>/T[]/Map<?,T>
	protected Type mapKeyType = null; // the type (T) for the Map<T,?>
	protected boolean isSingleValue = true; // indicates the field is a single value
	protected boolean isMongoType = false; // indicated the type is a mongo compatible type (our version of value-type)
	protected boolean isMap = false; // indicated if it implements Map interface
	protected boolean isSet = false; // indicated if the collection is a set
	//for debugging
	protected boolean isArray = false; // indicated if it is an Array
	protected boolean isCollection = false; // indicated if the collection is a list)
	
	/** the constructor */
	MappedField(Field f, Class<?> clazz) {
		final boolean accessible = f.isAccessible();
		try {
			f.setAccessible(true);
			field = f;
			persistedClass = clazz;
			discover();
		} finally {
			field.setAccessible(accessible);
		}
	}
	
	/** the constructor */
	protected MappedField(){}
	
	/** Discovers interesting (that we care about) things about the field. */
	protected void discover() {
		for (Class<? extends Annotation> clazz : interestingAnnotations)
			addAnnotation(clazz);
		
		//type must be discovered before the constructor.
		realType = discoverType();
		ctor = discoverCTor();
		discoverMultivalued();
		
		// check the main type
		isMongoType = ReflectionUtils.isPropertyType(realType);
		
		// if the main type isn't supported by the Mongo, see if the subtype is.
		// works for T[], List<T>, Map<?, T>, where T is Long/String/etc.
		if (!isMongoType && subType != null)
			isMongoType = ReflectionUtils.isPropertyType(subType);
		
		if (!isMongoType && !isSingleValue && (subType == null || subType.equals(Object.class))) {
			if(log.isWarningEnabled())
				log.warning("The multi-valued field '"
							+ getFullName()
							+ "' is a possible heterogenous collection. It cannot be verified. Please declare a valid type to get rid of this warning. " + subType );
			isMongoType = true;
		}
	}

	private void discoverMultivalued() {
		if (	realType.isArray() || 
				Collection.class.isAssignableFrom(realType) || 
				Map.class.isAssignableFrom(realType)) {
			
			isSingleValue = false;
			
			if (Map.class.isAssignableFrom(realType)) {
				isMap = true;
			} else {
				// HACK: Support EMap. Note that these classes must also provide explicit support for EMap:
				// 1. MapOfValuesConverter
				// 2. EmbeddedMapper.writeMap()
				isMap = false;
				try {
					final Method method = realType.getMethod("map");
					isMap = Map.class.isAssignableFrom(method.getReturnType());
				} catch (Exception e) {
				}
			}
			isSet = Set.class.isAssignableFrom(realType);
			//for debugging
			isCollection = Collection.class.isAssignableFrom(realType);
			isArray = realType.isArray();
			
			//for debugging with issue
			if (!isMap && !isSet && !isCollection && !isArray)
				throw new MappingException("type is not a map/set/collection/array : " + realType);
			
			// get the subtype T, T[]/List<T>/Map<?,T>; subtype of Long[], List<Long> is Long
			subType = (realType.isArray()) ? realType.getComponentType() : ReflectionUtils.getParameterizedType(field, (isMap) ? 1 : 0);

			if (isMap)
				mapKeyType = ReflectionUtils.getParameterizedType(field, 0);
		}
	}
	
	private Class discoverType() {
		Class type = field.getType();
		Type gType = field.getGenericType();
		TypeVariable<GenericDeclaration> tv = null;
		ParameterizedType pt = null;
		if (gType instanceof TypeVariable)
			tv = (TypeVariable<GenericDeclaration>) gType;
		else if (gType instanceof ParameterizedType)
			pt = (ParameterizedType) gType;
		
		if (tv != null) {
//			type = ReflectionUtils.getTypeArgument(persistedClass, tv);
			Class typeArgument = ReflectionUtils.getTypeArgument(persistedClass, tv);
			if(typeArgument != null)
				type = typeArgument;
		} else if (pt != null) {
			if(log.isDebugEnabled())
				log.debug("found instance of ParameterizedType : " + pt);
		}
		
		if (Object.class.equals(realType) && (tv != null || pt != null))
			if(log.isWarningEnabled())
				log.warning("Parameterized types are treated as untyped Objects. See field '" + field.getName() + "' on " + field.getDeclaringClass() );
		
		if (type == null)
			throw new MappingException("A type could not be found for " + this.field);
		
		return type;
	}
	
	private Constructor discoverCTor() {
		Constructor returnCtor = null;
		Class ctorType = null;
		// get the first annotation with a concreteClass that isn't Object.class
		for (Annotation an : foundAnnotations.values()) {
			try {
				Method m = an.getClass().getMethod("concreteClass");
				m.setAccessible(true);
				Object o = m.invoke(an);
				if (o != null && !(o.equals(Object.class))) {
					ctorType = (Class) o;
					break;
				}
			} catch (NoSuchMethodException e) {
				// do nothing
			} catch (IllegalArgumentException e) {
				if(log.isWarningEnabled())
					log.warning("There should not be an argument", e);
			} catch (Exception e) {
				if(log.isWarningEnabled())
					log.warning("", e);
			}
		}
		
		if (ctorType != null)
			try {
				returnCtor = ctorType.getDeclaredConstructor();
				returnCtor.setAccessible(true);
			} catch (NoSuchMethodException e) {
				if (!hasAnnotation(ConstructorArgs.class))
					if(log.isWarningEnabled())
						log.warning("No usable constructor for " + ctorType.getName(), e);
			}
		else {
			// see if we can create instances of the type used for declaration
			ctorType = getType();
			if (ctorType != null)
				try {
					returnCtor = ctorType.getDeclaredConstructor();
					returnCtor.setAccessible(true);
				} catch (NoSuchMethodException e) {
					// never mind.
				} catch (SecurityException e) {
					// never mind.
				}
		}
		return returnCtor;
	}
	
	/** Returns the name of the field's (key)name for mongodb */
	public String getNameToStore() {
		return getMappedFieldName();
	}
	
	/** Returns the name of the field's (key)name for mongodb, in order of loading. */
	public List<String> getLoadNames() {
		ArrayList<String> names = new ArrayList<String>();
		names.add(getMappedFieldName());
		
		AlsoLoad al = (AlsoLoad)this.foundAnnotations.get(AlsoLoad.class);
		if (al != null && al.value() != null && al.value().length > 0)
			names.addAll( Arrays.asList(al.value()));
		
		return names;
	}
	
	/** @return the value of this field mapped from the DBObject */
	public String getFirstFieldName(DBObject dbObj) {
		String fieldName = getNameToStore();
		boolean foundField = false;
		for (String n : getLoadNames()) {
			if (dbObj.containsField(n))
				if (!foundField) {
					foundField = true;
					fieldName = n;
				} else
					throw new MappingException(String.format("Found more than one field from @AlsoLoad %s", getLoadNames()));
		}
		return fieldName;
	}
	
	/** @return the value from best mapping of this field*/
	public Object getDbObjectValue(DBObject dbObj) {
		return dbObj.get(getFirstFieldName(dbObj));
	}
	
	/** Returns the name of the java field, as declared on the class */
	public String getJavaFieldName() {
		return field.getName();
	}
	
	/** returns the annotation instance if it exists on this field*/
	public <T extends Annotation> T getAnnotation(Class<T> clazz) {
		return (T) foundAnnotations.get(clazz);
	}

	public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
		return foundAnnotations;
	}
	
	/** Indicates whether the annotation is present in the mapping (does not check the java field annotations, just the ones discovered) */
	public boolean hasAnnotation(Class ann) {
		return foundAnnotations.containsKey(ann);
	}
	
	/** Adds the annotation, if it exists on the field. */
	public void addAnnotation(Class<? extends Annotation> clazz) {
		if (field.isAnnotationPresent(clazz))
			this.foundAnnotations.put(clazz, field.getAnnotation(clazz));
	}
	
	/** Adds the annotation, if it exists on the field. */
	public void addAnnotation(Class<? extends Annotation> clazz, Annotation ann) {
		this.foundAnnotations.put(clazz, ann);
	}
	
	/** Adds the annotation even if not on the declared class/field. */
	public Annotation putAnnotation(Annotation ann) {
		return this.foundAnnotations.put(ann.getClass(), ann);
	}

	/** returns the full name of the class plus java field name */
	public String getFullName() {
		return field != null ? ( (field.getDeclaringClass() != null ? field.getDeclaringClass().getName() : "null") + "." + field.getName() ) : "null";
	}
	
	/**
	 * Returns the name of the field's key-name for mongodb
	 */
	private String getMappedFieldName() {
		if (hasAnnotation(Id.class))
			return Mapper.ID_KEY;
		else if (hasAnnotation(Property.class)) {
			Property mv = (Property) foundAnnotations.get(Property.class);
			if (mv != null && !Mapper.IGNORED_FIELDNAME.equals(mv.value()))
				return mv.value();
		} else if (hasAnnotation(Reference.class)) {
			Reference mr = (Reference) foundAnnotations.get(Reference.class);
			if (mr != null && !Mapper.IGNORED_FIELDNAME.equals(mr.value()))
				return mr.value();
		} else if (hasAnnotation(Embedded.class)) {
			Embedded me = (Embedded) foundAnnotations.get(Embedded.class);
			if (me != null && !Mapper.IGNORED_FIELDNAME.equals(me.value()))
				return me.value();
		} else if (hasAnnotation(Serialized.class)) {
			Serialized me = (Serialized) foundAnnotations.get(Serialized.class);
			if (me != null && !Mapper.IGNORED_FIELDNAME.equals(me.value()))
				return me.value();
		} else if (hasAnnotation(Version.class)) {
			Version me = (Version) foundAnnotations.get(Version.class);
			if (me != null && !Mapper.IGNORED_FIELDNAME.equals(me.value()))
				return me.value();
		}
		
		return this.field != null ? this.field.getName() : null;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		String mappedFieldName;
		try {
			mappedFieldName = getMappedFieldName();
		} catch (Exception e) {
			// sometimes getMappedFieldName() throws exception?
			mappedFieldName = getFullName();
		}
		if (mappedFieldName != null)
			sb.append(mappedFieldName);	// or maybe because append can't handle null?
		sb.append(" (");
		sb.append(" type:").append(realType != null ? realType.getSimpleName() : "null").append(",");
		
		if(isSingleValue())
			sb.append(" single:true,");
		else {
			sb.append(" multiple:true,");
			sb.append(" subtype:").append(getSubClass()).append(",");
		}
		if(isMap()) {
			sb.append(" map:true,");
			if (getMapKeyClass() != null)
				sb.append(" map-key:").append(getMapKeyClass().getSimpleName());
			else
				sb.append(" map-key: class unknown! ");
		}
		
		if(isSet())
			sb.append(" set:true,");
		if(isCollection)
			sb.append(" collection:true,");
		if(isArray)
			sb.append(" array:true,");
		
		//remove last comma
		if (sb.charAt(sb.length()-1) == ',')
			sb.setLength(sb.length()-1);
		
		sb.append("); ").append(this.foundAnnotations != null ? this.foundAnnotations : "null");
		return sb.toString();
	}
	
	/** returns the type of the underlying java field*/
	public Class getType() {
		return realType;
	}
	
	/** returns the declaring class of the java field */
	public Class getDeclaringClass() {
		return field.getDeclaringClass();
	}

	/** If the underlying java type is a map then it returns T from Map<T,V> */
	public Class getMapKeyClass() {
		return toClass(mapKeyType);
	}

	protected Class toClass(Type t) {
		if(t == null) 
			return null;
		else if(t instanceof Class)
			return (Class) t;
		else if(t instanceof ParameterizedType)
			return (Class)((ParameterizedType) t).getRawType();
		else if(t instanceof WildcardType)
			return (Class)((WildcardType) t).getUpperBounds()[0];

		throw new RuntimeException("Generic TypeVariable not supported!");
		
	}
	/** If the java field is a list/array/map then the sub-type T is returned (ex. List<T>, T[], Map<?,T>*/
	public Class getSubClass() {
		return toClass(subType);
	}
	
	public Type getSubType() {
		return subType;
	}
	
	public boolean isSingleValue() {
		if(!isSingleValue && !isMap && !isSet && !isArray && !isCollection)
			throw new RuntimeException("Not single, but none of the types that are not-single.");
		return isSingleValue;
	}
	
	public boolean isMultipleValues() {
		return !isSingleValue;
	}
	
	public boolean isTypeMongoCompatible() {
		return isMongoType;
	}
	
	public boolean isMap() {
		return isMap;
	}
	
	public boolean isSet() {
		return isSet;
	}
	/** returns a constructor for the type represented by the field */
	public Constructor getCTor() {
		return ctor;
	}

	/** Returns the value stored in the java field */
	public Object getFieldValue(Object classInst) throws IllegalArgumentException {
		final boolean accessible = field.isAccessible();
		try {
			field.setAccessible(true);
			return field.get(classInst);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			field.setAccessible(accessible);
		}
	}
	
	/** Sets the value for the java field */	
	public void setFieldValue(Object classInst, Object value) throws IllegalArgumentException {
		// HACK: Make it work with EMF EMap
		if (isMap() && !getType().isAssignableFrom( Map.class ) ) {
			// it's not a standard JDK Map, so we use EMap#putAll()
			// but we need existing instance either from field or from getter
			// Note: EMap#addAll() doesn't work, probably for internal use
			List<Map.Entry<Object, Object>> currentValue = (List<Map.Entry<Object, Object>>) getFieldValue(classInst);
			if (currentValue == null) {
				final String getterName = "get" + Character.toUpperCase(getJavaFieldName().charAt(0)) + getJavaFieldName().substring(1);
				try {
					final Method getter = classInst.getClass().getMethod(getterName);
					currentValue = (List<Map.Entry<Object, Object>>) getter.invoke(classInst);
					if (currentValue == null)
						throw new MappingException("Getter " + getter + " returns null");
				} catch (Exception e) {
					// no existing Collection instance? you're giving no choice!
					throw new MappingException("If field " + this + " is not a JDK Map, you must provide an EMap instance via field/getter so Morphia can call putAll()", e);
				}
			}
			try {
				final Method putAllMethod = currentValue.getClass().getMethod("putAll", Map.class);
				putAllMethod.invoke(currentValue, value);
			} catch (InvocationTargetException e) {
				// with EMap<String, EList<?>> you'd get:
				// java.lang.ClassCastException: java.util.ArrayList cannot be cast to org.eclipse.emf.common.util.EList
				// Special support for EMap<String, EList<?>>
				try {
					final Method putEListMethod = currentValue.getClass().getMethod("put", Object.class, Object.class);
					final Class<?> basicEListClass = Class.forName("org.eclipse.emf.common.util.BasicEList");
					final Constructor<?> eListConstructor = basicEListClass.getConstructor(Collection.class);
					for (final Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
						final Object eList = eListConstructor.newInstance(entry.getValue());
						putEListMethod.invoke(currentValue, entry.getKey(), eList);
					}
				} catch (Exception e1) {
					throw new MappingException("Cannot set field " + this, e1);
				}
			} catch (Exception e) {
				throw new MappingException("Cannot set field " + this, e);
			}
		} else if ("org.eclipse.emf.common.util.EList".equals(getType().getName())) {
			// HACK: make it work with EList
			// it's not a standard JDK list, so we use List#addAll(),
			// but we need existing instance either from field or from getter
			Collection currentValue = (Collection) getFieldValue(classInst);
			if (currentValue == null) {
				final String getterName = "get" + Character.toUpperCase(getJavaFieldName().charAt(0)) + getJavaFieldName().substring(1);
				try {
					final Method getter = classInst.getClass().getMethod(getterName);
					currentValue = (Collection) getter.invoke(classInst);
					if (currentValue == null)
						throw new MappingException("Getter " + getter + " returns null");
				} catch (Exception e) {
					// no existing Collection instance? you're giving no choice!
					throw new MappingException("If field " + this.getFullName() + " is not a JDK Collection, you must provide an instance via field/getter so Morphia can call addAll()", e);
				}
			}
			currentValue.addAll((Collection) value);
		} else {
			// if the field type is standard Map, List, or other natively Morphia-supported value, then we can directly assign
			final boolean accessible = field.isAccessible();
			try {
				field.setAccessible(true);
				field.set(classInst, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} finally {
				field.setAccessible(accessible);
			}
		}
	}
	
	/** returned the underlying java field */
	public Field getField() {
		return field;
	}
	
	public Class getConcreteType() {
		Embedded e = getAnnotation(Embedded.class);
		if (e != null) {
			Class concrete = e.concreteClass();
			if (concrete != Object.class) {
				return concrete;
			}
		}
		
		Property p = getAnnotation(Property.class);
		if (p != null) {
			Class concrete = p.concreteClass();
			if (concrete != Object.class) {
				return concrete;
			}
		}
		return getType();
	}
}