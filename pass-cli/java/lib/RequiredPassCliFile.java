package com.dickimawbooks.passcli.lib;

import java.io.File;

import com.dickimawbooks.passlib.RequiredPassFile;

public class RequiredPassCliFile extends PassCliFile implements RequiredPassFile
{
   public RequiredPassCliFile(PassCliFile passfile)
   {
      this(passfile.getFile(), passfile.getLanguage());
   }

   public RequiredPassCliFile(File file, String language)
   {
      super(file, language);
   }

   public String getRequiredName()
   {
      return getFile().getName();
   }
}
