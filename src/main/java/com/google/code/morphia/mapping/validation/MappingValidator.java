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
import com.google.code.morphia.mapping.validation.classrules.EmbeddedAndId;
import com.google.code.morphia.mapping.validation.classrules.EmbeddedAndValue;
import com.google.code.morphia.mapping.validation.classrules.EntityAndEmbed;
import com.google.code.morphia.mapping.validation.classrules.MultipleId;
import com.google.code.morphia.mapping.validation.classrules.NoId;
import com.google.code.morphia.mapping.validation.fieldrules.LazyReferenceMissingDependencies;
import com.google.code.morphia.mapping.validation.fieldrules.LazyReferenceOnArray;
import com.google.code.morphia.mapping.validation.fieldrules.MisplacedProperty;
import com.google.code.morphia.mapping.validation.fieldrules.ReferenceToUnidentifiable;

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
			for (ClassConstraint v : rules) {
				v.check(c, ve);
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

			for (LogLine line : l) {
				line.log(logger);
			}
		}
	}

	private List<ClassConstraint> getConstraints() {
		List<ClassConstraint> constraints = new ArrayList<ClassConstraint>(32);
		
		// normally, i do this with scanning the classpath, but that´d bring
		// another dependency ;)
		
		// class-level
		constraints.add(new MultipleId());
		constraints.add(new NoId());
		constraints.add(new EmbeddedAndId());
		constraints.add(new EntityAndEmbed());
		constraints.add(new EmbeddedAndValue());
		// field-level
		constraints.add(new MisplacedProperty());
		constraints.add(new ReferenceToUnidentifiable());
		constraints.add(new LazyReferenceMissingDependencies());
		constraints.add(new LazyReferenceOnArray());

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
