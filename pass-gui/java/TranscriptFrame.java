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
package com.dickimawbooks.passgui;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.ImageIcon;

/**
 * A frame for showing transcript messages.
 */
public class TranscriptFrame extends JFrame
{
   /**
    * Creates a new instance.
    * @param gui the main GUI
    */ 
   public TranscriptFrame(PrepareAssignmentUpload gui)
   {
      super(gui.getPassTools().getMessage("message.transcript.title"));

      ImageIcon ic = gui.getLogoIcon();

      if (ic != null)
      {
         setIconImage(ic.getImage());
      }

      textArea = new JTextArea();
      textArea.setEditable(false);
      textArea.setLineWrap(true);

      DefaultCaret caret = (DefaultCaret)textArea.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

      getContentPane().add(new JScrollPane(textArea), "Center");
   }

   /**
    * Adds a new message line. A newline is appended after the
    * message text.
    * @param message the message text
    */ 
   public void message(String message)
   {
      textArea.append(String.format("%s%n", message));
   }

   /**
    * Adds a new message. No newline appended.
    * @param message the message text
    */ 
   public void messageNoLn(String message)
   {
      textArea.append(message);
   }

   /**
    * Adds a character to the transcript.
    * @param c the character
    */ 
   public void messageNoLn(char c)
   {
      textArea.append(String.format("%c", c));
   }

   private JTextArea textArea;
}
