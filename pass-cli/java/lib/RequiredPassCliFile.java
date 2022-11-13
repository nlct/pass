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

import com.dickimawbooks.passlib.RequiredPassFile;
import com.dickimawbooks.passlib.PassFile;

/**
 * A source code file that's required.
 */
public class RequiredPassCliFile extends PassCliFile implements RequiredPassFile
{
   /**
    * Creates a new instance where a PassFile is being replaced by a
    * a RequiredPassCliFile.
    * @param passfile the file
    */
   public RequiredPassCliFile(PassFile passfile)
   {
      this(passfile.getFile(), passfile.getLanguage());
   }

   /**
    * Creates a new instance.
    * @param file the file
    * @param language the language label
    */
   public RequiredPassCliFile(File file, String language)
   {
      super(file, language);
   }

   /**
    * Creates a new instance.
    * @param file the file
    * @param language the language label
    * @param identifier the file's identifer
    */
   public RequiredPassCliFile(File file, String language, String identifier)
   {
      super(file, language, identifier);
   }

   @Override
   public String getRequiredName()
   {
      return getFile().getName();
   }
}
