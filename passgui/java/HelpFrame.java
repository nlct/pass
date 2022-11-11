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
package com.dickimawbooks.passgui;

import java.net.URL;
import java.io.IOException;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.ImageIcon;

/**
 * The window used for the help page.
 */
public class HelpFrame extends JFrame
{
   /**
    * Creates a new frame.
    * @param gui the GUI
    */
   public HelpFrame(PrepareAssignmentUpload gui) throws IOException
   {
      super(gui.getPassTools().getMessage("manual.title"));

      ImageIcon ic = gui.getLogoIcon();

      if (ic != null)
      {
         setIconImage(ic.getImage());
      }

      getContentPane().add(new JScrollPane(loadHelpFile()), "Center");
   }

   /**
    * Loads the HTML file containing the help page and creates an
    * editor pane containing it.
    * @return the new editor pane
    */ 
   private JEditorPane loadHelpFile() throws IOException
   {
      URL url = getClass().getResource("/manual.html");

      JEditorPane editorPane = new JEditorPane(url);

      editorPane.setEditable(false);

      return editorPane;
   }

}
