package modules;

public class Writer implements Module {
	private static String fonts = "{'foo.ttf','bar.ttf'}"; // parse all *.ttf files in the module folder
	private static String sizes = "{6,7,8,9,10,11,12,13,14,15,16,18,20,22,24,26,28,32,36,40,44,48,54,60,66,72,80,88,96}";
	
	@Override
	public String getParamTypes() {
		return "[{'name':'Font','type':'int','values':" + fonts + "};" +
				"{'name':'Text size','type':'int','values':" + sizes + "}]";
	}

	@Override
	public String getSupportedFormats() {
		return "txt"; // .md should be useful also :) (in a future release)
	}

	@Override
	public String getName() {
		return "Writer";
	}

	@Override
	public void process(String inputFilePath, String outputFilePath) {
		// TODO 
		// I have absolutely no idea how to use .ttf files to draw something, but this module should be very fun.
	}

	@Override
	public String getDescription() {
		return "Write a text in the specified font and size.";
	}

}
