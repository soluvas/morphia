/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.utils.AbstractMongoEntity;
import com.mongodb.DBObject;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class SerializedNameTest extends JUnit3TestBase {
	public static class E extends AbstractMongoEntity {
		@Serialized("changedName")
		byte[] b = "foo".getBytes();
		
		@PreSave
		public void preSave(DBObject o) {
			document = o.toString();
			System.out.println(document);
		}
		
		@Transient
		String document;
	}
	

	public void testCheck() {
		
		E e = new E();
		ds.save(e);
		
		assertTrue(e.document.contains("changedName"));
	}
}
