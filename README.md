# Scientist for Java

<a href="https://travis-ci.org/mhvelplund/scientist.java"><img src="https://travis-ci.org/mhvelplund/scientist.java.svg?branch=master" alt="CI status"/></a>

A quick and nasty port of the [Scientist.net](https://github.com/github/Scientist.net) 
(commit #[bf32144](https://github.com/github/Scientist.net/commit/bf321444af36d2982744cc1f8b9c4c5a329b9bb2)) 
project, which itself is a port of the original [Scientist](https://github.com/github/scientist) 
library. Credits for good things should go to these guys, while blame for bugs 
belongs solely to me -- not legally though, see 
[LICENSE](https://github.com/mhvelplund/scientist.java/blob/master/LICENSE) ;)

The code is written to be compatible with Java 7 because not everyone gets to 
work with nice clean new code, but attempts have been made to make it lambda 
friendly for Java 8 users (lucky bastards!).

## Why a port of a port?

The code-base I work with in my day job is a Java back-end with a .NET desktop 
client, and having the same Science API on both sides made good sense.

## Usage

The architecture from Scientist.NET has been kept with the exception of 
asynchronous experiments. Java doesn't have the same ``asynch`` / ``await`` 
language features, so that functionality would require some extra thought. 
Another difference is that "Try()" is now "attempt()".

Java 7 example:

    public class ScientistTest {
    	boolean isNotCollaborator(String name) { return true; }
    	boolean isHasAccess(String name) { return true; }
    
    	@Test
    	public void testScience() {
    		final String user = "jdoe";
    		boolean isCollaborator = Scientist.science("widget-permissions", new ExperimentFunction<Boolean>() {
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
    
    		assertTrue(isCollaborator);
    	}
    }    

Java 8 example:

    public class ScientistTest {
    	boolean isNotCollaborator(String name) { return true; }
    	boolean isHasAccess(String name) { return true; }
    
    	@Test
    	public void testScience() {
    		final String user = "jdoe";
			boolean isCollaborator = Scientist.science("widget-permissions", experiment -> {
				experiment.use(() -> isNotCollaborator(user));
				experiment.attempt(() -> isHasAccess(user));
				experiment.setThrowOnMismatches(true);
			});
    
    		assertTrue(isCollaborator);
    	}
    }    

See the original [.NET documentation](https://github.com/github/Scientist.net) 
for a full description of how to setup experiments (the API in Java is the same).

## Lombok

This project uses [Project Lombok](https://projectlombok.org/) for boilerplate code. 
It will compile fine with Maven, but if you want to edit the sources in an IDE, you 
will need the [Project Lombok plugin](https://projectlombok.org/download.html).

