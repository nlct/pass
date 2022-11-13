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
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.URI;

import javax.swing.JEditorPane;
import javax.swing.text.*;
import javax.swing.event.*;

import com.dickimawbooks.passlib.*;

public class FileEditor extends FilePane implements PassFile
{
   public FileEditor(PassEditor gui, File file, String filename, String language)
     throws IOException
   {
      this(gui, new ProjectFile(file, language), filename, ProjectFileType.OPTIONAL);
   }

   public FileEditor(PassEditor gui, File file, String filename, String language,
     URI original)
     throws IOException
   {
      this(gui, new ProjectFile(file, language), filename, ProjectFileType.OPTIONAL, original);
   }

   public FileEditor(PassEditor gui, File file, String filename, String language, boolean autoOpen)
     throws IOException
   {
      this(gui, new ProjectFile(file, language), filename, ProjectFileType.OPTIONAL, autoOpen);
   }

   public FileEditor(PassEditor gui, ProjectFile projectFile, String filename)
     throws IOException
   {
      this(gui, projectFile, filename, ProjectFileType.OPTIONAL, projectFile.isText());
   }

   public FileEditor(PassEditor gui, ProjectFile projectFile, String filename, boolean autoOpen)
     throws IOException
   {
      this(gui, projectFile, filename, ProjectFileType.OPTIONAL, autoOpen);
   }

   public FileEditor(PassEditor gui, File file, String filename, String language, ProjectFileType type)
     throws IOException
   {
      this(gui, new ProjectFile(file, language), filename, type);
   }

   public FileEditor(PassEditor gui, File file, String filename, String language, ProjectFileType type, URI original)
     throws IOException
   {
      this(gui, new ProjectFile(file, language), filename, type, original);
   }

   public FileEditor(PassEditor gui, ProjectFile projectFile, String filename, ProjectFileType type)
     throws IOException
   {
      this(gui, projectFile, filename, type, true);
   }

   public FileEditor(PassEditor gui, ProjectFile projectFile, String filename,
     ProjectFileType type, URI original)
     throws IOException
   {
      this(gui, projectFile, filename, type, true, original);
   }

   public FileEditor(PassEditor gui, ProjectFile projectFile, String filename,
     ProjectFileType type, boolean autoOpen)
     throws IOException
   {
      this(gui, projectFile, filename, type, autoOpen, null);
   }

   public FileEditor(PassEditor gui, ProjectFile projectFile, String filename, 
    ProjectFileType type, boolean autoOpen, URI original)
     throws IOException
   {
      super(gui, projectFile, filename, type, !projectFile.isText(), autoOpen, original, projectFile.getMimeType());

      assert type != ProjectFileType.RESOURCE;
   }

   public void save() throws IOException
   {
      if (!getNode().isModified()) return;

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(getFile(), getEncoding());

         Document doc = getDocument();
         ((DefaultEditorKit)getEditorKit()).write(writer, doc, 0, doc.getLength());
      }
      catch (BadLocationException e)
      {// shouldn't happen
         throw new AssertionError(e.getMessage(), e);
      }
      finally
      {
         if (writer != null)
         {
            writer.close();
         }
      }

      setModified(false);
   }

   @Override
   public void setModified(boolean modified)
   {
      if (getNode() != null)
      {
         getNode().setModified(modified);
      }
   }

   @Override
   public boolean isModified()
   {
      if (getNode() != null)
      {
         return getNode().isModified();
      }

      return false;
   }
}
