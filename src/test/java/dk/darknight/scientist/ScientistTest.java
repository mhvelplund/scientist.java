package dk.darknight.scientist;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Comparator;

import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import dk.darknight.scientist.functions.ExperimentFunction;

public class ScientistTest {
	private final String user = "jdoe";
	private Supplier<Boolean> enabled = Suppliers.ofInstance(true);

	boolean isNotCollaborator(String name) {
		
		return user.equals(name);
	}

	boolean isHasAccess(String name) {
		return !user.equals(name);
	}

	@Test(expected=MismatchException.class)
	public void testScienceMisMatch() {
		Scientist.setEnabled(enabled);
		Scientist.science("widget-permissions", new ExperimentFunction<Boolean>() {
			public void apply(IExperiment<Boolean> experiment) {
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
				
				experiment.setThrowOnMismatches(true);
			}
		});

		fail("This should not happen");
	}

	@Test
	public void testScienceMatch() {
		Scientist.setEnabled(enabled);
		boolean isCollaborator = Scientist.science("widget-permissions", new ExperimentFunction<Boolean>() {
			public void apply(IExperiment<Boolean> experiment) {
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
				
				experiment.setThrowOnMismatches(true);
			}
		});

		assertTrue(isCollaborator);
	}
	
	@Test
	public void testScienceNotEnabled() {
		Scientist.setEnabled(Suppliers.ofInstance(false));
		Scientist.science("widget-permissions", new ExperimentFunction<Boolean>() {
			public void apply(IExperiment<Boolean> experiment) {
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
				
				experiment.setThrowOnMismatches(true);
			}
		});
	}

}