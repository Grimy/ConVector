package modules;

import model.Instruction;
import java.util.Vector;

public class PSGraphics {
	PSGraphics prev = null;

	double[] ctm = idMatrix();
	Vector<Instruction> path = new Vector<>();
	byte linecap = 0; // butt, round, projecting
	byte linejoin = 0; // Miter, round, Bevel
	double miterLimit = 10;
	double linewidth = 1;

	// clip path
	// raster output device

	PSGraphics dup() {
		PSGraphics clone = new PSGraphics();
		clone.ctm = ctm.clone();
		clone.path.addAll(path);
		clone.linecap = linecap;
		clone.linejoin = linejoin;
		clone.miterLimit = miterLimit;
		clone.linewidth = linewidth;
		clone.prev = this;
		return clone;
	}

	static double[] idMatrix() {
		return new double[]{1.0, 0.0, 0.0, 1.0, 0.0, 0.0};
	}
}
