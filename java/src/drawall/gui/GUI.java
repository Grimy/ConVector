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

package drawall.gui;

import drawall.Filetype;
import drawall.GLCBuilder;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;


public class GUI extends Canvas {

	private JFileChooser chooser = new JFileChooser();
	private CharArrayWriter data = new CharArrayWriter();

	public static void main(String... args) {
		new GUI().init();
	}

	private GUI() {
		chooser.setFileFilter(new FileNameExtensionFilter("Vectors",
					Filetype.map.keySet().toArray(new String[0])));
	}

	private void init() {
		JMenuItem in = new JMenuItem("Import");
		in.addActionListener(event -> {
			if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			File file = chooser.getSelectedFile();
			try (Reader input = new FileReader(file)) {
				GLCBuilder.run(input, new PrintWriter(data),
						Filetype.fromFilename(file.getName()), Filetype.GCODE);
				repaint();
			} catch (IOException e) {
				System.exit(1);
				// TODO: alert
			}
		});

		JMenuItem out = new JMenuItem("Export");
		out.addActionListener(event -> {
			if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			try (Writer output = new FileWriter(chooser.getSelectedFile())) {
				output.write(data.toCharArray());
			} catch (IOException e) {
				System.exit(1);
				// TODO: alert
			}
		});

		JMenuBar bar = new JMenuBar();
		bar.add(in);
		bar.add(out);

		JFrame frame = new JFrame();
		frame.setJMenuBar(bar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(BorderLayout.CENTER, this);
		frame.setSize(new Dimension(500,500));
		frame.setVisible(true);
	}

	@Override
	public void paint(Graphics gr) {
		Graphics2D g = (Graphics2D) gr;
		super.paint(g);
		double ratio = Math.min(getWidth() / 65535.0, getHeight() / 65535.0);
		g.scale(ratio, ratio);
		Filetype.GCODE.input().process(new CharArrayReader(data.toCharArray()), g);
	}
}
