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
package com.dickimawbooks.passcli;

import java.io.IOException;

import com.dickimawbooks.passlib.*;
import com.dickimawbooks.passcli.lib.*;

public class PassCliMain extends PassCli
{
   public PassCliMain() throws IOException
   {
      super();
   }

   public static void main(String[] args)
   {
      try
      {
         PassCli passcli = new PassCliMain();
         passcli.run(args);
      }
      catch (IOException e)
      {
         System.err.println(e.getMessage());
         e.printStackTrace();
         System.exit(EXIT_IO);
      }
   }
}
