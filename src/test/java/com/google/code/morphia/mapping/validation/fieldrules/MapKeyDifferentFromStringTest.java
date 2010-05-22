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
public class MapKeyDifferentFromStringTest extends JUnit3TestBase {
	
	public static class MapWithWrongKeyType1 extends AbstractMongoEntity {
		@Serialized
		Map<Integer, Integer> shouldBeOk = new HashMap();
		
	}
	
	public static class MapWithWrongKeyType2 extends AbstractMongoEntity {
		@Reference
		Map<Integer, Integer> shouldBeOk = new HashMap();
		
	}
	
	public static class MapWithWrongKeyType3 extends AbstractMongoEntity {
		@Embedded
		Map<Integer, Integer> shouldBeOk = new HashMap();
		
	}
	
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
