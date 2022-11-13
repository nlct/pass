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

public class InvalidFormatException extends IOException
{
   public InvalidFormatException(File file, int lineNum, String content)
   {
      super(String.format("%s:%d invalid content: %s", file.getName(),
         lineNum, content));
   }

   public InvalidFormatException(File file, String content)
   {
      super(String.format("%s: invalid content: %s", file.getName(), content));
   }

   public InvalidFormatException(File file, String key, String value)
   {
      super(String.format("%s: invalid value '%s' for key: %s", 
        file.getName(), value, key));
   }

   public InvalidFormatException(File file, String content, Throwable cause)
   {
      super(String.format("%s: invalid content: %s", file.getName(), content),
        cause);
   }
}
