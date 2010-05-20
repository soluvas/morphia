/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import com.google.code.morphia.Key;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public interface ProxiedEntityMap extends ProxiedReference {

	void __put(Object key, Key key2);

}
