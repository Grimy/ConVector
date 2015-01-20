MAKEFLAGS += -rR

build:
	tup

# "make doc": generate the documentation using Doxygen
doc:
	doxygen doc/Doxyfile

sonar: build
	sonar-runner
	DISPLAY=:0 xdg-open .sonar/issues-report/issues-report-light.html

.PHONY: build doc sonar
