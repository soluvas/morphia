package com.google.code.morphia.mapping.validation;

/**
 * 
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.validation.ConstraintViolation.Level;
import com.google.code.morphia.mapping.validation.rules.SingleId;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MappingValidator {
	
	static final Logger logger = Logger.getLogger(MappingValidator.class.getName());

	public void validate(List<MappedClass> classes) {
		Set<ConstraintViolation> ve = new TreeSet<ConstraintViolation>(new Comparator<ConstraintViolation>() {
			
			@Override
			public int compare(ConstraintViolation o1, ConstraintViolation o2) {
				return o1.getLevel().ordinal() > o2.getLevel().ordinal() ? -1 : 1;
			}
		});

		List<ClassConstraint> rules = getConstraints();
		for (MappedClass c : classes) {
			// log().debug("Validating " + c.getName());
			for (ClassConstraint v : rules) {
				// log().trace(" against " + v.getClass().getName());
				List<ConstraintViolation> validationErrors = v.check(c);
				if (validationErrors != null) {
					ve.addAll(validationErrors);
				}
			}
		}

		if (!ve.isEmpty()) {
			ConstraintViolation worst = ve.iterator().next();
			Level maxLevel = worst.getLevel();
			if (maxLevel.ordinal() >= Level.FATAL.ordinal()) {
				throw new ConstraintViolationException(ve);
			}
			
			// sort by class to make it more readable
			ArrayList<LogLine> l = new ArrayList<LogLine>();
			for (ConstraintViolation v : ve) {
				l.add(new LogLine(v));
			}
			Collections.sort(l);

			for (LogLine string : l) {
				string.log(logger);
			}
		}
	}

	private List<ClassConstraint> getConstraints() {
		List<ClassConstraint> constraints = new ArrayList(32);
		
		// normally, i do this with scanning the classpath.
		constraints.add(new SingleId());
		// TODO if you agree, id refactor all the checks there are into
		// Constraints.

		return constraints;
	}
	
	class LogLine implements Comparable<LogLine> {
		private ConstraintViolation v;

		LogLine(ConstraintViolation v) {
			this.v = v;
		}
		
		void log(Logger logger) {
			switch (v.getLevel()) {
				case SEVERE:
					logger.log(java.util.logging.Level.SEVERE, v.render());
				case WARNING:
					logger.log(java.util.logging.Level.WARNING, v.render());
				case INFO:
					logger.log(java.util.logging.Level.INFO, v.render());
					break;
				case MINOR:
					logger.log(java.util.logging.Level.FINE, v.render());
					break;
				
				default:
					throw new IllegalStateException("Cannot log " + ConstraintViolation.class.getSimpleName()
							+ " of Level " + v.getLevel());
			}
		}
		
		@Override
		public int compareTo(LogLine o) {
			return v.getPrefix().compareTo(o.v.getPrefix());
		}
	}
	
	/**
	 * i definitely vote for all at once validation
	 */
	@Deprecated
	public void validate(MappedClass mappedClass) {
		validate(Arrays.asList(mappedClass));
	}
}
