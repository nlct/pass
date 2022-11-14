../dist/pass-cli.zip	: ../dist lib/pass-cli.jar doc/pass-cli.1 doc/README
	make -C doc
	cd .. ; zip dist/pass-cli.zip pass-cli/bin/pass-cli pass-cli/lib/*.jar pass-cli/lib/resources.xml pass-cli/lib/dictionary/*.xml pass-cli/doc/*.{1,pdf} pass-cli/doc/README

../dist	:
	mkdir -p ../dist

tests	: test-hello test-subdirs test-hellogui test-readfileinfo

test-hello	: lib/pass-cli.jar
		bin/pass-cli --debug -F examples/HelloWorldJava.txt

test-subdirs	: lib/pass-cli.jar
		bin/pass-cli --debug -F examples/subdirs.txt

test-hellogui	: lib/pass-cli.jar
		bin/pass-cli --debug -F examples/HelloWorldGUI.txt

test-readfileinfo	: lib/pass-cli.jar
		bin/pass-cli --debug -F examples/TestReadFileInfo.txt

lib/pass-cli.jar	: lib/pass-cli-lib.jar lib/resources.xml java/Manifest.txt classes/com/dickimawbooks/passcli/PassCliMain.class
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/pass-cli.jar \
	com/dickimawbooks/passcli/*.class

lib/resources.xml	:
	cd lib; ln -s ../../pass-lib/lib/resources.xml

classes/com/dickimawbooks/passcli/PassCliMain.class	: classes/com/dickimawbooks/passcli java/PassCliMain.java
	cd java; \
	javac -cp ../lib/passlib.jar:../lib/pass-cli-lib.jar \
	-d ../classes -Xlint:unchecked -Xlint:deprecation *.java

lib/pass-cli-lib.jar	: lib classes/com/dickimawbooks/passcli/lib/PassCli.class
	cd classes; \
	jar cvf ../lib/pass-cli-lib.jar \
	com/dickimawbooks/passcli/lib/*.class

classes/com/dickimawbooks/passcli/lib/PassCli.class	: classes/com/dickimawbooks/passcli/lib \
	java/lib/InvalidSyntaxException.java \
	java/lib/PassCliFile.java \
	java/lib/PassCli.java \
	java/lib/RequiredPassCliFile.java \
	java/lib/UnknownIdentifierException.java \
	java/lib/UnsupportedSettingException.java
	make -C ../pass-lib lib/passlib.jar
	cd java/lib; \
	javac -cp ../../lib/passlib.jar -d ../../classes \
	-Xlint:unchecked -Xlint:deprecation *.java

classes/com/dickimawbooks/passcli/lib	:
	mkdir -p classes/com/dickimawbooks/passcli/lib

classes/com/dickimawbooks/passcli	:
	mkdir -p classes/com/dickimawbooks/passcli

doc/pass-cli.1	: doc/pass-cli.pod
		make -C doc

lib	:
	mkdir -p lib

clean	:
	\rm -f classes
	\rm -f lib/pass-cli.jar
	\rm -f lib/pass-cli-lib.jar
	make -C doc clean
