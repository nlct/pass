package testreadfile;

import java.io.*;

public class TestReadFile
{
   public static void main(String[] args)
   {
      try
      {
         File file = new File("dummy.txt");

         BufferedReader in = new BufferedReader(new FileReader(file));

         String line;
         int lineNum=0;

         while ((line = in.readLine()) != null)
         {
            lineNum++;
            System.out.println(String.format("%d. %s", lineNum, line));
         }

         in.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
