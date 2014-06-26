CLASSPATH = -classpath bin:/usr/share/java/junit-4.11.jar
IGNORE = nls,unqualifiedField,boxing,serial
WARN = unusedLocal,resource
JAVAC = ecj $(CLASSPATH) -sourcepath src -warn:+$(WARN) -warn:-$(IGNORE) -err:all -err:-$(IGNORE),$(WARN) -source 1.8 -d bin -maxProblems 10
JAVA = java $(CLASSPATH)
SOURCES = $(shell find drawall/* | sed -r 's!^!src/!;s!$$!.java!')
CLASSES = $(shell find drawall/* | sed -r 's!^!bin/!;s!$$!.class!')

# "make build": use ecj to compile .java files into .class files
build: format $(CLASSES)
bin/%.class: src/%.java
	$(JAVAC) $<

# "make format": use format.pl to create .java files
format: cache $(SOURCES)
src/%.java: %
	perl format.pl $< $< > $@

# "make cache": updates the class cache (not usually useful)
cache:
	cd bin; find org -name '*.class' | perl -ple 's!bin/|\.class!!g' > ~/.vim/cache/java/glc
	find drawall >> ~/.vim/cache/java/glc

# "make clean": remove generated files
clean:
	rm $(SOURCES) $(CLASSES)

# "make run ARGS=[...]": invokes java on the main class with specified arguments
run: build
	$(JAVA) drawall.GLCBuilder $(ARGS)

# "make test": runs tests (unused right now)
test: test/GCodeCleanerTest.class
	$(JAVA) test.GCodeCleanerTest

.SECONDARY:
.PHONY: all cache format build clean run test
