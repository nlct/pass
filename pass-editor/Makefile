IZPACKDIR=/usr/local/IzPack

test	: lib/pass-editor.jar
	bin/pass-editor --debug

lib/manual/en/passeditor.html	: doc/passeditor-en.xml doc/createpasseditordocs
	make -C doc

lib/pass-editor.jar	: java/Manifest.txt lib/resources.xml \
		lib/manual/en/passeditor.html \
		classes/com/dickimawbooks/passeditor/PassEditor.class 
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/pass-editor.jar \
	  com/dickimawbooks/passeditor/icons/*.png \
	  com/dickimawbooks/passeditor/*.class

lib/resources.xml	:
	cd lib; ln -s ../../pass-lib/lib/resources.xml

classes/com/dickimawbooks/passeditor/PassEditor.class	: \
	classes/com/dickimawbooks/passeditor \
	classes/com/dickimawbooks/passeditor/icons \
	java/AssignmentSelector.java \
	java/BuildWorker.java \
	java/ButtonAction.java \
	java/DefaultSourceEditorKit.java \
	java/DirectoryTreeModel.java \
	java/EditorNode.java \
	java/FileDirectoryDialog.java \
	java/FileEditor.java \
	java/FilePane.java \
	java/FilePaneProperties.java \
	java/FileTabComponent.java \
	java/FindDialog.java \
	java/GoToLineDialog.java \
	java/HelpSetIconMap.java \
	java/HtmlPlainTransferHandler.java \
	java/InvalidFormatException.java \
	java/InvalidSettingException.java \
	java/ListingLanguage.java \
	java/Manifest.txt \
	java/MenuItemButton.java \
	java/NavigationTreeNode.java \
	java/NodeTransferHandler.java \
	java/PassEditor.java \
	java/PathNode.java \
	java/PathReference.java \
	java/ProjectDetailsDialog.java \
	java/ProjectFile.java \
	java/ProjectFileType.java \
	java/Project.java \
	java/PropertiesDialog.java \
	java/RequiredFileEditor.java \
	java/SearchCriteria.java \
	java/SearchPanel.java \
	java/SearchResult.java \
	java/SelectProjectDialog.java \
	java/SlidingToolBar.java \
	java/SlidingToolControl.java \
	java/UnsupportedFileException.java
	make -C ../pass-lib lib/passlib.jar
	make -C ../pass-gui-lib lib/passguilib.jar
	cd java; javac -d ../classes \
	  -Xlint:unchecked -Xlint:deprecation \
	  -cp ../lib/javahelp2.jar:../lib/jlfgr-1_0.jar:../lib/passlib.jar:../lib/passguilib.jar \
	  *.java

classes/com/dickimawbooks/passeditor	:
	mkdir -p classes/com/dickimawbooks/passeditor

classes/com/dickimawbooks/passeditor/icons	: classes/com/dickimawbooks/passeditor
	cd classes/com/dickimawbooks/passeditor; ln -f -s ../../../../java/icons

../dist/pass-editor-installer.jar	: ../dist lib/pass-editor.jar installer/*.xml README lib/manual/en/passeditor.html
	$(IZPACKDIR)/bin/compile installer/install.xml -b . \
	-h $(IZPACKDIR) \
	-o ../dist/pass-editor-installer.jar  -k standard

../dist	:
	mkdir -p ../dist

clean	:
	\rm -f classes
	\rm -f lib/pass-editor.jar
