package model;

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

	public DrawLine(double[] points, boolean write) {
		this(points[0], points[1], 0, write);
	}

	@Override
	public String toGCode() {
		return (write ? "G01" : "G00") + String.format(" X%.3f Y%.3f Z%.3f", x, y, z);
	}

	@Override
	public String toSVG() {
		return (write ? "L " : "M ") + String.format("%.3f,%.3f", x, y);
	}
}
