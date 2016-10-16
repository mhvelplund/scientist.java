package dk.darknight.scientist;

import static org.junit.Assert.assertTrue;

import java.util.Comparator;

import org.junit.Test;

import com.google.common.base.Supplier;

import dk.darknight.scientist.functions.ExperimentFunction;

public class ScientistTest {
	boolean isNotCollaborator(String name) {
		return true;
	}

	boolean isHasAccess(String name) {
		return false;
	}

	@Test
	public void testScience() {
		final String user = "jdoe";

		boolean isCollaborator = Scientist.science("widget-permissions", new ExperimentFunction<Boolean>() {
			public void run(IExperiment<Boolean> experiment) {
				experiment.compare(new Comparator<Boolean>() {
					public int compare(Boolean o1, Boolean o2) {
						return o1.compareTo(!o2); // Reverse comparator
					}
				});

				experiment.use(new Supplier<Boolean>() {
					public Boolean get() {
						return isNotCollaborator(user);
					}
				});

				experiment.attempt(new Supplier<Boolean>() {
					public Boolean get() {
						return isHasAccess(user);
					}
				});
			}
		});

		assertTrue(isCollaborator);
	}
}