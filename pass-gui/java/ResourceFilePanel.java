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
import java.net.URI;
import java.net.MalformedURLException;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;

/**
 * Non-editable component that shows the name of a resource file.
 * This is for informational purposes to confirm the file name.
 * The non-editable state should highlight that the user can't
 * upload their own version of the file.
 */
public class ResourceFilePanel extends JPanel
{
   /**
    * Creates a new instance.
    * @param filename the URL of the resource file
    * @throws MalformedURLException if the URL is invalid
    */ 
   public ResourceFilePanel(String filename) throws MalformedURLException
   {
      super();

      url = new URL(filename);

      labelField = new JLabel(url.toString());
      labelField.setAlignmentX(0);
      add(labelField);
   }

   /**
    * Creates a new instance.
    * @param uri the URI of the resource file
    * @throws MalformedURLException if the URI to URL conversion
    * fails
    */ 
   public ResourceFilePanel(URI uri) throws MalformedURLException
   {
      super();

      url = uri.toURL();

      labelField = new JLabel(url.toString());
      labelField.setAlignmentX(0);
      add(labelField);
   }

   /**
    * Gets the file's URL.
    * @return the file's URL
    */ 
   public URL getURL()
   {
      return url;
   }

   /**
    * Sets the file path.
    * @param filename the URL of the resource file
    * @throws MalformedURLException if the URL is invalid
    */ 
   public void setFile(String name) throws MalformedURLException
   {
      url = new URL(name);
      labelField.setText(url.toString());
   }

   /**
    * Sets the file path.
    * @param uri the URI of the resource file
    * @throws MalformedURLException if the URI to URL conversion
    * fails
    */ 
   public void setFile(URI uri) throws MalformedURLException
   {
      url = uri.toURL();
      labelField.setText(url.toString());
   }

   private JLabel labelField;
   private URL url;
}
