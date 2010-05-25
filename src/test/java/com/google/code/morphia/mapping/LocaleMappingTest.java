/**
 * 
 */
package com.google.code.morphia.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class LocaleMappingTest extends JUnit3TestBase {
	
	public static class E {
		@Id
		String id;
		Locale l1;
		
		@Embedded
		List<Locale> l2 = new ArrayList();
		
		Locale[] l3;
	}
	
	public void testLocaleMapping() throws Exception {
		E e = new E();
		e.l1 = Locale.CANADA_FRENCH;
		e.l2 = Arrays.asList(Locale.GERMANY, Locale.TRADITIONAL_CHINESE);
		e.l3 = new Locale[] { Locale.TRADITIONAL_CHINESE, Locale.FRENCH };
		
		ds.save(e);
		e = ds.get(e);

		assertEquals(Locale.CANADA_FRENCH, e.l1);

		assertEquals(2, e.l2.size());
		assertEquals(Locale.GERMANY, e.l2.get(0));
		assertEquals(Locale.TRADITIONAL_CHINESE, e.l2.get(1));

		assertEquals(2, e.l3.length);
		assertEquals(Locale.TRADITIONAL_CHINESE, e.l3[0]);
		assertEquals(Locale.FRENCH, e.l3[1]);

	}
}
