package modules;

import java.util.Collection;
import model.Instruction;

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
	public String getDescription() {
		return "Import a GCode file.";
	}

	@Override
	public Collection<Instruction> process(String inputFilePath) {
		// TODO Move GCode-cleaner code here!
		throw new UnsupportedOperationException();
	}
}
