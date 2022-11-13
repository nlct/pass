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
import java.nio.file.Path;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.*;

public class FileTabComponent extends JPanel 
  implements PathReference,MouseListener,MouseMotionListener
{
   public FileTabComponent(EditorNode node)
   {
      super(new BorderLayout());
      this.node = node;

      setOpaque(false);

      tabLabel = new JLabel(node.getEditor().getName());
      tabLabel.setOpaque(false);
      add(tabLabel, "West");

      add(Box.createHorizontalStrut(GAP), "Center");

      JButton closeButton = node.createCloseButton();
      closeButton.setAlignmentY(0.5f);
      add(closeButton, "East");

      setTransferHandler(node.getEditor().getPassEditor().getNodeTransferHandler());

      addMouseListener(this);
      addMouseMotionListener(this);
   }

   @Override
   public void mouseDragged(MouseEvent evt)
   {
      if (checkDrag)
      {
         getTransferHandler().exportAsDrag(this, evt, TransferHandler.MOVE);
      }

      checkDrag = false;
   }

   @Override
   public void mouseMoved(MouseEvent evt)
   {
   }

   @Override
   public void mousePressed(MouseEvent evt)
   {
      checkDrag = true;
   }

   @Override
   public void mouseClicked(MouseEvent evt)
   {
      checkDrag = false;
      node.getEditor().getPassEditor().setSelectedFile(node);
   }

   @Override
   public void mouseReleased(MouseEvent evt)
   {
      checkDrag = false;
   }

   @Override
   public void mouseExited(MouseEvent evt)
   {
   }

   @Override
   public void mouseEntered(MouseEvent evt)
   {
   }

   @Override
   public boolean isFile(File file)
   {
      return node.isFile(file);
   }

   @Override
   public Path getPath()
   {
      return node.getPath();
   }

   public void updateTitle()
   {
      if (node.isModified())
      {
         tabLabel.setText(getName()+"*");
      }
      else
      {
         tabLabel.setText(getName());
      }
   }

   @Override
   public String getName()
   {
      return node == null ? null : node.getEditor().getName();
   }

   private EditorNode node;
   private JLabel tabLabel;
   private boolean checkDrag = false;
   private static final int GAP=10;
}
