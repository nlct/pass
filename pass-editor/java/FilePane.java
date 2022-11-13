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

import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.URI;

import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.undo.*;

import com.dickimawbooks.passlib.*;

public class FilePane extends JEditorPane 
  implements DocumentListener,CaretListener
{
   public FilePane(PassEditor gui, File file, String filename)
     throws IOException
   {
      this(gui, file, filename, ProjectFileType.RESOURCE);
   }

   public FilePane(PassEditor gui, File file, String filename, boolean isBinary,
     String mimetype)
     throws IOException
   {
      this(gui, file, filename, ProjectFileType.RESOURCE, isBinary, null, mimetype);
   }

   public FilePane(PassEditor gui, File file, String filename, boolean isBinary,
     URI original, String mimetype)
     throws IOException
   {
      this(gui, file, filename, ProjectFileType.RESOURCE, isBinary, original, mimetype);
   }

   public FilePane(PassEditor gui, File file, String filename, ProjectFileType type)
     throws IOException
   {
      this(gui, file, filename, type, false, null, "text/x-source");
   }

   public FilePane(PassEditor gui, ProjectFile projectFile, String filename, ProjectFileType type)
     throws IOException
   {
      this(gui, projectFile, filename, type, null);
   }

   public FilePane(PassEditor gui, ProjectFile projectFile, String filename,
     ProjectFileType type, URI original)
     throws IOException
   {
      this(gui, projectFile, filename, type, !projectFile.isText(), 
        original, projectFile.getMimeType());
   }

   public FilePane(PassEditor gui, File file, String filename, 
    ProjectFileType type, boolean isBinary, String mimetype)
     throws IOException
   {
      this(gui, new ProjectFile(file), filename, type, isBinary, mimetype);
   }

   public FilePane(PassEditor gui, File file, String filename, 
    ProjectFileType type, boolean isBinary, URI original, String mimetype)
     throws IOException
   {
      this(gui, new ProjectFile(file), filename, type, isBinary, !isBinary, original, mimetype);
   }

   public FilePane(PassEditor gui, ProjectFile projectFile, String filename, ProjectFileType type, boolean isBinary, String mimetype)
     throws IOException
   {
      this(gui, projectFile, filename, type, isBinary, !isBinary, null, mimetype);
   }

   public FilePane(PassEditor gui, ProjectFile projectFile, String filename, 
     ProjectFileType type, boolean isBinary, URI original, String mimetype)
     throws IOException
   {
      this(gui, projectFile, filename, type, isBinary, !isBinary, original, mimetype);
   }

   public FilePane(PassEditor gui, ProjectFile projectFile, String filename,
     ProjectFileType type, boolean isBinary, boolean autoOpen)
     throws IOException
   {
      this(gui, projectFile, filename, type, isBinary, autoOpen, null, 
       projectFile.getMimeType());
   }

   public FilePane(PassEditor gui, ProjectFile projectFile, String filename,
      ProjectFileType type, boolean isBinary, boolean autoOpen, URI original,
      String mimetype)
     throws IOException
   {
      super("text/x-source", "");
      this.gui = gui;
      this.projectFile = projectFile;
      this.type = type;
      this.isBinary = isBinary;
      this.original = original;
      this.mimetype = mimetype;

      Document doc = getDocument();

      if (type == ProjectFileType.RESOURCE || type == ProjectFileType.RESULT)
      {
         setEditable(false);
      }
      else
      {
         undoManager = new FilePaneUndoManager();
         doc.addUndoableEditListener(undoManager);
      }

      setName(filename);

      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
      setFont(gui.getEditorFont());

      addMouseListener(gui);

      if (!isBinary)
      {
         addCaretListener(this);

         doc.addDocumentListener(this);
      }

      lines = new Vector<Position>();
      lines.add(doc.getStartPosition());

      if (autoOpen)
      {
         readFile();
      }
   }

   @Override
   public boolean getScrollableTracksViewportWidth()
   {
      return true;
   }

   public boolean isBinary()
   {
      return isBinary;
   }

   public String getMimeType()
   {
      return mimetype;
   }

   public boolean allowDelete()
   {
      switch (type)
      {
         case OPTIONAL:
         case RESULT:
           return true;
      }

      return false;
   }

   public void reload() throws IOException
   {
      if (isModified())
      {
         if (gui.confirm(
              gui.getMessage("confirm.discard", getName()),
              gui.getMessage("confirm.discard.title"), JOptionPane.YES_NO_OPTION)
             != JOptionPane.YES_OPTION
            )
         {
            return;
         }
      }

      if (original != null)
      {
         String msg;
         File file = getFile();
         File backup = null;

         if (isEditable())
         {
            backup = new File(file.getParentFile(), file.getName()+"~");

            msg = gui.getMessage("filepane.query_download", getName(), backup);
         }
         else
         {
            msg = gui.getMessage("filepane.query_download.readonly", getName());
         }

         switch (gui.confirm(msg, gui.getMessage("confirm.title"),
                    JOptionPane.YES_NO_CANCEL_OPTION))
         {
            case JOptionPane.YES_OPTION:

              if (backup != null)
              {
                 Files.copy(file.toPath(), backup.toPath(), 
                   StandardCopyOption.REPLACE_EXISTING);
              }

              gui.refetch(original.toURL(), file);
            break;
            case JOptionPane.NO_OPTION:
            break;
            default:
              return;
         }
      }

      gui.messageLn(gui.getMessage("filepane.reloading", getName()));
      readFile();
   }

   public void readFile() throws IOException
   {
      clearUndoRedo();

      if (isBinary)
      {
         return;
      }

      int lineNum = currentLine+1;

      currentLine = 0;
      currentColumn = 0;
      currentPosition = 0;

      isLoading = true;
      setText("");
      setCaretPosition(0);
      lines.setSize(1);

      File file = getFile();

      long fileSize = file.length();

      BufferedReader reader = null;

      try
      {
         reader = gui.getPassTools().newBufferedReader(file);

         // file size in bytes, 2 bytes = 1 char
         StringBuilder builder = new StringBuilder((int)(fileSize*2));

         int chr;

         while ((chr = reader.read()) != -1)
         {
            builder.append((char)chr);
         }

         setText(builder.toString());
      }
      finally
      {
         isLoading = false;

         if (reader != null)
         {
            reader.close();
         }
      }

      goToLine(lineNum);
      setModified(false);

      getPassEditor().updateEditTools(isEditable(), false, false, false);
   }

   public ProjectFileType getType()
   {
      return type;
   }

   public void setNode(EditorNode node)
   {
      this.node = node;
      node.setLanguage(projectFile.getLanguage());
      node.updatePosition();
   }

   public EditorNode getNode()
   {
      return node;
   }

   public File getFile()
   {
      return projectFile.getFile();
   }

   public ProjectFile getProjectFile()
   {
      return projectFile;
   }

   // check if file can be changed before calling
   public void setFile(File file) throws IOException
   {
      if (file.isDirectory())
      {
         throw new IOException(gui.getMessage("error.is_dir", file));
      }

      projectFile.setFile(file);

      String name = file.getName();

      if (node != null)
      {
         NavigationTreeNode parent = node;

         while ((parent = (NavigationTreeNode)parent.getParent()) != null)
         {
            String parentName = parent.getName();

            if (parentName != null)
            {
               name = String.format("%s/%s", parentName, name);
            }
         }
      }

      setName(name);
   }

   @Override
   public String toString()
   {
      return getFile().getName();
   }

   public String getLanguage()
   {
      return projectFile.getLanguage();
   }

   public void setLanguage(ListingLanguage listLang)
   {
      setLanguage(listLang.getLanguage());
   }

   public void setLanguage(String language)
   {
      projectFile.setLanguage(language);
   }

   public String getEncoding()
   {
      return gui.getEncoding();
   }

   public PassEditor getPassEditor()
   {
      return gui;
   }

   public void setModified(boolean modified)
   {
   }

   public boolean isModified()
   {
      return false;
   }

   public boolean canUndo()
   {
      return undoManager == null ? false : undoManager.canUndo();
   }

   public boolean canRedo()
   {
      return undoManager == null ? false : undoManager.canRedo();
   }

   public void undo()
   {
      if (undoManager != null) undoManager.undo();
   }

   public void redo()
   {
      if (undoManager != null) undoManager.redo();
   }

   public void clearUndoRedo()
   {
      if (undoManager != null)
      {
         undoManager.discardAllEdits();
      }
   }

   public void updateUndoRedoActions()
   {
      gui.updateEditTools(isEditable(), canUndo(), canRedo(), 
        getSelectedText() != null);
   }

   @Override
   public void replaceSelection(String content)
   {
      if (compoundEdit == null)
      {
         compoundEdit = new CompoundEdit();
         super.replaceSelection(content);
         compoundEdit.end();

         undoManager.addEdit(compoundEdit);
         updateUndoRedoActions();

         compoundEdit = null;
      }
      else
      {
         super.replaceSelection(content);
      }
   }

   public void replaceAll(String content)
   {
      if (compoundEdit == null)
      {
         compoundEdit = new CompoundEdit();
         setText(content);
         compoundEdit.end();

         undoManager.addEdit(compoundEdit);
         updateUndoRedoActions();

         compoundEdit = null;
      }
      else
      {
         setText(content);
      }
   }

   protected void documentChanged(DocumentEvent evt)
   {
      setModified(true);
   }

   @Override
   public void changedUpdate(DocumentEvent evt)
   {
   }

   @Override
   public void insertUpdate(DocumentEvent evt)
   {
      try
      {
         Document doc = evt.getDocument();
         int offset = evt.getOffset();
         int n = evt.getLength();

         String text = doc.getText(offset, n);

         for (int i = 0; i < n; )
         {
            int cp = text.codePointAt(i);

            if (cp == '\n')
            {
               currentLine++;
               currentColumn = 0;
               lines.add(currentLine, doc.createPosition(offset+i));
            }
            else
            {
               currentColumn++;
            }

            i += Character.charCount(cp);
         }

         currentPosition = offset+n;
      }
      catch (BadLocationException e)
      {
      }

      documentChanged(evt);
   }

   @Override
   public void removeUpdate(DocumentEvent evt)
   {
      int length = evt.getDocument().getLength();

      if (length == 0)
      {
         lines.setSize(1);

         currentLine = 0;
         currentColumn = 0;
         currentPosition = 0;

         return;
      }

      boolean cr = false;

      try
      {
         cr = evt.getDocument().getText(evt.getOffset(), 1).charAt(0) == '\n';
      }
      catch (BadLocationException e)
      {
         throw new AssertionError(evt.getOffset());
      }

      Position currP = lines.get(currentLine);

      int prevOffset = (currentLine == lines.size()-1 ? length : 
         lines.get(currentLine+1).getOffset());

      for (int i = currentLine; i > 0; i--)
      {
         Position p = lines.get(i);

         if (p.getOffset() == evt.getOffset())
         {
            if (cr)
            {
               cr = false;
            }
            else
            {
               lines.remove(i);
            }
         }
         else if (p.getOffset() <= evt.getOffset() && evt.getOffset() < prevOffset)
         {
            currentLine = i;
            currP = p;
         }
         else if (evt.getOffset() > p.getOffset())
         {
            break;
         }
      }

      while (lines.size() > 1 && currentLine < lines.size()-1)
      {
         Position p = lines.get(currentLine+1);

         int nextOffset;
         int nextLine = currentLine+2;

         if (nextLine < lines.size())
         {
            nextOffset = lines.get(nextLine).getOffset();
         }
         else
         {
            nextOffset = length;
         }

         if (p.getOffset() == evt.getOffset())
         {
            if (cr)
            {
               cr = false;
            }
            else
            {
               lines.remove(p);
            }
         }
         else if (p.getOffset() <= evt.getOffset() && evt.getOffset() < nextOffset)
         {
            currentLine++;
            currP = p;
            break;
         }
         else
         {
            break;
         }
      }

      if (currentLine == 0)
      {
         currentColumn = currentPosition;
      }
      else
      {
         currentColumn = currentPosition - currP.getOffset() - 1;
      }

      documentChanged(evt);
   }

   @Override
   public void caretUpdate(CaretEvent evt)
   {
      int dot = evt.getDot();

      getPassEditor().updateEditTools(isEditable(),
         canUndo(), canRedo(), dot != evt.getMark());

      Position lastLine = lines.lastElement();

      if (currentPosition == dot)
      {// no change
      }
      else if (dot == 0)
      {
         currentPosition = 0;
         currentLine = 0;
         currentColumn = 0;
      }
      else if (lines.size() == 1 || dot <= lines.get(1).getOffset())
      {// first line
         currentLine = 0;
         currentColumn = dot;
         currentPosition = dot;
      }
      else if (dot > lastLine.getOffset())
      {// last line
         currentLine = lines.size()-1;
         currentColumn = dot - lastLine.getOffset()-1;
         currentPosition = dot;
      }
      else if (currentPosition > dot)
      {
         int prevOffset;

         if (currentLine == lines.size()-1)
         {
            prevOffset = getDocument().getEndPosition().getOffset()-1;
         }
         else
         {
            prevOffset = lines.get(currentLine+1).getOffset();
         }

         for (int i = currentLine; i > 0; i--)
         {
            Position pos = lines.get(i);

            if (dot > pos.getOffset() && dot <= prevOffset)
            {
               currentLine = i;
               currentColumn = dot - pos.getOffset() - 1;
               currentPosition = dot;

               break;
            }

            prevOffset = pos.getOffset();
         }
      }
      else
      {
         assert currentPosition < dot;

         Position prevPos = lines.get(currentLine);
         
         for (int i = currentLine+1; i < lines.size(); i++)
         {
            Position pos = lines.get(i);

            if (dot > prevPos.getOffset() && dot <= pos.getOffset())
            {
               currentLine = i-1;
               currentColumn = dot - prevPos.getOffset() - 1;
               currentPosition = dot;

               break;
            }

            prevPos = pos;
         }
      }

      if (node != null)
      {
         node.updatePosition();
      }
   }

   public void goToLine(int line)
   {
      goToLine(line, 0);
   }

   public void goToLine(int line, int col)
   {
      if (line <= 1)
      {
         if (getCaretPosition() == 0)
         {
            currentPosition = 0;
            currentLine = 0;
            currentColumn = 0;

            if (node != null)
            {
               node.updatePosition();
            }
         }
         else
         {
            setCaretPosition(0);
         }
      }
      else
      {
         Position pos;
         int idx = line-1;

         if (idx >= lines.size())
         {
            pos = lines.lastElement();
         }
         else
         {
            pos = lines.get(idx);
         }

         setCaretPosition(pos.getOffset()+1+col);
      }
   }

   public int getLineCount()
   {
      return lines.size();
   }

   public int getCurrentLine()
   {
      return currentLine+1;
   }

   public int getCurrentColumn()
   {
      return currentColumn;
   }

   public int getCurrentPosition()
   {
      return currentPosition;
   }

   class FilePaneUndoManager extends UndoManager
   {
      @Override
      public void undoableEditHappened(UndoableEditEvent evt)
      {
         if (isLoading) return;

         UndoableEdit edit = evt.getEdit();

         if (compoundEdit == null)
         {
            addEdit(edit);
         }
         else
         {
            compoundEdit.addEdit(edit);
         }

         updateUndoRedoActions();
      }
   }

   protected Vector<Position> lines;
   protected int currentLine=0, currentColumn=0, currentPosition=0;

   private PassEditor gui;
   private ProjectFile projectFile;
   private EditorNode node;
   private ProjectFileType type;
   private boolean isBinary = false;
   private URI original = null;
   private String mimetype;

   protected UndoManager undoManager;
   protected CompoundEdit compoundEdit = null;
   protected boolean isLoading = false;
}
