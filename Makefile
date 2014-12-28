CLASSPATH = -classpath bin
TESTCP = -classpath bin:/usr/share/java/*:test

ERR = assertIdentifier,charConcat,compareIdentical,conditionAssign,constructorName,deadCode,deprecation,discouraged,emptyBlock,enumIdentifier,enumSwitch,enumSwitchPedantic,fallthrough,fieldHiding,finalBound,finally,forbidden,hashCode,includeAssertNull,indirectStatic,inheritNullAnnot,intfAnnotation,intfNonInherited,intfRedundant,invalidJavadoc,invalidJavadocTag,invalidJavadocTagDep,invalidJavadocTagNotVisible,maskedCatchBlock,noEffectAssign,null,nullAnnotConflict,nullAnnotRedundant,nullDereference,paramAssign,pkgDefaultMethod,raw,resource,semicolon,specialParamHiding,staticReceiver,suppress,switchDefault,syncOverride,syntheticAccess,typeHiding,unavoidableGenericProblems,unchecked,unnecessaryElse,unusedAllocation,unusedImport,unusedLabel,unusedLocal,unusedParamOverriding,unusedParamImplementing,unusedParamIncludeDoc,unusedPrivate,unusedThrown,unusedThrownWhenOverriding,unusedThrownIncludeDocComment,unusedThrownExemptExceptionThrowable,unusedTypeArgs,uselessTypeCheck,varargsCast,warningToken,static-method,static-access
# TEMPORARILY DISABLED = super
# IGNORE = -nls,-unqualifiedField,-boxing,-serial,-hiding,-local
JAVAC = ecj $(CLASSPATH) -sourcepath src -warn:-serial -err:$(ERR) -source 1.8 -d bin -maxProblems 10
JAVA = java -ea $(CLASSPATH) $(JAVA_ARGS)
CLASSES = $(shell find src -name *.java | sed -r 's!src!bin!;s!java$$!class!')
PACKAGE = cc/drawall/

# "make build": use ecj to compile .java files into .class files
build: $(CLASSES)

bin/%.class: src/%.java
	$(JAVAC) $<

jar: build
	cd bin; zip -r drawall.jar *

# "make clean": remove generated files
clean:
	find bin -name '*.class' -delete

# "make run ARGS=[...]": invokes java on the main class with specified arguments
run: build
	$(JAVA) cc.drawall.ui.Main $(ARGS)
	#| perl -pe 's!^\s+at (drawall.*?)\.[^.]+\(.*?:(\d+)\)$!$2p src.$1/java!&&y!./!/.! && s!^!`sed -n $_`!e && s!^\s+!!'

# "make bench": runs benchmarks
bench:
	$(JAVA) -Xprof cc.drawall.GLCBuilder examples/lyra.svg blah.pdf

test:
	javac $(TESTCP) test/cc/drawall/SVGTest.java && java $(TESTCP) org.junit.runner.JUnitCore cc.drawall.SVGTest

deps:
	jdeps -v -p cc.drawall bin/cc/drawall/*.class | grep -v '^ '

sonar: build
	sonar-runner
	DISPLAY=:0 xdg-open .sonar/issues-report/issues-report-light.html

.SECONDARY:
.PHONY: all cache test format build clean run test bench sonar
