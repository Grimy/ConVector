package model;

public class Pause extends Instruction {
	@Override public String toGCode() {
		return "M0";
	}
}
