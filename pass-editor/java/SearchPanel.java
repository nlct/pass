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
import java.util.regex.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class SearchPanel extends JPanel implements ActionListener,DocumentListener
{
   public SearchPanel(EditorNode node)
   {
      super();
      this.node = node;

      node.getEditor().getDocument().addDocumentListener(this);

      criteria = new SearchCriteria();
      foundPositions = new Vector<SearchLocation>();

      findField = new JTextField(12);
      findField.getDocument().addDocumentListener(this);
      add(findField);

      add(node.createIconButton("navigation/Up", "findprev", this, 
       getMessage("find.findprev")));

      add(node.createIconButton("navigation/Down", "findnext", this, 
       getMessage("find.findnext")));

      add(node.createIconButton("general/Preferences", "settings", this,
       getMessage("find.settings")));

      add(node.createCloseButton(this));

      infoField = new JLabel();
      infoField.setBorder(BorderFactory.createEmptyBorder());
      add(infoField);
   }

   protected String getMessage(String label, Object... params)
   {
      return node.getEditor().getPassEditor().getMessage(label, params);
   }

   public String getSearchText()
   {
      return findField.getText();
   }

   public FilePane getFilePane()
   {
      return node.getEditor();
   }

   public Document getDocument()
   {
      return node.getEditor().getDocument();
   }

   public int getCaretPosition()
   {
      return node.getEditor().getCaretPosition();
   }

   protected void updateFindNext()
   {
      node.getEditor().getPassEditor().enableFindNextPrev(true);
   }

   @Override
   public void changedUpdate(DocumentEvent evt)
   {
      updateRequired = true;
   }

   @Override
   public void insertUpdate(DocumentEvent evt)
   {
      updateRequired = true;
   }

   @Override
   public void removeUpdate(DocumentEvent evt)
   {
      updateRequired = true;
   }

   public void apply(SearchCriteria criteria)
   {
      if (!this.criteria.equals(criteria))
      {
         this.criteria.set(criteria);
         updateRequired = true;
      }
   }

   public int replaceAll(String needle, String replacement, SearchCriteria criteria)
      throws PatternSyntaxException
   {
      FilePane filePane = node.getEditor();

      findField.setText(needle);
      apply(criteria);
      updateFindNext();

      String haystack = filePane.getText();

      if (haystack.isEmpty())
      {
         return 0;
      }

      Pattern pattern = getPattern(needle);

      Matcher matcher = pattern.matcher(haystack);

      int caret = filePane.getCaretPosition();

      int total = 0;

      StringBuilder builder = new StringBuilder(haystack.length());

      while (matcher.find())
      {
         matcher.appendReplacement(builder, replacement);
         total++;
      }

      if (total == 0) return 0;

      matcher.appendTail(builder);

      String result = builder.toString();

      filePane.replaceAll(result);

      if (caret > result.length())
      {
         caret = result.length();
      }

      filePane.setCaretPosition(caret);

      return total;
   }

   public boolean replace(String needle, String replacement,
     SearchCriteria criteria, SearchResult result)
   throws PatternSyntaxException
   {
      if (!(result == null
        || !findField.getText().equals(needle)
        || foundPositions.isEmpty()
        || currentFoundIndex >= foundPositions.size()
        || !criteria.equals(this.criteria)))
      {
         int caret = getCaretPosition();

         String selectedText = node.getEditor().getSelectedText();
         SearchLocation loc = foundPositions.get(currentFoundIndex);

         if (caret == loc.getStartOffset() || caret == loc.getEndOffset())
         {
            if (selectedText == null || !loc.getMatch().equals(selectedText))
            {
               loc.highlight();
               selectedText = node.getEditor().getSelectedText();
            }

            if (result.isMatch(selectedText, criteria))
            {
               try
               {
                  replaceSelected(needle, selectedText, replacement);
                  foundPositions.remove(currentFoundIndex);

                  if (currentFoundIndex >= foundPositions.size())
                  {
                     currentFoundIndex = 0;
                  }

                  return true;
               }
               catch (PatternSyntaxException e)
               {
               }
            }
         }
      }

      findField.setText(needle);
      apply(criteria);

      result = searchNext();

      if (result == null) return false;

      replaceSelected(needle, result.getMatched(), replacement);
      foundPositions.remove(currentFoundIndex);

      if (currentFoundIndex >= foundPositions.size())
      {
         currentFoundIndex = 0;
      }

      return true;
   }

   private void replaceSelected(String needle, String selectedText, String replacement)
      throws PatternSyntaxException
   {
      switch (criteria.getMatch())
      {
         case EXACT:
            getFilePane().replaceSelection(replacement);
         break;
         case REGEX:
            Pattern pattern = getPattern(needle);
            Matcher matcher = pattern.matcher(selectedText);

            getFilePane().replaceSelection(matcher.replaceFirst(replacement));
         break;
      }
   }

   public SearchResult search(String text, SearchCriteria criteria)
   {
      findField.setText(text);
      apply(criteria);
      updateFindNext();

      updateFoundPositions();

      if (foundPositions.isEmpty())
      {
         return null;
      }

      SearchLocation loc = foundPositions.get(currentFoundIndex);

      loc.highlight();

      return new SearchResult(currentFoundIndex, foundPositions.size(),
        loc.getStartOffset(), loc.getMatch(), criteria);
   }

   public SearchResult searchNext()
   {
      if (updateRequired)
      {
         updateFoundPositions(getCaretPosition());
      }

      switch (criteria.getDirection())
      {
         case FORWARDS:
           searchForwards();
         break;
         case BACKWARDS:
           searchBackwards();
         break;
      }

      if (foundPositions.isEmpty())
      {
         return null;
      }

      SearchLocation loc = foundPositions.get(currentFoundIndex);

      return new SearchResult(currentFoundIndex, foundPositions.size(),
        loc.getStartOffset(), loc.getMatch(), criteria);
   }

   public SearchResult searchPrevious()
   {
      if (updateRequired)
      {
         updateFoundPositions(getCaretPosition());
      }

      switch (criteria.getDirection())
      {
         case FORWARDS:
           searchBackwards();
         break;
         case BACKWARDS:
           searchForwards();
         break;
      }

      if (foundPositions.isEmpty())
      {
         return null;
      }

      SearchLocation loc = foundPositions.get(currentFoundIndex);

      return new SearchResult(currentFoundIndex, foundPositions.size(),
        loc.getStartOffset(), loc.getMatch(), criteria);
   }

   protected void searchForwards()
   {
      if (foundPositions.isEmpty())
      {
         infoField.setText(getMessage("find.not_found"));
         return;
      }

      String needle = findField.getText();

      if (needle.isEmpty())
      {
         infoField.setText("");
         return;
      }

      if (foundPositions.size() == 1)
      {
         foundPositions.get(0).highlight();
         updateInfoField();
         return;
      }

      int caret = getCaretPosition();

      int nextFoundIndex = (currentFoundIndex+1)%foundPositions.size();

      SearchLocation prevLoc = foundPositions.get(currentFoundIndex);
      SearchLocation nextLoc = foundPositions.get(nextFoundIndex);
      SearchLocation loc = null;

      int nextPos = nextLoc.getStartOffset();
      int prevPos = prevLoc.getStartOffset();

      if ((prevPos <= caret && caret < nextPos)
         || (nextPos < prevPos
               && (caret >= prevPos || caret < nextPos)))
      {
         currentFoundIndex = nextFoundIndex;
         loc = nextLoc;
      }
      else
      {
         // caret has moved to a different part of the document

         SearchLocation lastLoc = foundPositions.lastElement();

         if (caret >= lastLoc.getStartOffset())
         {
            currentFoundIndex = 0;
            loc = foundPositions.firstElement();
         }
         else
         {
            for (int i = 0; i < foundPositions.size(); i++)
            {
               loc = foundPositions.get(i);
               currentFoundIndex = i;

               if (caret < loc.getStartOffset())
               {
                  break;
               }
            }
         }
      }

      assert loc != null;

      loc.highlight();

      updateInfoField();
   }

   protected void searchBackwards()
   {
      if (foundPositions.isEmpty())
      {
         infoField.setText(getMessage("find.not_found"));
         return;
      }

      String needle = findField.getText();

      if (needle.isEmpty())
      {
         infoField.setText("");
         return;
      }

      if (foundPositions.size() == 1)
      {
         foundPositions.get(0).highlight();
         updateInfoField();
         return;
      }

      int caret = getCaretPosition();

      int nextFoundIndex = (foundPositions.size()+currentFoundIndex-1)
                         % foundPositions.size();

      SearchLocation prevLoc = foundPositions.get(currentFoundIndex);
      SearchLocation nextLoc = foundPositions.get(nextFoundIndex);
      SearchLocation loc = null;

      int nextPos = nextLoc.getStartOffset();
      int prevPos = prevLoc.getEndOffset();

      if ((nextPos < caret && caret <= prevPos)
         || (prevPos < nextPos && (caret <= prevPos || caret > nextPos)))
      {
         currentFoundIndex = nextFoundIndex;
         loc = nextLoc;
      }
      else
      {
         // caret has moved to a different part of the document

         SearchLocation firstLoc = foundPositions.firstElement();

         if (caret <= firstLoc.getEndOffset())
         {
            currentFoundIndex = foundPositions.size()-1;
            loc = foundPositions.lastElement();
         }
         else
         {
            for (int i = foundPositions.size()-1; i >= 0; i--)
            {
               loc = foundPositions.get(i);
               currentFoundIndex = i;

               if (caret > loc.getEndOffset())
               {
                  break;
               }
            }
         }
      }

      assert loc != null;

      loc.highlight();

      updateInfoField();
   }

   protected void updateFoundPositions()
   {
      int caret = 0;

      if (criteria.getFrom() == SearchCriteria.From.CURSOR)
      {
         caret = getCaretPosition();
      }

      updateFoundPositions(caret);
   }

   protected void updateFoundPositions(int caret)
   {
      updateRequired = false;

      foundPositions.clear();
      currentFoundIndex = 0;

      infoField.setText("");
      String needle = findField.getText();

      if (needle.isEmpty()) return;

      if (getDocument().getLength() == 0)
      {
         infoField.setText(getMessage("find.not_found"));
         return;
      }

      try
      {
         updateRegExFoundPositions(needle, caret);

         updateInfoField();
      }
      catch (PatternSyntaxException e)
      {
         node.getEditor().getPassEditor().error(e);
      }
   }

   protected Pattern getPattern(String needle)
      throws PatternSyntaxException
   {
      int flags = Pattern.UNIX_LINES | Pattern.MULTILINE;

      switch (criteria.getCase())
      {
         case INSENSITIVE:
            flags = flags | Pattern.CASE_INSENSITIVE;
         break;
         case SENSITIVE:
         break;
      }

      switch (criteria.getMatch())
      {
         case EXACT:
            flags = flags | Pattern.LITERAL;
         break;
         case REGEX:
         break;
      }

      return Pattern.compile(needle, flags);
   }

   protected void updateRegExFoundPositions(String needle, int caret)
      throws PatternSyntaxException
   {
      Pattern pattern = getPattern(needle);

      Matcher matcher = pattern.matcher(node.getEditor().getText());

      while (matcher.find())
      {
         try
         {
            SearchLocation loc = new SearchLocation(matcher.start(), matcher.end());

            foundPositions.add(loc);

            if (caret > loc.getStartOffset())
            {
               currentFoundIndex = foundPositions.size();
            }
         }
         catch (BadLocationException e)
         {// shouldn't happen
         }
      }

      int n = foundPositions.size();

      if (currentFoundIndex == n)
      {
         currentFoundIndex = 0;
      }

      if (criteria.getDirection() == SearchCriteria.Direction.BACKWARDS)
      {
         currentFoundIndex = (n + currentFoundIndex - 1) % n;
      }
   }

   private void updateInfoField()
   {
      if (foundPositions.isEmpty())
      {
         infoField.setText(getMessage("find.not_found"));
      }
      else
      {
         infoField.setText(getMessage("find.brief_found",
           currentFoundIndex+1, foundPositions.size()));
      }
   }

   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null) return;

      if (command.equals("findnext"))
      {
         searchNext();
      }
      else if (command.equals("findprev"))
      {
         searchPrevious();
      }
      else if (command.equals("settings"))
      {
         node.getEditor().getPassEditor().showSearchDialog(node.getEditor(), 
           getSearchText(), criteria);
      }
      else if (command.equals("close"))
      {
         setVisible(false);
      }
   }

   public SearchCriteria getCriteria()
   {
      return criteria;
   }

   class SearchLocation
   {
      public SearchLocation(int startIdx, int endIdx)
        throws BadLocationException
      {
         this.startIdx = startIdx;
         this.endIdx = endIdx;

         text = getDocument().getText(startIdx, endIdx-startIdx);
      }

      public int getStartOffset()
      {
         return startIdx;
      }

      public int getEndOffset()
      {
         return endIdx;
      }

      public boolean isInside(int caret)
      {
         return startIdx <= caret && caret <= endIdx;
      }

      public void highlight()
      {
         FilePane filePane = node.getEditor();
         filePane.setSelectionStart(startIdx);
         filePane.setSelectionEnd(endIdx);
         filePane.requestFocusInWindow();
      }

      public String getMatch()
      {
         return text;
      }

      public String toString()
      {
         return String.format("%s[start=%d,end=%d,text=%s]", 
             getClass().getSimpleName(), startIdx, endIdx, text);
      }

      int startIdx, endIdx;
      String text;
   }

   private EditorNode node;
   private JTextField findField;
   private JLabel infoField;
   private SearchCriteria criteria;

   private Vector<SearchLocation> foundPositions;
   private int currentFoundIndex = 0;
   private boolean updateRequired;
}
