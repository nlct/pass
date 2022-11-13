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

import java.util.Enumeration;
import java.util.regex.PatternSyntaxException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.MutableTreeNode;

import com.dickimawbooks.passlib.AssignmentData;
import com.dickimawbooks.passlib.PassTools;

public class EditorNode implements NavigationTreeNode,ItemListener,ActionListener
{
   public EditorNode(FilePane editor)
   {
      super();

      this.editor = editor;
      scrollPane = new JScrollPane(editor);

      JPanel headerPane = new JPanel(new BorderLayout());
      headerPane.setAlignmentY(0.5f);

      JPanel leftPanel = new JPanel();
      leftPanel.setAlignmentY(0.5f);
      headerPane.add(leftPanel, "West");

      PassTools passTools = editor.getPassEditor().getPassTools();

      JComponent labelComp;

      switch (editor.getType())
      {
         case REQUIRED:
            labelComp = new JLabel(passTools.getMessage("filepane.required"));
            langBox = new JComboBox<ListingLanguage>(
              editor.getPassEditor().getListingLanguages());
         break;
         case OPTIONAL:
            labelComp = new JLabel(passTools.getMessage("filepane.additional"));
            langBox = new JComboBox<ListingLanguage>(
               editor.getPassEditor().getListingLanguages());
         break;
         case RESOURCE:
            labelComp = new JLabel(passTools.getMessage("filepane.resource"));
         break;
         case RESULT:
            labelComp = new JLabel(passTools.getMessage("filepane.result"));
         break;
         default:
           throw new AssertionError(editor.getType());
      }

      labelComp.setAlignmentY(0.5f);
      leftPanel.add(labelComp);

      if (langBox != null)
      {
/*
         JLabel langLabel = new JLabel("Language:");
         langLabel.setAlignmentY(0.5f);
         langLabel.setDisplayedMnemonic('L');
         leftPanel.add(langLabel);
*/

         langBox.setAlignmentY(0.5f);
         langBox.setName("language");
         langBox.addItemListener(this);
         langBox.setToolTipText(passTools.getMessage("filepane.language"));
         //langLabel.setLabelFor(langBox);
         leftPanel.add(langBox);
      }

      modified = false;

      fileTabComponent = new FileTabComponent(this);

      if (editor.isBinary())
      {
         JComponent rightPanel = new JPanel();
         headerPane.add(rightPanel, "East");

         rightPanel.add(new JLabel(passTools.getMessage("filepane.binary",
           editor.getMimeType())));

         JButton viewButton = createIconButton("general/Zoom", "view", 
           KeyStroke.getKeyStroke(KeyEvent.VK_V,
              InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
           passTools.getMessage("filepane.view"));

         rightPanel.add(viewButton);
      }
      else
      {
         positionLabel = new JLabel();
         positionLabel.setAlignmentY(0.5f);
         positionLabel.setHorizontalAlignment(SwingConstants.CENTER);
         headerPane.add(positionLabel, "East");

         findComponent = new SearchPanel(this);
         findComponent.setVisible(false);
         headerPane.add(findComponent, "Center");
      }

      scrollPane.setColumnHeaderView(headerPane);

      editor.setNode(this);
   }

   public JButton createIconButton(String iconName, String action, String description)
   {
      return createJButton(
        editor.getPassEditor().getToolIcon(iconName, description, true), 
        action, null, false, description);
   }

   public JButton createIconButton(String iconName, String action,
      ActionListener listener, String description)
   {
      return createJButton(
        editor.getPassEditor().getToolIcon(iconName, description, true), 
        action, listener, null, false, description);
   }

   public JButton createIconButton(String iconName, String action,
      KeyStroke keyStroke, String description)
   {
      return createJButton(
        editor.getPassEditor().getToolIcon(iconName, description, true), 
        action, this, keyStroke, false, description);
   }

   public JButton createJButton(String label, String action)
   {
      return createJButton(label, -1, action);
   }

   public JButton createJButton(String label, int mnemonic, String action)
   {
      return createJButton(label, mnemonic, action, this);
   }

   public JButton createJButton(String label, int mnemonic, String action, ActionListener listener)
   {
      return editor.getPassEditor().createJButton(label, mnemonic, action, listener);
   }

   public JButton createJButton(Icon icon, String action)
   {
      return createJButton(icon, action, null, false, 
        editor.getPassEditor().getToolTipMessage("filepane."+action));
   }

   public JButton createJButton(Icon icon, String action, KeyStroke keyStroke,
      boolean isDefault, String toolTip)
   {
      return createJButton(icon, action, this, keyStroke, isDefault, toolTip);
   }

   public JButton createJButton(Icon icon, String action, ActionListener listener, 
      KeyStroke keyStroke, boolean isDefault, String toolTip)
   {
      JButton button = editor.getPassEditor().createJButton(icon, action, listener, 
        editor, keyStroke, isDefault, toolTip);

      //button.setContentAreaFilled(false);
      button.setMargin(new Insets(0,0,0,0));

      return button;
   }

   public JButton createCloseButton()
   {
      return createCloseButton(this);
   }

   public JButton createCloseButton(ActionListener listener)
   {
      JButton closeButton = createJButton("\u2715", -1, "close", listener);
      closeButton.setContentAreaFilled(false);
      closeButton.setMargin(new Insets(0,0,0,0));

      return closeButton;
   }

   public int replaceAll(String needle, String replacement, SearchCriteria criteria)
      throws PatternSyntaxException
   {
      if (findComponent != null)
      {
         return findComponent.replaceAll(needle, replacement, criteria);
      }

      return 0;
   }

   public boolean replace(String needle, String replacement, SearchCriteria criteria,
      SearchResult result)
      throws PatternSyntaxException
   {
      if (findComponent != null)
      {
         return findComponent.replace(needle, replacement, criteria, result);
      }

      return false;
   }

   public SearchResult search(String text, SearchCriteria criteria)
   {
      if (findComponent != null)
      {
         setSearchBoxVisible(true);
         return findComponent.search(text, criteria);
      }

      return null;
   }

   public SearchResult searchNext()
   {
      return searchNext(null);
   }

   public SearchResult searchNext(SearchCriteria criteria)
   {
      if (findComponent != null)
      {
         if (criteria != null)
         {
            findComponent.apply(criteria);
         }

         setSearchBoxVisible(true);
         return findComponent.searchNext();
      }

      return null;
   }

   public SearchResult searchPrevious()
   {
      return searchPrevious(null);
   }

   public SearchResult searchPrevious(SearchCriteria criteria)
   {
      if (findComponent != null)
      {
         if (criteria != null)
         {
            findComponent.apply(criteria);
         }

         setSearchBoxVisible(true);
         return findComponent.searchPrevious();
      }

      return null;
   }

   public void setSearchBoxVisible(boolean visible)
   {
      if (findComponent != null)
      {
         findComponent.setVisible(visible);
      }
   }

   public String getSearchTerm()
   {
      return findComponent == null ? "" : findComponent.getSearchText();
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null)
      {
         return;
      }

      if (command.equals("close"))
      {
         editor.getPassEditor().closeFileComponent(this);
      }
      else if (command.equals("view"))
      {
         try
         {
            editor.getPassEditor().getPassGuiTools().open(editor.getMimeType(),
               editor.getFile());
         }
         catch (Throwable e)
         {
            editor.getPassEditor().error(e);
         }
      }
   }

