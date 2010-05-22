/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class MapNotSerializableTest extends JUnit3TestBase {
	public static class Map1 extends AbstractMongoEntity {
		@Serialized
		Map<Integer, String> shouldBeOk = new HashMap();
		
	}
	
	public static class Map2 extends AbstractMongoEntity {
		@Reference
		Map<Integer, E1> shouldBeOk = new HashMap();
		
	}
	
	public static class Map3 extends AbstractMongoEntity {
		@Embedded
		Map<E2, Integer> shouldBeOk = new HashMap();
		
	}
	
	public static class E1 {
		
	}
	
	public static class E2 {
		
	}
	
	public void testCheck() {
		morphia.map(Map1.class);
		
		new AssertedFailure() {
			public void thisMustFail() throws Throwable {
				morphia.map(Map2.class);
			}
		};
		
		new AssertedFailure() {
			public void thisMustFail() throws Throwable {
				morphia.map(Map3.class);
			}
		};
	}
	
}
