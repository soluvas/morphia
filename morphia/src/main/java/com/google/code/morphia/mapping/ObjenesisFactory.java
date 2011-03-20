/**
 * 
 */
package com.google.code.morphia.mapping;

import java.lang.reflect.Constructor;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

/**
 * @author us@thomas-daily.de
 */
public class ObjenesisFactory implements InstanceFactory {
	
	private Objenesis objenesis = new ObjenesisStd(true);
	
	public <T> T newInstance(Class<T> c) {
		
		T instance = createInstance(c);
		onInstantiation(instance);
		return instance;
	}

	private <T> T createInstance(Class<T> c) throws Error {
		Constructor con = getNoArgsConstructor(c);
		if (con != null) {
			try {
				return (T) con.newInstance(null);
			} catch (Throwable e) {
				if (e instanceof RuntimeException)
					throw (RuntimeException) e;
				if (e instanceof Error)
					throw (Error) e;
				
				throw new RuntimeException(e);
			}
		}

		ObjectInstantiator instantiator = objenesis.getInstantiatorOf(c);
		T instance = (T) instantiator.newInstance();

		return instance;
	}
	
	public <T> T onInstantiation(T instance) {
		return instance;
	}
	
	private static Constructor getNoArgsConstructor(final Class ctorType) {
		try {
			Constructor ctor = ctorType.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

}
