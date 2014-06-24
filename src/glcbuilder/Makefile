CLASSPATH = -classpath .:/usr/share/java/junit-4.11.jar
IGNORE = nls,unqualifiedField,boxing,serial
WARN = unusedLocal,resource
JAVAC = ecj $(CLASSPATH) -warn:+$(WARN) -warn:-$(IGNORE) -err:all -err:-$(IGNORE),$(WARN) -source 1.8 -d . -maxProblems 10
JAVA = java $(CLASSPATH)
CLASSES = $(shell find -name '*.java' | sed -r 's!\.java!\.class!')
FORMATS = $(shell find -name '*.java' | sed -r 's!([^/]+)\.java!.\1.format!')

# "make" or "make build": use ecj to compile .java files into .class files
build: $(CLASSES)
%.class: %.java
	$(JAVAC) $<

# "make format": use format.pl to generate license, package and import statements
format: cache $(FORMATS)
.%.format: %.java
	perl -i format.pl $< $< && touch $@

# "make cache": updates the class cache (not usually useful)
cache:
	find * -name '*.class' | perl -nale '$$_ = $$F[-1]; s/\.class$$// and print' > ~/.vim/cache/java/glc

# "make all": format, then build
all: format build

# "make clean": remove generated files
clean:
	rm $(CLASSES) $(FORMATS)

# "make run ARGS=[...]": invokes java on the main class with specified arguments
run: drawall/GLCBuilder.class
	$(JAVA) drawall.GLCBuilder $(ARGS)

# "make test": runs tests (unused right now)
test: test/GCodeCleanerTest.class
	$(JAVA) test.GCodeCleanerTest

.PHONY: all format cache clean test run
