/*
 * This file is part of DraWall.
 * DraWall is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * DraWall is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with DraWall. If not, see <http://www.gnu.org/licenses/>.
 * © 2012–2014 Nathanaël Jourdane
 * © 2014 Victor Adam
 */

package cc.drawall.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.swing.JOptionPane;

/** A log handler that displays the logs in an alert box. */
public class AlertHandler extends Handler {

	private static final Map<Level, Integer> levels = new HashMap<>(); static {
		levels.put(Level.SEVERE, JOptionPane.ERROR_MESSAGE);
		levels.put(Level.WARNING, JOptionPane.WARNING_MESSAGE);
	}

	@Override
	public void close() {
		// do nothing
	}

	@Override
	public void flush() {
		// do nothing
	}

	@Override
	public void publish(final LogRecord record) {
		JOptionPane.showMessageDialog(null, record.getMessage(), "DraWall",
				levels.getOrDefault(record.getLevel(), JOptionPane.INFORMATION_MESSAGE));
	}
}
