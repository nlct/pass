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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;

public class ButtonAction extends AbstractAction
{
   public ButtonAction(AbstractButton button,
     ActionListener buttonListener)
   {
      super(button.getActionCommand());

      putValue(ACTION_COMMAND_KEY, button.getActionCommand());

      this.listener = buttonListener;
   }

   public void actionPerformed(ActionEvent evt)
   {
      listener.actionPerformed(evt);
   }

   private ActionListener listener;
}
