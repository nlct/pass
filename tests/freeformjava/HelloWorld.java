package unicodetest;

public class HelloWorld
{
   public static void codePointTest(String str)
   {
      System.out.println("String: "+ str);

      for (int i = 0; i < str.length(); )
      {
         int cp = str.codePointAt(i);
         i += Character.charCount(cp);

         System.out.format("c: %c (%X, %s)%n", cp, cp, Character.getName(cp));
      }
   }

   public static void main(String[] args)
   {
      codePointTest(String.format("abcd%c%c%c%c",
        0x1D56C, // bold fraktur A
        0x1D56D, // bold fraktur B
        0x1D56E, // bold fraktur C
        0x1D56F  // bold fraktur D
      ));

      codePointTest("\uD83E\uDDB8\u200D\u2640\uFE0F");
   }
}
