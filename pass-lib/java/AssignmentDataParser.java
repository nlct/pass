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
import java.text.ParseException;
import java.io.*;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Used when parsing assignment XML files. Each course has a
 * separate XML file that lists all the assignments for that course.
 * See https://www.dickimaw-books.com/software/pass/#assignxml
 * for the syntax. There should be a single "assignments" block,
 * inside of which there should be an "assignment" block for each
 * assignment with a label identified by the "name" attribute.
 *
 * Each assignment is stored in an AssignmentData object.
 */

public class AssignmentDataParser extends XMLReaderAdapter
{
   /**
    * Creates a new XML parser handler.
    * @param main the main PASS application
    */ 
   public AssignmentDataParser(Pass main) throws SAXException
   {
      super();
      this.main = main;
   }

   /**
    * Loads all the assignments defined in the given course's XML
    * file using this as the XML parser. 
    * The XML file is identified by course.getURL() which must
    * be the actual location. Redirects aren't permitted.
    * @param course the course data
    * @throws IOException if I/O error occurs
    * @throws SAXException if XML parser error occurs
    */ 
   public void loadAssignments(Course course) throws IOException,SAXException
   {
      this.course = course;

      URL url = course.getURL();

      PassTools passTools = main.getPassTools();
      int status = passTools.testHttpURLConnection(url);

      if (status > 299)
      {
         throw new IOException(
           passTools.getMessageWithDefault("error.http_status",
             "Unable to access ''{0}''. Status code: {1,number,integer}.",
              url, status));
      }

      BufferedReader in = null;

      try
      {
         in = new BufferedReader(new InputStreamReader(url.openStream()));
         parse(new InputSource(in));
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }
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

      PassTools passTools = main.getPassTools();

      if ("assignments".equals(qName))
      {
         if (assignments != null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.only_one_tag",
              "Only one <{0}> tag permitted.", qName));
         }

