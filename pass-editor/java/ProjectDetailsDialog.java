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
import java.io.IOException;

import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.dickimawbooks.passlib.*;

public class ProjectDetailsDialog extends JDialog 
  implements ActionListener,ListSelectionListener
{
   public ProjectDetailsDialog(PassEditor gui)
   {
      super(gui, gui.getMessage("projectdetails.title"), true);
      this.gui = gui;

      JComponent topPanel = Box.createVerticalBox();
      topPanel.setAlignmentX(0.0f);
      getContentPane().add(topPanel, "North");

      courseComp = createLabel("course_not_set");
      topPanel.add(courseComp);

      assignmentComp = createLabel("assignment_not_set");
      topPanel.add(assignmentComp);

      dueComp = createLabel("due_date");
      topPanel.add(dueComp);

      topPanel.add(Box.createVerticalStrut(20));
      topPanel.add(createLabel("group_info"));
      topPanel.add(Box.createVerticalStrut(20));

      JComponent mainPanel = Box.createVerticalBox();
      mainPanel.setAlignmentX(0.0f);
      getContentPane().add(mainPanel, "Center");

      AssignmentProcessConfig config = gui.getPassTools().getConfig();

      tableModel = new DefaultTableModel(
        new Object[] {"", 
           config.getUserNameTitle(),
           config.getRegNumTitle()}, 1)
      {
         public boolean isCellEditable(int row, int column)
         {
            return column > 0;
         }
      };

      studentTable = new JTable(tableModel);
      studentTable.setAlignmentX(0.0f);
      studentTable.getSelectionModel().addListSelectionListener(this);
      studentTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

      mainPanel.add(new JScrollPane(studentTable));

      JComponent sidePanel = Box.createVerticalBox();
      getContentPane().add(sidePanel, "East");

      sidePanel.add(createIconButton("table/RowInsertBefore", "insertbefore", 
         KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_DOWN_MASK),
        gui.getMessage("projectdetails.insertrow")));

      sidePanel.add(createIconButton("table/RowInsertAfter", "insertafter", 
         KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 
           InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
         gui.getMessage("projectdetails.appendrow")));

      deleteRowButton = createIconButton("table/RowDelete", "deleterow",
         KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK),
         gui.getMessage("projectdetails.deleterow"));
      deleteRowButton.setEnabled(false);
      sidePanel.add(deleteRowButton);

      JComponent bottomPanel = new JPanel(new BorderLayout());
      getContentPane().add(bottomPanel, "South");

      bottomPanel.add(gui.createHelpButton("projectdetails"), "East");

      JComponent buttonPanel = new JPanel();
      bottomPanel.add(buttonPanel, "Center");

      buttonPanel.add(createJButton("okay", null, true));

      buttonPanel.add(createJButton("cancel", 
        KeyStroke.getKeyStroke(KeyEvent.VK_CANCEL, 0), false));

      pack();
      setLocationRelativeTo(gui);
   }

   protected JButton createJButton(String action, 
      KeyStroke keyStroke, boolean isDefault)
   {
      String label = "button."+action;

      return gui.createJButton(gui.getMessage(label), gui.getMnemonic(label),
        action, this, getRootPane(), keyStroke, isDefault);
   }

   protected JButton createIconButton(String icName, String action,
     KeyStroke keyStroke, String toolTip)
   {
      JButton button = gui.createJButton(gui.getToolIcon(icName, toolTip), action, 
        this, getRootPane(), keyStroke, false, toolTip);

      //button.setContentAreaFilled(false);
      button.setMargin(new Insets(0,0,0,0));

      return button;
   }

   protected JLabel createLabel(String labelTail)
   {
      JLabel label = gui.createJLabel("projectdetails."+labelTail);

      label.setAlignmentX(0.0f);

      return label;
   }

   public void display()
   {
      project = gui.getProject();

      Course course = project.getCourse();

      courseComp.setText(gui.getMessage("projectdetails.course_info",
         course.getCode(), course.getTitle()));

      AssignmentData assignment = project.getAssignment();

      assignmentComp.setText(assignment.getTitle());

      dueComp.setText(gui.getMessage("projectdetails.due_info",
         assignment.formatDueDate()));

      Vector<Student> students = project.getStudents();

      tableModel.setRowCount((int)Math.max(1, students.size()));

      boolean isgroup = tableModel.getRowCount() > 1;

      if (!isgroup)
      {
         tableModel.setValueAt(gui.getMessage("projectdetails.solo_student"),
            0, COLUMN_LABEL);
      }

      for (int i = 0; i < students.size(); i++)
      {
         Student student = students.get(i);

         if (isgroup)
         {
            tableModel.setValueAt(gui.getMessage("projectdetails.student_n", (i+1)),
              i, COLUMN_LABEL);
         }

         tableModel.setValueAt(student.getUserName(), i, COLUMN_USERNAME);
         tableModel.setValueAt(student.getRegNumber(), i, COLUMN_REGNUM);
      }

      setVisible(true);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null) return;

      if (command.equals("okay"))
      {
         project.clearStudents();

         if (studentTable.isEditing())
         {
            studentTable.getCellEditor().stopCellEditing();
         }

         PassTools passTools = gui.getPassTools();

         for (int i = 0; i < tableModel.getRowCount(); i++)
         {
            String id = (String)tableModel.getValueAt(i, COLUMN_USERNAME);
            String regNum = (String)tableModel.getValueAt(i, COLUMN_REGNUM);

            if ((id == null || id.isEmpty())
             && (regNum == null || regNum.isEmpty()))
            {
               continue;
            }

            if (id == null || id.isEmpty())
            {
               gui.error(this, 
                 gui.getMessage("error.missing_input_in_row",
                 passTools.getConfig().getUserNameText(), (i+1)));

               return;
            }
            else if (!passTools.isValidUserName(id))
            {
               gui.error(passTools.getMessage("error.invalid_input",
                  passTools.getConfig().getUserNameText(), id));

               return;
            }
            else if (regNum == null || regNum.isEmpty())
            {
               gui.error(this, 
                 gui.getMessage("error.missing_input_in_row",
                  passTools.getConfig().getRegNumText(), (i+1)));

               return;
            }
            else if (!passTools.isValidRegNum(regNum))
            {
               gui.error(passTools.getMessage("error.invalid_input",
                  passTools.getConfig().getRegNumText(), regNum));

               return;
            }

            project.addStudent(new Student(id, regNum));
         }

         if (project.getStudents().isEmpty())
         {
            gui.error(this, gui.getMessage("error.missing_student_id"));

            return;
         }

         try
         {
            project.save();
            setVisible(false);
         }
         catch (IOException e)
         {
            gui.error(this, e.getMessage());
         }
      }
      else if (command.equals("insertbefore"))
      {
         int idx = studentTable.getSelectedRow();

         if (idx == -1)
         {
            idx = 0;
         }

         tableModel.insertRow(idx, createNewRowData(idx));

         updateRowLabels();
      }
      else if (command.equals("insertafter"))
      {
         int idx = studentTable.getSelectedRow();

         if (idx == -1 || idx == studentTable.getRowCount()-1)
         {
            tableModel.addRow(createNewRowData(studentTable.getRowCount()));
         }
         else
         {
            idx++;
            tableModel.insertRow(idx, createNewRowData(idx));
         }

         updateRowLabels();
      }
      else if (command.equals("deleterow"))
      {
         int[] indexes = studentTable.getSelectedRows();

         for (int i = indexes.length-1; i >= 0; i--)
         {
            tableModel.removeRow(indexes[i]);
         }

         updateRowLabels();
      }
      else if (command.equals("cancel"))
      {
         setVisible(false);
      }
   }

   public void valueChanged(ListSelectionEvent evt)
   {
      if (!evt.getValueIsAdjusting())
      {
         deleteRowButton.setEnabled(studentTable.getRowCount() > 1
           && studentTable.getSelectedRow() != -1);
      }
   }

   protected Object[] createNewRowData(int idx)
   {
      return new Object[] {"", "" , ""};
   }

   protected void updateRowLabels()
   {
      int n = studentTable.getRowCount();

      if (n == 1)
      {
         tableModel.setValueAt(gui.getMessage("projectdetails.solo_student"),
           0, COLUMN_LABEL);
      }
      else
      {
         for (int i = 0; i < n; i++)
         {
            tableModel.setValueAt(gui.getMessage("projectdetails.student_n", (i+1)),
               i, COLUMN_LABEL);
         }
      }
   }

   private PassEditor gui;
   private Project project;
   private JLabel courseComp, assignmentComp, dueComp;
   private JButton deleteRowButton;
   private JTable studentTable;
   private DefaultTableModel tableModel;

   public static final int COLUMN_LABEL = 0;
   public static final int COLUMN_USERNAME = 1;
   public static final int COLUMN_REGNUM = 2;
}
