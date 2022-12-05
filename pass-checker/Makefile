../dist/pass-checker.zip	: ../dist lib/pass-checker.jar lib/dictionary/*.xml doc/pass-checker.1 README
	make -C doc
	cd .. ; zip dist/pass-checker.zip pass-checker/bin/pass-checker* pass-checker/lib/*.jar pass-checker/lib/dictionary/*.xml pass-checker/doc/*.{1,pdf} pass-checker/README

../dist	:
	mkdir -p ../dist

test	: lib/pass-checker.jar
	bin/pass-checker -j ../tests/pass-checker/uploads.tsv -o ../tests/pass-checker/results.tsv ../tests/pass-checker/*.pdf

lib/pass-checker.jar	: java/Manifest.txt classes/com/dickimawbooks/passchecker/PassChecker.class
	make -C ../pass-lib lib/passlib.jar
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/pass-checker.jar \
	com/dickimawbooks/passchecker/*.class

classes/com/dickimawbooks/passchecker/PassChecker.class	: classes/com/dickimawbooks/passchecker \
	java/AssignmentMetaData.java \
	java/MessageSystem.java \
	java/ServerJobData.java \
	java/FileFormatException.java \
	java/PassChecker.java
	cd java; \
	javac -cp .:../lib/passlib.jar:../lib/pdfbox.jar:../lib/commons-logging.jar \
	-d ../classes -Xlint:unchecked -Xlint:deprecation *.java

classes/com/dickimawbooks/passchecker	:
	mkdir -p classes/com/dickimawbooks/passchecker

doc/pass-checker.1	: doc/pass-checker.pod
	make -C doc

clean	:
	\rm -rf classes
	\rm -f lib/pass-checker.jar
	make -C doc clean
