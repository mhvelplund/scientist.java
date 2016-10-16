package dk.darknight.scientist;

import java.text.MessageFormat;
import java.util.*;

import com.google.common.base.*;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.DoubleAction;
import dk.darknight.scientist.functions.DoubleFunction;
import lombok.NonNull;

class Experiment<T, TClean> implements IExperiment<T> {
	private final class DefaultComparator implements Comparator<T> {
		private static final int EQUAL = 0;
		private static final int NOT_EQUAL = -1; // False is always -1, regardless of actual result

		@Override
		public int compare(T o1, T o2) {
			boolean equal = (o1 == null && o2 == null) ||
					(o1 != null && o1.equals(o2));
			
			return equal ? EQUAL : NOT_EQUAL; 
		}
	}


	private static final String CANDIDATE_EXPERIMENT_NAME = "candidate";
	
	private static final Supplier<Boolean> ALWAYS_RUN = new Supplier<Boolean>() {
		@Override
		public Boolean get() {
			return true;
		}
	};
	
	private static final DoubleAction<Operation, Exception> ALWAYS_THROW = new DoubleAction<Operation, Exception>() {
		@Override
		public Void apply(Operation op, Exception exception) {
			throw Throwables.propagate(exception);
		}
	};

	private Action<Void> beforeRun;
	private final Map<String, Supplier<T>> candidates;
	private Function<T, ?> cleaner;
	private Comparator<T> comparator = new DefaultComparator();
	
	private final int concurrentTasks;
	private final Map<String, Object> contexts = new HashMap<>();
	private Supplier<T> control;
	private final Supplier<Boolean> enabled;
	private final List<DoubleFunction<T, T, Boolean>> ignores = new ArrayList<>();
	private final String name;
	private Supplier<Boolean> runIf = ALWAYS_RUN;
	private DoubleAction<Operation, Exception> thrown = ALWAYS_THROW;
	private boolean throwOnMismatches;

	public Experiment(@NonNull String name, @NonNull Supplier<Boolean> enabled, int concurrentTasks) {
		Preconditions.checkArgument(concurrentTasks > 0, "concurrentTasks must be greater than 0");
		this.name = name;
		this.candidates = new HashMap<String, Supplier<T>>();
		this.enabled = enabled;
		this.concurrentTasks = concurrentTasks;
	}

	@Override
	public void addContext(@NonNull String key, Object value) {
		contexts.put(key, value);
	}

	@Override
	public void attempt(@NonNull String name, @NonNull Supplier<T> candidate) {
		if (candidates.containsKey(name)) {
			throw new IllegalArgumentException(MessageFormat
					.format("You already have a candidate named {0}. Provide a different name for this test.", name));
		}
		candidates.put(name, candidate);
	}

	@Override
	public void attempt(@NonNull Supplier<T> candidate) {
		if (candidates.containsKey(CANDIDATE_EXPERIMENT_NAME)) {
			throw new IllegalArgumentException("You have already added a default try. "
					+ "Give this candidate a new name with the attempt(String, Supplier<T> candidate) overload");
		}
		candidates.put(CANDIDATE_EXPERIMENT_NAME, candidate);
	}

	@Override
	public void beforeRun(@NonNull Action<Void> action) {
		this.beforeRun = action;
	}

	public ExperimentInstance<T, TClean> build() {
		final ExperimentSettings<T, TClean> settings = new ExperimentSettings<T, TClean>(beforeRun, candidates, cleaner,
				comparator, concurrentTasks, contexts, control, enabled, ignores, name, runIf, thrown,
				throwOnMismatches);
		return new ExperimentInstance<>(settings);
	}

	@Override
	public void clean(@NonNull Function<T, ?> cleaner) {
		this.cleaner = cleaner;
	}

	@Override
	public void compare(@NonNull Comparator<T> comparator) {
		this.comparator = comparator;
	}

	@Override
	public void ignore(@NonNull DoubleFunction<T, T, Boolean> block) {
		this.ignores.add(block);
	}

	@Override
	public boolean isThrowOnMismatches() {
		return throwOnMismatches;
	}

	@Override
	public void runIf(@NonNull Supplier<Boolean> check) {
		this.runIf = check;
	}

	@Override
	public void setThrowOnMismatches(boolean throwOnMismatches) {
		this.throwOnMismatches = throwOnMismatches;
	}

	@Override
	public void thrown(@NonNull DoubleAction<Operation, Exception> block) {
		this.thrown = block;
	}

	@Override
	public void use(@NonNull Supplier<T> control) {
		this.control = control;
	}
}