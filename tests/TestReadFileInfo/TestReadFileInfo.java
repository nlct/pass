import java.io.File;

public class TestReadFileInfo
{
   public static void fileInfo(String filename)
   {
      File file = new File(filename);

      System.out.println("File: "+filename);
      System.out.println("Absolute Path: "+file.getAbsolutePath());

      if (file.exists())
      {
         System.out.println("File exists.");
         System.out.format("File size: %dbytes.%n", file.length());
      }
      else
      {
         System.out.println("File doesn't exist.");
      }
   }

   public static void main(String[] args)
   {
      fileInfo("dummy.txt");
      fileInfo("dummy.png");
   }
}
