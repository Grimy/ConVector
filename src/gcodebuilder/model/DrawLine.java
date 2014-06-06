package model;

public class DrawLine extends Instruction {
	/** Coordinates of the end of the line, in an arbitrary unit. */
	private double x, y, z;

	/** When false, move without actually drawing. */
	private boolean write;

	public DrawLine(double x, double y, double z, boolean write) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.write = write;
	}

	@Override
	public String toGCode() {
		return (write ? "G01" : "G00") + " X" + x + " Y" + y + " Z" + z;
	}
}
