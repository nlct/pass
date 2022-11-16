/*
   Copyright 2022 Nicola L. C. Talbot

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.dickimawbooks.passlib;

import java.util.Vector;
import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.net.URL;

/**
 * Interface for a Pass application.
 */ 
public interface Pass
{
   /**
    * Gets the submission date. This is only used with ServerPass,
    * where the submission date is the upload date, otherwise this
    * method should return null. 
    * @return the submission date or null if not enabled
    */
   public Date getSubmittedDate();

   /**
    * Gets the timeout value in seconds.
    * Spawned sub-processes will timeout after this value.
    * @return the timeout value in seconds
    */ 
   public long getTimeOut();

   /**
    * Sets the timeout value.
    * @param value the timeout value in seconds
    */ 
   public void setTimeOut(long value);

   /**
    * Gets a list of all the specified files.
    * These are the files supplied by the student that make up their
    * coursework (required files and any additional files that
    * contain source code).
    * @return a vector of files
    */ 
   public Vector<PassFile> getFiles();

   /**
    * Gets the base path of the specified files.
    * This allows relative paths to be formed.
    * @return the base path
    */ 
   public Path getBasePath();

   /**
    * Gets the assignment data. This data is obtained from the
    * assignment XML file supplied by the lecturer.
    * @return the assignment specification
    */ 
   public AssignmentData getAssignment();

   /**
    * Gets the encoding of the student's files.
    * There are only three supported encodings: ASCII, UTF-8 and
    * Latin-1. LuaLaTeX will be used with UTF-8. (Although modern
    * LaTeX kernels provide improved support for UTF-8, an older
    * TeX distribution may be installed. It's less problematic to
    * use a native Unicode engine. XeLaTeX doesn't work with
    * attachfile.sty so LuaLaTeX is used.) PDFLaTeX is used for the
    * other encodings with inputenc.sty. Note that this requires the
    * student to correctly identify the file encoding used by their
    * IDE, but the AssignmentProcess class will check for signs of
    * encoding problems and warn that the files might not match the
    * specified encoding.
    * @return the file encoding name
    */ 
   public String getEncoding();

   /**
    * Gets the PassTools object. 
    * @return the PassTools object
    */ 
   public PassTools getPassTools();

   /**
    * Indicates whether or not this is a group project.
    * @return true if the assignment is a group project
    */ 
   public boolean isGroupProject();

   /**
    * Gets the list of students in the group if the assignment is a group project.
    * @return list of students in the group
    */ 
   public Vector<Student> getProjectTeam();

   /**
    * Gets the student if this is a solo project.
    * @return the student's details
    */ 
   public Student getStudent();

   /**
    * Gets the name of the PASS application that's using this
    * library.
    * @return the PASS application's name
   */ 
   public String getApplicationName();

   /**
    * Gets the version information of the PASS application that's using this
    * library.
    * @return the PASS application's version
   */ 
   public String getApplicationVersion();

   /**
    * Indicates whether or not the student has selected the
    * confirmed box. Students need to agree that it's their
    * responsibility to check the PDF before submitting it otherwise
    * PASS won't process the assignment.
    * @return true if the student has confirmed their agreement to
    * the terms
    */ 
   public boolean isConfirmed();

   /**
    * Writes a message to the transcript.
    * @param msg the message
    */ 
   public void transcriptMessage(String msg);

   /**
    * Writes an error message. This may be written to the transcript
    * or to STDERR or both, depending on the application.
    * @param msg the error message
    */ 
   public void error(String msg);

   /**
    * Writes an error message and possible the stack trace. 
    * This may be written to the transcript
    * or to STDERR or both, depending on the application.
    * @param throwable the exception/error that has occurred
    */ 
   public void error(Throwable throwable);

   /**
    * Indicates whether or not the PASS application is running in
    * debug mode.
    * @return true if debug mode is on
    */ 
   public boolean isDebugMode();

   /**
    * Writes a message if debugging is enabled followed by a
    * line break.
    * @param msg the message
    */ 
   public void debug(String msg);

   /**
    * Writes a message if debugging is enabled without a
    * line break.
    * @param msg the message
    */ 
   public void debugNoLn(String msg);

   /**
    * Writes a warning message.
    * @param msg the message
    */ 
   public void warning(String msg);

   /**
    * Writes a character to the transcript if verbose mode is on.
    * @param cp the character's codepoint
    */ 
   public void verboseCodePoint(int cp);

   /**
    * Writes a message to the transcript if verbose mode is on.
    * @param msg the message
    */ 
   public void verbose(String msg);

   /**
    * Version information for passlib.jar.
    */ 
   public static final String PASSLIB_VERSION = "1.3.1";
   public static final String PASSLIB_VERSION_DATE = "2022-11-16";

   /**
    * This library only supports three encodings: UTF-8, ASCII and
    * Latin-1. PDFLaTeX is used with ASCII and Latin-1. LuaLaTeX is
    * used with UTF-8. XeLaTeX isn't used as it's incompatible with
    * the attachfile package.
    */ 
   public static final String ENCODING_UTF8 = "UTF-8";
   public static final String ENCODING_ASCII = "US-ASCII";
   public static final String ENCODING_LATIN1 = "ISO-8859-1";

   public static final String SHORT_TITLE="PASS";

}
