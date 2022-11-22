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
   public FileSearcher(PassGui main, File dir)
   {
      this.main = main;
      this.dir = dir;
   }

   @Override
   public Void doInBackground()
     throws IOException,InterruptedException
   {
      orgCursor = main.getCursor();
      main.setCursor(new Cursor(Cursor.WAIT_CURSOR));

      maxFileCount = main.getFileSearchMax();

      fileCount = 0;
      foundCount = 0;
      success = true;

      data = main.getAssignment();
      mainLanguage = data.getMainLanguage();

      // Make a copy of reports so that found reports can be removed
      // locally.

      Vector<String> orgReports = data.getReports();

      if (orgReports.isEmpty())
      {
         reports = orgReports;
      }
      else
      {
         reports = new Vector<String>(orgReports.size());

         for (String report : orgReports)
         {
            reports.add(report);
         }
      }

      binaryFilters = data.getAllowedBinaryFilters();

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
         main.fileSearchMessage(e);
      }

      main.setCursor(orgCursor);

      try
      {
         main.fileSearchCompleted();
      }
      catch (Exception e)
      {
         main.fileSearchMessage(e);
      }
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

      if (main.getPassTools().isRequiredFile(path, data, main.getBasePath()))
      {
         main.setRequiredFileComponent(file);

         foundCount++;

         return FileVisitResult.CONTINUE;
      }
                   
      String filename = file.getName();

      int idx = filename.lastIndexOf(".");

      String ext = null;
      String basename = filename;

      if (idx > -1)
      {
         ext = filename.substring(idx+1);
         basename = filename.substring(0, idx);
      }

      if (ext != null && mainLanguage != null
           && mainLanguage.equals(data.getListingLanguage(ext, null))
         )
      {
         main.addAdditionalFileComponent(file);

         foundCount++;

         return FileVisitResult.CONTINUE;
      }

      if (ext != null && ext.matches("pdf|docx?"))
      {
         File parentFile = file.getParentFile();

         for (int i = 0; i < reports.size(); i++)
         {
            String report = reports.get(i);
            String name = report;
      
            if (report.lastIndexOf(".") == -1)
            {
               // Give precedence to PDF files as they can be
               // included.

               if (ext.startsWith("doc") && basename.equals(report))
               {
                  File pdfFile = new File(parentFile, report+".pdf");

                  if (pdfFile.exists())
                  {
                     main.addAdditionalFileComponent(pdfFile);

                     foundCount++;

                     reports.remove(i);

                     return FileVisitResult.CONTINUE;
                  }
               }

               name = report+"."+ext;
            }

            if (name.equals(filename))
            {
               main.addAdditionalFileComponent(file);

               foundCount++;

               reports.remove(i);

               return FileVisitResult.CONTINUE;
            }
         }
      }

      for (FileFilter filter : binaryFilters)
      {
          if (filter.accept(file))
          {  
             main.addBinaryFileComponent(file);

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
   private String mainLanguage;
   private Vector<String> reports;
   private Vector<AllowedBinaryFilter> binaryFilters;

   private int fileCount = 0;
   private int maxFileCount;
   protected int foundCount = 0;
   private boolean success;
   private Cursor orgCursor;
   private File dir;
   private PassGui main;
}
