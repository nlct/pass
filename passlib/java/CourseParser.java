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

import java.util.Vector;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Parser handler for the resources XML file.
 * See https://www.dickimaw-books.com/software/pass/#resourcefiles
 * All content should be in the body of a single "resources"
 * element. Each course should be identified by a "resource"
 * element. A remote resource file can be identified with the
 * "courses" element. This makes it easier to add new courses
 * without having to edit the local lib/resources.xml file in each
 * Pass installation.
 *
 * For example, the local lib/resources.xml may contain:
 *<pre>
  &lt;courses href="http://www.dickimaw-books.com/software/pass/dummy-resources.xml" /&gt;
  </pre>
 * This indicates that all the course information is in the location
 * identified by the href attribute.
 *
 * The identified remote resources.xml file can then contain all the
 * course data. For example:
 * <pre>
   &lt;resource name="CMP-123XY" debug="true"
  href="http://www.dickimaw-books.com/software/pass/dummy-assignments.xml"&gt;
 Dummy Course for Testing
 &lt;/resource&gt;
   </pre>
 * This describes a course with the code (label) "CMP-123XY" with
 * assignment data provided in the XML file identified by the href
 * attribute. The body of the "resource" element is the course
 * title. The debug attribute may be set to "true" or "on" to
 * indicate that the course should only be available in debug mode.
 *
 * Settings that are specific to the device that a PASS application has been
 * installed on should go in the local lib/resources.xml file. For
 * example, the process timeout and application paths.
 *
 */
public class CourseParser extends XMLReaderAdapter
{
   /**
    * Creates a new XML parser handler.
    * @param passTools set of common Pass methods
    */ 
   public CourseParser(PassTools passTools) throws SAXException
   {
      super();
      this.passTools = passTools;
   }

