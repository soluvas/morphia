/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class NastyEnumTest extends JUnit3TestBase {
	public enum NastyEnum {
		A {
			@Override
			public String toString() {
				return "Never use toString for other purposes than debugging";
			}
		},
		B {
			public String toString() {
				return "Never use toString for other purposes than debugging ";
			}
		}
	}
	
	public static class NastyEnumEntity extends AbstractMongoEntity {
		NastyEnum e1 = NastyEnum.A;
		NastyEnum e2 = NastyEnum.B;
		NastyEnum e3 = null;
	}

	public void testNastyEnumPerisistence() throws Exception {
		NastyEnumEntity n = new NastyEnumEntity();
		ds.save(n);
		n = ds.get(n);
		assertSame(NastyEnum.A, n.e1);
		assertSame(NastyEnum.B, n.e2);
		assertNull(n.e3);
	}
}
