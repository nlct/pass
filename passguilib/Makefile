lib/passguilib.jar	: classes/com/dickimawbooks/passguilib/PassGui.class 
	cd classes; \
	jar cvf ../lib/passguilib.jar \
	com/dickimawbooks/passguilib/*.class

classes/com/dickimawbooks/passguilib/PassGui.class	: classes/com/dickimawbooks/passguilib \
	java/AssignmentProcessWorker.java \
	java/ConfirmCheckBox.java \
	java/PassGui.java \
	java/PassGuiTools.java \
	java/ProgressPanel.java
	cd java; javac -d ../classes \
	-Xlint:unchecked -Xlint:deprecation \
	*.java

classes/com/dickimawbooks/passguilib	:
	mkdir -p classes/com/dickimawbooks/passguilib

clean	:
	\rm -rf classes lib/passguilib.jar
