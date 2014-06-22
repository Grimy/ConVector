# VPATH = .
CLASSPATH = -classpath .:/usr/share/java/junit-4.11.jar
IGNORE = nls,unqualifiedField,boxing,serial
WARN = unusedLocal,resource
JAVAC = ecj $(CLASSPATH) -warn:+$(WARN) -err:all -err:-$(IGNORE),$(WARN) -source 1.8 -d . -maxProblems 10
JAVA = java $(CLASSPATH)
CLASSES = $(shell find -name '*.java' | sed -r 's!\.java!\.class!')
FORMATS = $(shell find -name '*.java' | sed -r 's!([^/]+)\.java!.\1.format!')

build: $(CLASSES)

%.class: %.java
	$(JAVAC) $<

all: cache format $(CLASSES)

cache:
	./_cache

format: $(FORMATS)

.%.format: %.java
	perl -i format.pl $< $< && touch $@

clean:
	rm $(CLASSES)

test: test/GCodeCleanerTest.class
	$(JAVA) test.GCodeCleanerTest

run: drawall/GLCBuilder.class
	$(JAVA) drawall.GLCBuilder $(ARGS)

.PHONY: all format cache clean test run
