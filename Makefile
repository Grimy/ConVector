MAKEFLAGS += --no-builtin-rules --silent

convector.jar: $(shell find src -name '*.java')
	mkdir tmp
	pkill -xf 'java -ea -jar convector.jar 3434' || true
	LANG=en_US.UTF-8 javac -classpath bin -sourcepath src -source 1.8 -d tmp $?
	cd tmp; zip -q -r -m ../$@ *; cd ../src; zip -q ../$@ `find . -type f -not -name '*.java'`
	rmdir tmp

serve:
	while true; do java -ea -jar convector.jar 3434; done

doc:
	doxygen doc/Doxyfile

sonar: convector.jar
	sonar-runner
	DISPLAY=:0 xdg-open .sonar/issues-report/issues-report-light.html

.PHONY: serve doc sonar
