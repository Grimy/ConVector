package model;

/**
 * Intern representation of a single machine instruction.
 */
public abstract class Instruction {
	public abstract String toGCode();

	public String toSVG() {
		return "";
	}

	@Override
	public String toString() {
		return getClass().getName() + " " + toGCode();
	}
}
