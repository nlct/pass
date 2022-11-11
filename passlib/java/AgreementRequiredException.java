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
 * Exception thrown if user hasn't checked the agreement checkbox
 * where agreement is required.
 */ 
public class AgreementRequiredException extends Exception
{
   /**
    * Creates an exception using the localised message provided in
    * the dictionary.
    * @param passTools the PassTools object that has the dictionary
    * loaded
    */ 
   public AgreementRequiredException(PassTools passTools)
   {
      super(passTools.getMessageWithDefault("error.user_must_confirm",
         "User must agree to \"{0}\" statement.",
          passTools.getConfirmText()
         ));
   }

   /**
    * Creates an exception using the localised message provided in
    * the dictionary.
    * @param passTools the PassTools object that has the dictionary
    * loaded
    * @param cause
    */ 
   public AgreementRequiredException(PassTools passTools, Throwable cause)
   {
      super(passTools.getMessageWithDefault("error.user_must_confirm",
         "User must agree to \"{0}\" statement.",
          passTools.getConfirmText()
         ), cause);
   }

   /**
    * Creates an exception with the given message.
    * @param msg the error message
    */ 
   public AgreementRequiredException(String msg)
   {
      super(msg);
   }

   /**
    * Creates an exception with the given message and cause.
    * @param msg the error message
    * @param cause the cause of this exception
    */ 
   public AgreementRequiredException(String msg, Throwable cause)
   {
      super(msg, cause);
   }
}
