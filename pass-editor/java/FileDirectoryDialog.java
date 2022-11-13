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
package com.dickimawbooks.passeditor;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.TreeModel;

public class FileDirectoryDialog extends JDialog implements ActionListener
{
   public FileDirectoryDialog(PassEditor gui)
   {
      super(gui, true);
      this.gui = gui;

      infoArea = new JLabel(gui.getMessage("filedir.info.default"));
      getContentPane().add(infoArea, "North");

      directoryTree = new JTree(new DirectoryTreeModel(gui.getNavigationModel()));

      getContentPane().add(new JScrollPane(directoryTree), "Center");

      JPanel buttonPanel = new JPanel();

      buttonPanel.add(createJButton("okay", null, true));
      buttonPanel.add(createJButton("cancel", 
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      getContentPane().add(buttonPanel, "South");

      pack();
      setLocationRelativeTo(gui);
   }

   private JButton createJButton(String action, KeyStroke keyStroke)
   {
      String propLabel = "button."+action;

      return gui.createJButton(gui.getMessage(propLabel), 
         gui.getMnemonic(propLabel), action, this, getRootPane(), keyStroke);
   }

   private JButton createJButton(String action, KeyStroke keyStroke, boolean isDefault)
   {
      String propLabel = "button."+action;

      return gui.createJButton(gui.getMessage(propLabel), 
         gui.getMnemonic(propLabel), action, this, getRootPane(),
         keyStroke, isDefault);
   }

   public PathNode showMoveTo(String srcName)
   {
      infoArea.setText(gui.getMessage("filedir.info.moveto", srcName));
      directoryTree.setModel(new DirectoryTreeModel(gui.getNavigationModel()));
      result = null;
      setTitle(gui.getMessage("filedir.title.moveto"));
      setVisible(true);

      return result;
   }

   public PathNode showCopyTo(String srcName)
   {
      infoArea.setText(gui.getMessage("filedir.info.copyto", srcName));
      directoryTree.setModel(new DirectoryTreeModel(gui.getNavigationModel()));
      result = null;
      setTitle(gui.getMessage("filedir.title.copyto"));
      setVisible(true);

      return result;
   }

   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null) return;

      if (command.equals("okay"))
      {
         result = (PathNode)directoryTree.getLastSelectedPathComponent();

         if (result == null)
         {
            JOptionPane.showMessageDialog(this, 
              gui.getMessage("error.no_dir_selected"),
              gui.getMessage("error.title"), JOptionPane.ERROR_MESSAGE);
         }
         else
         {
            setVisible(false);
         }
      }
      else if (command.equals("cancel"))
      {
         result = null;
         setVisible(false);
      }
   }

   private JLabel infoArea;
   private JTree directoryTree;
   private PassEditor gui;
   private PathNode result;
}
