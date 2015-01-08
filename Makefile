CLASSPATH = -classpath bin
TESTCP = -classpath bin:/usr/share/java/*:test

JAVAC = javac $(CLASSPATH) -sourcepath src -source 1.8 -d bin
JAVA = java -ea $(CLASSPATH) -Xss228m $(JAVA_ARGS)
CLASSES = $(shell find src -name *.java | sed -r 's!src!bin!;s!java$$!class!')
PACKAGE = cc/drawall/
IMAGES := $(shell find examples/* -maxdepth 0 -type f)
IMAGES := $(subst examples,output,$(IMAGES))
IMAGES := $(IMAGES:=.png)

# "make build": use ecj to compile .java files into .class files
build: $(CLASSES)

bin/%.class: src/%.java
	$(JAVAC) $<

jar: build
	cd bin; zip -r drawall.jar *

# "make doc": generate the documentation using Doxygen
doc:
	doxygen doc/Doxyfile

# "make clean": remove generated files
clean:
	find bin -name '*.class' -delete

# "make run ARGS=[...]": invokes java on the main class with specified arguments
run: build
	$(JAVA) cc.drawall.ConVector $(ARGS)

# "make bench": runs benchmarks
bench:
	$(JAVA) -Xprof cc.drawall.ConVector $(ARGS)

mass: $(IMAGES)

output/%.png: examples/% $(CLASSES)
	./montage.sh $(notdir $<)

test:
	javac $(TESTCP) test/cc/drawall/SVGTest.java && java $(TESTCP) org.junit.runner.JUnitCore cc.drawall.SVGTest

deps:
	jdeps -v -p cc.drawall bin/cc/drawall/*.class | grep -v '^ '

sonar: build
	sonar-runner
	DISPLAY=:0 xdg-open .sonar/issues-report/issues-report-light.html

.SECONDARY:
.PHONY: all doc cache test format build clean run test bench sonar
