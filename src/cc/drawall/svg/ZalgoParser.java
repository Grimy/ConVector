package cc.drawall.svg;

import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public abstract class ZalgoParser {
	private static final Pattern ATTR = Pattern.compile("\\s*+=(['\"])");
	private static final Pattern TEXT = Pattern.compile("(?:[^<]|"
			+ "<![^\134>]*(\\[.*?\\])?>|<\\?.*?\\?>)*+");
	private static final Pattern NAME = Pattern.compile("[<?]*\\s*(/?[\\w:\\-]+|/|>)");
	protected String currentTag;
	protected Scanner scanner;

	protected abstract void tag(String tagname);
	protected abstract void gt(String tagname);
	protected abstract void endTag(String tagname);
	protected abstract void attribute(String key);
	protected abstract void text(String text);
	
	public final void parse(Scanner scanner) {
		this.scanner = scanner;
		scanner.useLocale(Locale.US);
MAIN:
		for (;;) {
			text(scanner.skip(TEXT).match().group());
			currentTag = scanner.skip(NAME).match().group(1);
			// System.out.println("Tag: " + currentTag);
			if (currentTag.startsWith("/")) {
				endTag(currentTag.substring(1));
				scanner.skip(">");
				if (currentTag.equals("/svg")) {
					return;
				}
				continue;
			}
			tag(currentTag);
			String key = scanner.skip(NAME).match().group(1);
			while (!key.equals(">")) {
				if (key.equals("/")) {
					gt(currentTag);
					endTag(currentTag);
					scanner.skip(">");
					continue MAIN;
				}
				// System.out.println("Attr: " + key);
				String quote = scanner.skip(ATTR).match().group(1);
				attribute(key);
				scanner.skip(".*?" + quote);
				key = scanner.skip(NAME).match().group(1);
			}
			gt(currentTag);
		}
	}
}
