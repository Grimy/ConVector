JAVA_FLAGS = -classpath /usr/share/java/junit-4.11.jar:.

all: test/GCodeCleanerTest.class controller/GCodeCleaner.class

%.class: %.java
	javac $(JAVA_FLAGS) $<

clean:
	find . -name '*.class' -delete

test: test/GCodeCleanerTest.class
	java $(JAVA_FLAGS) test.GCodeCleanerTest

.PHONY: test
