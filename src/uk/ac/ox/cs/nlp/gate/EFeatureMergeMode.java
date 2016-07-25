package uk.ac.ox.cs.nlp.gate;

public enum EFeatureMergeMode {
	MERGE ("Merge"),
	SELECT_ONE ("SelectOne"),
	;
	private final String mergeMode;
	EFeatureMergeMode(String mergeMode) {
		this.mergeMode = mergeMode;
	}
	public final String getName() {
		return this.mergeMode;
	}
	@Override public String toString() {
		return getName();
	}
}
