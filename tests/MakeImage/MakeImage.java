package makeimage;

import java.io.*;
import java.awt.image.*;
import java.awt.*;
import javax.imageio.ImageIO;

public class MakeImage
{
   private static void test() throws IOException
   {
      BufferedImage image = new BufferedImage(100, 100,
       BufferedImage.TYPE_INT_ARGB);

      Graphics2D g2 = image.createGraphics();

      if (g2 != null)
      {
         g2.setComposite(AlphaComposite.Src);

         g2.setColor(Color.YELLOW);
         g2.fillRect(0, 0, 100, 100);

         g2.setColor(Color.BLUE);

         g2.drawRect(10, 10, 80, 60);

         g2.dispose();
      }

      ImageIO.write(image, "png", new File("image.png"));

      File file = new File("output.txt");

      PrintWriter writer = new PrintWriter(file);

      writer.println("Test output.");

      writer.close();    
   }


   public static void main(String[] args)
   {
      try
      {
         test();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }
}
