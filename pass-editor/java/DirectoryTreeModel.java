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

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;

public class DirectoryTreeModel implements TreeModel
{
   public DirectoryTreeModel(TreeModel navigatorModel)
   {
      this.navigatorModel = navigatorModel;
   }

   @Override
   public void addTreeModelListener(TreeModelListener l)
   {
   }

   @Override
   public void removeTreeModelListener(TreeModelListener l)
   {
   }

   @Override
   public void valueForPathChanged(TreePath path, Object newValue)
   {
   }

   @Override
   public Object getChild(Object parent, int index)
   {
      NavigationTreeNode node = (NavigationTreeNode)parent;

      for (int i = 0, j = 0; i < node.getChildCount(); i++)
      {
         Object child = node.getChildAt(i);

         if (child instanceof PathNode)
         {
            if (j == index)
            {
               return child;
            }

            j++;
         }
      }

      return new ArrayIndexOutOfBoundsException(index);
   }

   @Override
   public int getChildCount(Object parent)
   {
      NavigationTreeNode node = (NavigationTreeNode)parent;

      int n = 0;

      for (int i = 0; i < node.getChildCount(); i++)
      {
         if (node.getChildAt(i) instanceof PathNode)
         {
            n++;
         }
      }

      return n;
   }

   @Override
   public int getIndexOfChild(Object parent, Object child)
   {
      if (parent == null || child == null) return -1;

      NavigationTreeNode parentNode = (NavigationTreeNode)parent;

      for (int i = 0, j = 0; i < parentNode.getChildCount(); i++)
      {
         Object node = parentNode.getChildAt(i);

         if (node instanceof PathNode)
         {
            if (node.equals(child))
            {
               return j;
            }

            j++;
         }
      }

      return -1;
   }

   @Override
   public Object getRoot()
   {
      return navigatorModel.getRoot();
   }

   @Override
   public boolean isLeaf(Object node)
   {
      return false;
   }

   private TreeModel navigatorModel;
}
