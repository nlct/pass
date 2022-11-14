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
import javax.swing.filechooser.FileFilter;
import javax.swing.JLabel;

import com.dickimawbooks.passlib.AllowedBinaryFilter;

/**
 * File panel selector for allowed binary files.
 * A remove button is put in the left component if the user wants to
 * deselect the file.
 */
public class BinaryFilePanel extends FilePanel
  implements ActionListener
{
   /**
    * Creates a new instance.
    * @param main the GUI
    * @param chooser a file chooser
    */ 
   public BinaryFilePanel(PrepareAssignmentUpload main,
     JFileChooser chooser, FileFilter... filters)
   {
      this(main, null, chooser, filters);
   }

   /**
    * Creates a new instance.
    * @param main the GUI
    * @param file the selected file
    * @param chooser a file chooser
    */ 
   public BinaryFilePanel(PrepareAssignmentUpload main, File file,
     JFileChooser chooser, FileFilter... filters)
   {
      super(main, null, "", false, 
        chooser, main.getImageURL("general/Open"), filters);

      addLeftComponent(main.createJButton("table/RowDelete", "remove", this));

      typeLabel = new JLabel();

      addRightComponent(typeLabel);

      if (file != null)
      {
         AllowedBinaryFilter binaryFilter = null;

         for (FileFilter filter : filters)
         {
            if (filter instanceof AllowedBinaryFilter
                && filter.accept(file))
            {
               binaryFilter = (AllowedBinaryFilter)filter;
               break;
            }
         }

         setFile(file, binaryFilter);
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      if ("remove".equals(evt.getActionCommand()))
      {
         main.removeBinaryFilePanel(this);
      }
   }

   @Override
   public String getLanguage()
   {
      return null;
   }

   @Override
   public void setFile(File file, FileFilter filter)
   {
      setFilename(file.getAbsolutePath());

      if (filter instanceof AllowedBinaryFilter)
      {
         AllowedBinaryFilter binaryFilter = (AllowedBinaryFilter)filter;

         typeLabel.setText(binaryFilter.getMimeType());
      }
      else
      {
         typeLabel.setText("");
      }
   }

   private JLabel typeLabel;
}
