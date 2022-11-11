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

import java.io.File;

/**
 * Interface for a file provided to the Pass application.
 * The file should either be a text file (containing the source code)
 * or a report (PDF or Word).
 */ 

public interface PassFile
{
   /**
    * Gets the programming language (file type) identifier for this file.
    * For example, "Java" or "C++". May return "Plain Text" for
    * non-code text files, "PDF" for PDF files (.pdf) or "DOC" for a Word
    * document (.doc or .docx) or null for other allowed binary files.
    * See AssignmentData.LISTING_LANGUAGES
    * for known language labels.
    * @return the file's type/language identifier
    */ 
   public String getLanguage();

   /**
    * Gets this file as a File object.
    * @return this file location
    */ 
   public File getFile();
}
