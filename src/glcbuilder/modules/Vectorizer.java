package modules;

import controller.Dependencies;
import java.io.IOException;
import java.util.Collection;
import model.Instruction;

public class Vectorizer implements Module {

	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return "png,bmp";
	}

	@Override
	public String getDescription() {
		return "Vectorizes a monochrome image.";
	}

	@Override
	public Collection<Instruction> process(String inputFilePath) {
		// try {
			// Process process = new ProcessBuilder(Dependencies.getPotracePath(),
					// inputFilePath, outputFilePath).start();
			// process.waitFor();
			// TODO catch potrace error
		// } catch (IOException e) {
			// System.out.println("Can not lauch potrace. Please check the path: it's currently "
					// + Dependencies.getPotracePath() + ", is it correct ?");
			// e.printStackTrace();
		// } catch (InterruptedException e) {
			// throw new RuntimeException("Process potrace aborted.");
		// }
		// TODO: call Vector module!
		throw new UnsupportedOperationException();
	}
}
