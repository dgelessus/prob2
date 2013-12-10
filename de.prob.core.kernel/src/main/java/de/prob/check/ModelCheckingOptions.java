package de.prob.check;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class ModelCheckingOptions {

	public static ModelCheckingOptions DEFAULT = new ModelCheckingOptions()
			.checkDeadlocks(true).checkInvariantViolations(true);
	private final EnumSet<Options> options;

	public enum Options {
		breadth_first_search, find_deadlocks, find_invariant_violations, find_assertion_violations, inspect_existing_nodes, stop_at_full_coverage;
	}

	public ModelCheckingOptions() {
		options = EnumSet.noneOf(Options.class);
	}

	private ModelCheckingOptions(final EnumSet<Options> options) {
		this.options = options;
	}

	public ModelCheckingOptions breadthFirst(final boolean value) {
		return changeOption(value, Options.breadth_first_search);
	}

	public ModelCheckingOptions checkDeadlocks(final boolean value) {
		return changeOption(value, Options.find_deadlocks);
	}

	public ModelCheckingOptions checkInvariantViolations(final boolean value) {
		return changeOption(value, Options.find_invariant_violations);
	}

	public ModelCheckingOptions checkTheorems(final boolean value) {
		return changeOption(value, Options.find_assertion_violations);
	}

	public ModelCheckingOptions recheckExisting(final boolean value) {
		return changeOption(value, Options.inspect_existing_nodes);
	}

	public ModelCheckingOptions stopAtFullCoverage(final boolean value) {
		return changeOption(value, Options.stop_at_full_coverage);
	}

	private ModelCheckingOptions changeOption(final boolean value,
			final Options o) {
		EnumSet<Options> copyOf = EnumSet.copyOf(options);
		if (value) {
			copyOf.add(o);
		} else {
			copyOf.remove(o);
		}
		return new ModelCheckingOptions(copyOf);
	}

	public Set<Options> getPrologOptions() {
		return Collections.unmodifiableSet(options);
	}

}
