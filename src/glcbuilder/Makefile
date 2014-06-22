CLASSPATH = -classpath .:/usr/share/java/junit-4.11.jar:lib/*
JAVAC = javac $(CLASSPATH) -Werror -Xlint:all -Xmaxerrs 10
JAVA = java $(CLASSPATH)

all: $(shell echo */*.java | sed 's/\.java/\.class/g')

%.class: %.java
	@$(JAVAC) $<

clean:
	@find . -name '*.class' -delete

test: test/GCodeCleanerTest.class
	@$(JAVA) test.GCodeCleanerTest

run: controller/GLCBuilder.class
	@$(JAVA) controller.GLCBuilder $(ARGS)

.PHONY: test
