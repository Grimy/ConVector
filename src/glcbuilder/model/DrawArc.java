package model;

import java.util.Locale;

public class DrawArc extends Instruction {
	/** Coordinates of the end of this arc. */
	private double x, y;

	/** Coordinates of the cented of this arc. */
	private double i, j;

	/** true to move clockwise, false to move counterclockwise. */
	private boolean clockwise;

	public DrawArc(double x, double y, double i, double j, boolean clockwise) {
		this.x = x;
		this.y = y;
		this.i = i;
		this.j = j;
		this.clockwise = clockwise;
	}

	@Override
	public String toGCode() {
		return (clockwise ? "G02" : "G03") + " X" + String.format(Locale.US, "%.3f", x) + " Y"
				+ String.format(Locale.US, "%.3f", y) + " I" + String.format(Locale.US, "%.3f", i)
				+ " J" + String.format(Locale.US, "%.3f", j);
	}
}
