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

import java.net.URI;

/**
 * A class representing a resource file. This is a file supplied by
 * the lecturer that the student's application must input. It should
 * be located in a place that can be read but can't be edited by the
 * student. This prevents the student from tampering with the file.
 * The file is identified with the <code>resourcefile</code> element
 * in the assignment XML file.
 *
 * For example, the student's application may be required to read a
 * CSV file, but the CSV file may contain awkward cases, such as a
 * comma within a cell. The student is required to deal with this
 * case, but some can't and try editing the file to remove the
 * awkward bits.
 *
 * It may also be that the student is given a file to test out their
 * application but a different file is supplied for the submission.
 * This can be used to test if the student has hard-coded for the
 * specific test file instead of providing a general purpose
 * solution.
 *
 * Whilst it is technically possible for the student to access the
 * temporary directory that PASS uses to store local copies of all
 * the files, PASS creates a new directory every instance it runs
 * and deletes the directory on exit. Since it's not possible to
 * re-process the files using the same instance of PASS, the student
 * would need to be extremely quick to identify the temporary
 * directory and alter the file after PASS fetches it and before PASS
 * runs the student's application.
 */

public class ResourceFile
{
   /**
    * Creates a new object representing a plain text resource file.
    * @param uri the location of the resource file
    * @throws NullPointerException if uri is null
    */ 
   public ResourceFile(URI uri) throws NullPointerException
   {
      this(uri, null);
   }

   /**
    * Creates a new object representing a resource file with the
    * given mime type.
    * @param uri the location of the resource file
    * @param mimetype the file's mime type which may be empty or
    * null to indicate "text/plain"
    * @throws NullPointerException if uri is null
    */ 
   public ResourceFile(URI uri, String mimetype)
     throws NullPointerException
   {
      if (uri == null)
      {
         throw new NullPointerException();
      }

      this.uri = uri;

      if (mimetype == null || mimetype.isEmpty())
      {
         this.mimetype = "text/plain";
      }
      else
      {
         this.mimetype = mimetype;
      }

      basename = null;

      String path = uri.getPath();

      if (path != null)
      {
         String[] split = path.split("/");

         basename = split[split.length-1];
      }
   }

   /**
    * Gets the file's basename. This is the file name without the
    * path.
    * @return the resource file's name
    */ 
   public String getBaseName()
   {
      return basename;
   }

   /**
    * Gets the location of the resource file.
    * @return the URI of the resource file
    */ 
   public URI getUri()
   {
      return uri;
   }

   /**
    * Gets the resource files mime type.
    * @return the mime type
    */ 
   public String getMimeType()
   {
      return mimetype;
   }

   private URI uri;
   private String mimetype, basename;
}
