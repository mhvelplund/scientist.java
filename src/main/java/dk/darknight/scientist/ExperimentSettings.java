package dk.darknight.scientist;

import java.util.*;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.DoubleAction;
import dk.darknight.scientist.functions.DoubleFunction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Declares all of the settings necessary in order to create a new
 * {@link ExperimentInstance}.
 *
 * @param <T>
 *           The result type for the experiment.
 * @param <TClean>
 *           The cleaned type of the experiment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class ExperimentSettings<T, TClean> {
	private Action<Void> beforeRun;
	private Map<String, Supplier<T>> candidates;
	private Function<T, ?> cleaner;
	private Comparator<T> comparator;
	private int concurrentTasks;
	private Map<String, Object> contexts = new HashMap<String, Object>();
	private Supplier<T> control;
	private Supplier<Boolean> enabled;
	private List<DoubleFunction<T, T, Boolean>> ignores = new ArrayList<DoubleFunction<T,T,Boolean>>();
	private String name;
	private Supplier<Boolean> runIf;
	private DoubleAction<Operation, Exception> thrown;
	private boolean throwOnMismatches;
}
