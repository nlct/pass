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
package com.dickimawbooks.passguilib;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

/**
 * A component that provides a text field for file selection.
 */
public interface FileTextField
{
   /**
    * Gets the value in the filename text field.
    * @return the specified filename
    */ 
   public String getFilename();

   /**
    * Gets the file as indicated in the filename text field.
    * @return the specified file if the field isn't empty otherwise
    * null
    */ 
   public File getFile();

   /**
    * Sets the selected file and identifies the selected file filter
    * if it was selected using the file chooser.
    * @param file the file
    * @param filter the file filter (may be null)
    */ 
   public void setFile(File file, FileFilter filter);

   /**
    * Gets the underlying text component.
    */ 
   public JTextComponent getTextComponent();
}
