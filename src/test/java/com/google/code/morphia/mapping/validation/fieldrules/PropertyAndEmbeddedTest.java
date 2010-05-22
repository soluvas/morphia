/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Property;
import com.google.code.morphia.annotations.Transient;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;
import com.mongodb.DBObject;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class PropertyAndEmbeddedTest extends JUnit3TestBase {
	public static class E extends AbstractMongoEntity {
		@Embedded("myFunkyR")
		R r = new R();
		
		@PreSave
		public void preSave(DBObject o) {
			document = o.toString();
			System.out.println(document);
		}
		
		@Transient
		String document;
	}
	
	public static class E2 extends AbstractMongoEntity {
		@Embedded
		@Property("myFunkyR")
		String s;
	}
	
	public static class R {
		String foo = "bar";
	}
	

	public void testCheck() {
		
		E e = new E();
		ds.save(e);
		
		assertTrue(e.document.contains("myFunkyR"));
		
		new AssertedFailure(ConstraintViolationException.class) {
			public void thisMustFail() throws Throwable {
				morphia.map(E2.class);
			}
		};
	}
}