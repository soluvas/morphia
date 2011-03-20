/**
 * 
 */
package com.google.code.morphia.mapping;

/**
 * @author us@thomas-daily.de
 */
public interface InstanceFactory {
	<T> T newInstance(Class<T> c);
	
	<T> T onInstantiation(T instance);
}
