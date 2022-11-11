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
import java.io.IOException;
import java.beans.PropertyChangeListener;

import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.dickimawbooks.passlib.Pass;

/**
 * Interface for a Pass GUI application.
 */
public interface PassGui extends Pass,PropertyChangeListener
{
   /**
    * Sets the indeterminate state of the progress bar.
    * @param state true if the progress bar's status should be indeterminate
    */ 
   public void setIndeterminateProgress(boolean state);

   /**
    * Indicates that a worker thread has finished. If the temporary
    * file exists, the application needs to provide the user a way
    * of saving it to their preferred location. (Temporary files are
    * deleted on exit.)
    * @param success true if the process was successful
    * @param tmpFile the temporary file created by the process
    */ 
   public void finished(boolean success, File tmpFile);

   /**
    * Gets the icon for a tool corresponding to the given name.
    * @param name the name identifying the tool icon
    * @param the icon's description
    * @param true if a compact version of the icon is required
    */ 
   public Icon getToolIcon(String name, String description, boolean isCompact);

   /**
    * Creates an action button.
    */ 
   public JButton createButton(Icon icon, String action,
     ActionListener listener, JComponent component, KeyStroke keyStroke,
     boolean isDefault, String toolTip, boolean isCompact);

   /**
    * Prompts the user for confirmation.
    * @param msg the message requesting confirmation
    * @param title the dialog title
    * @param options options (as for JOptionPane)
    * @return response (as for JOptionPane)
    */ 
   public int confirm(String msg, String title, int options);

   /**
    * Gets a message from the language resource file.
    * @param label identifies the message
    * @param params message parameters
    * @return the formatted message
    */ 
   public String getMessage(String label, Object... params);

   /**
    * Gets a mnemonic from the language resource file.
    * @param label identifies the mnemonic
    * @return the mnemonic code point or -1 if no mnemonic associated with label
    */
   public int getMnemonic(String label);

   /**
    * Gets the tooltip text from the language resource file.
    * @param label identifies the tooltip
    * @param params message parameters
    * @return the tooltip text or null if no tooltip associated with label
    */
   public String getToolTipMessage(String label, Object... params);
}
