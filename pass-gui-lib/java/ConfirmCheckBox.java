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
package com.dickimawbooks.passguilib;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import com.dickimawbooks.passlib.PassTools;

/**
 * A checkbox with the I/We agreement text.
 * If it's selected, then this indicates that the student has
 * confirmed that they agree to check the PDF before submitting it.
 */ 
public class ConfirmCheckBox extends JCheckBox
{
   /**
    * Creates a new instance.
    * @param gui the Pass GUI application
    */ 
   public ConfirmCheckBox(PassGui gui)
   {
      super();
      setText(getConfirmText(gui));

      setVerticalTextPosition(SwingConstants.TOP);
   }

   /**
    * Gets the localised confirmation message.
    * Called by the constructor to get the button text and also set
    * the mnemonic, if available.
    * @param gui the Pass GUI application
    */ 
   protected String getConfirmText(PassGui gui)
   {
      PassTools passTools = gui.getPassTools();

      String text = gui.getMessage("message.i_we_confirm");

      int mnemonic = gui.getMnemonic("message.i_we_confirm");

      if (mnemonic == -1)
      {
         return text;
      }

      setMnemonic(mnemonic);

      String mnemStr = new String(Character.toChars(mnemonic));

      int idx = text.indexOf(mnemStr);

      if (idx == -1)
      {
         return String.format("<html>%s</html>", passTools.stringWrap(
          text, 80, "<br>"));
      }

      int mnemLength = mnemStr.length();
      mnemStr = String.format("<u>%s</u>", mnemStr);

      if (idx == 0)
      {
         return String.format("<html>%s</html>", passTools.stringWrap(
           mnemStr + text.substring(mnemLength), 80, "<br>"));
      }
      else
      {
         return String.format("<html>%s</html>", passTools.stringWrap(
           text.substring(0, idx) + mnemStr
           +text.substring(idx+mnemLength),
           80, "<br>"));
      }
   }
}
