package com.google.code.morphia.mapping.validation;

import java.util.List;

import com.google.code.morphia.mapping.MappedClass;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public interface ClassConstraint {
	List<ConstraintViolation> check(MappedClass mc);
}