         assignments = new Vector<AssignmentData>();
      }
      else if ("assignment".equals(qName))
      {
         if (assignments == null)
         {
            throw new SAXException(
               passTools.getMessageWithDefault("error.tag_must_be_inside",
               "<{0}> tag must be inside <{1}> tag.",
               qName, "assignments"));
         }

         current = new AssignmentData(course);

         String label = atts.getValue("name");

         if (label == null && label.isEmpty())
         {
            throw new SAXException(
               passTools.getMessageWithDefault("error.tag_missing_attribute",
               "<{0}> ''{1}'' attribute missing or empty.",
               qName, "name")
            );
         }

         label = label.trim();

         if (!label.matches("[a-zA-Z0-9\\+\\.\\-]+"))
         {
            throw new SAXException(
             passTools.getMessageWithDefault("error.forbidden_chars_in_tag_attribute",
             "<{0}> ''{1}'' attribute ''{2}'' contains forbidden characters.\n(Permitted characters: ''a''-''z'', ''A''-''Z'', ''0''-''9'', ''.'', ''+'' and ''-''.)",
             qName, "name", label));
         }

         current.setLabel(label);

         String language = atts.getValue("language");

         if (language != null && !language.isEmpty())
         {
            current.setMainLanguage(language);
         }

         String variant = atts.getValue("variant");

         if (variant != null && !variant.isEmpty())
         {
            current.setLanguageVariant(variant);
         }

         String build = atts.getValue("build");
         URL buildURL = null;

         if (build != null && !build.isEmpty())
         {
            try
            {
               buildURL = new URI(build).toURL();
               current.setBuildScript(buildURL);
            }
            catch (URISyntaxException | MalformedURLException e)
            {
               throw new SAXException(
                 passTools.getMessageWithDefault("error.uri_tag_attribute_required",
                 "<{0}> tag attribute ''{1}'' must be a well-formed URI (found ''{2}'').",
                 qName, "build", build), e);
            }
         }

         String nopdfbuild = atts.getValue("nopdfbuild");
         URL nopdfBuildURL = null;

         if (nopdfbuild != null)
         {
            if (!nopdfbuild.isEmpty())
            {
               try
               {
                  nopdfBuildURL = new URI(nopdfbuild).toURL();
                  current.setNoPdfBuildScript(nopdfBuildURL);
               }
               catch (URISyntaxException | MalformedURLException e)
               {
                  throw new SAXException(
                    passTools.getMessageWithDefault("error.uri_tag_attribute_required",
                    "<{0}> tag attribute ''{1}'' must be a well-formed URI (found ''{2}'').",
                    qName, "nopdfbuild", nopdfbuild), e);
               }
            }
         }
         else if (buildURL != null)
         {
            nopdfBuildURL = buildURL;
            current.setNoPdfBuildScript(buildURL);
         }

         if (atts.getValue("run") == null)
         {
            current.setRunTest(true);
         }
         else if (passTools.isBoolAttributeOn("run", atts, qName, true))
         {
            if (buildURL != null)
            {
               throw new SAXException(
                  passTools.getMessageWithDefault("error.tag_attribute_conflict",
                  "<{0}> tag contains conflicting attributes ''{1}'' and ''{2}''.",
                  qName, "build", "run"));
            }
            else
            {
               current.setRunTest(true);
            }
         }
         else
         {
            current.setRunTest(false);
         }

         if (atts.getValue("nopdfrun") == null)
         {
            current.setNoPdfRunTest(true);
         }
         else if (passTools.isBoolAttributeOn("nopdfrun", atts, qName, true))
         {
            if (nopdfBuildURL != null)
            {
               throw new SAXException(
                  passTools.getMessageWithDefault("error.tag_attribute_conflict",
                  "<{0}> tag contains conflicting attributes ''{1}'' and ''{2}''.",
                  qName, "nopdfbuild", "nopdfrun"));
            }
            else
            {
               current.setNoPdfRunTest(true);
            }
         }
         else
         {
            current.setNoPdfRunTest(false);
         }

         if (atts.getValue("compile") == null)
         {
            current.setCompileTest(true);
         }
         else if (passTools.isBoolAttributeOn("compile", atts, qName, true))
         {
            if (build != null)
            {
               throw new SAXException(
                  passTools.getMessageWithDefault("error.tag_attribute_conflict",
                  "<{0}> tag contains conflicting attributes ''{1}'' and ''{2}''.",
                  qName, "build", "compile"));
            }
            else
            {
               current.setCompileTest(true);
            }
         }
         else
         {
            current.setCompileTest(false);
         }

         current.setRelativePathsDefault(
           passTools.isBoolAttributeOn("relpath", atts, qName, false));
      }
      else if ("verbatim".equals(qName))
      {
         if (assignments == null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
               qName, "assignments"));
         }

         if (current != null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_mustnt_be_inside_tag",
              "<{0}> tag mustn''t be inside <{1}> tag.",
              qName, "assignments"));
         }

         if (builder != null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.misplaced_tag",
              "Misplaced <{0}> tag.", qName));
         }

         String maxCharsStr = atts.getValue("maxchars");

         if (maxCharsStr != null)
         {
            try
            {
               verbMaxCharsPerLine = Integer.valueOf(maxCharsStr);
            }
            catch (NumberFormatException e)
            {
               throw new SAXException(
                  passTools.getMessageWithDefault("error.int_tag_attribute_required",
                  "<{0}> tag attribute ''{1}'' must have an integer value (found ''{2}'').",
                  qName, "maxchars", maxCharsStr), e);
            }

            if (verbMaxCharsPerLine <= 0)
            {
               throw new SAXException(
                  passTools.getMessageWithDefault(
                   "error.positive_int_tag_attribute_required",
                   "<{0}> tag attribute ''{1}'' must have a positive integer value (found {2} \u226F 0).",
                   qName, "maxchars", maxCharsStr));
            }
         }

         String tabCharCountStr = atts.getValue("tabcount");

         if (tabCharCountStr != null)
         {
            try
            {
               verbTabCharCount = Integer.valueOf(tabCharCountStr);
            }
            catch (NumberFormatException e)
            {
               throw new SAXException(
                  passTools.getMessageWithDefault("error.int_tag_attribute_required",
                  "<{0}> tag attribute ''{1}'' must have an integer value (found ''{2}'').",
                  qName, "tabcount", tabCharCountStr), e);
            }

            if (verbTabCharCount <= 0)
            {
               throw new SAXException(
                   passTools.getMessageWithDefault(
                   "error.positive_int_tag_attribute_required",
                   "<{0}> tag attribute ''{1}'' must have a positive integer value (found {2} \u226F 0).",
                   qName, "tabcount", tabCharCountStr));
            }
         }
      }
      else if ("listings".equals(qName) || "geometry".equals(qName) 
             || "maxoutput".equals(qName) || "fontspec".equals(qName)
             || "fontenc".equals(qName))
      {
         if (assignments == null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
              qName, "assignments"));
         }

         if (current != null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_mustnt_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
              qName, "assignment"));
         }

         if ("fontspec".equals(qName))
         {
            String options = atts.getValue("options");

            if (options != null)
            {
               if (fontSpecOptions == null)
               {
                  fontSpecOptions = options;
               }
               else
               {
                  fontSpecOptions += ","+options;
               }
            }
         }
         else if ("fontenc".equals(qName))
         {
            String options = atts.getValue("options");

            if (options != null)
            {
               if (fontEncOptions == null)
               {
                  fontEncOptions = options;
               }
               else
               {
                  fontEncOptions += ","+options;
               }
            }
         }

         builder = new StringBuilder();
      }
      else if ("file".equals(qName) || "mainfile".equals(qName))
      {
         if (current == null)
         {
            throw new SAXException(
             passTools.getMessageWithDefault("error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
              qName, "assignment"));
         }

         builder = new StringBuilder();

         templateURL = null;
         String template = atts.getValue("template");

         // template attribute is optional (used by PASS Editor)
         if (template != null && !template.isEmpty())
         {
            try
            {
               templateURL = new URI(template).toURL();
            }
            catch (URISyntaxException | MalformedURLException e)
            {
               throw new SAXException(
                 passTools.getMessageWithDefault("error.uri_tag_attribute_required",
                 "<{0}> tag attribute ''{1}'' must be a well-formed URI (found ''{2}'').",
                 qName, "template", template), e);
            }
         }

      }
      else if ("title".equals(qName)
             || "arg".equals(qName)
             || "compiler-arg".equals(qName)
             || "invoker-arg".equals(qName)
             || "input".equals(qName)
             || "due".equals(qName)
             || "report".equals(qName))
      {
         if (current == null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
              qName, "assignment"));
         }

         builder = new StringBuilder();
      }
      else if ("allowedbinary".equals(qName))
      {
         if (current == null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_must_be_inside_tag",
              "<{0}> tag must be inside <{1}> tag.",
              qName, "assignment"));
         }

         String mimetype = atts.getValue("type");
         String ext = atts.getValue("ext");

         if (ext == null || ext.isEmpty())
         {
            throw new SAXException(
               passTools.getMessageWithDefault("error.tag_missing_attribute",
               "<{0}> ''{1}'' attribute missing or empty.",
               qName, "ext"));
         }

         if (mimetype == null || mimetype.isEmpty())
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_missing_attribute",
               "<{0}> ''{1}'' attribute missing or empty.",
              qName, "type"));
         }

         boolean showListing = passTools.isBoolAttributeOn("listing",
            atts, qName, true);

         boolean isCaseSensitive = passTools.isBoolAttributeOn("case",
            atts, qName, true);

         currentAllowedBinaryFilter = new AllowedBinaryFilter(mimetype,
          showListing, isCaseSensitive,
          passTools.getMessageWithDefault("file."+mimetype, null),
          ext.trim().split(" *, *"));

         current.addAllowedBinary(currentAllowedBinaryFilter);

         builder = new StringBuilder();
      }
      else if ("resourcefile".equals(qName))
      {
         String src = atts.getValue("src");

         if (src == null || src.isEmpty())
         {
            throw new SAXException(
               passTools.getMessageWithDefault("error.tag_missing_attribute",
               "<{0}> ''{1}'' attribute missing or empty.",
               qName, "src"));
         }

         try
         {
            // type attribute is optional (used by PASS Editor)
            current.addResourceFile(src, atts.getValue("type"));
         }
         catch (URISyntaxException e)
         {
            throw new SAXException(
             passTools.getMessageWithDefault("error.uri_tag_attribute_required",
              "<{0}> tag attribute ''{1}'' must be a well-formed URI (found ''{2}'').",
              qName, "src", src), e);
         }
      }
      else if ("resultfile".equals(qName))
      {
         String name = atts.getValue("name");
         String mimetype = atts.getValue("type");

         if (name == null || name.isEmpty())
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_missing_attribute",
               "<{0}> ''{1}'' attribute missing or empty.",
              qName, "name"));
         }

         if (mimetype == null || mimetype.isEmpty())
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.tag_missing_attribute",
               "<{0}> ''{1}'' attribute missing or empty.",
              qName, "type"));
         }

         boolean showListing = passTools.isBoolAttributeOn("listing",
            atts, qName, true);

         current.addResultFile(new ResultFile(name, mimetype, showListing));
      }
      else
      {
         main.warning(passTools.getMessageWithDefault("error.unknown_tag",
          "Unknown <{0}> tag", qName));
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

      PassTools passTools = main.getPassTools();

      if ("assignment".equals(qName))
      {
         if (current.getTitle() == null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.missing_inner_tag",
              "Missing required <{0}> tag inside <{1}>.",
              "title", String.format("assignment name=\"%s\"", current.getLabel()))
            );
         }

         if (current.getDueDate() == null)
         {
            throw new SAXException(
              passTools.getMessageWithDefault("error.missing_inner_tag",
              "Missing required <{0}> tag inside <{1}>.",
              "due", String.format("assignment name=\"%s\"", current.getLabel()))
            );
         }

         assignments.add(current);
         current = null;
      }
      else if ("listings".equals(qName))
      {
         if (listingsSettings == null)
         {
            listingsSettings = new StringBuilder(
              builder.toString().trim());
         }
         else
         {
            listingsSettings.append(",");
            listingsSettings.append(builder.toString().trim());
         }
      }
      else if ("geometry".equals(qName))
      {
         if (geometrySettings == null)
         {
            geometrySettings = new StringBuilder(
              builder.toString().trim());
         }
         else
         {
            geometrySettings.append(",");
            geometrySettings.append(builder.toString().trim());
         }
      }
      else if ("fontspec".equals(qName))
      {
         if (fontSpecSettings == null)
         {
            fontSpecSettings = new StringBuilder();
            fontSpecSettings.append(builder);
         }
         else
         {
            fontSpecSettings.append(builder);
         }
      }
      else if ("fontenc".equals(qName))
      {
         if (fontEncSettings == null)
         {
            fontEncSettings = new StringBuilder();
            fontEncSettings.append(builder);
         }
         else
         {
            fontEncSettings.append(builder);
         }
      }
      else if ("maxoutput".equals(qName))
      {
         String str = builder.toString().trim();

         try
         {
            Long value = Long.valueOf(str);

            if (value <= 0L)
            {
               throw new SAXException(passTools.getMessageWithDefault(
                 "error.positive_int_tag_content_required",
                 "<{0}> tag content must have a positive integer value (found {2} &#x226F; 0).",
                 qName, str));
            }

            maxOutputSetting = value;
         }
         catch (NumberFormatException e)
         {
            throw new SAXException(passTools.getMessageWithDefault(
            "error.int_tag_content_required",
            "<{0}> tag content must have an integer value (found ''{2}'').",
            qName, str), e);
         }
      }
      else if ("title".equals(qName))
      {
         String str = builder.toString().trim();

         if (str.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_content_required",
              "<{0}> tag content can''t be empty.", qName
            ));
         }

         current.setTitle(str);
      }
      else if ("input".equals(qName))
      {
         current.addInput(builder.toString().trim());
      }
      else if ("report".equals(qName))
      {
         String str = builder.toString().trim();

         if (str.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_content_required",
              "<{0}> tag content can''t be empty.", qName
            ));
         }

         current.addReport(str);
      }
      else if ("allowedbinary".equals(qName))
      {
         String str = builder.toString().trim();

         if (!str.isEmpty())
         {
            currentAllowedBinaryFilter.setDescription(str);
         }
      }
      else if ("arg".equals(qName))
      {
         current.addArg(builder.toString());
      }
      else if ("compiler-arg".equals(qName))
      {
         String str = builder.toString().trim();

         if (str.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_content_required",
              "<{0}> tag content can''t be empty.", qName
            ));
         }

         current.addCompilerArg(str);
      }
      else if ("invoker-arg".equals(qName))
      {
         String str = builder.toString();

         if (str.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_content_required",
              "<{0}> tag content can''t be empty.", qName
            ));
         }

         current.addInvokerArg(str);
      }
      else if ("file".equals(qName))
      {
         String str = builder.toString().trim();

         if (str.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_content_required",
              "<{0}> tag content can''t be empty.", qName
            ));
         }

         current.addFile(str, templateURL);
         templateURL = null;
      }
      else if ("mainfile".equals(qName))
      {
         String str = builder.toString().trim();

         if (str.isEmpty())
         {
            throw new SAXException(passTools.getMessageWithDefault(
              "error.tag_content_required",
              "<{0}> tag content can''t be empty.", qName
            ));
         }

         current.addMainFile(str, templateURL);
         templateURL = null;
      }
      else if ("due".equals(qName))
      {
         String str = builder.toString().trim();

         try
         {
            current.setDueDate(str);
         }
         catch (ParseException e)
         {
            throw new SAXException(
               passTools.getMessageWithDefault(
                 "error.datetime_tag_content_required",
                 "<{0}> tag content must have a numeric date-time value (found ''{2}'').",
               str), e);
         }
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
    * Gets the data that has been obtained by parsing the XML file.
    * @return list of assignment data
    */ 
   public Vector<AssignmentData> getData()
   {
      return assignments;
   }

   /**
    * Gets any listing.sty options provided in the XML file.
    * @return listing.sty options
    */ 
   public CharSequence getListingsSettings()
   {
      return listingsSettings;
   }

   /**
    * Gets any geometry.sty options provided in the XML file.
    * @return geometry.sty options
    */ 
   public CharSequence getGeometrySettings()
   {
      return geometrySettings;
   }

   /**
    * Gets any fontspec.sty package options provided in the XML file.
    * These will only be used if LuaLaTeX is used to compile the
    * PDF. This information is identified in the "options" attribute
    * of the fontspec element.
    * @return fontspec.sty package options
    */ 
   public String getFontSpecOptions()
   {
      return fontSpecOptions;
   }

   /**
    * Gets any fontspec.sty settings provided in the XML file.
    * These will only be used if LuaLaTeX is used to compile the
    * PDF. For example, any packages that should be loaded if
    * fontspec is loaded or any fonts that should be set. 
    * This information is identified within the
    * body of the fontspec element.
    * @return LaTeX code to add after fontspec.sty is loaded
    */ 
   public CharSequence getFontSpecSettings()
   {
      return fontSpecSettings;
   }

   /**
    * Gets any fontenc.sty package options provided in the XML file.
    * These will only be used if PDFLaTeX is used to compile the
    * PDF. This information is identified in the "options" attribute
    * of the fontenc element.
    * @return fontenc.sty package options
    */ 
   public String getFontEncOptions()
   {
      return fontEncOptions;
   }

   /**
    * Gets any fontenc.sty settings provided in the XML file.
    * These will only be used if PDFLaTeX is used to compile the
    * PDF. For example, any packages that should be loaded if
    * fontenc is loaded. This information is identified within the
    * body of the fontenc element.
    * @return LaTeX code to add after fontenc.sty is loaded
    */ 
   public CharSequence getFontEncSettings()
   {
      return fontEncSettings;
   }

   /**
    * Gets the maximum number of characters captured from STDOUT
    * when running the student's application. Any additional
    * characters will be replaced by an ellipsis. This prevents
    * overly verbose applications creating a huge PDF. This value is
    * identified within the body of the maxoutput element.
    */ 
   public Long getMaxOutputSetting()
   {
      return maxOutputSetting;
   }

   /**
    * Gets the maximum number of characters per line in verbatim
    * blocks. This value is identified by the maxchars attribute
    * of the verbatim element.
    */ 
   public Integer getVerbMaxCharsPerLine()
   {
      return verbMaxCharsPerLine;
   }

   /**
    * Gets the maximum of characters a TAB should span in verbatim
    * blocks. This value is identified by the tabcount attribute
    * of the verbatim element.
    */ 
   public Integer getVerbTabCharCount()
   {
      return verbTabCharCount;
   }

   private Vector<AssignmentData> assignments=null;
   private AssignmentData current=null;
   private AllowedBinaryFilter currentAllowedBinaryFilter = null;
   private StringBuilder builder;
   private StringBuilder listingsSettings = null;
   private StringBuilder geometrySettings = null;
   private StringBuilder fontSpecSettings = null;
   private StringBuilder fontEncSettings = null;
   private String fontSpecOptions = null;
   private String fontEncOptions = null;
   private Long maxOutputSetting = null;
   private Integer verbMaxCharsPerLine, verbTabCharCount;
   private URL templateURL = null;
   private Pass main = null;
   private Course course;
}
