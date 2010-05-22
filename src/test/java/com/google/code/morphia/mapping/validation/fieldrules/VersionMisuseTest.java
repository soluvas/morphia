/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class VersionMisuseTest extends JUnit3TestBase {
	
	public static class Fail1 extends AbstractMongoEntity {
		@Version
		long hubba = 1;
	}
	
	public static class Fail2 extends AbstractMongoEntity {
		@Version
		Long hubba = 1L;
	}

	public static class OK1 extends AbstractMongoEntity {
		@Version
		long hubba;
	}
	
	public static class OK2 extends AbstractMongoEntity {
		@Version
		long hubba;
	}
	public void testCheck() {
		new AssertedFailure(ConstraintViolationException.class) {
			public void thisMustFail() throws Throwable {
				morphia.map(Fail1.class);
			}
		};
		new AssertedFailure(ConstraintViolationException.class) {
			public void thisMustFail() throws Throwable {
				morphia.map(Fail2.class);
			}
		};
		morphia.map(OK1.class);
		morphia.map(OK2.class);
	
	}
	
}
