/**
 * 
 */
package com.google.code.morphia.mapping.validation.classrules;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class TestSerializedMap extends JUnit3TestBase {
	
	public static class Map1 extends AbstractMongoEntity {
		@Serialized(zip = true)
		Map<Integer, Foo> shouldBeOk = new HashMap();
		
	}
	
	public static class Foo implements Serializable {
		
		final String id;
		
		public Foo(String id) {
			this.id = id;
		}
	}
	
	public void testSerialization() throws Exception {
		Map1 map1 = new Map1();
		map1.shouldBeOk.put(3, new Foo("peter"));
		map1.shouldBeOk.put(27, new Foo("paul"));
		
		ds.save(map1);
		map1 = ds.get(map1);
		
		assertEquals("peter", map1.shouldBeOk.get(3).id);
		assertEquals("paul", map1.shouldBeOk.get(27).id);
		
	}
}
