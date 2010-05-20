package com.google.code.morphia.issue50;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.utils.AbstractMongoEntity;

public class TestIdTwice extends TestBase {

	@Test
	public final void testRedundantId() {
		A a = new A();
		ds.save(a);
		a = ds.get(a);

		morphia.map(A.class);
		// i expected that to...
		fail();
	}

	public static class A extends AbstractMongoEntity {
		@Id
		String foo;
		@Id
		String bar;
	}

}
