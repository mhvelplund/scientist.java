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

@Data
@NoArgsConstructor
@AllArgsConstructor
class ExperimentSettings<T, TClean> {
	private Action<Void> beforeRun;
	private Map<String, Supplier<T>> candidates;
	private Function<T, ?> cleaner;
	private Comparator<T> comparator;
	private int concurrentTasks;
	private Map<String, Object> contexts = new HashMap<>();
	private Supplier<T> control;
	private Supplier<Boolean> enabled;
	private List<DoubleFunction<T, T, Boolean>> ignores = new ArrayList<>();
	private String name;
	private Supplier<Boolean> runIf;
	private DoubleAction<Operation, Exception> thrown;
	private boolean throwOnMismatches;
}
