# VPATH = .
CLASSPATH = -classpath .:/usr/share/java/junit-4.11.jar
JAVAC = ecj $(CLASSPATH) -source 1.8 -d . -maxProblems 10
JAVA = java $(CLASSPATH)
CLASSES = $(shell find -name '*.java' | sed -r 's!\.java!\.class!')
FORMATS = $(shell find -name '*.java' | sed -r 's!([^/]+)\.java!.\1.format!')

all: format $(CLASSES)

format: $(FORMATS)
.%.format: %.java
	# perl -i format.pl $< $< && touch $@

%.class: %.java
	$(JAVAC) $<

clean:
	rm $(CLASSES)

test: test/GCodeCleanerTest.class
	$(JAVA) test.GCodeCleanerTest

run: drawall/GLCBuilder.class
	$(JAVA) drawall.GLCBuilder $(ARGS)

.PHONY: all format clean test run
