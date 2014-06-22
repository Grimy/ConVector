package model;

public class DrawBézier extends Instruction {
	/** Coordinates of the control points. */
	private double[] points;

	public DrawBézier(double... points) {
		assert points.length % 2 == 0; // TODO: use a Point[] instead?
		this.points = points;
	}

	@Override
	public String toGCode() {
		throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public String toSVG() {
		return String.format("C %.3f,%.3f %.3f,%.3f %.3f,%.3f",
				points[0], points[1], points[2], points[3], points[4], points[5]);
	}
}
