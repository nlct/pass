IZPACKDIR=/usr/local/IzPack

test	: lib/progassignsys.jar
	bin/progassignsys --debug

lib/progassignsys.jar	: java/Manifest.txt \
		classes/com/dickimawbooks/passgui/PrepareAssignmentUpload.class 
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/progassignsys.jar \
	  com/dickimawbooks/passgui/*.class

classes/com/dickimawbooks/passgui/PrepareAssignmentUpload.class	: classes/com/dickimawbooks/passgui \
	java/AdditionalFilePanel.java \
	java/ApplicationProperties.java \
	java/BinaryFilePanel.java \
	java/FilePanel.java \
	java/HelpFrame.java \
	java/Manifest.txt \
	java/PrepareAssignmentUpload.java \
	java/RequiredFilePanel.java \
	java/ResourceFilePanel.java \
	java/StartupDirType.java \
	java/TranscriptFrame.java \
	../pass-lib/java/*.java \
	../pass-gui-lib/java/*.java
	make -C ../pass-gui-lib lib/passguilib.jar
	cd java; javac -d ../classes \
	  -Xlint:unchecked -Xlint:deprecation \
	  -cp ../lib/jlfgr-1_0.jar:../lib/passlib.jar:../lib/passguilib.jar \
	  *.java

classes/com/dickimawbooks/passgui	:
	mkdir -p classes/com/dickimawbooks/passgui


../dist/pass-installer.jar	: ../dist lib/resources.xml lib/progassignsys.jar installer/*.xml README POST-INSTALL.md lib/manual.html
	$(IZPACKDIR)/bin/compile installer/install.xml -b . \
	-h $(IZPACKDIR) \
	-o ../dist/pass-installer.jar  -k standard

../dist	:
	mkdir -p ../dist

clean	:
	\rm -f classes
	\rm -f lib/progassignsys.jar
