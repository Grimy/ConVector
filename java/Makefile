CLASSPATH = -classpath bin:lib/batik.jar:/usr/share/java/junit-4.11.jar
IGNORE = nls,unqualifiedField,boxing,serial
WARN = unusedLocal,resource
JAVAC = ecj $(CLASSPATH) -sourcepath src -warn:all -warn:-$(IGNORE) -err:all -err:-$(IGNORE),$(WARN) -source 1.8 -d bin -maxProblems 10
JAVA = java -ea $(CLASSPATH)
SOURCES = $(shell find drawall/* -type f -not -name .gitignore | sed -r 's!^!src/!;s!$$!.java!')
CLASSES = $(shell find drawall/* -type f -not -name .gitignore | sed -r 's!^!bin/!;s!$$!.class!')

# "make build": use ecj to compile .java files into .class files
build: format $(CLASSES)
bin/%.class: src/%.java
	$(JAVAC) $<

# "make format": use format.pl to create .java files
format: cache $(SOURCES)
src/%.java: % format.pl
	@mkdir -p $$(dirname $@)
	perl format.pl $< $< > $@

# "make cache": updates the class cache (not usually useful)
cache:
	find drawall >> ~/.vim/cache/java/drawall

# "make clean": remove generated files
clean:
	find src -name '*.java' -delete
	find bin -name '*.class' -delete

# "make run ARGS=[...]": invokes java on the main class with specified arguments
run: build
	$(JAVA) drawall.GLCBuilder $(ARGS)

# "make test": runs tests (unused right now)
test: test/GCodeCleanerTest.class
	$(JAVA) test.GCodeCleanerTest

.SECONDARY:
.PHONY: all cache format build clean run test
