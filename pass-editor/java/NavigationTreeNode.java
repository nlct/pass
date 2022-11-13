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
import java.io.IOException;
import java.nio.file.Path;

import java.awt.Font;
import javax.swing.tree.MutableTreeNode;

public interface NavigationTreeNode extends MutableTreeNode
{
   public boolean isFile(File file);

   public Path getPath();

   public void setPath(Path path) throws IOException;

   public String getName();

   // Is node (not file) editable (can be moved, renamed or deleted)?
   // Should return false for required files as well as resource
   // files. Should return false for paths that contain non-editable
   // children.
//TODO split into three separate methods
   public boolean isEditable();

   public void updateEditorFont(Font font);
}
