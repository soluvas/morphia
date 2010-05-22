/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class EmbeddedAndSerializableTest extends JUnit3TestBase {
	public static class E extends AbstractMongoEntity {
		@Embedded
		@Serialized
		R r;
	}
	
	public static class R {
	}
	
	public void testCheck() {
		new AssertedFailure(ConstraintViolationException.class) {
			public void thisMustFail() throws Throwable {
				morphia.map(E.class);
			}
		};
	}
	
}
