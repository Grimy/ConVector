package cc.drawall;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.hamcrest.CoreMatchers.*;

public class SVGTest {
	@Test
	public void ok() {
		WriterGraphics2D g = new WriterGraphics2D();
		new SVGImporter().process(getClass().getResourceAsStream("ellipses.svg"), g);
		Drawing d1 = g.getDrawing();
		d1.flatten();
		g = new WriterGraphics2D();
		new SVGImporter().process(getClass().getResourceAsStream("ellipses_simple.svg"), g);
		Drawing d2 = g.getDrawing();
		d2.flatten();
		assertTrue(d1.looksLike(d2));
	}
}
