package helloworld;

public class HelloWorld
{
   public static void printHex(String str)
   {
      for (int i = 0; i < str.length(); )
      {
         int cp = str.codePointAt(i);
         i += Character.charCount(cp);
         System.out.format("0x%04X ", cp);
      }
   }

   public static void main(String[] args)
   {
      System.out.println("Hello World!");
      System.out.println("Java version: "+System.getProperty("java.version"));
      System.out.println("Java vendor: "+System.getProperty("java.vendor")
       + " "+System.getProperty("java.vendor.url"));
      System.out.println("OS arch: "+System.getProperty("os.arch"));
      System.out.println("OS name: "+System.getProperty("os.name"));
      System.out.println("OS version: "+System.getProperty("os.version"));
      System.out.println("Line ending: "+System.getProperty("line.separator"));
      printHex(System.getProperty("line.separator"));
      System.out.println();
   }
}
