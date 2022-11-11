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
import java.awt.BorderLayout;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.filechooser.FileFilter;

import com.dickimawbooks.passlib.PassFile;
import com.dickimawbooks.passlib.AssignmentData;

/**
 * A component identifying a file that forms part of the student's
 * project. The programming language can sometimes be identified by
 * the file extension (such as ".java" or ".c") but sometimes it can
 * be ambiguous (such as "*.h"), or may not be present (for example, a *nix script).
 * In which case, the user can select the appropriate language
 * using the language box. This affects the way the file is included
 * in the PDF. (Pretty-printing for known languages, verbatim for
 * unknown plain text, or an attachment for a document.)
 */
public class FilePanel extends JPanel implements PassFile
{
   /**
    * Creates a new component.
    * @param main the GUI
    * @param dir the parent directory
    * @param filename the filename
    * @param showLanguageBox true if language selector should be
    * shown
    * @param chooser the file chooser for the FileFieldButton
    */ 
   public FilePanel(PrepareAssignmentUpload main, File dir,
     String filename, JFileChooser chooser,
     boolean showLanguageBox)
   {
      this(main, dir, filename, showLanguageBox, chooser, null);
   }

   /**
    * Creates a new component with language selector.
    * @param main the GUI
    * @param dir the parent directory
    * @param filename the filename
    * @param chooser the file chooser for the FileFieldButton
    */ 
   public FilePanel(PrepareAssignmentUpload main, File dir,
      String filename, JFileChooser chooser)
   {
      this(main, dir, filename, true, chooser, null);
   }

   /**
    * Creates a new component with language selector.
    * @param main the GUI
    * @param dir the parent directory
    * @param filename the filename
    * @param chooser the file chooser for the FileFieldButton
    * @param imageURL image for the FileFieldButton (may be null)
    */ 
   public FilePanel(PrepareAssignmentUpload main, File dir,
      String filename, JFileChooser chooser, URL imageURL)
   {
      this(main, dir, filename, true, chooser, imageURL);
   }

   /**
    * Creates a new component.
    * @param main the GUI
    * @param dir the parent directory
    * @param filename the filename
    * @param showLanguageBox true if language selector should be
    * shown
    * @param chooser the file chooser for the FileFieldButton
    * @param imageURL image for the FileFieldButton (may be null)
    * @param filters file filters for the FileFieldButton
    */ 
   public FilePanel(PrepareAssignmentUpload main, File dir, String filename,
     boolean showLanguageBox, 
     JFileChooser chooser, URL imageURL, FileFilter... filters)
   {
      super(new BorderLayout());
      setAlignmentX(0);
      this.main = main;

      leftComponent = new JPanel();
      leftComponent.setAlignmentX(0);

      add(leftComponent, "West");

      languageBox = new JComboBox<String>(
        AssignmentData.LISTING_LANGUAGES);
      languageBox.setVisible(showLanguageBox);

      textField = new JTextField(20)
      {
         public void setText(String text)
         {
            super.setText(text);
            updateLanguageBox();
         }
      };

      textField.setAlignmentX(0);
      add(textField, "Center");

      if (dir != null && filename != null && !filename.isEmpty())
      {
         File file = new File(dir, filename);

         if (file.exists())
         {
            textField.setText(file.getAbsolutePath());
         }
      }
      else
      {
         textField.setText(filename);
      }

      rightComponent = new JPanel();
      rightComponent.setAlignmentX(0);
      add(rightComponent, "East");

      fileFieldButton = new FileFieldButton(main.getPassTools(),
         textField, chooser, imageURL, filters);

      rightComponent.add(fileFieldButton);

      languageBox.setAlignmentX(0);
      rightComponent.add(languageBox);
   }

   /**
    * Gets the selected file's language (type).
    * @return the language selector's selected item
    */ 
   public String getLanguage()
   {
      return languageBox.getSelectedItem().toString();
   }

   /**
    * Gets the default file name.
    * @return empty if no default name otherwise the expected file
    * name
    */ 
   public String getDefaultName()
   {
      return "";
   }

   /**
    * Updates the language selector based on the filename.
    * Does nothing if the language selector isn't visible otherwise
    * it will try to determine the setting based on the file
    * extension using AssignmentData.getListingLanguage(String).
    */ 
   public void updateLanguageBox()
   {
      if (!languageBox.isVisible()) return;

      String filename = getFilename();
      String ext = null;

      if (filename.isEmpty())
      {
         ext = getDefaultName();
      }
      else
      {
         File file = new File(filename);
         ext = file.getName();
      }

      int idx = ext.lastIndexOf(".");

      if (idx > -1)
      {
         ext = ext.substring(idx+1);
      }

      ext = main.getAssignment().getListingLanguage(ext);

      if (ext == null)
      {
         languageBox.setSelectedItem(AssignmentData.UNKNOWN_LANGUAGE);
      }
      else
      {
         languageBox.setSelectedItem(ext);
      }
   }

   /**
    * Gets the value in the filename text field.
    * @return the specified filename
    */ 
   public String getFilename()
   {
      return textField.getText();
   }

   /**
    * Gets the file as indicated in the filename text field.
    * @return the specified file if the field isn't empty otherwise
    * null
    */ 
   public File getFile()
   {
      String name = getFilename();

      return name.isEmpty() ? null : new File(name);
   }

   /**
    * Gets the filename text field.
    * @return the text field
    */ 
   public JTextField getTextField()
   {
      return textField;
   }

   /**
    * Adds a component to the area on the left.
    */ 
   public void addLeftComponent(JComponent comp)
   {
      leftComponent.add(comp);
   }

   /**
    * Adds a component to the area on the right.
    */ 
   public void addRightComponent(JComponent comp)
   {
      rightComponent.add(comp);
   }

   /**
    * Sets the file filter.
    * @param filter the file filter
    */ 
   public void setFileFilter(FileFilter filter)
   {
      fileFieldButton.setFileFilter(filter);
   }

   /**
    * Sets the selected file. The absolute path is put in the text
    * field.
    * @param file the file
    */ 
   public void setFilename(File file)
   {
      textField.setText(file.getAbsolutePath());
   }

   /**
    * Sets the selected filename.
    * @param filename the filename
    */ 
   public void setFilename(String filename)
   {
      textField.setText(filename);
   }

   private JTextField textField;
   private JComboBox<String> languageBox;
   private FileFieldButton fileFieldButton;
   private JComponent leftComponent, rightComponent;

   protected PrepareAssignmentUpload main;
}