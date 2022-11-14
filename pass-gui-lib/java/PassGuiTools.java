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
import java.io.FileNotFoundException;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.Icon;
import javax.swing.JTextArea;

import com.dickimawbooks.passlib.AssignmentProcessConfig;
import com.dickimawbooks.passlib.AssignmentData;

/**
 * Set of methods useful for a GUI.
 */
public class PassGuiTools
{
   public PassGuiTools(PassGui gui)
   {
      this.gui = gui;
   }

   /**
    * Opens a PDF file.
    * @param pdfFile the pdf file
    * @throws IOException if I/O error occurs
    */ 
   public void openPdf(File pdfFile) throws IOException
   {
      open(pdfFile, "pdfviewer", "PDFVIEWER");
   }

   /**
    * Opens a text file. 
    * @param textFile the text file
    * @throws IOException if I/O error occurs
    */ 
   public void openEditor(File textFile) throws IOException
   {
      open(textFile, "editor", "EDITOR");
   }

   /**
    * Opens an image file.
    * @param imageFile the image file
    * @throws IOException if I/O error occurs
    */ 
   public void openImage(File imageFile) throws IOException
   {
      open(imageFile, "imageviewer", null);
   }

   /**
    * Opens a file based on MIME type.
    * @param mimetype the MIME type
    * @param file the file
    * @throws IOException if I/O error occurs
    */ 
   public void open(String mimeType, File file) throws IOException
   {
      if (mimeType == null)
      {
         open(file, null, null);
      }
      else if (mimeType.startsWith("text/"))
      {
         openEditor(file);
      }
      else if (mimeType.startsWith("image/"))
      {
         openImage(file);
      }
      else if (mimeType.equals(AssignmentData.MIME_PDF))
      {
         openPdf(file);
      }
      else
      {
         open(file, null, null);
      }
   }

   /**
    * Opens a file. The Desktop.open(File) method isn't
    * reliable, so provide alternatives. If the given application
    * name is provided in resources.xml that takes precedence, then the
    * environment variable, otherwise the Desktop method will be
    * used if the Desktop is supported.
    * @param file the file to open
    * @param name the application name used in resources.xml (may be
    * null or empty)
    * @param envname an environment variable that provides the path
    * to a viewer/editor (may be null or empty)
    */ 
   public void open(File file, String name, String envname) throws IOException
   {
      if (!file.exists())
      {
         throw new FileNotFoundException(gui.getMessage(
          "error.file_doesnt_exist", file.toString()));
      }

      // check if resources.xml has set a path for given name

      File viewer = null;

      if (name != null && !name.isEmpty())
      {
         try
         {
            viewer = gui.getPassTools().findResourceApplication(name);

            gui.debug("Found "+name+" setting: "+viewer);
         }
         catch (FileNotFoundException e)
         {
            gui.debug(e.getMessage());
         }

         if (viewer == null && envname != null && !envname.isEmpty())
         {
            viewer = System.getenv(envname);

            if ("".equals(viewer))
            {
               viewer = null;
            }
         }
      }

      if (viewer != null)
      {
         ProcessBuilder pb = new ProcessBuilder(viewer.toString(), 
           file.toString());
         pb.inheritIO();
         Process p = pb.start();
      }      
      else if (Desktop.isDesktopSupported())
      {
         gui.debug("Desktop supported");
         Desktop.getDesktop().open(file);
      }
      else 
      {
         throw new IOException(gui.getMessage("error.cant_view_file", 
          file.toString()));
      }

   }

   /**
    * Creates a new menu item using localisation. 
    * @param parent label prefix
    * @param action label suffix and action command
    */
   public JMenuItem createJMenuItem(String parent, String action,
     ActionListener listener, KeyStroke keyStroke)
   {  
      String propLabel = parent+"."+action;
      String text = gui.getMessage(propLabel);
      int mnemonic = gui.getMnemonic(propLabel);
      String tooltip = gui.getToolTipMessage(propLabel);

      return createJMenuItem(text, mnemonic, listener, action, tooltip, keyStroke);
   }

