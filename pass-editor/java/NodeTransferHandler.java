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

import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

public class NodeTransferHandler extends TransferHandler
{
   public NodeTransferHandler(PassEditor gui)
   {
      super();
      this.gui = gui;
   }

   @Override
   public int getSourceActions(JComponent comp)
   {
      return MOVE;
   }

   @Override
   public Transferable createTransferable(JComponent comp)
   {
      String name = comp.getName();

      return name == null ? null : new StringSelection(name);
   }

   @Override
   public boolean canImport(TransferHandler.TransferSupport support)
   {
      Component comp = support.getComponent();

      if (comp instanceof PathReference 
           || PassEditor.FILE_TABS_NAME.equals(comp.getName()))
      {
         return true;
      }

      return false;
   }

   @Override
   public boolean importData(TransferHandler.TransferSupport support)
   {
      Component comp = support.getComponent();
      Transferable transfer = support.getTransferable();

      return gui.movePathReference(comp, transfer);
   }

   private PassEditor gui;
}
