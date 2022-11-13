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
package com.dickimawbooks.passeditor;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.dickimawbooks.passlib.*;

public class RequiredFileEditor extends FileEditor implements RequiredPassFile
{
   public RequiredFileEditor(PassEditor gui, File file, String filename, String language)
     throws IOException
   {
      super(gui, file, filename, language, ProjectFileType.REQUIRED);
   }

   public RequiredFileEditor(PassEditor gui, File file, String filename, String language, URI original)
     throws IOException
   {
      super(gui, file, filename, language, ProjectFileType.REQUIRED, original);
   }

   public RequiredFileEditor(PassEditor gui, File file, String filename, String language, boolean autoOpen)
     throws IOException
   {
      super(gui, new ProjectFile(file, language), filename, ProjectFileType.REQUIRED, autoOpen);
   }

   public RequiredFileEditor(PassEditor gui, ProjectFile projectFile, String filename)
     throws IOException
   {
      super(gui, projectFile, filename, ProjectFileType.REQUIRED);
   }

   public RequiredFileEditor(PassEditor gui, ProjectFile projectFile, String filename, boolean autoOpen)
     throws IOException
   {
      super(gui, projectFile, filename, ProjectFileType.REQUIRED, autoOpen);
   }

   @Override
   public String getRequiredName()
   {
      return getFile().getName();
   }
}
