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

/**
 * Class representing a result file. This is a file listed in the
 * assignment XML file with the <code>resultfile</code> element and
 * should be created by the student's application. After running the
 * student's application, the Pass application will search for this
 * file and include it in the PDF as an attachment or will include a
 * line that states the file wasn't found.
 *
 * In addition to adding the file as an attachment, the file can
 * also be displayed as verbatim text or included as an image in the
 * document if the listing boolean variable is true. Note that the
 * mime type must start with <code>text/</code> or <code>image/</code>
 * and, if an image, must be a format supported by graphics.sty.
 */
public class ResultFile
{
   /**
    * Creates a new instance.
    * @param name the file name
    * @param mimetype the file's mime type
    * @param listing true if the file should be shown in the PDF
    * @throws NullPointerException if the name or mimetype is null
    */ 
   public ResultFile(String name, String mimetype, boolean listing)
     throws NullPointerException
   {
      if (name == null || mimetype == null)
      {
         throw new NullPointerException();
      }

      this.name = name;
      this.mimetype = mimetype;
      this.listing = listing;
   }

   /**
    * Gets the file name.
    * @return the file name
    */ 
   public String getName()
   {
      return name;
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
    * The file will be attached (if it exists) regardless of this
    * value.
    * @return true if the file should be shown in the PDF
    */ 
   public boolean showListing()
   {
      return listing;
   }

   private String name;
   private String mimetype;
   private boolean listing;
}
