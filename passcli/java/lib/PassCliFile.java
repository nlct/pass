package com.dickimawbooks.passcli.lib;

import java.io.File;

import com.dickimawbooks.passlib.PassFile;
import com.dickimawbooks.passlib.AssignmentData;

public class PassCliFile implements PassFile
{
   public PassCliFile(String filename)
   {
      this(new File(filename), AssignmentData.UNKNOWN_LANGUAGE);
   }

   public PassCliFile(File file)
   {
      this(file, AssignmentData.UNKNOWN_LANGUAGE);
   }

   public PassCliFile(File file, String language)
   {
      this.file = file;
      this.language = language;
   }

   public File getFile()
   {
      return file;
   }

   public String getLanguage()
   {
      return language;
   }

   public void setLanguage(String language)
   {
      this.language = language;
   }

   public String getFilename()
   {
      return file.getAbsolutePath();
   }

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

   public String toString()
   {
      return String.format("%s[file=%s,language=%s]",
        getClass().getSimpleName(), file, language);
   }

   private File file;
   private String language;
}
