../dist/pass-cli-server.tgz	: lib/pass-cli-server.jar ../dist
			cd ..; tar zcvf dist/pass-cli-server.tgz --dereference \
			pass-cli-server/bin/pass-cli-server \
			pass-cli-server/lib/dictionary/*.xml \
			pass-cli-server/lib/*.jar \
			pass-cli-server/lib/*.xml 

../dist	:
	mkdir -p ../dist

tests	: test-hello test-subdirs test-hellogui

test-hello	: lib/pass-cli-server.jar
		bin/pass-cli-server -F examples/HelloWorldJava.txt

test-subdirs	: lib/pass-cli-server.jar
		bin/pass-cli-server -F examples/subdirs.txt

test-hellogui	: lib/pass-cli-server.jar
		bin/pass-cli-server -F examples/HelloWorldGUI.txt

lib/pass-cli-server.jar	: lib/pass-cli-lib.jar lib/resources.xml java/Manifest.txt classes/com/dickimawbooks/passcli/server/PassCliServer.class
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/pass-cli-server.jar \
	com/dickimawbooks/passcli/server/PassCliServer.class

lib/resources.xml	:
	cd lib; ln -s ../../pass-cli/lib/resources.xml

classes/com/dickimawbooks/passcli/server/PassCliServer.class	: classes/com/dickimawbooks/passcli/server java/PassCliServer.java
	make -C ../pass-cli lib/pass-cli-lib.jar
	cd java; \
	javac -cp ../lib/passlib.jar:../lib/pass-cli-lib.jar \
	-d ../classes -Xlint:unchecked -Xlint:deprecation *.java

classes/com/dickimawbooks/passcli/server	:
	mkdir -p classes/com/dickimawbooks/passcli/server

lib	:
	mkdir -p lib

clean	:
	\rm -f classes/com/dickimawbooks/passcli/*/*.class
	\rm -f lib/pass-cli-server.jar
