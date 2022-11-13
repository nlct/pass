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

import com.dickimawbooks.passlib.PassTools;

public class FilePaneProperties
{
   public FilePaneProperties()
   {
      showLines = true;
      showColumns = true;
      showPosition = true;
      locationFormat = LocationFormat.LONG;
   }

   public boolean isShowLines()
   {
      return showLines;
   }

   public boolean isShowColumns()
   {
      return showColumns;
   }

   public boolean isShowPosition()
   {
      return showPosition;
   }

   public LocationFormat getLocationFormat()
   {
      return locationFormat;
   }

   public void setShowLines(boolean show)
   {
      showLines = show;
   }

   public void setShowColumns(boolean show)
   {
      showColumns = show;
   }

   public void setShowPosition(boolean show)
   {
      showPosition = show;
   }

   public void setLocationFormat(LocationFormat locationFormat)
   {
      this.locationFormat = locationFormat;
   }

   public String formatLocation(FilePane filePane)
   {
      String result = "";
      String sep = " ";

      PassTools passTools = filePane.getPassEditor().getPassTools();

      if (locationFormat == LocationFormat.NUMERIC)
      {
         sep = passTools.getMessage("filepane.location.numeric_sep");
      }

      if (showLines)
      {
         int line = filePane == null ? 0 : filePane.getCurrentLine();

         switch (locationFormat)
         {
            case LONG: 
               result = passTools.getMessage("filepane.location_long.line", line);
            break;
            case SHORT: 
               result = passTools.getMessage("filepane.location_short.line", line);
            break;
            case NUMERIC:
               result = String.format("%d", line);
            break;
         }

      }

      if (showColumns)
      {
         int column = filePane == null ? 0 : filePane.getCurrentColumn();

         String tag = "";

         switch (locationFormat)
         {
            case LONG: 
               tag = passTools.getMessage("filepane.location_long.column", column);
            break;
            case SHORT: 
               tag = passTools.getMessage("filepane.location_short.column", column);
            break;
            case NUMERIC:
               tag = String.format("%d", column);
            break;
         }

         if (result.isEmpty())
         {
            result = tag;
         }
         else
         {
            result = String.format("%s%s%s", result, sep, tag);
         }
      }

      if (showPosition)
      {
         int position = filePane == null ? 0 : filePane.getCurrentPosition();

         String tag = "";

         switch (locationFormat)
         {
            case LONG: 
               tag = passTools.getMessage("filepane.location_long.position", position);
            break;
            case SHORT: 
               tag = passTools.getMessage("filepane.location_short.position", position);
            break;
            case NUMERIC:
               tag = String.format("%d", position);
            break;
         }

         if (result.isEmpty())
         {
            result = String.format("%s%d", tag, position);
         }
         else
         {
            result = String.format("%s%s%s", result, sep, tag);
         }
      }

      return result;
   }

   public void set(FilePaneProperties other)
   {
      showLines = other.showLines;
      showColumns = other.showColumns;
      showPosition = other.showPosition;
      locationFormat = other.locationFormat;
   }

   public enum LocationFormat
   {
      LONG, SHORT, NUMERIC;

      public static LocationFormat parse(String str)
      {
         if (str.equals("LONG"))
         {
            return LONG;
         }
         else if (str.equals("SHORT"))
         {
            return SHORT;
         }
         else if (str.equals("NUMERIC"))
         {
            return NUMERIC;
         }

         throw new IllegalArgumentException("Invalid position format "+str);
      }
   }

   boolean showLines, showColumns, showPosition;
   LocationFormat locationFormat = LocationFormat.LONG;
}
