package model;

public class EndProgram extends Instruction {
	@Override public String toSVG() {
		return "'/></svg>";
	}

	@Override public String toGCode() {
		return "M30";
	}
}
