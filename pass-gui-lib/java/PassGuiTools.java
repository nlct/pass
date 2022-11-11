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
package com.dickimawbooks.passguilib;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.awt.Desktop;

/**
 * Set of methods useful for a GUI.
 */
public class PassGuiTools
{
   public PassGuiTools(PassGui gui)
   {
      this.gui = gui;
   }

   /**
    * Opens a PDF file.
    */ 
   public void openPdf(File pdfFile) throws IOException
   {
      open(pdfFile, "pdfviewer", "PDFVIEWER");
   }

   /**
    * Opens a text file. 
    */ 
   public void openEditor(File textFile) throws IOException
   {
      open(textFile, "editor", "EDITOR");
   }

   /**
    * Opens a file. The Desktop.open(File) method isn't
    * reliable, so provide alternatives. If the given application
    * name is provided in resources.xml that takes precedence. The
    * environment variable is used as a fallback if Desktop isn't
    * supported.
    * @param file the file to open
    * @param name the application name used in resources.xml (may be
    * null or empty)
    * @param envname an environment variable that provides the path
    * to a viewer/editor (may be null or empty)
    */ 
   public void open(File file, String name, String envname) throws IOException
   {
      if (!file.exists())
      {
         throw new FileNotFoundException(gui.getMessage(
          "error.file_doesnt_exist", file.toString()));
      }

      // check if resources.xml has set a path for given name

      File viewer = null;

      if (name != null && !name.isEmpty())
      {
         try
         {
            viewer = gui.getPassTools().findResourceApplication(name);

            gui.debug("Found "+name+" setting: "+viewer);
         }
         catch (FileNotFoundException e)
         {
            gui.debug(e.getMessage());
         }
      }

      if (viewer != null)
      {
         ProcessBuilder pb = new ProcessBuilder(viewer.toString(), 
           file.toString());
         pb.inheritIO();
         Process p = pb.start();
      }      
      else if (Desktop.isDesktopSupported())
      {
         gui.debug("Desktop supported");
         Desktop.getDesktop().open(file);
      }
      else 
      {
         String path = null;

         if (envname != null && !envname.isEmpty())
         {
            path = System.getenv(envname);
         }

         if (path != null && !path.isEmpty())
         {
            ProcessBuilder pb = new ProcessBuilder(path, file.toString());
            pb.inheritIO();
            Process p = pb.start();
         }
         else
         {
            throw new IOException(gui.getMessage("error.cant_view_file", 
             file.toString()));
         }
      }

   }

   protected PassGui gui;
}
