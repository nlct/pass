package com.dickimawbooks.passcli.lib;

public class UnsupportedSettingException extends Exception
{
   public UnsupportedSettingException(String msg)
   {
      super(msg);
   }

   public UnsupportedSettingException(String msg, Throwable cause)
   {
      super(msg, cause);
   }
}