   /**
    * Creates a new menu item. 
    * @param text the menu item text
    * @param mnemonic the mnemonic code point or -1 if none
    * @param listener action listener (may be null)
    * @param action the action command (may be null)
    * @param tooltip the tooltip text (may be null)
    * @param keyStroke the accelerator (may be null)
    * @return new menu item
    */ 
   public JMenuItem createJMenuItem(String text, int mnemonic,
    ActionListener listener, String action,
    String tooltip, KeyStroke keyStroke)
   {
      JMenuItem item = new JMenuItem(text, mnemonic);

      if (listener != null)
      {
         item.addActionListener(listener);
      }

      if (action != null)
      {
         item.setActionCommand(action);
      }

      if (tooltip != null)
      {
         item.setToolTipText(tooltip);
      }

      if (keyStroke != null)
      {
         item.setAccelerator(keyStroke);
      }
      
      return item;
   }

   /**
    * Creates a label with localised text.
    * @param propLabel the label identifying the dictionary message
    * @param params the message parameters
    */  
   public JLabel createJLabel(String propLabel, Object... params)
   {
      JLabel label = new JLabel(gui.getMessage(propLabel, params));
   
      int mnemonic = gui.getMnemonic(propLabel);
         
      if (mnemonic != -1)
      {     
         label.setDisplayedMnemonic(mnemonic);
      }  
   
      return label;
   }

   /**
    * Creates a label with localised text.
    * @param comp component this label is for
    * @param propLabel the label identifying the dictionary message
    * @param params the message parameters
    */  
   public JLabel createJLabel(Component comp, String propLabel, Object... params)
   {
      JLabel label = createJLabel(propLabel, params);

      if (comp != null)
      {
         label.setLabelFor(comp);
      }

      return label;
   }

   /**
    * Creates label for username field.
    */ 
   public JLabel createUserNameLabel(Component comp)
   {
      AssignmentProcessConfig config = gui.getPassTools().getConfig();

      JLabel label = new JLabel(config.getUserNameTitle());
   
      int mnemonic = config.getUserNameMnemonic();
         
      if (mnemonic != -1)
      {     
         label.setDisplayedMnemonic(mnemonic);
      }  
   
      if (comp != null)
      {
         label.setLabelFor(comp);
      }

      return label;
   }

   /**
    * Creates label for registration number field.
    */ 
   public JLabel createRegNumLabel(Component comp)
   {
      AssignmentProcessConfig config = gui.getPassTools().getConfig();

      JLabel label = new JLabel(config.getRegNumTitle());
   
      int mnemonic = config.getRegNumMnemonic();
         
      if (mnemonic != -1)
      {     
         label.setDisplayedMnemonic(mnemonic);
      }  
   
      if (comp != null)
      {
         label.setLabelFor(comp);
      }

      return label;
   }

   /**
    * Creates a new button with the localised text.
    * @param parent label prefix
    * @param action label suffix and button action command
    * @param listener button action listener
    * @param icon the button icon (may be null)
    */ 
   public JButton createJButton(String parent, String action,
     ActionListener listener, Icon icon)
   {     
      String label = parent+"."+action;
      
      return createJButton(gui.getMessage(label),
        gui.getMnemonic(label), icon, action, listener,
        gui.getToolTipMessage(label));
   }

   /**
    * Creates a new button with the given text.
    * @param text button text
    * @param mnemonic button mnemonic codepoint or -1 if none
    * @param icon button icon or null if none
    * @param action button action command
    * @param listener button action listener
    * @param tooltipText button tooltip text
    */ 
   public JButton createJButton(String text, int mnemonic, Icon icon,
      String action, ActionListener listener, String tooltipText)
   {
      JButton button = new JButton(text);
      
      if (tooltipText != null)
      {  
         button.setToolTipText(tooltipText);
      }
      
      if (icon != null)
      {  
         button.setIcon(icon);
      }
      
      if (mnemonic != -1)
      {  
         button.setMnemonic(mnemonic);
      }

      if (action != null)
      {  
         button.setActionCommand(action);
      }

      if (listener != null)
      {  
         button.addActionListener(listener);
      }

      return button;
   }

   /**
    * Creates a non-editable plain text message area with line wrap.
    * @param text the message text
    * @return the new text area
    */ 
   public JTextArea createMessageArea(String message)
   {
      JTextArea textArea = new JTextArea(message);
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);
      textArea.setEditable(false);
      return textArea;
   }

   /**
    * Creates a non-editable plain text message area with line wrap.
    * @param text the message text
    * @param rows number of rows
    * @param columns number of columns
    * @return the new text area
    */ 
   public JTextArea createMessageArea(String message, int rows, int columns)
   {
      JTextArea textArea = new JTextArea(message, rows, columns);
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);
      textArea.setEditable(false);
      return textArea;
   }

   protected PassGui gui;
}
