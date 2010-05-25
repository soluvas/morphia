/**
 * 
 */
package com.google.code.morphia.mapping;

import org.junit.Test;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class ByteArrayMappingTest extends JUnit3TestBase {
	public static class ContainsByteArray {
		@Id
		String id;
		Byte[] ba;
	}
	

	@Test
	public void testCharMapping() throws Exception {
		morphia.map(ContainsByteArray.class);
		ContainsByteArray entity = new ContainsByteArray();
		Byte[] test = new Byte[] { 6, 9, 1, -122 };
		entity.ba = test;
		ds.save(entity);
		ContainsByteArray loaded = ds.get(entity);
	
		for (int i = 0; i < test.length; i++) {
			Byte c = test[i];
			assertEquals(c, entity.ba[i]);
		}
		assertNotNull(loaded.id);
	}
	
	
}
