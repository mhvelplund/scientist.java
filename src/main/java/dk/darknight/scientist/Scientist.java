package dk.darknight.scientist;


import com.google.common.base.Functions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import dk.darknight.scientist.functions.Action;
import dk.darknight.scientist.functions.ExperimentFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

/**
 * A class for carefully refactoring critical paths.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Scientist {
	private static Supplier<Boolean> enabled = Suppliers.ofInstance(true);
	
	private static <T, TClean> Experiment<T, TClean> build(String name, int concurrentTasks, Action<IExperiment<T>> experiment) {
		val experimentBuilder = new Experiment<T, TClean>(name, enabled, concurrentTasks);
		experiment.apply(experimentBuilder);
		return experimentBuilder;
	}
	
	/**
	 * Conduct a synchronous experiment
	 * 
	 * @param <T>
	 *            The return type of the experiment.
	 * @param name
	 *            Name of the experiment
	 * @param experiment
	 *            Experiment callback used to configure the experiment
	 * @return The value of the experiment's control function.
	 */
	public static <T> T science(@NonNull String name, @NonNull ExperimentFunction<T> experiment) {
		val builder = build(name, 1, experiment);
		builder.clean(Functions.<T>identity());		
		return builder.build().run();
	}

	public static synchronized void setEnabled(@NonNull Supplier<Boolean> enabled) {
		Scientist.enabled = enabled;
	}

}