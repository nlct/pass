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
import java.io.IOException;

/**
 * An allowed binary file. For example, image files that the
 * student needs to create for their application.
 */
public class AllowedBinaryFile implements PassFile
{
   /**
    * Creates a new instance.
    * @param file the file
    * @param filter the allowed binary filter
    * @throws NullPointerException if the file or the filter is null
    */ 
   public AllowedBinaryFile(File file, AllowedBinaryFilter filter)
     throws NullPointerException
   {
      if (file == null || filter == null)
      {
         throw new NullPointerException();
      }

      this.file = file;
      this.filter = filter;
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

   @Override
   public String getLanguage()
   {
      return null;
   }

   /**
    * Gets the file's mime type.
    * @return the file's mime type
    */ 
   public String getMimeType()
   {
      return filter.getMimeType();
   }

   /**
    * Indicates whether or not to show this file in the PDF.
    * @return true if the file should be shown in the PDF
    */ 
   public boolean showListing()
   {
      return filter.showListing();
   }

   /**
    * Gets the underlying filter.
    */ 
   public AllowedBinaryFilter getFilter()
   {
      return filter;
   }

   private File file;
   private AllowedBinaryFilter filter;
}
