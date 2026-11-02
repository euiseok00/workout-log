package com.workoutlog.backend.exercise;

public enum SetType {
	WORKING, WARMUP, SUPER, TRI, GIANT, DROP, TOP, FAILURE, ASSIST, BACKOFF;

	public boolean requiresGroup() {
		return this == SUPER || this == TRI || this == GIANT;
	}

	public boolean countsTowardVolume() {
		return this != WARMUP;
	}

	public boolean countsAsSet() {
		return this != WARMUP && this != DROP;
	}

	public boolean fitsGroupSize(int size) {
		return switch (this) {
			case SUPER -> size == 2;
			case TRI -> size == 3;
			case GIANT -> size >= 4;
			default -> true;
		};
	}
}
