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

import javax.swing.JMenuItem;
import javax.swing.JButton;

public class MenuItemButton extends JMenuItem
{
   public MenuItemButton(String label, JButton button)
   {
      super(label);
      this.button = button;
   }

   public JButton getButton()
   {
      return button;
   }

   @Override
   public void setEnabled(boolean enable)
   {
      super.setEnabled(enable);
      button.setEnabled(enable);
   }

   @Override
   public String toString()
   {
      return String.format("%s[item=%s,button=%s]", getClass().getSimpleName(),
        super.toString(), button);
   }

   private JButton button;
}
