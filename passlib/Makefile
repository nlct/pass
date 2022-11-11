lib/passlib.jar	: classes/com/dickimawbooks/passlib/Pass.class lib/resources.xml
	cd classes; \
	jar cvf ../lib/passlib.jar \
	com/dickimawbooks/passlib/*.class

classes/com/dickimawbooks/passlib/Pass.class	: classes/com/dickimawbooks/passlib \
	AgreementRequiredException.java \
	AllowedBinaryFile.java \
	AllowedBinaryFilter.java \
	AssignmentData.java \
	AssignmentDataParser.java \
	AssignmentProcessConfig.java \
	AssignmentProcess.java \
	Course.java \
	CourseParser.java \
	DuplicateFileException.java \
	InputResourceException.java \
	InvalidFileException.java \
	PassFile.java \
	Pass.java \
	PassTask.java \
	PassTools.java \
	ProgressListener.java \
	RequiredPassFile.java \
	ResourceFile.java \
	ResultFile.java \
	Student.java
	cd java/passlib; javac -d ../classes \
	-Xlint:unchecked -Xlint:deprecation \
	*.java

clean	:
	\rm -rf classes lib/passlib.jar
