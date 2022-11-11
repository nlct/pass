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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import javax.swing.*;

/**
 * Application settings dialog. Allows the user to set up 
 * preferred defaults.
 */
public class ApplicationProperties extends JDialog
 implements ActionListener
{
   /**
    * Creates a new dialog.
    * @param main the GUI
    * @param disableTimeOut if true don't allow user to alter the
    * timeout
    */ 
   public ApplicationProperties(PrepareAssignmentUpload main, boolean disableTimeOut)
   {
      super(main, main.getPassTools().getMessage("properties.title"));
      this.main = main;

      getContentPane().add(
        new JLabel(main.getPassTools().getMessage("properties.shared",
          "Pass Editor")), "North");

      JTabbedPane tabbedPane = new JTabbedPane();
      getContentPane().add(tabbedPane, "Center");

      tabbedPane.add(createDefaultDirectoryPanel());
      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1, 
        main.getPassTools().getMnemonic("properties.default_dir", -1));

      tabbedPane.add(createTimeOutPanel(disableTimeOut));
      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1, 
        main.getPassTools().getMnemonic("properties.timeout", -1));

      tabbedPane.add(createUIPanel());
      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1, 
        main.getPassTools().getMnemonic("properties.ui", -1));

      JPanel panel = new JPanel();
      getContentPane().add(panel, "South");

      JButton okayButton = createButton("okay");
      getRootPane().setDefaultButton(okayButton);
      panel.add(okayButton);

      panel.add(createButton("cancel"));

      pack();
      setLocationRelativeTo(main);
   }

   /**
    * Creates a new button associated with the action.
    * The localised button text needs to have the label
    * "button.<em>action</em>" in the dictionary.
    * @param action the action
    * @return the new button
    */ 
   private JButton createButton(String action)
   {
      String propLabel = "button."+action;
      JButton button = new JButton(getMessage(propLabel));
      button.setMnemonic(getMnemonic(propLabel));
      button.setActionCommand(action);
      button.addActionListener(this);

      return button;
   }

   /**
    * Creates a new radio button associated with the action.
    * The localised button text needs to have the label
    * "properties.<em>label</em>" in the dictionary.
    * @param label the properties label suffix
    * @param action the action
    * @param bg the button group
    * @param state the default button state (true if selected)
    * @return the new button
    */ 
   private JRadioButton createRadioButton(String label, 
      String action, ButtonGroup bg, boolean state)
   {
      String propLabel = "properties."+label;
      JRadioButton button = new JRadioButton(getMessage(propLabel));
      button.setMnemonic(getMnemonic(propLabel));
      button.setSelected(state);
      bg.add(button);
      button.setActionCommand(action);
      button.addActionListener(this);

      return button;
   }

   /**
    * Gets the mnemonic associated with the label.
    * @param label the label identifying the mnemonic
    * @return the mnemonic code point or -1 if no match
    */ 
   protected int getMnemonic(String label)
   {
      return main.getPassTools().getMnemonic(label, -1);
   }

   /**
    * Gets the localised message identified by the label.
    * @param label the label identifying the message
    * @param params the message parameters
    * @return the message
    */ 
   protected String getMessage(String label, Object... params)
   {
      return main.getPassTools().getMessage(label, params);
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("okay".equals(action))
      {
         if (homeDirButton.isSelected())
         {
            main.setStartUpDirectoryHome();
         }
         else if (lastDirButton.isSelected())
         {
            main.setStartUpDirectoryLast();
         }
         else
         {
            main.setStartUpDirectory(customField.getText(),
              StartupDirType.CUSTOM);
         }

         if (timeoutSpinner.isEnabled())
         {
            main.setTimeOutProperty(timeoutModel.getNumber().longValue());
         }

         main.setFileSearchMax(maxFileSearchModel.getNumber().intValue());

         int idx = lookAndFeelBox.getSelectedIndex();
         UIManager.LookAndFeelInfo info = lookAndFeelInfo[idx];
         LookAndFeel lookandfeel = UIManager.getLookAndFeel();
         String current = lookandfeel.getClass().getName();

         if (!current.equals(info.getClassName()))
         {
            try
            {
               main.setUI(info.getClassName());
               currentLookAndFeelInfo = info;
            }
            catch (Exception e)
            {// shouldn't happen
               main.error(e);
               e.printStackTrace();
               return;
            }
         }

         setVisible(false);
      }
      else if ("cancel".equals(action))
      {
         setVisible(false);
      }
      else if ("dirchoice".equals(action))
      {
         boolean custom = customDirButton.isSelected();
         customField.setEnabled(custom);
         customFieldButton.setEnabled(custom);

         if (custom)
         {
            customFieldButton.requestFocusInWindow();
         }
      }
   }

   /**
    * Show this dialog and ensure components reflect current state.
    */ 
   public void display()
   {
      switch (main.getStartupDirType())
      {
         case LAST:
            lastDirButton.setSelected(true);
         break;
         case HOME:
            homeDirButton.setSelected(true);
         break;
         case CUSTOM:
            customDirButton.setSelected(true);
         break;
      }

      customField.setText(main.getStartupDirectoryProperty());

      boolean custom = customDirButton.isSelected();
      customField.setEnabled(custom);
      customFieldButton.setEnabled(custom);

      setVisible(true);
   }

   /**
    * Creates the component used to specify the default directory.
    * @return the default directory component
    */ 
   private JComponent createDefaultDirectoryPanel()
   {
      JPanel panel = new JPanel();

      panel.setName(main.getPassTools().getMessage("properties.default_dir.title"));

      ButtonGroup bg = new ButtonGroup();

      StartupDirType startupDirType = main.getStartupDirType();

      lastDirButton = createRadioButton("last", "dirchoice", bg, 
        startupDirType == StartupDirType.LAST);
      panel.add(lastDirButton);

      homeDirButton = createRadioButton("home", "dirchoice", bg, 
        startupDirType == StartupDirType.HOME);
      panel.add(homeDirButton);

      customDirButton = createRadioButton("custom", "dirchoice", bg, 
        startupDirType == StartupDirType.CUSTOM);
      panel.add(customDirButton);

      customField = new JTextField(main.getStartupDirectoryProperty(), 20);
      customField.setEnabled(customDirButton.isSelected());
      panel.add(customField);

      customFieldButton = new FileFieldButton(main.getPassTools(),
        customField, main.getDirChooser(),
        main.getImageURL("general/Open"));
      panel.add(customFieldButton);
      customFieldButton.setEnabled(customDirButton.isSelected());

      return panel;
   }

   /**
    * Creates the component used to specify the timeout value.
    * @return the timeout component
    */ 
   private JComponent createTimeOutPanel(boolean disableTimeOut)
   {
      JComponent comp = new JPanel(new BorderLayout());
      comp.setName(main.getPassTools().getMessage("properties.timeout.title"));

      JPanel panel = new JPanel();
      comp.add(panel, "Center");

      JLabel timeoutLabel = main.createJLabel("properties.process_timeout");
      panel.add(timeoutLabel);

      timeoutModel = new SpinnerNumberModel(
        (int)main.getTimeOutProperty(), 1, 3600, 1);

      timeoutSpinner = new JSpinner(timeoutModel);

      panel.add(timeoutSpinner);
      timeoutLabel.setLabelFor(timeoutSpinner);

      if (disableTimeOut)
      {
         timeoutLabel.setEnabled(false);
         timeoutSpinner.setEnabled(false);

         timeoutDisabledField = main.createJLabel("properties.fixed_timeout",
           main.getTimeOut());

         comp.add(timeoutDisabledField, "North");
      }

      JPanel fileSearchPanel = new JPanel();
      comp.add(fileSearchPanel, "South");

      JLabel maxFileSearchLabel = main.createJLabel("properties.max_file_search");
      fileSearchPanel.add(maxFileSearchLabel);

      maxFileSearchModel = new SpinnerNumberModel(
        main.getFileSearchMax(), 0, 1000, 1);

      maxFileSearchSpinner = new JSpinner(maxFileSearchModel);

      fileSearchPanel.add(maxFileSearchSpinner);
      maxFileSearchLabel.setLabelFor(maxFileSearchSpinner);

      return comp;
   }

   /**
    * Creates the component used to specify the look and feel.
    * @return the UI component
    */ 
   private JComponent createUIPanel()
   {
      JPanel panel = new JPanel();
      panel.setName(main.getPassTools().getMessage("properties.ui.title"));

      JLabel lookAndFeelLabel = main.createJLabel("properties.lookandfeel");
      panel.add(lookAndFeelLabel);

      LookAndFeel lookandfeel = UIManager.getLookAndFeel();
      String current = lookandfeel.getClass().getName();

      lookAndFeelInfo = UIManager.getInstalledLookAndFeels();
      String[] names = new String[lookAndFeelInfo.length];

      int selectedIdx = -1;

      for (int i = 0; i < lookAndFeelInfo.length; i++)
      {
         names[i] = lookAndFeelInfo[i].getName();

         if (selectedIdx == -1 && lookAndFeelInfo[i].getClassName().equals(current))
         {
            selectedIdx = i;
            currentLookAndFeelInfo = lookAndFeelInfo[i];
         }
      }

      lookAndFeelBox = new JComboBox<String>(names);
      lookAndFeelLabel.setLabelFor(lookAndFeelBox);
      lookAndFeelBox.setSelectedIndex(selectedIdx);
      panel.add(lookAndFeelBox);

      return panel;
   }

   private PrepareAssignmentUpload main;
   private FileFieldButton customFieldButton;

   private JRadioButton homeDirButton, lastDirButton, customDirButton;
   private JTextField customField;

   private JSpinner timeoutSpinner, maxFileSearchSpinner;
   private SpinnerNumberModel timeoutModel, maxFileSearchModel;
   private JLabel timeoutDisabledField;

   private JComboBox<String> lookAndFeelBox;

   private UIManager.LookAndFeelInfo currentLookAndFeelInfo;
   private UIManager.LookAndFeelInfo[] lookAndFeelInfo;
}

