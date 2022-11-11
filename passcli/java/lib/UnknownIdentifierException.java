package com.dickimawbooks.passcli.lib;

public class UnknownIdentifierException extends Exception
{
   public UnknownIdentifierException(String msg)
   {
      super(msg);
   }

   public UnknownIdentifierException(String msg, Throwable cause)
   {
      super(msg, cause);
   }
}
