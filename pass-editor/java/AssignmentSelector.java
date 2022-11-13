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

import java.util.Vector;

import java.time.*;
import java.time.format.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.dickimawbooks.passlib.*;
import com.dickimawbooks.passguilib.ConfirmCheckBox;
import com.dickimawbooks.passguilib.PassGuiTools;

public class AssignmentSelector extends JDialog implements ActionListener,ChangeListener
{
   public AssignmentSelector(PassEditor gui, Vector<AssignmentData> assignments, Course course)
   {
      super(gui, gui.getMessage("assignment_selector.title", course.getCode()), true);
      this.gui = gui;
      PassGuiTools passGuiTools = gui.getPassGuiTools();

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            System.exit(0);
         }
      });

      JComponent mainPanel = Box.createVerticalBox();
      mainPanel.setAlignmentX(0.0f);
      getContentPane().add(mainPanel, "Center");

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
      panel.setAlignmentX(0.0f);
      mainPanel.add(panel);

      userField = new JTextField(12);

      JLabel userLabel = passGuiTools.createUserNameLabel(userField);
      userLabel.setAlignmentX(0.0f);
      panel.add(userLabel);

      userField.setAlignmentX(0.0f);
      panel.add(userField);

      String username = gui.getProperties().getProperty("student.id");

      if (username != null)
      {
         userField.setText(username);
      }

      regNumField = new JTextField(12);

      JLabel regNumLabel = passGuiTools.createRegNumLabel(regNumField);
      regNumLabel.setAlignmentX(0.0f);
      panel.add(regNumLabel);

      regNumField.setAlignmentX(0.0f);
      panel.add(regNumField);

      String regNum = gui.getProperties().getProperty("student.number");

      if (regNum != null)
      {
         regNumField.setText(regNum);
      }

      panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
      panel.setAlignmentX(0.0f);
      mainPanel.add(panel);

      JLabel assignmentLabel = createLabel("assignment");
      panel.add(assignmentLabel);

      assignmentBox = new JComboBox<AssignmentData>(assignments);
      panel.add(assignmentBox);
      assignmentLabel.setLabelFor(assignmentBox);

      panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
      panel.setAlignmentX(0.0f);
      mainPanel.add(panel);

      panel.add(createLabel("due"));

      dueField = new JLabel();
      panel.add(dueField);

      LocalDateTime now = LocalDateTime.now();

      for (int i = 0; i < assignments.size(); i++)
      {
         AssignmentData assignment = assignments.get(i);
         LocalDateTime due = assignment.getDueDate();

         assignmentBox.setSelectedIndex(i);
         dueField.setText(assignment.formatDueDate());

         if (due.isAfter(now))
         {
            break;
         }
      }

      assignmentBox.addItemListener(new ItemListener()
      {
         public void itemStateChanged(ItemEvent e)
         {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
               AssignmentData data =
                 (AssignmentData)assignmentBox.getSelectedItem();
               dueField.setText(data.formatDueDate());
            }
         }
      });

      agreeBox = new ConfirmCheckBox(gui);
      agreeBox.addChangeListener(this);
      mainPanel.add(agreeBox);

      JPanel bottomPanel = new JPanel(new BorderLayout());
      getContentPane().add(bottomPanel, "South");

      JPanel buttonPanel = new JPanel();
      bottomPanel.add(buttonPanel, "Center");

      okayButton = createJButton("okay");
      okayButton.setEnabled(false);
      buttonPanel.add(okayButton);

      buttonPanel.add(createJButton("cancel"));
      buttonPanel.add(createJButton("exit"));

      bottomPanel.add(gui.createHelpButton("selectassignment"), "East");

      pack();
      setLocationRelativeTo(gui);
   }

   private JLabel createLabel(String propLabelTail)
   {
      String propLabel = "assignment_selector." + propLabelTail;

      PassTools passTools = gui.getPassTools();

      JLabel label = new JLabel(passTools.getMessage(propLabel));

      int mnemonic = passTools.getMnemonic(propLabel, -1);

      if (mnemonic != -1)
      {
         label.setDisplayedMnemonic(mnemonic);
      }

      return label;
   }

   private JButton createJButton(String action)
   {
      return gui.getPassGuiTools().createJButton("button", action, this, null);
   }

   private void enableOkay(boolean enable)
   {
      okayButton.setEnabled(enable);

      if (enable)
      {
         getRootPane().setDefaultButton(okayButton);
      }
   }

   public boolean isConfirmed()
   {
      return agreeBox.isSelected();
   }

   public void stateChanged(ChangeEvent e)
   {
      if (e.getSource() == agreeBox)
      {
         enableOkay(agreeBox.isSelected());
      }
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("okay".equals(action))
      {
         String username = userField.getText();
         String regnum = regNumField.getText();
         PassTools passTools = gui.getPassTools();

         if (username.isEmpty())
         {
            JOptionPane.showMessageDialog(this, 
              passTools.getMessage("error.missing_input",
                passTools.getConfig().getUserNameText()),
              passTools.getMessage("error.title"), JOptionPane.ERROR_MESSAGE);
            return;
         }

         if (!passTools.isValidUserName(username))
         {
            JOptionPane.showMessageDialog(this, 
              passTools.getMessage("error.invalid_input",
                passTools.getConfig().getUserNameText(), username),
              passTools.getMessage("error.title"), JOptionPane.ERROR_MESSAGE);
            return;
         }

         if (regnum.isEmpty())
         {
            JOptionPane.showMessageDialog(this, 
              passTools.getMessage("error.missing_input",
                 passTools.getConfig().getRegNumText()),
              passTools.getMessage("error.title"), JOptionPane.ERROR_MESSAGE);
            return;
         }

         if (!passTools.isValidRegNum(regnum))
         {
            JOptionPane.showMessageDialog(this, 
              passTools.getMessage("error.invalid_input",
                 passTools.getConfig().getRegNumText(), regnum),
              passTools.getMessage("error.title"), JOptionPane.ERROR_MESSAGE);
            return;
         }

         gui.addStudent(new Student(username, regnum));
         gui.setAssignment((AssignmentData)assignmentBox.getSelectedItem());
         setVisible(false);
      }
      else if ("cancel".equals(action))
      {
         setVisible(false);
      }
      else if ("exit".equals(action))
      {
         System.exit(0);
      }
   }

   private JComboBox<AssignmentData> assignmentBox;
   private JLabel dueField;
   private JTextField userField, regNumField;
   private PassEditor gui;
   private JCheckBox agreeBox;
   private JButton okayButton;
}
