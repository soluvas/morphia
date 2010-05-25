/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class LazyReferenceOnArrayTest extends TestBase {
	
	public static class LazyOnArray extends AbstractMongoEntity {
		@Reference(lazy = true)
		R[] r;
	}
	
	public static class R extends AbstractMongoEntity {
	}
	
	@Test
	public void testLazyRefOnArray() {
		new AssertedFailure(ConstraintViolationException.class) {
			
			@Override
			protected void thisMustFail() throws Throwable {
				morphia.map(LazyOnArray.class);
			}
		};
	}
}