   @Override
   public void itemStateChanged(ItemEvent evt)
   {
      Object src = evt.getSource();

      if (src == langBox)
      {
         editor.setLanguage((ListingLanguage)langBox.getSelectedItem());
      }
   }

   public void updatePosition()
   {
      if (positionLabel != null)
      {
         positionLabel.setText(editor.getPassEditor().formatLocation(editor));
      }
   }

   public void setLanguage(ListingLanguage language)
   {
      if (langBox != null)
      {
         langBox.setSelectedItem(language);
      }
   }

   public void setLanguage(String language)
   {
      if (langBox != null)
      {
         for (int i = 0, n = langBox.getItemCount(); i < n; i++)
         {
            ListingLanguage item = langBox.getItemAt(i);

            if (item.getLanguage().equals(language))
            {
               langBox.setSelectedIndex(i);
               break;
            }
         }
      }
   }

   public void setModified(boolean modified)
   {
      if (this.modified == modified)
      {
         return;
      }

      this.modified = modified;

      fileTabComponent.updateTitle();

      editor.getPassEditor().setModified(modified, this);
   }

   public boolean isModified()
   {
      return modified;
   }

   public JScrollPane getScrollPane()
   {
      return scrollPane;
   }

   public FilePane getEditor()
   {
      return editor;
   }

   @Override
   public Path getPath()
   {
      return editor.getFile().toPath();
   }

   @Override
   public void setPath(Path path) throws IOException
   {
      editor.setFile(path.toFile());
   }

   @Override
   public String toString()
   {
      return editor.getFile().getName();
   }

   @Override
   public String getName()
   {
      return editor.getName();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj) return true;

      if (obj == null || !(obj instanceof EditorNode)) return false;

      EditorNode node = (EditorNode)obj;

      return editor.getFile().equals(node.editor.getFile());
   }

   @Override
   public Enumeration<NavigationTreeNode> children()
   {
      return null;
   }

   @Override
   public int getChildCount()
   {
      return 0;
   }

   @Override
   public TreeNode getChildAt(int childIndex)
   {
      return null;
   }

   @Override
   public int getIndex(TreeNode node)
   {
      return -1;
   }

   @Override
   public boolean getAllowsChildren()
   {
      return false;
   }

   @Override
   public boolean isLeaf()
   {
      return true;
   }

   @Override
   public TreeNode getParent()
   {
      return parent;
   }

   @Override
   public void insert(MutableTreeNode child, int index)
   {
   }

   @Override
   public void remove(int index)
   {
   }

   @Override
   public void remove(MutableTreeNode node)
   {
   }

   @Override
   public void removeFromParent()
   {
      if (parent != null)
      {
         parent.remove(this);
      }
   }

   @Override
   public void setParent(MutableTreeNode parent)
   {
      this.parent = parent;
   }

   @Override
   public boolean isFile(File file)
   {
      return editor.getFile().equals(file);
   }

   @Override
   public boolean isEditable()
   {
      return editor.getType() == ProjectFileType.OPTIONAL;
   }

   @Override
   public void setUserObject(Object object)
   {
      if (!(object instanceof FilePane))
      {
         throw new IllegalArgumentException("User object must be a FilePane for "
           + getClass().getSimpleName());
      }

      editor = (FilePane)object;
   }

   public FileTabComponent getTabLabelComponent()
   {
      return fileTabComponent;
   }

   @Override
   public void updateEditorFont(Font font)
   {
      editor.setFont(font);
   }

   private FilePane editor;
   private JScrollPane scrollPane;
   private MutableTreeNode parent;
   private JLabel positionLabel;
   private JComboBox<ListingLanguage> langBox;
   private FileTabComponent fileTabComponent;
   private boolean modified = false;
   private SearchPanel findComponent;
}
