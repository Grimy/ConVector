package model;

public class DrawArc extends Instruction {
	/** Coordinates of the end of this arc. */
	private double x, y;

	/** Coordinates of the center of this arc. */
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
		return (clockwise ? "G02" : "G03") + String.format(" X%.3f Y%.3f I%.3f J%.3f", x, y, i, j);
	}

	@Override
	public String toSVG() {
		// TODO
		throw new UnsupportedOperationException("Converting arcs to SVG is not yet supported");
	}
}
