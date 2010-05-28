/**
 * 
 */
package com.google.code.morphia.mapping.cache;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public abstract class AbstractCache implements Cache {
	protected final Map<CacheKey, Object> cache = new HashMap<CacheKey, Object>();
	
	
	@Override
	public void clearAll() {
		cache.clear();
	}
	
	@Override
	public Object get(CacheKey key) {

		Object object = cache.get(key);
		return object;
	}
	
	@Override
	public Object put(CacheKey key, Object entity) {
		Object ret = cache.put(key, entity);
		return ret;
	}
	
	@Override
	public Object removeByKey(CacheKey key) {
		return cache.remove(key);
	}
	
	@Override
	public int size() {
		return cache.size();
	}

}
