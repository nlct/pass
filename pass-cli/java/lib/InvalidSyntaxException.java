package com.dickimawbooks.passcli.lib;

public class InvalidSyntaxException extends UnknownIdentifierException
{
   public InvalidSyntaxException(String msg)
   {
      super(msg);
   }

   public InvalidSyntaxException(String msg, Throwable cause)
   {
      super(msg, cause);
   }
}
