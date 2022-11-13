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

import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.*;

public class GoToLineDialog extends JDialog implements ActionListener
{
   public GoToLineDialog(PassEditor gui)
   {
      super(gui, gui.getMessage("goto.title"), false);

      this.gui = gui;

      JComponent panel = Box.createHorizontalBox();
      getContentPane().add(panel, "Center");

      JLabel label = gui.createJLabel("goto.line");
      panel.add(label);

      lineSpinnerModel = new SpinnerNumberModel(Integer.valueOf(0),
        Integer.valueOf(0), null, Integer.valueOf(1));
      lineSpinner = new JSpinner(lineSpinnerModel);
      label.setLabelFor(lineSpinner);
      panel.add(lineSpinner);

      JComponent bottomPanel = new JPanel(new BorderLayout());
      getContentPane().add(bottomPanel, "South");

      bottomPanel.add(gui.createHelpButton("goto"), "East");

      panel = new JPanel();
      bottomPanel.add(panel, "Center");

      panel.add(createJButton("okay", null, true));
      panel.add(createJButton("cancel",
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      pack();
      setLocationRelativeTo(gui);
   }

   private JButton createJButton(String action, KeyStroke keyStroke)
   {
      String propLabel = "button."+action;

      return gui.createJButton(gui.getMessage(propLabel), gui.getMnemonic(propLabel),
        action, this, getRootPane(), keyStroke);
   }

   private JButton createJButton(String action,
     KeyStroke keyStroke, boolean isDefault)
   {
      String propLabel = "button."+action;

      return gui.createJButton(gui.getMessage(propLabel), 
        gui.getMnemonic(propLabel), action, this, getRootPane(),
         keyStroke, isDefault);
   }

   public void display(FilePane filePane)
   {
      this.filePane = filePane;

      lineSpinnerModel.setMaximum(Integer.valueOf(filePane.getLineCount()));
      lineSpinnerModel.setValue(Integer.valueOf(filePane.getCurrentLine()));

      setVisible(true);

      lineSpinner.requestFocusInWindow();
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null)
      {
         return;
      }

      if (command.equals("okay"))
      {
         filePane.goToLine(lineSpinnerModel.getNumber().intValue());
         filePane.requestFocusInWindow();
         setVisible(false);
      }
      else if (command.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private PassEditor gui;
   private FilePane filePane;
   private JSpinner lineSpinner;
   private SpinnerNumberModel lineSpinnerModel;
}
