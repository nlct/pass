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

import java.io.IOException;

/**
 * Exception thrown if a problem occurs with an input file that
 * needs to be fetched from a remote location.
 */
public class InputResourceException extends IOException
{
   public InputResourceException(String msg)
   {
      super(msg);
   }

   public InputResourceException(String msg, Throwable cause)
   {
      super(msg, cause);
   }
}
