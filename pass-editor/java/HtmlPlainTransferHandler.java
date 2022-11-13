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

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.TransferHandler;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/** Transfer handler that adds newlines when copying and
 * pasting plain text from HTML documents.
 * adapted from https://stackoverflow.com/questions/7745087
 */

public class HtmlPlainTransferHandler extends TransferHandler
{
   protected Transferable createTransferable(JComponent c)
   {
      final JEditorPane pane = (JEditorPane)c;
      final String htmlText = pane.getText();
      final String plainText = extractText(new StringReader(htmlText));

      return new HtmlPlainTransferable(plainText, htmlText);
   }

   public String extractText(Reader reader)
   {
      final ArrayList<String> list = new ArrayList<String>();

      HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback()
      {
         public void handleText(final char[] data, final int pos)
         {
             list.add(new String(data));
         }

         public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos)
         {
         }

         public void handleEndTag(HTML.Tag t, final int pos)
         {
            if (t.equals(HTML.Tag.P) || t.equals(HTML.Tag.PRE))
            {
               list.add("\n");
            }
         }

         public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, final int pos)
         {
            if (t.equals(HTML.Tag.BR))
            {
               list.add("\n");
            }
         }

         public void handleComment(final char[] data, final int pos)
         {
         }

         public void handleError(final String errMsg, final int pos)
         {
         }
      };

      try
      {
         new ParserDelegator().parse(reader, parserCallback, true);
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

      String result = "";

      for (String s : list)
      {
         result += s;
      }

      return result;
   }

   @Override
   public void exportToClipboard(JComponent comp, Clipboard clip, int action)
      throws IllegalStateException
   {
      if (action == COPY)
      {
         clip.setContents(this.createTransferable(comp), null);
      }
   }

   @Override
   public int getSourceActions(JComponent c)
   {
      return COPY;
   }
}

class HtmlPlainTransferable implements Transferable 
{
   private static final DataFlavor[] supportedFlavors;

   static
   {
      try
      {
         supportedFlavors = new DataFlavor[]
         {
            new DataFlavor("text/html;class=java.lang.String"),
            new DataFlavor("text/plain;class=java.lang.String")
         };
      }
      catch (ClassNotFoundException e)
      {
         throw new ExceptionInInitializerError(e);
      }
   }

   private final String plainData;
   private final String htmlData;

   public HtmlPlainTransferable(String plainData, String htmlData)
   {
      this.plainData = plainData;
      this.htmlData = htmlData;
   }

   public DataFlavor[] getTransferDataFlavors()
   {
      return supportedFlavors;
   }

   public boolean isDataFlavorSupported(DataFlavor flavor)
   {
      for (DataFlavor supportedFlavor : supportedFlavors)
      {
         if (supportedFlavor == flavor)
         {
            return true;
         }
      }

      return false;
   }

   public Object getTransferData(DataFlavor flavor)
      throws UnsupportedFlavorException, IOException
   {
      if (flavor.equals(supportedFlavors[0]))
      {
         return htmlData;
      }

      if (flavor.equals(supportedFlavors[1]))
      {
         return plainData;
      }

      throw new UnsupportedFlavorException(flavor);
   }
}
