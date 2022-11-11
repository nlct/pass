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
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;

import com.dickimawbooks.passlib.RequiredPassFile;

/**
 * File selector for required files. The selected filename needs to
 * match the required name. The required name is shown in a label in
 * the left component.
 */
public class RequiredFilePanel extends FilePanel implements RequiredPassFile
{
   /**
    * Creates a new instance.
    * @param main the main GUI
    * @param dir the default parent directory
    * @param filename the required filename
    * @param chooser file chooser
    * @param imageURL image for the file chooser button (may be
    * null)
    */ 
   public RequiredFilePanel(PrepareAssignmentUpload main, File dir, 
     String filename, JFileChooser chooser, URL imageURL)
   {
      super(main, dir, filename, chooser, imageURL);
      this.name = filename;

      labelField = new JLabel(String.format("%s: ", filename));
      labelField.setAlignmentX(0);
      addLeftComponent(labelField);

      setFileFilter(
         new FileFilter()
         {
            public boolean accept(File f)
            {
               return f.getName().equals(name) || f.isDirectory();
            }

            public String getDescription()
            {
               return name;
            }
         }
      );
   }

   /**
    * Gets the required filename.
    * @return the required filename
    */ 
   public String getRequiredName()
   {
      return name;
   }

   /**
    * Gets the preferred size of the label field.
    * @return the label field preferred size
    */ 
   public Dimension getLabelPreferredSize()
   {
      return labelField.getPreferredSize();
   }

   /**
    * Sets the preferred size of the label field.
    * @param dim the label field preferred size
    */ 
   public void setLabelPreferredSize(Dimension dim)
   {
      labelField.setPreferredSize(dim);
   }

   @Override
   public String getDefaultName()
   {
      return name;
   }

   private JLabel labelField;
   private String name;
}
