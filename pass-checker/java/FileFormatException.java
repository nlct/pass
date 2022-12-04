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
package com.dickimawbooks.passchecker;

import java.io.File;
import java.io.IOException;

/**
 * Thrown when TSV parsing fails.
 */
public class FileFormatException extends IOException
{
   public FileFormatException(String message, File file, int lineNum)
   {
      super(String.format("%s:%d: %s", file.getName(), lineNum, message));
   }

   public FileFormatException(String message, File file, int lineNum,
    Throwable cause)
   {
      super(String.format("%s:%d: %s", file.getName(), lineNum, message), cause);
   }
}
