package model;

public class Dwell extends Instruction {
	/** Time to sleep, in seconds. */
	private int t;

	public Dwell(int t) {
		this.t = t;
	}

	@Override
	public String toGCode() {
		return "G04 P" + t;
	}
}
