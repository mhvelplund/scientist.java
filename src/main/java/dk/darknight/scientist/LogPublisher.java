package dk.darknight.scientist;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class LogPublisher implements IResultPublisher {

	@Override
	public <T, TClean> void publish(Result<T, TClean> result) {
		ImmutableList<Observation<T, TClean>> mismatchedObservations = result.getMismatchedObservations();
		if (!mismatchedObservations.isEmpty()) {
			for (Observation<T, TClean> observation : mismatchedObservations) {
				Object r = observation.isThrown() ? observation.getException() : observation.getValue();
				log.debug("{} ({}ms): {}", observation.getName(), observation.getDuration(), r);
			}
		}
	}
	
}
