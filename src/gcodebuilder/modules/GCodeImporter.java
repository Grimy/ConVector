package modules;

public class GCodeImporter implements Module {

	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return ".ngc,.gcode";
	}

	@Override
	public String getName() {
		return "GCode importer";
	}

	@Override
	public String getDescription() {
		return "Import a GCode file.";
	}

	@Override
	public void process(String inputFilePath, String outputFilePath) {
		// TODO Move GCode-cleaner code here!

	}

}
