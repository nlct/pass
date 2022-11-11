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
import java.net.URL;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JTextField;
import javax.swing.ImageIcon;

import com.dickimawbooks.passlib.PassTools;

/**
 * A button used to show a file chooser and update the corresponding
 * text field with the selected file name. There may be one or more filters,
 * which will be set when the file chooser opens. This allows the
 * same file chooser to be shared across all the file fields and the
 * filter will be set as appropriate when the file chooser needs to
 * be displayed. (This makes it easier to retain the last directory
 * the user selected a file from.)
 */
public class FileFieldButton extends JButton
  implements ActionListener
{
   /**
    * Creates a new instance.
    * @param passTools the PassTools to access localisation messages
    * @param field the text field that should show the file name
    * @param chooser the file chooser
    */
   public FileFieldButton(PassTools passTools, JTextField field, JFileChooser chooser)
   {
      this(passTools, field, chooser, null);
   }

   /**
    * Creates a new instance.
    * @param passTools the PassTools to access localisation messages
    * @param field the text field that should show the file name
    * @param chooser the file chooser
    * @param imageURL the image to use in the button (may be null)
    * @param filters the file filters (may be empty)
    */
   public FileFieldButton(PassTools passTools, JTextField field, 
     JFileChooser chooser, URL imageURL, FileFilter... filters)
   {
      super();
      addActionListener(this);
      setAlignmentX(0);

      if (chooser == null)
      {
         throw new NullPointerException();
      }

      fileChooser = chooser;
      textField = field;
      setFileFilters(filters);

      selectText = passTools.getMessage("filefield.select");

      if (imageURL == null)
      {
         setText(selectText);
         setMnemonic(passTools.getMnemonic("filefield.select", -1));
      }
      else
      {
         setIcon(new ImageIcon(imageURL, selectText));
         setToolTipText(passTools.getMessageWithDefault(
           "filefield.select.tooltip", selectText));
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String text = textField.getText();
      File file = null;

      if (!text.isEmpty())
      {
         file = new File(text);
      }

      if (filters != null)
      {
         boolean foundMatch = false;

         for (FileFilter filter : filters)
         {
            fileChooser.addChoosableFileFilter(filter);

            if (!foundMatch && file != null)
            {
               foundMatch = filter.accept(file);

               if (foundMatch)
               {
                  fileChooser.setFileFilter(filter);
               }
            }
         }
      }

      if (file != null)
      {
         fileChooser.setSelectedFile(file);
      }

      if (fileChooser.showDialog(getParent(), selectText)
       == JFileChooser.APPROVE_OPTION)
      {
         textField.setText(fileChooser.getSelectedFile().toString());
      }

      if (filters != null)
      {
         for (FileFilter filter : filters)
         {
            fileChooser.removeChoosableFileFilter(filter);
         }
      }
   }

   /**
    * Sets the file filter.
    * @param filter the filter (may be null)
    */ 
   public void setFileFilter(FileFilter filter)
   {
      if (filter == null)
      {
         this.filters = null;
      }
      else
      {
         this.filters = new FileFilter[] { filter };
      }
   }

   /**
    * Sets the file filters.
    * @param filters the filters
    */ 
   public void setFileFilters(FileFilter... filters)
   {
      if (filters.length == 0)
      {
         this.filters = null;
      }
      else
      {
         this.filters = filters;
      }
   }

   private JFileChooser fileChooser;
   private JTextField textField;
   private FileFilter[] filters;
   private String selectText="Select";
}
