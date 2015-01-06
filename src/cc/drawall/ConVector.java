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

package cc.drawall;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;

/** User interface. */
class ConVector extends Canvas {
	static {
		// Ensure the decimal separator is "." everywhere.
		Locale.setDefault(Locale.Category.FORMAT, Locale.US);
		System.setProperty("java.util.logging.config.file", "bin/log.properties");
	}
	private static final Logger log = Logger.getLogger(ConVector.class.getName());

	private final JFileChooser chooser = new JFileChooser();
	private transient Drawing drawing = new Drawing();

	ConVector() {
		// TODO: get list of valid formats
		chooser.setFileFilter(new FileNameExtensionFilter("Vectors",
					new String[] {"svg", "pdf", "ps", "gcode", "dov"}));
		setSize(800, 600);
	}

	/** If command lines arguments are given, process them. Otherwise, start the GUI.
	  * @param args first argument is the input file, second is the output file */
	public static void main(final String... args) {
		if (args.length == 0) {
			new ConVector().createAndShowGUI();
			return;
		}
		if (args.length != 2) {
			log.severe("Too many arguments.");
			return;
		}
		exportFile(new File(args[1]), importFile(new File(args[0])));
	}

	private static String getExtension(final File file) {
		final String filename = file.getName();
		return filename.substring(filename.lastIndexOf('.') + 1);
	}

	private static Drawing importFile(final File file) {
		try (final FileInputStream input = new FileInputStream(file);
				final FileChannel chan = input.getChannel()) {
			return PluginOverseer.importStream(chan);
		} catch (final IOException e) {
			log.severe("Problem reading file " + file.getName() + ": " + e);
			return new Drawing();
		}
	}

	private static void exportFile(final File file, final Drawing drawing) {
		try (final RandomAccessFile handle = new RandomAccessFile(file, "rw");
				final FileChannel chan = handle.getChannel()) {
			final ByteBuffer output = chan.map(FileChannel.MapMode.READ_WRITE, 0, 10 << 20);
			PluginOverseer.exportStream(output, getExtension(file), drawing);
			chan.truncate(output.position());
		} catch (final IOException e) {
			log.severe("Can’t open file for writing: " + file.getName());
		}
	}

	private void createAndShowGUI() {
		final JMenuItem in = new JMenuItem("Import");
		in.addActionListener(event -> {
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				drawing = importFile(chooser.getSelectedFile());
				repaint();
			}
		});

		final JMenuItem out = new JMenuItem("Export");
		out.addActionListener(event -> {
			if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
				exportFile(chooser.getSelectedFile(), drawing);
			}
		});

		final JMenuBar bar = new JMenuBar();
		bar.add(in);
		bar.add(out);

		final JFrame frame = new JFrame();
		frame.setJMenuBar(bar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(BorderLayout.CENTER, this);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void paint(final Graphics graphics) {
		assert graphics instanceof Graphics2D;
		final Graphics2D g = (Graphics2D) graphics;
		super.paint(g);
		final Rectangle2D bounds = drawing.getBounds();
		final double ratio = Math.min(getWidth() / bounds.getWidth(),
				getHeight() / bounds.getHeight());
		g.scale(ratio, ratio);
		g.translate(-bounds.getX(), -bounds.getY());
		for (final Drawing.Splash splash: drawing) {
			g.setColor(splash.color);
			g.fill(splash.shape);
		}
	}
}
