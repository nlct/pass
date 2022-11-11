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
package com.dickimawbooks.passlib;

import java.net.URL;

/**
 * Class representing a course. The course has an identifying label
 * (the course code), a title, and a URL that specifies the location
 * of the XML file containing all the assignment data for this
 * course. A course may also have the debug flag set, which means
 * that it should only be available if Pass is running in debug
 * mode. This allows dummy courses to be tested.
 */
public class Course implements Comparable<Course>
{
   /**
    * Creates a new untitled course with the given code.
    * @param code the course code uniquely identifying this course
    * @param url the location of the course's assignment data XML
    * file
    * @throws NullPointerException if any arguments are null
    */ 
   public Course(String code, URL url)
   {
      this("Untitled", code, url);
   }

   /**
    * Creates a new course with the given title and code.
    * @param title the course title
    * @param code the course code uniquely identifying this course
    * @param url the location of the course's assignment data XML
    * file
    * @throws NullPointerException if any arguments are null
    */ 
   public Course(String title, String code, URL url)
   {
      if (title == null || code == null || url == null)
      {
         throw new NullPointerException();
      }

      this.title = title;
      this.code = code;
      this.url = url;
   }

   /**
    * Sets the course title.
    * @param title the course title
    * @throws NullPointerException if the argument is null
    */ 
   public void setTitle(String title)
   {
      if (title == null)
      {
         throw new NullPointerException();
      }

      this.title = title;
   }

   /**
    * Gets the course title.
    * @return the course title
    */ 
   public String getTitle()
   {
      return title;
   }

   /**
    * Gets the course code.
    * @return the course code
    */ 
   public String getCode()
   {
      return code;
   }

   /**
    * Gets the URL of the course's assignment XML file.
    * @return the URL of the assignment data XML file
    */ 
   public URL getURL()
   {
      return url;
   }

   /**
    * Compares this course to another, to allow for alphabetical
    * listing if multiple courses are available for the student to
    * select.
    * @param other the other course
    * @return the result of a string comparison of this course code
    * with the other course code
    */
   @Override
   public int compareTo(Course other)
   {
      return code.compareTo(other.code);
   }

   /**
    * Textual representation of this course.
    * @return the code followed by the title in parentheses
    */ 
   @Override
   public String toString()
   {
      return String.format("%s (%s)", code, title);
   }

   /**
    * Sets the debugging status of this course.
    * @param debugOnly true if this course should only be available
    * in debug mode
    */ 
   public void setDebugModeOnly(boolean debugOnly)
   {
      debugModeOnly = debugOnly;
   }

   /**
    * Indicates if this course is only available in debug mode.
    * @return true if this course should only be available
    * in debug mode
    */ 
   public boolean isDebugModeOnly()
   {
      return debugModeOnly;
   }

   private String title;
   private String code;
   private URL url;
   private boolean debugModeOnly=false;
}
