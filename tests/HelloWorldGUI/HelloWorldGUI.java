import java.net.URL;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class HelloWorldGUI extends JFrame
{
   public HelloWorldGUI()
   {
      super("Hello World!");

      ImageIcon ic = getIcon("logo.png", "Logo");

      if (ic != null)
      {
         setIconImage(ic.getImage());
      }

      getContentPane().add(
        new JLabel("Hello World!",
                   getIcon("sample.png", "sample image"),
                   SwingConstants.LEFT),
        "Center");

      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension dim = tk.getScreenSize();
      setSize(dim.width*3/4, dim.height*3/4);

      setExtendedState(MAXIMIZED_BOTH);
   }

   public ImageIcon getIcon(String name, String description)
   {
      String imgLocation = "icons/"+name;
      URL imageURL = getClass().getResource(imgLocation);

      ImageIcon ic = null;

      if (imageURL != null)
      {
         ic = new ImageIcon(imageURL, description);
      }

      return ic;
   }

   public static void main(String[] args)
   {  
      try
      {
         SwingUtilities.invokeAndWait(new Runnable()
         {
            public void run()
            {
               (new HelloWorldGUI()).setVisible(true);
            }
         });
      }
      catch (InterruptedException | java.lang.reflect.InvocationTargetException e)
      {
         e.printStackTrace();
      }
   }
}
