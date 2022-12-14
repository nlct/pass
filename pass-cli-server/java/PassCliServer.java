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
package com.dickimawbooks.passcli.server;

import java.io.IOException;

import java.util.Date; 
import java.text.SimpleDateFormat; 

import com.dickimawbooks.passlib.*;
import com.dickimawbooks.passcli.lib.*;

public class PassCliServer extends PassCli
{
   public PassCliServer() throws IOException
   {
      super();
   }

   @Override
   public boolean isSubmittedDateEnabled()
   {
      return true;
   }

   @Override
   protected String formatMsg(int messageType, String message)
   {
      if (messageType == MESSAGE_TYPE_WARNING)
      {
         message = getMessageWithDefault("message.warning",
         "WARNING: {0}", message);
      }
      else if (messageType == MESSAGE_TYPE_ERROR)
      {
         message = getMessageWithDefault("message.error",
         "ERROR: {0}", message);
      }

      return String.format("%s %s: %s",
         MESSAGE_DATE_FORMAT.format(new Date()),
         getTaskIdentifier(), message);
   }

   @Override
   public String getApplicationName()
   {
      return APP_NAME+"-server";
   }

   public static void main(String[] args)
   {
      try
      {
         PassCli passcli = new PassCliServer();
         passcli.run(args);
      }
      catch (IOException e)
      {
         System.err.println(e.getMessage());
         e.printStackTrace();
         System.exit(EXIT_IO);
      }
   }

   public static final SimpleDateFormat MESSAGE_DATE_FORMAT
     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

}
