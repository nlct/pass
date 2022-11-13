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

import java.io.IOException;
import java.io.File;

public class UnsupportedFileException extends IOException
{
   public UnsupportedFileException(File file)
   {
      super(String.format("unsupported file: %s", file.getName()));

      this.file = file;
   }

   public UnsupportedFileException(File file, String mimeType)
   {
      super(String.format("unsupported file %s with MIME type %s", 
       file.getName(), mimeType));

      this.file = file;
      this.mimeType = mimeType;
   }

   public UnsupportedFileException(File file, Throwable cause)
   {
      super(String.format("unsupported file: %s", file.getName()),
        cause);

      this.file = file;
   }

   public UnsupportedFileException(File file, String mimeType, Throwable cause)
   {
      super(String.format("unsupported file %s with MIME type %s", 
       file.getName(), mimeType), cause);

      this.file = file;
      this.mimeType = mimeType;
   }

   public File getFile()
   {
      return file;
   }

   public String getMimeType()
   {
      return mimeType;
   }

   private File file;
   private String mimeType;
}
