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

package cc.drawall.gui;

import cc.drawall.DrawMaster;
import cc.drawall.Drawing;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


public class GUI extends Canvas {

	private final JFileChooser chooser = new JFileChooser();
	private transient Drawing drawing = new Drawing();

	public GUI() {
		// chooser.setFileFilter(new FileNameExtensionFilter("Vectors",
			// DrawMaster.map.keySet().toArray(new String[DrawMaster.map.size()])));
	}

	public void init() {
		final JMenuItem in = new JMenuItem("Import");
		in.addActionListener(event -> {
			if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			final File file = chooser.getSelectedFile();
			drawing = DrawMaster.importFile(file.getAbsolutePath());
			repaint();
		});

		final JMenuItem out = new JMenuItem("Export");
		out.addActionListener(event -> {
			if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			final File file = chooser.getSelectedFile();
			DrawMaster.exportFile(file.getAbsolutePath(), drawing);
		});

		final JMenuBar bar = new JMenuBar();
		bar.add(in);
		bar.add(out);

		final JFrame frame = new JFrame();
		frame.setJMenuBar(bar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(BorderLayout.CENTER, this);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}

	@Override
	public void paint(final Graphics graphics) {
		assert graphics instanceof Graphics2D;
		Graphics2D g = (Graphics2D) graphics;
		super.paint(g);
		final Rectangle bounds = drawing.getBounds();
		double ratio = Math.min((double) getWidth() / bounds.width,
				(double) getHeight() / bounds.height);
		g.scale(ratio, ratio);
		g.translate(-bounds.x, -bounds.y);
		for (Drawing.Splash splash: drawing) {
			g.setColor(splash.color);
			g.fill(splash.shape);
		}
	}
}
