package dk.darknight.scientist;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;

public class LogPublisher implements IResultPublisher {
	public static final LogPublisher DEFAULT = new LogPublisher(); 
	private final Logger log;
	
	private LogPublisher() {
		log = LoggerFactory.getLogger(LogPublisher.class);
	}
		
	public LogPublisher(@NonNull Logger log) {
		this.log = log;
	}

	public <T, TClean> void publish(Result<T, TClean> result) {
		final ImmutableList<Observation<T, TClean>> mismatchedObservations = result.getMismatchedObservations();
		final ImmutableList<Observation<T, TClean>> observations = result.getObservations();
		final String[] durations = new String[observations.size()];
		final String experimentId = result.getExperimentName() + " (" + UUID.randomUUID().toString() + ")";
		
		for (int i = 0; i < observations.size(); i++) {
			Observation<T, TClean> o = observations.get(i);
			durations[i] = o.getName() + ": " + o.getDuration() + "ms";
		}

		log.info(experimentId + ": " + Joiner.on(", ").join(durations));

		if (!mismatchedObservations.isEmpty()) {
			final Observation<T, TClean> control = result.getControl();
			String expectedValue;
			if (control.isThrown()) {
				expectedValue = "thrown exception " + exceptionToString(control.getException());
			} else {
				expectedValue = " '" + control.getCleanedValue() + "'";
			}

			for (Observation<T, TClean> observation : mismatchedObservations) {
				StringBuilder sb = new StringBuilder();
				sb.append(experimentId);
				sb.append(" mismatch: ");
				sb.append(observation.getName());
				sb.append(" ");
				if (observation.isThrown()) {
					Exception e = observation.getException();
					sb.append("threw ");
					sb.append(exceptionToString(e));
				} else {
					sb.append("returned '");
					sb.append(observation.getCleanedValue());
					sb.append("'");
				}
				sb.append("; expected " + expectedValue);
				
				log.info(sb.toString());
			}
		}
	}

	private String exceptionToString(Exception e) {
		return e.getClass().getSimpleName() + " (" + e.getMessage() + ")";
	}

}