   /**
    * Called when a start element is encountered.
    */ 
   @Override
   public void startElement(String uri, String localName, String qName,
     Attributes atts)
   throws SAXException
   {
      super.startElement(uri, localName, qName, atts);

      if ("resources".equals(qName))
      {
         if (courses != null)
         {
            throw new SAXException(passTools.getMessageWithDefault(
             "error.only_one_tag", "Only one <resources> tag permitted."));
         }

         courses = new Vector<Course>();
      }
      else if ("courses".equals(qName))
      {
         if (courses == null)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_must_be_inside_tag", 
              "<{0}> tag must be inside <{1}> tag.",
              qName, "resources"));
         }

         if (inProcessesTag)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_mustnt_be_inside_tag", 
              "<{0}> tag must not be inside <{1}> tag.",
              qName, "processes"));
         }

         String href = atts.getValue("href");

         if (href == null || href.isEmpty())
         {
            throw new SAXException(
              passTools.getMessageWithDefault(
                "error.tag_missing_attribute",
                "<{0}> ''{1}'' attribute missing or empty.", 
                 qName, "href"));
         }

         ignoreResourceTag = true;
         URL hrefUrl = null;

         try
         {
            URI hrefUri = new URI(href);
            hrefUrl = hrefUri.toURL();

            CourseParser parser = new CourseParser(passTools);

            BufferedReader in = null;

            try
            {
               in = new BufferedReader(new InputStreamReader(
                  hrefUrl.openStream()));

               parser.parse(new InputSource(in));
            }
            finally
            {
               if (in != null)
               {
                  in.close();
               }
            }

            Vector<Course> remoteData = parser.getData();

            if (remoteData != null)
            {
               for (Course c : remoteData)
               {
                  if (getCourse(c.getCode()) != null)
                  {
                     throw new SAXException(
                       passTools.getMessageWithDefault(
                        "error.course_already_defined_in_other",
                        "Course ''{0}'' provided in {1} has already been defined in a parent resource file.",
                        c.getCode(), href));
                  }

                  courses.add(c);
               }
            }
         }
         catch (URISyntaxException | MalformedURLException e)
         {
            throw new SAXException(
              passTools.getMessageWithDefault(
               "error.uri_tag_attribute_required",
               "<{0}> tag attribute ''{1}'' must be a well-formed URI (found ''{2}'').", 
               qName, "href", href), e);
         }
         catch (IOException | SAXException e)
         {
            int code = -1;

            if (hrefUrl != null)
            {
               try
               {
                  code = passTools.testHttpURLConnection(hrefUrl);

                  if (code == 404 || code == 301)
                  {
                     ignoreResourceTag = false;

                     passTools.getPass().debug(String.format(
                       "HTTP error code %d returned for remote resource %s",
                        code, href));
                  }
               }
               catch (IOException ioex)
               {// ignore, exception will be thrown below
               }
            }

            if (ignoreResourceTag)
            {
               if (code >= 300)
               {
                  throw new SAXException(passTools.getMessageWithDefault(
                   "error.http_status",
                   "Unable to access ''{0}''. HTTP response code: {1,number,integer}.",
                    href, code), e);
               }
               else
               {
                  throw new SAXException(passTools.getMessageWithDefault(
                   "error.remote_resource_failed",
                   "Error reading remote resource ''{0}'': {1}", 
                    href, e.getMessage()), e);
               }
            }
         }
      }
      else if ("resource".equals(qName))
      {
         if (courses == null)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
              qName, "resources"));
         }

         if (inProcessesTag)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_mustnt_be_inside_tag",
              "<{0}> tag must not be inside <{1}> tag.",
              qName, "processes"));
         }

         String name = atts.getValue("name");

         if (name == null || name.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_missing_attribute",
              "<{0}> ''{1}'' attribute missing or empty.", qName, "name"));
         }

         if (ignoreResourceTag)
         {
            passTools.getPass().debug(String.format("Ignoring <%s name=\"%s\">", 
               qName, name));
         }
         else
         {
            Course c = getCourse(name);

            if (c != null)
            {
               throw new SAXException(
                 passTools.getMessageWithDefault(
                 "error.course_already_defined",
                 "Course ''{0}'' already defined.", name));
            }

            String href = atts.getValue("href");

            if (href == null || href.isEmpty())
            {
               throw new SAXException(passTools.getMessageWithDefault(
                 "error.tag_missing_attribute",
                 "<{0}> ''{1}'' attribute missing or empty.", qName, "href"));
            }

            try
            {
               URI hrefUri = new URI(href);

               current = new Course(name, hrefUri.toURL());
            }
            catch (URISyntaxException | MalformedURLException e)
            {
               throw new SAXException(
                 passTools.getMessageWithDefault(
                  "error.uri_tag_attribute_required", 
                  "<{0}> tag attribute ''{1}'' must be a well-formed URI (found ''{2}'').", 
                  qName, "href", href), e);
            }

            current.setDebugModeOnly(
              passTools.isBoolAttributeOn("debug", atts, qName, false));

            builder = new StringBuilder();
         }
      }
      else if ("application".equals(qName))
      {
         if (courses == null)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_must_be_inside_tag", 
              "<{0}> tag must be inside <{1}> tag.",
              qName, "resources"));
         }

         if (inProcessesTag)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_mustnt_be_inside_tag",
              "<{0}> tag must not be inside <{1}>.",
              qName, "processes"));
         }

         String name = atts.getValue("name");

         if (name == null || name.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_missing_attribute",
              "<{0}> ''{1}'' attribute missing or empty.", qName, "name"));
         }

         String appuri = atts.getValue("uri");

         if (appuri == null || appuri.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_missing_attribute",
              "<{0}> ''{1}'' attribute missing or empty.", qName, "uri"));
         }

         try
         {
            passTools.addApplication(name, new File(new URI(appuri)));
         }
         catch (URISyntaxException e)
         {
            throw new SAXException(
              passTools.getMessageWithDefault(
               "error.uri_tag_attribute_required",
               "<{0}> tag attribute ''{1}'' must be a well-formed URI (found ''{2}'').",
               qName, "uri", appuri), e);
         }
      }
      else if ("processes".equals(qName))
      {
         if (courses == null)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.", 
              qName, "resources"));
         }

         String timeout = atts.getValue("timeout");

         if (timeout != null && !timeout.isEmpty())
         {
            try
            {
               long value = Long.parseLong(timeout);

               if (value <= 0L)
               {
                  throw new SAXException(passTools.getMessageWithDefault(
                   "error.positive_int_tag_attribute_required",
                   "<{0}> tag attribute ''{1}'' must have a positive integer value (found {2} \u226F 0).",
                   qName, "timeout", value));
               }

               passTools.getPass().setTimeOut(value);

               passTools.getPass().transcriptMessage(
                passTools.getMessageWithDefault(
                "message.resource_timeout",
                "Timeout property {0,number} found in resources file. This will override any default or user supplied setting.", 
                value));
            }
            catch (NumberFormatException e)
            {
               throw new SAXException(
                 passTools.getMessageWithDefault(
                  "error.int_tag_attribute_required",
                  "<{0}> tag attribute ''{1}'' must have an integer value (found ''{2}'').",
                  qName, "timeout", timeout), e);
            }
         }

         inProcessesTag = true;
      }
      else if ("env".equals(qName))
      {
         if (!inProcessesTag)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
              qName, "processes"));
         }

         if (builder != null || currentEnvName != null)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.misplaced_tag",
              "Misplaced or nested <{0}> tag", qName));
         }

         currentEnvName = atts.getValue("name");

         if (currentEnvName == null || currentEnvName.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_missing_attribute",
              "<{0}> ''{1}'' attribute missing or empty.", qName, "name"));
         }

         builder = new StringBuilder();
      }
      else if ("agree".equals(qName))
      {
         if (courses == null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault(
               "error.tag_must_be_inside_tag",
               "<{0}> tag must be inside <{1}> tag.",
               qName, "resources"));
         }

         if (inProcessesTag)
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_mustnt_be_inside_tag",
              "<{0}> tag must not be inside <{1}> tag.",
              qName, "processes"));
         }

         passTools.setAgreeRequired(
            passTools.isRequiredBoolAttributeOn("required", atts, qName));
      }
      else
      {
         throw new SAXException(passTools.getMessageWithDefault(
           "error.unknown_tag",
           "Unknown '{0}' tag.", qName));
      }
   }

   /**
    * Called when an end element is encountered.
    */ 
   @Override
   public void endElement(String uri, String localName, String qName)
    throws SAXException
   {
      super.endElement(uri, localName, qName);

      if (!ignoreResourceTag && "resource".equals(qName))
      {
         if (!current.isDebugModeOnly() || passTools.isDebugMode())
         {
            current.setTitle(builder.toString().trim());
            courses.add(current);
         }

         current = null;
      }
      else if ("courses".equals(qName))
      {
         ignoreResourceTag = false;
      }
      else if ("processes".equals(qName))
      {
         inProcessesTag = false;
      }
      else if ("env".equals(qName))
      {
         passTools.addProcessEnvironmentVariable(currentEnvName, builder.toString());
         currentEnvName = null;
      }

      builder = null;
   }

   /**
    * Called when reading characters in from the XML file.
    */ 
   @Override
   public void characters(char[] ch, int start, int length)
    throws SAXException
   {
      super.characters(ch, start, length);

      if (builder != null)
      {
         builder.append(ch, start, length);
      }
   }

   /**
    * Gets a list of all the courses identified in the XML file.
    * @return list of all available courses
    */ 
   public Vector<Course> getData()
   {
      return courses;
   }

   /**
    * Get a course identified in the XML file.
    * @param code the code identifying the required course
    * @return the course if found or null otherwise
    */ 
   public Course getCourse(String code)
   {
      if (courses == null) return null;

      for (Course c : courses)
      {
         if (c.getCode().equals(code)) return c;
      }

      return null;
   }

   private boolean ignoreResourceTag=false;
   private boolean inProcessesTag = false;
   private String currentEnvName = null;
   private Vector<Course> courses = null;
   private Course current = null;
   private StringBuilder builder = null;
   private PassTools passTools;
}
