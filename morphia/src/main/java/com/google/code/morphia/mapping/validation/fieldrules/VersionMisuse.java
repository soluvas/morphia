/**
 * 
 */
package com.google.code.morphia.mapping.validation.fieldrules;

import java.util.Set;

import com.google.code.morphia.ObjectFactory;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MapperOptions;
import com.google.code.morphia.mapping.validation.ConstraintViolation;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class VersionMisuse extends FieldConstraint {
	private ObjectFactory f;
	
	public VersionMisuse(MapperOptions o) {
		this.f = o.objectFactory;
	}

	@Override
	protected void check(MappedClass mc, MappedField mf, Set<ConstraintViolation> ve) {
		if (mf.hasAnnotation(Version.class)) {
			Class<?> type = mf.getType();
			if (Long.class.equals(type) || long.class.equals(type)) {
				
				Object testInstance = f.createInstance(mc.getClazz());
				
				// check initial value
				if (Long.class.equals(type)) {
					if (mf.getFieldValue(testInstance) != null)
						ve.add(new ConstraintViolation(Level.FATAL, mc, mf, this.getClass(), "When using @"
								+ Version.class.getSimpleName() + " on a Long field, it must be initialized to null."));
				} else if (long.class.equals(type)) {
					if ((Long) mf.getFieldValue(testInstance) != 0L)
						ve.add(new ConstraintViolation(Level.FATAL, mc, mf, this.getClass(), "When using @"
								+ Version.class.getSimpleName() + " on a long field, it must be initialized to 0."));
				}
			} else
				ve.add(new ConstraintViolation(Level.FATAL, mc, mf, this.getClass(), "@"
						+ Version.class.getSimpleName() + " can only be used on a Long/long field."));
		}
	}
	
}
