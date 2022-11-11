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
package com.dickimawbooks.passgui;

import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;

import java.awt.Cursor;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import com.dickimawbooks.passlib.AssignmentData;
import com.dickimawbooks.passlib.AllowedBinaryFilter;

/**
 * SwingWorker to search for assignment files.
 * 
 */
public class FileSearcher extends SwingWorker<Void,Void>
implements FileVisitor<Path>
{
   /**
    * Creates a new instance.
    * @param main the GUI
    * @param dir the base directory
    */ 
   public FileSearcher(PrepareAssignmentUpload main, File dir)
   {
      this.main = main;
      this.dir = dir;
   }

   @Override
   public Void doInBackground()
     throws IOException,InterruptedException
   {
      maxFileCount = main.getFileSearchMax();

      fileCount = 0;
      foundCount = 0;
      success = true;

      data = main.getAssignment();
      defExt = data.getDefaultExtension();

      reports = data.getReports();
      binaryFilters = data.getAllowedBinaryFilters();

      orgCursor = main.getCursor();
      main.setCursor(new Cursor(Cursor.WAIT_CURSOR));

      Files.walkFileTree(dir.toPath(), this);

      return null;
   }

   @Override
   protected void done()
   { 
      try
      {
         get();

         if (success)
         {
            main.fileSearchMessage(
              main.getPassTools().getChoiceMessage(
               "message.file_search.found.choice", foundCount, 
               Integer.valueOf(foundCount)));
         }
      }
      catch (Exception e)
      {
         main.error(e);
      }

      main.setCursor(orgCursor);

      main.fileSearchCompleted();
   }

   @Override
   public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
   throws IOException
   {  
      fileCount++;

      if (fileCount >= maxFileCount)
      {
         success = false;

         main.fileSearchMessage(
           main.getMessage("error.file_search.maxed", maxFileCount));

         return FileVisitResult.TERMINATE;
      }

      if (attrs.isSymbolicLink() || Files.isHidden(path))
      {
         return FileVisitResult.CONTINUE;
      }

      File file = path.toFile();

      main.fileSearchMessage(file.toString());

      String filename = file.getName();

      if (data.hasFile(filename))
      {
         main.setRequiredFileComponent(file);

         foundCount++;

         return FileVisitResult.CONTINUE;
      }
                   
      int idx = filename.lastIndexOf(".");

      String ext = null;

      if (idx > -1)
      {
         ext = filename.substring(idx);
      }

      if (defExt != null && defExt.equals(ext))
      {
         main.addAdditionalFileComponent(file);

         foundCount++;

         return FileVisitResult.CONTINUE;
      }

      if (ext != null && ext.matches("pdf|docx?"))
      {
         for (String report : reports)
         {
            String name = report;
      
            if (report.lastIndexOf(".") == -1)
            {
               name = report+"."+ext;
            }

            if (name.equals(filename))
            {
               main.addAdditionalFileComponent(file);

               foundCount++;

               return FileVisitResult.CONTINUE;
            }
         }
      }

      for (FileFilter filter : binaryFilters)
      {
          if (filter.accept(file))
          {  
             main.addBinaryFilePanel(file);

             foundCount++;

             return FileVisitResult.CONTINUE;
          }
      }

      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attr)
   throws IOException
   {
      if (attr.isDirectory() && Files.isHidden(path))
      {
         return FileVisitResult.SKIP_SUBTREE;
      }

      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult visitFileFailed(Path path, IOException exc)
   throws IOException
   {
      if (exc != null)
      {
         main.fileSearchMessage(exc);
      }

      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult postVisitDirectory(Path path, IOException exc)
   throws IOException
   {
      if (exc != null)
      {
         main.fileSearchMessage(exc.getMessage());
      }

      return FileVisitResult.CONTINUE;
   }

   private AssignmentData data;
   private String defExt;
   private Vector<String> reports;
   private Vector<AllowedBinaryFilter> binaryFilters;

   private int fileCount = 0;
   private int maxFileCount;
   private int foundCount = 0;
   private boolean success;
   private Cursor orgCursor;
   private File dir;
   private PrepareAssignmentUpload main;
}
