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

import java.util.regex.PatternSyntaxException;

import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.*;

public class FindDialog extends JDialog 
  implements ActionListener,DocumentListener
{
   public FindDialog(PassEditor gui)
   {
      super(gui, gui.getMessage("find.title"), false);

      this.gui = gui;

      findTitle = getTitle();
      findReplaceTitle = gui.getMessage("find.replace.title");
      notFoundText = gui.getMessage("find.not_found");

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, "Center");

      JComponent panel = createRow();
      mainPanel.add(panel);

      JLabel label = createLabel("search");
      panel.add(label);

      searchBox = new JTextField(12);
      searchBox.getDocument().addDocumentListener(this);
      label.setLabelFor(searchBox);
      panel.add(searchBox);

      panel = createRow();
      mainPanel.add(panel);

      replaceComp = createRow();
      panel.add(replaceComp);

      replaceLabel = createLabel("replace_with");
      replaceComp.add(replaceLabel);

      replaceBox = new JTextField(12);
      replaceLabel.setLabelFor(replaceBox);
      replaceComp.add(replaceBox);

      panel.add(Box.createRigidArea(new Dimension(4,14)));

      infoField = new JLabel();
      infoField.setFont(searchBox.getFont());
      panel.add(infoField);

      panel = createRow();
      mainPanel.add(panel);

      regexBox = createCheckBox("regexp", "criteria");
      panel.add(regexBox);

      panel = createRow();
      mainPanel.add(panel);

      matchCaseBox = createCheckBox("match_case", "criteria");
      panel.add(matchCaseBox);

      panel = createRow();
      mainPanel.add(panel);

      ButtonGroup buttonGrp = new ButtonGroup();

      label = createLabel("search_from");
      panel.add(label);

      fromCursorButton = createRadioButton("cursor", buttonGrp, "criteria");
      panel.add(fromCursorButton);

      fromStartButton = createRadioButton("start", buttonGrp, "criteria");
      panel.add(fromStartButton);

      panel = createRow();
      mainPanel.add(panel);

      buttonGrp = new ButtonGroup();

      label = createLabel("direction");
      panel.add(label);

      forwardsButton = createRadioButton("forwards", buttonGrp, "criteria");
      panel.add(forwardsButton);

      backwardsButton = createRadioButton("backwards", buttonGrp, "criteria");
      panel.add(backwardsButton);

      searchCriteria = new SearchCriteria();
      applyCriteria(searchCriteria);

      JComponent bottomPanel = new JPanel(new BorderLayout());
      getContentPane().add(bottomPanel, "South");

      bottomPanel.add(gui.createHelpButton("find"), "East");

      JComponent buttonPanel = new JPanel();
      bottomPanel.add(buttonPanel, "Center");

      findCardLayout = new CardLayout();
      findCardComp = new JPanel(findCardLayout);

      buttonPanel.add(findCardComp);

      findButtonComp1 = createRow();

      findButton = createJButton("find", null, true, "general/Find");
      findButtonComp1.add(findButton);

      findCardComp.add(findButtonComp1);

      findButtonComp2 = createRow();

      findNextButton = createJButton("findnext", 
         KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), true, "navigation/Down");
      findButtonComp2.add(findNextButton);

      findPrevButton = createJButton("findprev", 
         KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), 
         false, "navigation/Up");
      findButtonComp2.add(findPrevButton);

      findCardComp.add(findButtonComp2);

      replaceAllButton = createJButton("replaceall", null, false);
      buttonPanel.add(replaceAllButton);

      replaceButton = createJButton("replace", null, false);
      buttonPanel.add(replaceButton);

      buttonPanel.add(createJButton("close",
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      pack();
      setLocationRelativeTo(gui);

      replaceComp.setVisible(false);
      replaceAllButton.setVisible(false);
      replaceButton.setVisible(false);

      regexBox.addActionListener(this);
      matchCaseBox.addActionListener(this);
      fromCursorButton.addActionListener(this);
      fromStartButton.addActionListener(this);
      forwardsButton.addActionListener(this);
      backwardsButton.addActionListener(this);
   }

   private JComponent createRow()
   {
      JComponent comp = Box.createHorizontalBox();
      comp.setAlignmentX(0.0f);
      return comp;
   }

   private JLabel createLabel(String propTail)
   {
      String propLabel = "find."+propTail;

      JLabel label = new JLabel(gui.getMessage(propLabel));
      label.setAlignmentX(0.0f);

      int mnemonic = gui.getMnemonic(propLabel);

      if (mnemonic != -1)
      {
         label.setDisplayedMnemonic(mnemonic);
      }

      return label;
   }

   private JCheckBox createCheckBox(String propTail)
   {
      return createCheckBox(propTail, null);
   }

   private JCheckBox createCheckBox(String propTail, String action)
   {
      String propLabel = "find."+propTail;

      JCheckBox box = new JCheckBox(gui.getMessage(propLabel));

      int mnemonic = gui.getMnemonic(propLabel);

      if (mnemonic != -1)
      {
         box.setMnemonic(mnemonic);
      }

      if (action != null)
      {
         box.setActionCommand(action);
      }

      box.setAlignmentX(0.0f);

      return box;
   }

   private JRadioButton createRadioButton(String propTail, ButtonGroup grp)
   {
      return createRadioButton(propTail, grp, null);
   }

   private JRadioButton createRadioButton(String propTail, ButtonGroup grp,
     String action)
   {
      String propLabel = "find."+propTail;

      JRadioButton button = new JRadioButton(gui.getMessage(propLabel));

      int mnemonic = gui.getMnemonic(propLabel);

      if (mnemonic != -1)
      {
         button.setMnemonic(mnemonic);
      }

      grp.add(button);
      button.setAlignmentX(0.0f);

      if (action != null)
      {
         button.setActionCommand(action);
      }

      return button;
   }

   private JButton createJButton(String action, KeyStroke keyStroke)
   {
      String propLabel = "find."+action;

      return gui.createJButton(gui.getMessage(propLabel), 
         gui.getMnemonic(propLabel), action, this, getRootPane(),
         keyStroke);
   }

   private JButton createJButton(String action, KeyStroke keyStroke, boolean isDefault)
   {
      return createJButton(action, keyStroke, isDefault, (Icon)null);
   }

   private JButton createJButton(String action,
     KeyStroke keyStroke, boolean isDefault, String toolIconName)
   {
      String propLabel = "find."+action;
      String text = gui.getMessage(propLabel);

      Icon icon = gui.getToolIcon(toolIconName, text, true);

      JButton button = gui.createJButton(text, 
         gui.getMnemonic(propLabel), action, this, getRootPane(),
         keyStroke, isDefault);

      if (icon != null)
      {
         button.setIcon(icon);
      }

      return button;
   }

   private JButton createJButton(String action,
     KeyStroke keyStroke, boolean isDefault, Icon icon)
   {
      String propLabel = "find."+action;

      JButton button = gui.createJButton(gui.getMessage(propLabel), 
         gui.getMnemonic(propLabel), action, this, getRootPane(),
         keyStroke, isDefault);

      if (icon != null)
      {
         button.setIcon(icon);
      }

      return button;
   }

   public void setReplace(boolean allow)
   {
      replace = allow;
      replaceComp.setVisible(allow);
      replaceAllButton.setVisible(allow);
      replaceButton.setVisible(false);

      setTitle(allow ? findReplaceTitle : findTitle);
   }

   public void enableReplace(boolean enable)
   {
      replaceLabel.setEnabled(enable);
      replaceBox.setEnabled(enable);
      replaceAllButton.setEnabled(enable);
      replaceButton.setEnabled(enable);
   }

   public void updateNextPrev()
   {
      showNextPrev(result != null);
   }

   private void showNextPrev(boolean visible)
   {
      if (visible)
      {
         findCardLayout.last(findCardComp);
      }
      else
      {
         findCardLayout.first(findCardComp);
      }

      if (visible && replace)
      {
         replaceButton.setVisible(true);
      }
      else
      {
         replaceButton.setVisible(false);
      }
   }

   private void criteriaChanged()
   {
      result = null;
      infoField.setText("");
      showNextPrev(false);
   }

   public void display(FilePane filePane)
   {
      display(filePane, filePane.getSelectedText(), null, false);
   }

   public void display(FilePane filePane, boolean allowReplace)
   {
      display(filePane, filePane.getSelectedText(), null, allowReplace);
   }

   public void display(FilePane filePane, String text)
   {
      display(filePane, text, null, false);
   }

   public void display(FilePane filePane, String text, SearchCriteria criteria)
   {
      display(filePane, text, criteria, replace);
   }

   public void display(FilePane filePane, String text, SearchCriteria criteria,
    boolean allowReplace)
   {
      this.filePane = filePane;

      infoField.setText("");

      enableReplace(filePane.isEditable());
      setReplace(allowReplace);

      if (text != null)
      {
         searchBox.setText(text);
      }

      if (criteria != null)
      {
         applyCriteria(criteria);
      }

      setVisible(true);

      searchBox.requestFocusInWindow();
   }

   public void updateFilePane(FilePane filePane)
   {
      this.filePane = filePane;

      enableReplace(filePane.isEditable());

      criteriaChanged();
   }

   public void applyCriteria(SearchCriteria criteria)
   {
      switch (criteria.getCase())
      {
         case SENSITIVE:
            matchCaseBox.setSelected(true);
         break;
         case INSENSITIVE:
            matchCaseBox.setSelected(false);
         break;
      }

      switch (criteria.getFrom())
      {
         case CURSOR:
            fromCursorButton.setSelected(true);
         break;
         case START:
            fromStartButton.setSelected(true);
         break;
      }

      switch (criteria.getMatch())
      {
         case EXACT:
           regexBox.setSelected(false);
         break;
         case REGEX:
           regexBox.setSelected(true);
         break;
      }

      switch (criteria.getDirection())
      {
         case FORWARDS:
            forwardsButton.setSelected(true);
         break;
         case BACKWARDS:
            backwardsButton.setSelected(true);
         break;
      }
   }

   public SearchCriteria getSearchCriteria()
   {
      if (matchCaseBox.isSelected())
      {
         searchCriteria.setCase(SearchCriteria.Case.SENSITIVE);
      }
      else
      {
         searchCriteria.setCase(SearchCriteria.Case.INSENSITIVE);
      }

      if (fromCursorButton.isSelected())
      {
         searchCriteria.setFrom(SearchCriteria.From.CURSOR);
      }
      else if (fromStartButton.isSelected())
      {
         searchCriteria.setFrom(SearchCriteria.From.START);
      }

      if (regexBox.isSelected())
      {
         searchCriteria.setMatch(SearchCriteria.Match.REGEX);
      }
      else
      {
         searchCriteria.setMatch(SearchCriteria.Match.EXACT);
      }

      if (forwardsButton.isSelected())
      {
         searchCriteria.setDirection(SearchCriteria.Direction.FORWARDS);
      }
      else if (backwardsButton.isSelected())
      {
         searchCriteria.setDirection(SearchCriteria.Direction.BACKWARDS);
      }

      return searchCriteria;
   }

   private void updateInfo()
   {
      if (result == null)
      {
         infoField.setText(notFoundText);
         showNextPrev(false);
      }
      else
      {
         infoField.setText("<html>"
           + gui.getMessage("find.found", 
           result.getFoundIndex()+1, result.getTotalMatches(), 
           "<strong>"+result.getMatched()+"</strong>")+"</html>");

         showNextPrev(true);
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null)
      {
         return;
      }

      if (command.equals("find"))
      {
         String needle = searchBox.getText();

         if (needle.isEmpty())
         {
            gui.error(this, gui.getMessage("error.no_search_term"));
            return;
         }

         result = filePane.getNode().search(
            needle, getSearchCriteria());

         updateInfo();

         filePane.requestFocus();
      }
      else if (command.equals("findnext"))
      {
         result = filePane.getNode().searchNext(getSearchCriteria());

         updateInfo();

         filePane.requestFocus();
      }
      else if (command.equals("findprev"))
      {
         result = filePane.getNode().searchPrevious(getSearchCriteria());

         updateInfo();

         filePane.requestFocus();
      }
      else if (command.equals("replace"))
      {
         String message = "";

         try
         {
            boolean found = filePane.getNode().replace(searchBox.getText(),
               replaceBox.getText(), getSearchCriteria(), result);

            if (found)
            {
               message = gui.getMessage("find.n_replaced", 1);
            }
            else
            {
               message = notFoundText;
            }
         }
         catch (PatternSyntaxException e)
         {
            gui.error(this, e.getMessage());
         }

         infoField.setText(message);
      }
      else if (command.equals("replaceall"))
      {
         String message = "";

         try
         {
            int total = filePane.getNode().replaceAll(searchBox.getText(),
               replaceBox.getText(), getSearchCriteria());

            if (total == 0)
            {
               message = notFoundText;
            }
            else
            {
               message = gui.getMessage("find.n_replaced", total);
            }

            result = null;
            showNextPrev(false);
         }
         catch (PatternSyntaxException e)
         {
            gui.error(this, e.getMessage());
         }

         infoField.setText(message);
      }
      else if (command.equals("close"))
      {
         setVisible(false);
      }
      else if (command.equals("criteria"))
      {
         criteriaChanged();
      }
   }

   @Override
   public void changedUpdate(DocumentEvent evt)
   {
      criteriaChanged();
   }

   @Override
   public void insertUpdate(DocumentEvent evt)
   {
      criteriaChanged();
   }

   @Override
   public void removeUpdate(DocumentEvent evt)
   {
      criteriaChanged();
   }

   private boolean replace = false;

   private PassEditor gui;
   private FilePane filePane;

   private JTextField searchBox, replaceBox;
   private JCheckBox matchCaseBox, regexBox;
   private JRadioButton fromCursorButton, fromStartButton,
    forwardsButton, backwardsButton;
   private JButton findButton, findNextButton, findPrevButton, replaceAllButton, replaceButton;
   private JLabel infoField, replaceLabel;

   private JComponent replaceComp, findButtonComp1, findButtonComp2, findCardComp;

   private CardLayout findCardLayout;

   private SearchCriteria searchCriteria;
   private SearchResult result;

   private String findTitle, findReplaceTitle, notFoundText;
}
