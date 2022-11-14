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
 * An allowed binary file filter.
 */
public class AllowedBinaryFilter
 extends javax.swing.filechooser.FileFilter
 implements java.io.FileFilter
{
   /**
    * Creates a new instance.
    * @param mimetype the file's mime type
    * @param listing true if the file should be shown in the PDF
    * @param caseSensitive true if the case must match
    * @param description filter description
    * @param extensions allowed extensions
    * @throws NullPointerException if the mimetype or extensions is null
    * @throws IllegalArgumentException if extensions is empty
    */ 
   public AllowedBinaryFilter(String mimetype, boolean listing,
     boolean caseSensitive, String description, String... extensions)
   throws NullPointerException
   {
      if (mimetype == null || extensions == null)
      {
         throw new NullPointerException();
      }

      if (extensions.length == 0)
      {
         throw new IllegalArgumentException("One or more extensions required");
      }

      this.extensions = extensions;
      this.caseSensitive = caseSensitive;
      this.description = description;
      this.mimetype = mimetype;
      this.listing = listing;
   }

   /**
    * Gets the file's mime type.
    * @return the file's mime type
    */ 
   public String getMimeType()
   {
      return mimetype;
   }

   /**
    * Indicates whether or not to show this file in the PDF.
    * @return true if the file should be shown in the PDF
    */ 
   public boolean showListing()
   {
      return listing;
   }

   @Override
   public String getDescription()
   {
      return description;
   }

   /**
    * Sets the description.
    * @param description the new description
    */ 
   public void setDescription(String description)
   {
      this.description = description;
   }

   /**
    * Gets the list of allowed extensions.
    * @return list of allowed extensions
    */ 
   public String[] getExtensions()
   {
      return extensions;
   }

   /**
    * Indicates if this filter is case sensitive.
    * @return true if case sensitive
   */
   public boolean isCaseSensitive()
   {
      return caseSensitive;
   }

   @Override
   public boolean accept(File f)
   {
      String name = f.getName();

      int idx = name.lastIndexOf(".");

      if (idx == -1)
      {
         return false;
      }

      String ext = name.substring(idx+1);

      if (isCaseSensitive())
      {
         ext = ext.toLowerCase();
      }

      for (String filterExt : extensions)
      {
         if (isCaseSensitive())
         {
            filterExt = filterExt.toLowerCase();
         }

         if (ext.equals(filterExt))
         {
            return true;
         }
      }

      return false;
   }

   @Override
   public String toString()
   {
      return String.format(
       "%s[type=%s,listing=%s,case=%s,description=%s,extensions=%s]",
       getClass().getSimpleName(),
       mimetype, listing, caseSensitive, description, extensions);
   }

   private String mimetype;
   private boolean listing, caseSensitive;
   private String description;
   private String[] extensions;
}
