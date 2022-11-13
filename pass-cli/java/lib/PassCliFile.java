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
package com.dickimawbooks.passcli.lib;

import java.io.File;

import com.dickimawbooks.passlib.PassFile;
import com.dickimawbooks.passlib.AssignmentData;

/**
 * File for inclusion in the PDF. Files may have been specified with
 * an identifier that could be the language or the file's MIME type.
 */

public class PassCliFile implements PassFile
{
   /**
    * Creates a new instance. The language and identifier are set to unknown.
    * @param filename the filename
    */
   public PassCliFile(String filename)
   {
      this(new File(filename), AssignmentData.UNKNOWN_LANGUAGE);
   }

   /**
    * Creates a new instance. The language and identifier are set to unknown.
    * @param file the file
    */
   public PassCliFile(File file)
   {
      this(file, AssignmentData.UNKNOWN_LANGUAGE);
   }

   /**
    * Creates a new instance.
    * @param file the file
    * @param language the language and identifier
    */
   public PassCliFile(File file, String language)
   {
      this(file, language, language);
   }

   /**
    * Creates a new instance. The language should be a language
    * label recognised by AssignmentData. The identifier may be the
    * language label or may be the file's MIME type.
    * @param file the file
    * @param language the language
    * @param identifier the identifier
    */
   public PassCliFile(File file, String language, String identifier)
   {
      this.file = file;
      this.language = language;
      this.identifier = identifier;
   }

   /**
    * Gets the file.
    * @return the file
    */ 
   @Override
   public File getFile()
   {
      return file;
   }

   /**
    * Gets the file's language label.
    * @return the language label
    */ 
   @Override
   public String getLanguage()
   {
      return language;
   }

   /**
    * Sets the file's language label.
    * @param language the language label
    */ 
   public void setLanguage(String language)
   {
      this.language = language;
   }

   /**
    * Gets the file's identifier.
    * @return the identifier
    */ 
   public String getIdentifier()
   {
      return identifier;
   }

   /**
    * Gets the filename.
    * @return the filename
    */ 
   public String getFilename()
   {
      return file.getAbsolutePath();
   }

   /**
    * Gets the file extension. In the case of an extensionless
    * Makefile, this method will return "mk";
    * @return the file extension
    */ 
   public String getExtension()
   {
      String filename = getFilename();

      int idx = filename.lastIndexOf(".");

      if (idx == -1)
      {
         if (filename.toLowerCase().endsWith("makefile"))
         {
            return "mk";
         }

         return "";
      }

      return filename.substring(idx+1);
   }

   @Override
   public String toString()
   {
      return String.format("%s[file=%s,language=%s,identifier=%s]",
        getClass().getSimpleName(), file, language, identifier);
   }

   private File file;
   private String language, identifier;
}
