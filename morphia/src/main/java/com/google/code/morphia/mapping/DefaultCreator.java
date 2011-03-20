/**
 * 
 */
package com.google.code.morphia.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.morphia.ObjectFactory;
import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.mongodb.DBObject;

/**
 * @author ScottHernandez
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class DefaultCreator implements ObjectFactory {
	private static final Logr log = MorphiaLoggerFactory.get(DefaultCreator.class);
	private static final InstanceFactory DEFAULT_FACTORY = new DefaultInstanceFactory();;
	private final InstanceFactory factory;

	public DefaultCreator() {
		this(DEFAULT_FACTORY);
	}

	public DefaultCreator(InstanceFactory factory) {
		this.factory = factory;
	}

	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createInstance(java.lang.Class)
	 */
	public Object createInstance(Class clazz) { 
		return createInst(clazz);
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createInstance(java.lang.Class, com.mongodb.DBObject)
	 */
	public Object createInstance(Class clazz, DBObject dbObj) {
		Class c = getClass(dbObj);
		if (c == null)
			c = clazz;
		return createInstance(c);	
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createInstance(com.google.code.morphia.mapping.Mapper, com.google.code.morphia.mapping.MappedField, com.mongodb.DBObject)
	 */
	public Object createInstance(Mapper mapr, MappedField mf, DBObject dbObj) {
		Class c = getClass(dbObj);
		if (c == null)
			c = mf.isSingleValue ? mf.getConcreteType() : mf.getSubClass();
			return createInstance(c, dbObj);
	}

	private Class getClass(DBObject dbObj) {
		// see if there is a className value
		String className = (String) dbObj.get(Mapper.CLASS_NAME_FIELDNAME);
		Class c = null;
		if (className != null) {
			// try to Class.forName(className) as defined in the dbObject first,
			// otherwise return the entityClass
			try {
				c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
			} catch (ClassNotFoundException e) {
				if (log.isWarningEnabled())
					log.warning("Class not found defined in dbObj: " , e);
			}
		}
		return c;
	}

	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createMap(com.google.code.morphia.mapping.MappedField)
	 */
	public Map createMap(MappedField mf) {
		return (Map) newInstance(mf.getCTor(), HashMap.class);
	}

	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createList(com.google.code.morphia.mapping.MappedField)
	 */
	public List createList(MappedField mf) {
		return (List) newInstance(mf.getCTor(), ArrayList.class);
	}

	/* (non-Javadoc)
	 * @see com.google.code.morphia.ObjectFactory#createSet(com.google.code.morphia.mapping.MappedField)
	 */
	public Set createSet(MappedField mf) {
		return (Set) newInstance(mf.getCTor(), HashSet.class);
	}

	
	public Object createInst(Class clazz) {
		return factory.newInstance(clazz);
	}
	
    /** creates an instance of testType (if it isn't Object.class or null) or fallbackType */
	private Object newInstance(final Constructor tryMe, final Class fallbackType) {
		if (tryMe != null) {
			tryMe.setAccessible(true);
			try {
				return tryMe.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return createInst(fallbackType);
    }
    
	private static Constructor getNoArgsConstructor(final Class ctorType) {
		try {
			Constructor ctor = ctorType.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor;
		} catch (NoSuchMethodException e) {
			throw new MappingException("No usable constructor for " + ctorType.getName(), e);
		}
	}
}
