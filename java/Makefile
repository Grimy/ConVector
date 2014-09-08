CLASSPATH = -classpath bin:lib/batik.jar

ERR = assertIdentifier,charConcat,compareIdentical,conditionAssign,constructorName,deadCode,deprecation,discouraged,emptyBlock,enumIdentifier,enumSwitch,enumSwitchPedantic,fallthrough,fieldHiding,finalBound,finally,forbidden,hashCode,includeAssertNull,indirectStatic,inheritNullAnnot,intfAnnotation,intfNonInherited,intfRedundant,invalidJavadoc,invalidJavadocTag,invalidJavadocTagDep,invalidJavadocTagNotVisible,maskedCatchBlock,noEffectAssign,null,nullAnnotConflict,nullAnnotRedundant,nullDereference,paramAssign,pkgDefaultMethod,raw,resource,semicolon,specialParamHiding,staticReceiver,suppress,switchDefault,syncOverride,syntheticAccess,typeHiding,unavoidableGenericProblems,unchecked,unnecessaryElse,unusedAllocation,unusedImport,unusedLabel,unusedLocal,unusedParamOverriding,unusedParamImplementing,unusedParamIncludeDoc,unusedPrivate,unusedThrown,unusedThrownWhenOverriding,unusedThrownIncludeDocComment,unusedThrownExemptExceptionThrowable,unusedTypeArgs,uselessTypeCheck,varargsCast,warningToken,static-method,static-access
# TEMPORARILY DISABLED = super
# IGNORE = -nls,-unqualifiedField,-boxing,-serial,-hiding,-local
JAVAC = ecj $(CLASSPATH) -sourcepath src -warn:-serial -err:$(ERR) -source 1.8 -d bin -maxProblems 10
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
	$(JAVA) drawall.GLCBuilder $(ARGS) #| perl -pe 's!^\s+at (drawall.*?)\.[^.]+\(.*?:(\d+)\)$!$2p src.$1/java!&&y!./!/.! && s!^!`sed -n $_`!e && s!^\s+!!'

# "make bench": runs benchmarks
bench:
	$(JAVA) -Xprof drawall.GLCBuilder examples/dragon.ps examples/dragon.svg

gui: build
	$(JAVA) drawall.gui.GUI

.SECONDARY:
.PHONY: all cache format build clean run test bench gui
