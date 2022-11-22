lib/passlib.jar	: classes/com/dickimawbooks/passlib/Pass.class
	cd classes; \
	jar cvf ../lib/passlib.jar \
	com/dickimawbooks/passlib/*.class

classes/com/dickimawbooks/passlib/Pass.class	: classes/com/dickimawbooks/passlib \
	java/AgreementRequiredException.java \
	java/AllowedBinaryFile.java \
	java/AllowedBinaryFilter.java \
	java/AssignmentData.java \
	java/AssignmentDataParser.java \
	java/AssignmentProcessConfig.java \
	java/AssignmentProcess.java \
	java/Course.java \
	java/CourseParser.java \
	java/DuplicateFileException.java \
	java/InputResourceException.java \
	java/InvalidFileException.java \
	java/PassFile.java \
	java/Pass.java \
	java/PassTask.java \
	java/PassTools.java \
	java/ProgressListener.java \
	java/RequiredPassFile.java \
	java/ResourceFile.java \
	java/ResultFile.java \
	java/Student.java
	cd java; javac -d ../classes \
	-Xlint:unchecked -Xlint:deprecation \
	*.java

classes/com/dickimawbooks/passlib	:
	mkdir -p classes/com/dickimawbooks/passlib

clean	:
	\rm -rf classes lib/passlib.jar
