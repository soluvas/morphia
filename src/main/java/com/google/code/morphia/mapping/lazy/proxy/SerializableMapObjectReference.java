/**
 * 
 */
package com.google.code.morphia.mapping.lazy.proxy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.code.morphia.Key;
import com.google.code.morphia.mapping.lazy.DatastoreProvider;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
@SuppressWarnings("unchecked")
public class SerializableMapObjectReference extends AbstractReference implements
ProxiedEntityMap {

	private final HashMap<Object, Key<?>> keyMap;

	public SerializableMapObjectReference(final Map mapToProxy,
			final Class referenceObjClass, final boolean ignoreMissing,
			final DatastoreProvider p) {

		super(p, referenceObjClass, ignoreMissing);
		object = mapToProxy;
		keyMap = new LinkedHashMap<Object, Key<?>>();
	}

	@Override
	public void __put(final Object key, final Key k) {
		keyMap.put(key, k);
	}

	@Override
	protected Object fetch() {
		Map m = (Map) object;
		m.clear();
		for (Map.Entry<?, Key<?>> e : keyMap.entrySet()) {
			m.put(e.getKey(), fetch(e.getValue()));
		}
		return m;
	}

	@Override
	protected void beforeWriteObject() {
		((Map) object).clear();
	}


}
