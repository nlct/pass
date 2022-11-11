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

/**
 * Exception thrown if a student tries to include a forbidden file.
 * Forbidden files include:
 * <ul>
 * <li>Files with the same name as a resource file. That is, a file
 * that the Pass application has to fetch from a remote location
 * that the student's application needs to read. The student's local
 * copy may have been altered or the lecturer may provide a
 * different file for Pass to use, than the file supplied for the
 * student, to test their application to discourage students from
 * producing a solution that's hard-coded for a specific input file.
 *
 * <li>Files with the same name as a result file. That is, a file
 * that the student's application must create. This is to ensure
 * that the file is actually created by the student's application
 * and not faked with another application.
 *
 * <li>Banned binary files. Any file with the name "a.out", or 
 * with one of the following extensions: "exe", "o", 
 * "class", "zip", "tar", "gz", "tgz", "jar", "a", "ar", "iso",
 * "bz2", "lz", "lz4", "xz", "7z", "s7z", or "cab".
 * The Pass application can compile the student's source code, if
 * applicable, and run the result. There's no reason for any of
 * these binary files to be included and their inclusion runs the
 * risk of including malware. This means that students won't be able
 * to include arbitrary Java libraries that aren't installed on the
 * Java class path or that can't be created from the supplied source
 * code.
 * </ul>
 */ 
public class InvalidFileException extends Exception
{
   public InvalidFileException(String msg)
   {
      super(msg);
   }

   public InvalidFileException(String msg, Throwable cause)
   {
      super(msg, cause);
   }
}
