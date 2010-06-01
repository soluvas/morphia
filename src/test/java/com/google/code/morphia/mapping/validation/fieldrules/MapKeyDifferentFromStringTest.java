/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.testutil.TestEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class MapKeyDifferentFromStringTest extends TestBase {
	
	public static class MapWithWrongKeyType1 extends TestEntity {
		@Serialized
		Map<Integer, Integer> shouldBeOk = new HashMap();
		
	}
	
	public static class MapWithWrongKeyType2 extends TestEntity {
		@Reference
		Map<Integer, Integer> shouldBeOk = new HashMap();
		
	}
	
	public static class MapWithWrongKeyType3 extends TestEntity {
		@Embedded
		Map<Integer, Integer> shouldBeOk = new HashMap();
		
	}
	
	@Test
	public void testCheck() {
		morphia.map(MapWithWrongKeyType1.class);
		
		new AssertedFailure() {
			public void thisMustFail() throws Throwable {
				morphia.map(MapWithWrongKeyType2.class);
			}
		};
		
		new AssertedFailure() {
			public void thisMustFail() throws Throwable {
				morphia.map(MapWithWrongKeyType3.class);
			}
		};
	}
	
}
