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
public class SerializableMapObjectReference extends AbstractReference implements ProxiedEntityReferenceMap {

	private final HashMap<String, String> keyMap;
	
	public SerializableMapObjectReference(final Map mapToProxy, final Class referenceObjClass,
			final boolean ignoreMissing, final DatastoreProvider p) {

		super(p, referenceObjClass, ignoreMissing);
		object = mapToProxy;
		keyMap = new LinkedHashMap<String, String>();
	}

	@Override
	public void __put(final String key, final Key k) {
		// TODO clear up key -> String business.
		keyMap.put(key, k.toRef().getId().toString());
	}

	@Override
	protected Object fetch() {
		Map m = (Map) object;
		m.clear();
		// TODO us: change to getting them all at once and yell according to
		// ignoreMissing in order to a) increase performance and b) resolve
		// equals keys to the same instance
		// that should really be done in datastore.
		for (Map.Entry<?, String> e : keyMap.entrySet()) {
			String entityKey = e.getValue();
			Object entity = fetch(entityKey);
			m.put(e.getKey(), entity);
		}
		return m;
	}

	@Override
	protected void beforeWriteObject() {
		((Map) object).clear();
	}

	@Override
	public Map<String, String> __getReferenceMap() {
		return keyMap;
	}

}
