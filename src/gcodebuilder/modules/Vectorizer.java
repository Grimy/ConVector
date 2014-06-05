package modules;

import java.io.IOException;

import controller.Dependencies;

public class Vectorizer implements Module {

	@Override
	public void process(String inputFilePath, String outputFilePath) {
		try {
			Process process = new ProcessBuilder(Dependencies.getPotracePath(),
					inputFilePath, outputFilePath).start();
			process.waitFor();
			// TODO catch potrace error
		} catch (IOException e) {
			System.out.println("Can not lauch potrace. Please check the path: it's currently "
					+ Dependencies.getPotracePath() + ", is it correct ?");
			e.printStackTrace();
		} catch (InterruptedException e) {
			throw new RuntimeException("Process potrace aborted.");
		}
	}
	
	@Override
	public String getParamTypes() {
		return "[]";
	}

	@Override
	public String getSupportedFormats() {
		return "png,bmp";
	}

	@Override
	public String getName() {
		return "Vectorizer";
	}

}