/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class ReferenceAndSerializableTest extends JUnit3TestBase {
	public static class E extends AbstractMongoEntity {
		@Reference
		@Serialized
		R r;
	}
	
	public static class R extends AbstractMongoEntity {
	}
	
	public void testCheck() {
		new AssertedFailure(ConstraintViolationException.class) {
			public void thisMustFail() throws Throwable {
				morphia.map(E.class);
			}
			
			@Override
			protected boolean dumpToSystemOut() {
				return true;
			}
		};
	}
}
