package model;

import java.util.Locale;

public class DrawLine extends Instruction {
	/** Coordinates of the end of this line. */
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
		return (write ? "G01" : "G00") + " X" + String.format(Locale.US, "%.3f", x) + " Y"
				+ String.format(Locale.US, "%.3f", y) + " Z" + String.format(Locale.US, "%.3f", z);
	}
}
