MAKEFLAGS += --no-builtin-rules --silent

convector.jar: $(shell find src -name '*.java')
	mkdir tmp
	pkill -xf 'java -ea -jar convector.jar 3434' || true
	LANG=en_US.UTF-8 javac -classpath bin -sourcepath src -source 1.8 -d tmp $?
	cd tmp; zip -q -r -m ../$@ *; cd ../src; zip -q ../$@ `find . -type f -not -name '*.java'`
	rmdir tmp

serve:
	while true; do java -ea -jar convector.jar 3434; done

test: convector.jar
	cd test; mkdir -p pdf ps svg; for file in *.svg; do \
		echo $$file; \
		java -ea -jar ../$< $$file svg/$$file.svg ps/$$file.ps pdf/$$file.pdf; \
	done; md5sum pdf/* ps/* svg/* >md5sums; diff md5sums.ok md5sums

doc:
	doxygen doc/Doxyfile

sonar: convector.jar
	sonar-runner
	DISPLAY=:1 xdg-open .sonar/issues-report/issues-report-light.html

.PHONY: serve test doc sonar
