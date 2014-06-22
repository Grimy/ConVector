# VPATH = .
CLASSPATH = -classpath .:/usr/share/java/junit-4.11.jar
JAVAC = ecj $(CLASSPATH) -source 1.8 -d . -maxProblems 10
JAVA = java $(CLASSPATH)
CLASSES = $(shell find -name '*.java' | sed 's!\.java!\.class!')

all: $(CLASSES)

%.class: %.java
	perl -i format.pl $< $<
	$(JAVAC) $<

clean:
	rm $(CLASSES)

test: test/GCodeCleanerTest.class
	$(JAVA) test.GCodeCleanerTest

run: controller/GLCBuilder.class
	$(JAVA) controller.GLCBuilder $(ARGS)

.PHONY: test
