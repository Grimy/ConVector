JAVA_FLAGS = -classpath /usr/share/java/junit-4.11.jar:.

all: $(shell find . -name '*.java' | sed 's/\.java/\.class/')

%.class: %.java
	javac $(JAVA_FLAGS) $<

clean:
	find . -name '*.class' -delete

test: test/GCodeCleanerTest.class
	java $(JAVA_FLAGS) test.GCodeCleanerTest

run: controller/GLCBuilder.class
	java $(JAVA_FLAGS) controller.GLCBuilder

.PHONY: test
