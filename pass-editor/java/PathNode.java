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
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import java.awt.Font;
import javax.swing.tree.TreeNode;
import javax.swing.tree.MutableTreeNode;

public class PathNode implements NavigationTreeNode
{
   public PathNode(PassEditor gui, PathNode parent, File dir)
     throws IOException
   {
      super();
      this.gui = gui;
      this.parent = parent;
      children = new Vector<NavigationTreeNode>();

      setFile(dir);
   }

   @Override
   public String toString()
   {
      return baseName;
   }

   @Override
   public String getName()
   {
      return dir == null ? null : dir.getName();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj) return true;

      if (obj == null || !(obj instanceof PathNode)) return false;

      PathNode node = (PathNode)obj;

      if (dir == null)
      {
         return node.dir == null;
      }
      else if (node.dir == null)
      {
         return false;
      }

      return dir.equals(node.dir);
   }

   public File getDirectory()
   {
      return dir;
   }

   @Override
   public Path getPath()
   {
      return dir == null ? null : dir.toPath();
   }

   public void setPath(Path path) throws IOException
   {
      if (path == null)
      {
         dir = null;
         baseName = String.format("[%s]", gui.getMessage("navigator.base"));
         return;
      }

      if (!Files.isDirectory(path))
      {
         throw new NotDirectoryException(path.toString());
      }

      dir = path.toFile();

      baseName = dir.getName();
   }

   public void setFile(File file) throws IOException
   {
      if (file == null)
      {
         dir = null;
         baseName = String.format("[%s]", gui.getMessage("navigator.base"));
         return;
      }

      if (!file.isDirectory())
      {
         throw new NotDirectoryException(file.toString());
      }

      dir = file;

      baseName = dir.getName();
   }

   @Override
   public Enumeration<NavigationTreeNode> children()
   {
      return children.elements();
   }

   @Override
   public int getChildCount()
   {
      return children.size();
   }

   @Override
   public TreeNode getChildAt(int childIndex)
   {
      return children.get(childIndex);
   }

   @Override
   public boolean isFile(File file)
   {
      return dir.equals(file);
   }

   @Override
   public boolean isEditable()
   {
      if (dir == null) return false;

      for (int i = 0; i < getChildCount(); i++)
      {
         NavigationTreeNode child = children.get(i);

         if (!child.isEditable())
         {
            return false;
         }
      }

      return true;
   }

   public NavigationTreeNode getChild(File file)
   {
      for (int i = 0, n = getChildCount(); i < n; i++)
      {
         NavigationTreeNode childNode = children.get(i);

         if (childNode.isFile(file))
         {
            return childNode;
         }
      }

      return null;
   }

   @Override
   public int getIndex(TreeNode node)
   {
      return children.indexOf(node);
   }

   @Override
   public boolean getAllowsChildren()
   {
      return true;
   }

   @Override
   public boolean isLeaf()
   {
      return false;
   }

   @Override
   public TreeNode getParent()
   {
      return parent;
   }

   @Override
   public void insert(MutableTreeNode child, int index)
   {
      children.add(index, (NavigationTreeNode)child);
      child.setParent(this);
   }

   @Override
   public void remove(int index)
   {
      children.remove(index);
   }

   @Override
   public void remove(MutableTreeNode node)
   {
      children.remove(node);
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
      this.parent = (NavigationTreeNode)parent;
   }

   @Override
   public void setUserObject(Object object)
   {
      try
      {
         if (object instanceof File)
         {
            setFile((File)object);
         }
         else if (object instanceof Path)
         {
            setPath((Path)object);
         }
         else
         {
            throw new IllegalArgumentException(
              "User object must be a File or Path for "
              + getClass().getSimpleName());
         }
      }
      catch (IOException e)
      {
         throw new IllegalArgumentException("Invalid user object for "
           + getClass().getSimpleName(), e);
      }
   }

   @Override
   public void updateEditorFont(Font font)
   {
      for (int i = 0; i < children.size(); i++)
      {
         children.get(i).updateEditorFont(font);
      }
   }

   private PassEditor gui;
   private File dir;
   private NavigationTreeNode parent;
   private Vector<NavigationTreeNode> children;
   private String baseName;
}
