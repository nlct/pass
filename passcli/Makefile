all	: lib/pass-cli.jar

dist	: ../dist/pass-cli.zip

../dist/pass-cli.zip	: ../dist/pass-cli lib/pass-cli.jar ../dist/pass-cli doc/pass-cli.1 doc/README
	make -C doc
	cd ../dist ; zip pass-cli.zip pass-cli/bin/pass-cli pass-cli/lib/*.jar pass-cli/doc/*.{1,pdf} pass-cli/doc/README

../dist/pass-cli	: ../dist
	cd ../dist; ln -s ../passcli pass-cli

../dist	:
	mkdir -p ../dist

lib/pass-cli.jar	: lib/pass-cli-lib.jar java/Manifest.txt classes/com/dickimawbooks/passcli/PassCliMain.class
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/pass-cli.jar \
	com/dickimawbooks/passcli/PassCliMain.class

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
