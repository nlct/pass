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

import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

import org.xml.sax.SAXException;

public class SelectProjectDialog extends JDialog implements ActionListener
{
   public SelectProjectDialog(PassEditor gui, JDialog parent)
   {
      super(parent, gui.getMessage("selectproject.title"), true);
      this.gui = gui;
      init();
   }

   public SelectProjectDialog(PassEditor gui, JFrame parent)
   {
      super(parent, gui.getMessage("selectproject.title"), true);
      this.gui = gui;
      init();
   }

   private void init()
   {
      JComponent mainPanel = Box.createVerticalBox();
      mainPanel.setAlignmentX(0.0f);
      getContentPane().add(mainPanel, "Center");

      ButtonGroup grp = new ButtonGroup();

      newButton = createRadioButton("new", grp);
      mainPanel.add(newButton);
      newButton.setSelected(true);

      openButton = createRadioButton("open", grp);
      mainPanel.add(openButton);

      importButton = createRadioButton("import", grp);
      mainPanel.add(importButton);

      JTextArea infoArea = new JTextArea(gui.getMessage("selectproject.info",
         PassEditor.APP_NAME, openButton.getText(), importButton.getText(),
         newButton.getText()),
       5, 40);

      infoArea.setLineWrap(true);
      infoArea.setWrapStyleWord(true);
      infoArea.setEditable(false);

      getContentPane().add(infoArea, "North");

      JComponent bottomPanel = new JPanel(new BorderLayout());
      getContentPane().add(bottomPanel, "South");

      JComponent buttonPanel = new JPanel();
      bottomPanel.add(buttonPanel, "Center");

      okayButton = createJButton("okay", null, true);

      buttonPanel.add(okayButton);
      buttonPanel.add(createJButton("exit", 
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      bottomPanel.add(gui.createHelpButton("selectproject"), "East");

      pack();
      setLocationRelativeTo(getParent());
   }

   private JButton createJButton(String action, KeyStroke keyStroke)
   {
      return createJButton(action, keyStroke, false);
   }

   private JButton createJButton(String action,
     KeyStroke keyStroke, boolean isDefault)
   {
      String label = "button."+action;

      return gui.createJButton(gui.getMessage(label), 
         gui.getMnemonic(label), action, this, getRootPane(),
         keyStroke, isDefault);
   }

   private JRadioButton createRadioButton(String propTail, ButtonGroup grp)
   {
      String label = "selectproject."+propTail;

      JRadioButton button = new JRadioButton(gui.getMessage(label));

      int mnemonic = gui.getMnemonic(label);

      if (mnemonic != -1)
      {
         button.setMnemonic(mnemonic);
      }

      grp.add(button);

      button.setAlignmentX(0.0f);

      return button;
   }

   public void display()
   {
      okayButton.setEnabled(true);
      setVisible(true);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null)
      {
         return;
      }

      if (command.equals("okay"))
      {
         okayButton.setEnabled(false);

         try
         {
            if (okay())
            {
               setVisible(false);
            }
         }
         catch (SAXException | IOException e)
         {
            gui.error(e);
         }

         okayButton.setEnabled(true);
      }
      else if (command.equals("exit"))
      {
         System.exit(0);
      }
   }

   protected boolean okay() throws SAXException,IOException
   {
      if (newButton.isSelected())
      {
         return gui.newProject();
      }
      else if (openButton.isSelected())
      {
         return gui.openProject();
      }
      else if (importButton.isSelected())
      {
         return gui.importProject();
      }
      else
      {// shouldn't happen
         gui.error("No action selected!");
      }

      return false;
   }

   private JButton okayButton;
   private JRadioButton newButton, openButton, importButton;
   private PassEditor gui;
}
