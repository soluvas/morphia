/**
 * 
 */
package com.google.code.morphia.optimisticlocks;

import java.util.ConcurrentModificationException;

import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;
import com.mongodb.DBObject;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class VersionTest extends JUnit3TestBase {
	
	public static class Along extends AbstractMongoEntity {
		@Version
		long hubba;
		
		String text;
		
		@PreSave
		public void preSave(DBObject o) {
			System.out.println(o);
		}
		
	}

	public void testVersions() throws Exception {
		Along a = new Along();
		assertEquals(0, a.hubba);
		ds.save(a);
		assertTrue(a.hubba > 0);
		long version1 = a.hubba;
		
		ds.save(a);
		assertTrue(a.hubba > 0);
		long version2 = a.hubba;
		
		assertFalse(version1 == version2);
	}
	
	public void testConcurrentModDetection() throws Exception {
		Along a = new Along();
		assertEquals(0, a.hubba);
		ds.save(a);
		final Along a1 = a;
		
		Along a2 = ds.get(a);
		ds.save(a2);
		

		new AssertedFailure(ConcurrentModificationException.class) {
			public void thisMustFail() throws Throwable {
				ds.save(a1);
			}
		};
	}

}
