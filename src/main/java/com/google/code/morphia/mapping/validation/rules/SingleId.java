/**
 * 
 */
package com.google.code.morphia.mapping.validation.rules;

import java.util.Arrays;
import java.util.List;

import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.validation.ClassConstraint;
import com.google.code.morphia.mapping.validation.ConstraintViolation;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class SingleId implements ClassConstraint {
	
	@Override
	public List<ConstraintViolation> check(MappedClass mc) {
		
		List<MappedField> idFields = mc.getFieldsAnnotatedWith(Id.class);
		
		if (idFields.size() > 1) {
			StringBuffer sb = new StringBuffer(128);
			for (MappedField mappedField : idFields) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(mappedField.getClassFieldName());
			}
			String fieldEnum = sb.toString();

			return Arrays.asList(new ConstraintViolation(Level.FATAL, mc, "More than one @Id Field found (" + fieldEnum
					+ ")."));
		}

		return null;
	}
	
}
