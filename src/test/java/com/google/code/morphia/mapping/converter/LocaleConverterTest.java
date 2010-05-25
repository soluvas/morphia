/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import java.util.Locale;

import com.google.code.morphia.mapping.lazy.JUnit3TestBase;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class LocaleConverterTest extends JUnit3TestBase {
	
	public void testConv() throws Exception {
		LocaleConverter c = new LocaleConverter();
		
		Locale l = Locale.CANADA_FRENCH;
		Locale l2 = (Locale) c.decode(Locale.class, c.encode(l));
		assertEquals(l, l2);
		
		l = new Locale("de", "DE", "bavarian");
		l2 = (Locale) c.decode(Locale.class, c.encode(l));
		assertEquals(l, l2);
		assertEquals("de", l2.getLanguage());
		assertEquals("DE", l2.getCountry());
		assertEquals("bavarian", l2.getVariant());

	}
}
