/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * Copyright (c) 2012-2014 Nathanaël Jourdane.
 */

package drawall;


/**
 * Intern representation of a single machine instruction.
 */
public class Instruction {

	// return (clockwise ? "G02" : "G03") + String.format(" X%.3f Y%.3f I%.3f J%.3f", x, y, i, j);
	public enum Kind {
		MOVE("G00 X% Y%", "M %,%"),
		LINE("G01 X% Y%", "L %,%"),
		ARC_CW("G02 X% Y% I% J%", ""),
		ARC_CCW("G03 X% Y% I% J%", ""),
		DWELL("G04 P%", ""),
		CUBIC("G05 I% J% P% Q% X% Y%", "C %,% %,% %,%"),
		QUADRATIC("G5.1 I% J% X% Y%", "Q %,% %,%"),
		PAUSE("M0", ""),
		END("M30", "'/></svg>");
		
		String gcodeFormat, svgFormat;
		Kind(String gcodeFormat, String svgFormat) {
			this.gcodeFormat = gcodeFormat;
			this.svgFormat = svgFormat;
		}
	}

	private Kind kind;
	private double[] args;

	public Instruction(Kind kind, double... args) {
		assert args.length == count(kind.gcodeFormat);
		this.kind = kind;
		this.args = args;
	}

	public String toGCode() {
		return format(this.kind.gcodeFormat);
	}

	public String toSVG() {
		return format(this.kind.svgFormat);
	}

	private static int count(String format) {
		int i = 0;
		for (char c: format.toCharArray()) {
			i += c == '%' ? 1 : 0;
		}
		return i;
	}

	// perf
	private String format(String format) {
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (char c: format.toCharArray()) {
			if (c == '%') {
				double arg = args[i++];
				builder.append((int) arg);
				builder.append('.');
				builder.append((int) arg * 1E3);
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		return super.toString() + toGCode();
	}
}

