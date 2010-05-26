/**
 * 
 */
package com.google.code.morphia;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.utils.AbstractEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class TestGetByKeys extends TestBase {
	@Test
	public final void testGetByKeys() {
		A a1 = new A();
		A a2 = new A();
		
		Iterable<Key<A>> keys = ds.save(a1, a2);
		
		List<A> reloaded = ds.getByKeys(keys);
		
		Iterator<A> i = reloaded.iterator();
		Assert.assertNotNull(i.next());
		Assert.assertNotNull(i.next());
		Assert.assertFalse(i.hasNext());
	}
	
	public static class A extends AbstractEntity {
		String foo = "bar";
	}

}
