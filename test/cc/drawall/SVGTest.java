package cc.drawall;

import static org.junit.Assert.assertTrue;

import java.nio.channels.Channels;

import org.junit.Test;

import cc.drawall.svg.SVGImporter;

public class SVGTest {
	@Test
	public void ok() {
		Drawing drawing = new SVGImporter().process(Channels.newChannel(
				getClass().getResourceAsStream("ellipses.svg"))).drawing;
		drawing.mergeLayers();
		Drawing simpleDrawing = new SVGImporter().process(Channels.newChannel(
				getClass().getResourceAsStream("ellipses_simple.svg"))).drawing;
		simpleDrawing.mergeLayers();
		assertTrue(drawing.looksLike(simpleDrawing));
	}
}
