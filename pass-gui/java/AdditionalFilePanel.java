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

import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.JComboBox;

/**
 * File panel selector for additional (non-required) files.
 * A remove button is put in the left component if the user wants to
 * deselect the file.
 */
public class AdditionalFilePanel extends FilePanel
  implements ActionListener
{
   /**
    * Creates a new instance.
    * @param main the GUI
    * @param chooser a file chooser
    */ 
   public AdditionalFilePanel(PrepareAssignmentUpload main,
     JFileChooser chooser)
   {
      this(null, "", main, chooser);
   }

   /**
    * Creates a new instance.
    * @param file the default file
    * @param main the GUI
    * @param chooser a file chooser
    */ 
   public AdditionalFilePanel(File file, 
     PrepareAssignmentUpload main,
     JFileChooser chooser)
   {
      this(file.getParentFile(), file.getName(), main, chooser);
   }

   public AdditionalFilePanel(File dir, String filename,
     PrepareAssignmentUpload main,
     JFileChooser chooser)
   {
      super(main, dir, filename, chooser, main.getImageURL("general/Open"));

      addLeftComponent(main.createButton("table/RowDelete", "remove", this));
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      if ("remove".equals(evt.getActionCommand()))
      {
         main.removeOptionalFilePanel(this);
      }
   }

}
