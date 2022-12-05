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

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.BreakIterator;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ChoiceFormat;
import java.text.NumberFormat;
import java.security.NoSuchAlgorithmException;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;

/**
 * Provides common methods used by the various PASS applications.
 */ 
public class PassTools
{
   /**
    * Creates a new instance without a dictionary.
    * @param pass the main Pass application
    */ 
   public PassTools(Pass pass)
   {
      this.pass = pass;
      config = new AssignmentProcessConfig(this);

      applications = new HashMap<String,File>();

      isWindows = System.getProperty("os.name").startsWith("Win");
   }

   /**
    * Creates a new instance and loads the dictionary corresponding
    * to the given locale.
    * @param pass the main Pass application
    * @param locale the preferred dictionary
    * @throws IOException if an I/O error occurs when loading the
    * dictionary
    */ 
   public PassTools(Pass pass, Locale locale) throws IOException
   {
      this(pass);
      loadDictionary("passlib", locale);
   }

   /**
    * Gets the dictionary file for the given tag and locale as an
    * InputStream. The file should be named
    * &lt;tag&gt;-&lt;localeid&gt;.xml where the locale id is a
    * match for the given locale. This first tries the language tag
    * for the given locale, then &lt;lang&gt;-&lt;REGION&gt;, and
    * then just &lt;lang&gt;, where &lt;lang&gt; is the language
    * code and &lt;REGION&gt; is the region code. If there's no
    * match, it will fallback on "en".
    * @param tag dictionary basename prefix
    * @param locale the locale
    * @return the input stream
    */ 
   public InputStream getDictionaryInputStream(String tag, Locale locale)
    throws IOException
   {
      String langTag = locale.toLanguageTag();

      InputStream in = getClass().getResourceAsStream(
       "/dictionary/"+tag+"-"+locale.toLanguageTag()+".xml");

      if (in == null)
      {
         String lang = locale.getLanguage();
         String country = locale.getCountry();

         if (country.isEmpty())
         {
            langTag = lang;

            in = getClass().getResourceAsStream(
               String.format("/dictionary/%s-%s.xml", tag, langTag));
         }
         else
         {
            String langCountryTag = String.format("%s-%s", lang, country);

            if (!langCountryTag.equals(langTag))
            {
               langTag = langCountryTag;

               in = getClass().getResourceAsStream(
                  String.format("/dictionary/%s-%s.xml", tag, langTag));
            }

            if (in == null)
            {
               langTag = lang;

               in = getClass().getResourceAsStream(
                  String.format("/dictionary/%s-%s.xml", tag, langTag));
            }
         }

         if (in == null)
         {
            langTag = "en";

            in = getClass().getResourceAsStream(
               String.format("/dictionary/%s-%s.xml", tag, langTag));

            if (in == null)
            {
               throw new FileNotFoundException(
                "Can't find dictionary file for locale "+locale
                  + " and 'en' fallback doesn't exist.");
            }
         }
      }

      return in;
   }

   /**
    * Loads the dictionary file.
    * @param tag the basename prefix
    * @param locale the locale
    */ 
   public void loadDictionary(String tag, Locale locale)
     throws IOException
   {
      InputStream in = null;

      try
      {
         in = getDictionaryInputStream(tag, locale);

         Properties prop = new Properties();
         prop.loadFromXML(in);

         if (dictionary == null)
         {
            dictionary = new HashMap<String,Object>();
         }

         for (Iterator<Object> it = prop.keySet().iterator(); it.hasNext(); )
         {
            String key = (String)it.next();
            String value = prop.getProperty(key);

            if (value != null)
            {
               if (key.endsWith(".choice"))
               {
                  dictionary.put(key, new ChoiceFormat(value));
               }
               else if (key.endsWith(".mnemonic"))
               {
                  dictionary.put(key, Integer.valueOf(value.codePointAt(0)));
               }
               else
               {
                  dictionary.put(key, new MessageFormat(value));
               }
            }
         }
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
    * Gets the configuration.
    * @return the confiuration
    */ 
   public AssignmentProcessConfig getConfig()
   {
      return config;
   }

   /**
    * Gets a message from the dictionary with parameters.
    * @param key the label identifying the message
    * @param params the parameters
    * @return the localised message text
    */ 
   public String getMessage(String key, Object... params)
   {
      Object value = null;

      if (dictionary != null)
      {
         value = dictionary.get(key);
      }

      if (value != null)
      {
         if (value instanceof NumberFormat && params.length == 1
                && params[0] instanceof Number)
         {
            if (params[0] instanceof Long || params[0] instanceof Integer)
            {
               return ((NumberFormat)value).format(((Number)params[0]).longValue());
            }
            else
            {
               return ((NumberFormat)value).format(((Number)params[0]).doubleValue());
            }
         }
         else if (value instanceof Format)
         {
            return ((Format)value).format(params);
         }
         else
         {
            return value.toString();
         }
      }

      return key;
   }

   /**
    * Gets a message from the dictionary with parameters.
    * If the key isn't found in the dictionary or if the dictionary
    * hasn't been loaded, this will use the default message format
    * instead.
    * @param key the label identifying the message
    * @param defFmt the default message format if no match on the
    * label
    * @param params the parameters
    * @return the localised message text
    */ 
   public String getMessageWithDefault(String key, String defFmt, Object... params)
   {
      Object value = null;

      if (dictionary != null)
      {
         value = dictionary.get(key);
      }

      if (value == null)
      {
         if (defFmt == null || params.length == 0)
         {
            return defFmt;
         }
         else
         {
            return MessageFormat.format(defFmt, params);
         }
      }

      if (value instanceof Format)
      {
         return ((Format)value).format(params);
      }
      else
      {
         return value.toString();
      }
   }

   /**
    * Gets a choice message from the dictionary.
    * @param key the label identifying the message
    * @param param the choice value
    * @param msgParams message parameters
    * @return the localised message
    */
   public String getChoiceMessage(String key, double param, Object... msgParams)
   {
      if (!key.endsWith(".choice"))
      {
         key += ".choice";
      }

      Object value = null;
      ChoiceFormat choiceFormat = null;

      if (dictionary != null)
      {
         value = dictionary.get(key);

         if (value instanceof ChoiceFormat)
         {
            choiceFormat = (ChoiceFormat)value;
         }
      }

      if (choiceFormat != null)
      {
         String val = choiceFormat.format(param);

         if (msgParams.length == 0)
         {
            return val;
         }
         else
         {
            MessageFormat fmt = new MessageFormat(val);

            return fmt.format(msgParams);
         }
      }
      else if (value instanceof Format)
      {
         return ((Format)value).format(msgParams);
      }
      else if (value != null)
      {
         return value.toString();
      }
      else
      {
         return key;
      }
   }

   /**
    * Gets a choice message from the dictionary.
    * If there's no match on the supplied label or the dictionary
    * hasn't been loaded, the default format
    * will be used instead.
    * @param key the label identifying the message
    * @param defFmt the default format
    * @param param the choice value
    * @return the localised message
    */
   public String getChoiceMessageWithDefault(String key, String defFmt, double n)
   {
      if (!key.endsWith(".choice"))
      {
         key += ".choice";
      }

      return getMessageWithDefault(key, defFmt, n);
   }

   /**
    * Gets a mnemonic from the dictionary file.
    * @param key the label identifying the mnemonic
    * @return the mnemonic's code point or -1 if not found
    */ 
   public int getMnemonic(String key)
   {
      return getMnemonic(key, -1);
   }

   /**
    * Gets a mnemonic from the dictionary file.
    * @param key the label identifying the mnemonic
    * @param defValue the default value if not found
    * @return the mnemonic's code point or the default value if not found
    */ 
   public int getMnemonic(String key, int defValue)
   {
      if (!key.endsWith(".mnemonic"))
      {
         key += ".mnemonic";
      }

      Object value = null;

      if (dictionary != null)
      {
         value = dictionary.get(key);
      }

      if (value == null) return defValue;

      if (value instanceof Number)
      {
         return ((Number)value).intValue();
      }

      return value.toString().codePointAt(0);
   }

   /**
    * Gets the agreement text from the dictionary file with a
    * default if not set.
    * @return the agreement text
    */ 
   public String getConfirmText()
   {
      if (pass.isGroupProject())
      {
         return getMessageWithDefault("message.we_confirm",
           "We agree that by submitting a PDF generated by PASS we are confirming that we have checked the PDF and that it correctly represents our submission.");
      }
      else
      {
         return getMessageWithDefault("message.i_confirm",
           "I agree that by submitting a PDF generated by PASS I am confirming that I have checked the PDF and that it correctly represents my submission.");
      }
   }

   /**
    * Tests if the given text is a valid user name.
    * @param text the text that may be a user name
    * @return true if the given text matches the user name regular
    * expression
    */ 
   public boolean isValidUserName(String text)
   {
      return text.matches(AssignmentProcessConfig.USER_NAME_REGEX);
   }

   /**
    * Tests if the given text is a registration number.
    * @param text the text that may be a registration number
    * @return true if the given text matches the registration number regular
    * expression
    */ 
   public boolean isValidRegNum(String text)
   {
      return text.matches(AssignmentProcessConfig.REG_NUM_REGEX);
   }

   /**
    * Tests if the given attribute is considered true/on.
    * @param attributeName the attribute name
    * @param atts the attributes that may contain the attribute name
    * @param qName the qualified name of the element with the
    * attributes
    * @param defValue the default value if the attribute hasn't been
    * set
    * @return true if the given attribute value is "true" or "yes", or false if
    * the attribute value is "false" or "no", or the
    * default value if the attribute isn't set
    * @throws SAXException if the attribute is set but isn't
    * "true" or "yes" or "false" or "no"
    */ 
   public boolean isBoolAttributeOn(String attributeName, Attributes atts,
    String qName, boolean defValue)
   throws SAXException
   {
      String value = atts.getValue(attributeName);

      if (value == null) return defValue;

      return isBoolValueOn(attributeName, value, qName);
   }

   /**
    * Tests if the given required attribute is considered true/on.
    * @param attributeName the attribute name
    * @param atts the attributes that may contain the attribute name
    * @param qName the qualified name of the element with the
    * attributes
    * @return true if the given attribute is true/yes, or false if
    * the attribute is set but is false/no, otherwise throws an
    * exception
    */ 
   public boolean isRequiredBoolAttributeOn(String attributeName, Attributes atts,
     String qName)
   throws SAXException
   {
      String value = atts.getValue(attributeName);

      if (value == null || value.isEmpty())
      {
         throw new SAXException(getMessageWithDefault(
           "error.tag_missing_attribute",
           "<{0}> ''{1}'' attribute missing or empty.", qName, attributeName));
      }

      return isBoolValueOn(attributeName, value, qName);
   }

   /**
    * Tests if the given attribute value is considered true/on.
    * @param attributeName the attribute name
    * @param value the attribute's value
    * @param qName the qualified name of the element with the
    * attributes
    * @return true if the given value is "true" or "yes", or false if
    * the value is "false" or "no", otherwise throws an
    * exception
    */ 
   public boolean isBoolValueOn(String attributeName, String value, String qName)
   throws SAXException
   {
      if (value.equals("true") || value.equals("yes"))
      {
         return true;
      }

      if (value.equals("false") || value.equals("no"))
      {
         return false;
      }

      throw new SAXException(getMessageWithDefault(
         "error.boolean_tag_attribute_required",
         "<{0}> tag attribute ''{1}'' must have a boolean value (found ''{2}'').",
         attributeName, value, qName));
   }

   /**
    * Line wraps the string. If the given text is longer than the
    * line width, it will be broken up.
    * @param text the text to line wrap
    * @param lineWidth the line width
    * @param linebreak the line break character(s)
    * @return the processed text
    */ 
   public String stringWrap(String text, int lineWidth, String linebreak)
   {
      if (text.length() < lineWidth) return text;

      StringBuilder builder = new StringBuilder(text.length());

      BreakIterator it = BreakIterator.getLineInstance();
      it.setText(text);

      int start = it.first();
      int j = 0;

      if (start >= lineWidth)
      {
         builder.append(text.substring(0, start));
         builder.append(linebreak);
      }
      else if (start > 0)
      {
         builder.append(text.substring(0, start));
         j = start;
      }

      for (int end = it.next(); end != BreakIterator.DONE;
           start = end, end = it.next())
      {
         j += end-start;

         if (j >= lineWidth)
         {
            builder.append(linebreak);
            j = 0;

            int cp = text.codePointAt(start);

            while (start < end && Character.isWhitespace(cp))
            {
               start += Character.charCount(cp);
               cp = text.codePointAt(start);
            }
         }

         builder.append(text.substring(start, end));
      }

      return builder.toString();
   }

   /**
    * Test the HTTP connection to the given URL.
    * @param url the URL to test
    * @return the response code
    */ 
   public int testHttpURLConnection(URL url) throws IOException
   {
      HttpURLConnection con = null;

      try
      {
         con = (HttpURLConnection)url.openConnection();
         con.setRequestMethod("HEAD");
         con.setConnectTimeout(5000);// 5 seconds
         con.setReadTimeout(5000);// 5 seconds
         HttpURLConnection.setFollowRedirects(false);

         return con.getResponseCode();
      }
      catch (javax.net.ssl.SSLHandshakeException e)
      {
         throw new InputResourceException(getMessageWithDefault(
           "error.http_ssl_failed", 
           "Can't fetch ''{0}''. It''s possible you''re trying to access a file across SSL/TCP but you don''t have the certificate listed in your Java cacerts file.\nIf you have an old version of Java you may find that upgrading it resolves the issue. Otherwise you will need to add the required certificate.",
           url), e);
      }
      finally
      {
         if (con != null)
         {
            con.disconnect();
         }
      }
   }

   /**
    * Gets the main Pass object.
    */ 
   public Pass getPass()
   {
      return pass;
   }

   /**
    * Checks the debug mode. This is just a shortcut that uses the
    * same method in the Pass object.
    * @return true if in debug mode 
    */ 
   public boolean isDebugMode()
   {
      return pass.isDebugMode();
   }

   /**
    * Adds an application named in the resources.xml file with the
    * application's path.
    * @param name the name as identified in the resource file
    * @param file the file obtained from the uri attribute
    */ 
   public void addApplication(String name, File file)
   {
      applications.put(name, file);
   }

   /**
    * Loads the list of available courses from the given URL. Each course is
    * identified by the resource element.
    * @param resourceURL the URL of the resources XML file
    * @return a list of all available courses
    */ 
   public Vector<Course> loadCourseData(URL resourceURL) throws IOException,SAXException
   {
      if (resourceURL == null)
      {
         throw new NullPointerException();
      }

      CourseParser parser = new CourseParser(this);

      BufferedReader in = null;

      try
      {
         in = new BufferedReader(new InputStreamReader(
            resourceURL.openStream()));
         parser.parse(new InputSource(in));
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      return parser.getData();
   }

   /**
    * Gets the path of an application identified in the
    * resources.xml file. The application should have previously
    * been added with addApplication() if it was identified in the resources
    * XML file.
    * @param name the application identifier use in resources.xml
    * @return the path obtained from the resource file setting or
    * null if not set
    */ 
   public File findResourceApplication(String name)
      throws IOException
   {
      if (applications != null)
      {
         File f = applications.get(name);

         if (f != null)
         {
            return f;
         }
      }  

      return null;
   }

   /**
    * Gets the path of an application. This first tries
    * findResourceApplication(name) and then tries to find name or
    * any of the provided alternatives the system path. When search
    * the system path this will first try the supplied name and then
    * retry with ".exe" appended.
    * @param name the application name
    * @return the path to the application
    * @throws IOException if no match is found or if I/O error
    * occurs
    */ 
   public File findApplication(String name, String... alternatives)
      throws IOException
   {
      File f = findResourceApplication(name);

      if (f != null)
      {
         return f;
      }

      String path = null;

      try
      {
         path = System.getenv("PATH");
      }
      catch (SecurityException e)
      {
         throw new IOException(
           getMessageWithDefault("error.cant_find_application.path_not_accessible",
           "Can''t find application ''{0}'': PATH not accessible.",
           name), e);
      }

      if (path == null || path.isEmpty())
      {
         throw new FileNotFoundException(
           getMessageWithDefault("error.cant_find_application.path_not_set",
           "Can''t find application ''{0}'': PATH not set.",
           name));
      }

      String[] dirs = path.split(File.pathSeparator);

      for (String dirname : dirs)
      {
         File dir = new File(dirname);

         File file = new File(dir, name);

         if (file.exists())
         {
            return file;
         }

         file = new File(dir, name+".exe");

         if (file.exists())
         {
            return file;
         }
      }

      for (String other : alternatives)
      {
         for (String dirname : dirs)
         {
            File dir = new File(dirname);

            File file = new File(dir, other);

            if (file.exists())
            {
               return file;
            }

            file = new File(dir, other+".exe");

            if (file.exists())
            {
               return file;
            }
         }
      }

      StringBuilder builder = new StringBuilder();

      builder.append(String.format("'%s', '%s.exe'", name, name));

      for (String other : alternatives)
      {
         builder.append(String.format(", '%s', '%s.exe'", other, other));
      }

      pass.debug("PATH="+path);

      throw new FileNotFoundException(
         getMessageWithDefault("error.cant_find_application.tried",
         "Can''t find application ''{0}'' (tried: {1}).",
          name, builder.toString()));
   }

   /**
    * Tries to find the application with the given name on the
    * system path. If the given name isn't found and name doesn't
    * have an extension, and on Windows, then retries with
    * ".exe" appended.
    * @param name the application name
    * @return the path to the application or null if not found
    */ 
   public File findOnPath(String name) throws IOException
   {
      String path = null;

      try
      {
         path = System.getenv("PATH");
      }
      catch (SecurityException e)
      {
         throw new IOException(
           getMessageWithDefault("error.cant_find_application.path_not_accessible",
           "Can''t find application ''{0}'': PATH not accessible.",
           name), e);
      }

      if (path == null || path.isEmpty())
      {
         throw new FileNotFoundException(
           getMessageWithDefault("error.cant_find_application.path_not_set",
           "Can''t find application ''{0}'': PATH not set.", name));
      }

      String ext = null;

      int idx = name.lastIndexOf(".");

      if (idx != -1)
      {
         ext = name.substring(idx+1).toLowerCase();
      }

      String[] dirs = path.split(File.pathSeparator);

      for (String dirname : dirs)
      {
         File dir = new File(dirname);

         File file = new File(dir, name);

         if (file.exists())
         {
            return file;
         }

         if (ext == null && isWindows())
         {
            file = new File(dir, name+".exe");

            if (file.exists())
            {
               return file;
            }
         }
      }

      return null;
   }

   /**
    * Gets the path to the JRE. On Windows, this first tries "javaw"
    * and then tries "java". Otherwise it just tries "java".
    * @return the path to java
    */ 
   public File getJavaInvoker() throws IOException
   {
      return isWindows() ? findApplication("javaw", "java")
        : findApplication("java");
   }

   /**
    * Gets the path to the Java compiler.
    * @return the path to javac
    */ 
   public File getJavaCompilerInvoker() throws IOException
   {
      return findApplication("javac");
   }

   /**
    * Adds the default set of compiler flags for Java to the
    * argument list. This is in addition to any instances of
    * compiler-arg in the assignment XML file.
    * @param argList the argument list
    */ 
   public void addJavaCompilerArgs(Vector<String> argList)
   {
      argList.add("-Xlint:unchecked");
      argList.add("-Xlint:deprecation");
      argList.add("-encoding");
      argList.add(pass.getEncoding());
   }

   /**
    * Gets the path to the C++ compiler.
    * @return the path to g++ or c++ or gcc-c++
    */ 
   public File getCppCompilerInvoker() throws IOException
   {
      return findApplication("g++", "c++", "gcc-c++");
   }

   /**
    * Adds the default set of compiler flags for C++ to the
    * argument list. This is in addition to any instances of
    * compiler-arg in the assignment XML file.
    * @param argList the argument list
    */ 
   public void addCppCompilerArgs(Vector<String> argList)
   {
      argList.add("-Wall");
      argList.add("-o");
      argList.add(getCppOutputName());
   }

   /**
    * Gets the default output name for C++ applications.
    * @return the default filename
    */ 
   public String getCppOutputName()
   {
      String label = pass.getAssignment().getLabel();

      if (isWindows())
      {
         label += ".exe";
      }

      return label;
   }

   /**
    * Gets the path to the C compiler.
    * @return the path to gcc or cc
    */ 
   public File getCCompilerInvoker() throws IOException
   {
      return findApplication("gcc", "cc");
   }

   /**
    * Adds the default set of compiler flags for C to the
    * argument list. This is in addition to any instances of
    * compiler-arg in the assignment XML file.
    * @param argList the argument list
    */ 
   public void addCCompilerArgs(Vector<String> argList)
   {
      argList.add("-Wall");
      argList.add("-o");
      argList.add(getCOutputName());
   }

   /**
    * Gets the default output name for C applications.
    * @return the default filename
    */ 
   public String getCOutputName()
   {
      String label = pass.getAssignment().getLabel();

      if (isWindows())
      {
         label += ".exe";
      }

      return label;
   }

   /**
    * Gets the path to the Perl interpreter.
    * @return the path to perl
    */ 
   public File getPerlInvoker() throws IOException
   {
      return findApplication("perl");
   }

   /**
    * Adds the default set of options for Perl to the
    * argument list. This is in addition to any instances of
    * invoker-arg in the assignment XML file.
    * @param argList the argument list
    */ 
   public void addPerlArgs(Vector<String> argList)
   {
      argList.add("-w");
   }

   /**
    * Gets the path to the Lua interpreter.
    * @return the path to lua or texlua
    */ 
   public File getLuaInvoker() throws IOException
   {
      return findApplication("lua", "texlua");
   }

   /**
    * Gets the path to the Bash.
    * @return the path to bash
    */ 
   public File getBashInvoker() throws IOException
   {
      return findApplication("bash");
   }

   /**
    * Finds a file with the given base name relative to the given
    * starting point.
    * @param start the search starting point
    * @param name the base name of the file
    * @return the file if found or null if not found
    */ 
   public static File findFile(File start, String name)
   {
      if (start == null) return null;

      Path startPath = start.toPath();

      File relFile = new File(name);
      Path relPath = relFile.toPath();

      String basename = relFile.getName();

      Path parentPath = relPath.getParent();

      if (parentPath != null)
      {
         Path path = startPath.resolve(relPath);
         File f = path.toFile();

         if (f.exists())
         {
            return f;
         }
      }

      if (!start.isDirectory())
      {
         if (startPath.endsWith(relPath))
         {
            return start;
         }

         return null;
      }

      File[] list = start.listFiles();

      for (File file : list)
      {
         File f = findFile(file, name);

         if (f != null)
         {
            return f;
         }
      }

      return null;
   }

   /**
    * Returns true if Pass is running on Windows.
    */ 
   public boolean isWindows()
   {
      return isWindows;
   }

   /**
    * Returns true if the student is required to agree to the
    * agreement text.
    * @return true if agreement required
    */ 
   public boolean isAgreeRequired()
   {
      return agreeRequired;
   }

   /**
    * Sets whether or not agreement is required.
    * @param required true if agreement is required
    */ 
   public void setAgreeRequired(boolean required)
   {
      agreeRequired = required;
   }

   /**
    * Loads all assignment data for the given course.
    * @param course the course
    * @return the list of all assignments defined in the course XML
    * file
    */ 
   public Vector<AssignmentData> loadAssignments(Course course)
    throws SAXException,IOException
   {
      AssignmentDataParser parser = new AssignmentDataParser(pass);

      parser.loadAssignments(course);

      Vector<AssignmentData> assignments = parser.getData();

      listingSettings = parser.getListingsSettings();
      geometrySettings = parser.getGeometrySettings();
      fontSpecSettings = parser.getFontSpecSettings();
      fontSpecOptions = parser.getFontSpecOptions();
      fontEncSettings = parser.getFontEncSettings();
      fontEncOptions = parser.getFontEncOptions();

      Long val = parser.getMaxOutputSetting();

      if (val != null)
      {
         maxOutputSetting = val.longValue();
      }

      Integer intVal = parser.getVerbMaxCharsPerLine();

      if (intVal != null)
      {
         verbMaxCharsPerLine = intVal.intValue();
      }

      intVal = parser.getVerbTabCharCount();

      if (intVal != null)
      {
         verbTabCharCount = intVal.intValue();
      }

      return assignments;
   }

   /**
    * Sets the options for the listings package.
    * @param settings the listings.sty settings
    */ 
   public void setListingSettings(CharSequence settings)
   {
      listingSettings = settings;
   }

   /**
    * Gets the listings settings.
    * @return the settings for listings.sty
    */ 
   public CharSequence getListingSettings()
   {
      return listingSettings;
   }

   /**
    * Sets the options for the geometry package.
    * @param settings the geometry.sty settings
    */ 
   public void setGeometrySettings(CharSequence settings)
   {
      geometrySettings = settings;
   }

   /**
    * Gets the geometry settings.
    * @return the settings for geometry.sty
    */ 
   public CharSequence getGeometrySettings()
   {
      return geometrySettings;
   }

   /**
    * Gets the fontspec settings.
    * @return the settings for fontspec.sty
    */ 
   public CharSequence getFontSpecSettings()
   {
      return fontSpecSettings;
   }

   /**
    * Sets the options for the fontspec package.
    * @param settings the fontspec.sty settings
    */ 
   public String getFontSpecOptions()
   {
      return fontSpecOptions;
   }

   /**
    * Gets the fontenc settings.
    * @return the settings for fontenc.sty
    */ 
   public CharSequence getFontEncSettings()
   {
      return fontEncSettings;
   }

   /**
    * Sets the options for the fontenc package.
    * @param settings the fontenc.sty settings
    */ 
   public String getFontEncOptions()
   {
      return fontEncOptions;
   }

   /**
    * Sets the maximum length of output. This is necessary in case a
    * student makes their application so verbose that the document
    * ends up hundreds of pages long, so it needs to be truncated at
    * a reasonable limit otherwise the LaTeX process will time out.
    * @param setting the maximum number of characters to write
    * before truncating
    */ 
   public void setMaxOutputSetting(long setting)
   {
      maxOutputSetting = setting;
   }

   /**
    * Gets the maximum length of output.
    * @return the maximum number of output characters to write before
    * truncating
    */ 
   public long getMaxOutputSetting()
   {
      return maxOutputSetting;
   }

   /**
    * Gets the maximum number of characters to allow in a line of
    * verbatim text. (This is easier to implement in Java than
    * trying to set the line wrapping parameters in listings.)
    * @return the maximum number of characters in a line of verbatim
    * text
    */ 
   public int getVerbMaxCharsPerLine()
   {
      return verbMaxCharsPerLine;
   }

   /**
    * Gets the number of characters a TAB spans.
    * @return the TAB character count
    */ 
   public int getVerbTabCharCount()
   {
      return verbTabCharCount;
   }

   /**
    * Sets the braces setting for LuaLaTeX.
    * @param value true if braces need to be added for filenames
    * that don't have an extension
    */
   public void setLuaLaTeXBraces(boolean value) 
   {
      luaLaTeXBraces = value;
   }

   /**
    * Indicates whether or not a filename needs to be enclosed with
    * an extra set of braces for code listings. This is needed for
    * old LuaLaTeX if the filename doesn't have an extension but
    * breaks with new versions.
    * @return true if braces need to be added
    */ 
   public boolean areBracesRequired(boolean isLua, String filename)
   {
      return luaLaTeXBraces && isLua && !filename.contains(".");
   }

   /**
    * Puts an environment variable and its value to the process 
    * environment map. This will replace any previous value of that
    * variable, if it already exists in the map
    * @param envName the environment variable name
    * @param envValue the environment variable's value
    */ 
   public void addProcessEnvironmentVariable(String envName, String envValue)
   {
      if (processEnvMap == null)
      {
         processEnvMap = new HashMap<String,String>();
      }

      processEnvMap.put(envName, envValue);
   }

   /**
    * Adds the environment variable map to the process builder.
    * @param pb the process builder
    */ 
   public void addEnvironmentVariablesToProcess(ProcessBuilder pb)
   {
      if (processEnvMap == null) return;

      Map<String, String> env = pb.environment();

      env.putAll(processEnvMap);
   }

   /**
    * Creates a temporary directory in which all the temporary files
    * will be placed. This directory and all its contents will be
    * deleted on exit.
    * @return the temporary directory
    */
   public File createTempDirectory() throws IOException
   {
      if (tmpDir == null || !tmpDir.exists())
      {
         tmpDir = Files.createTempDirectory("prepasg").toFile();
      }

      return tmpDir;
   }

   /**
    * Gets the temporary directory.
    * @return the temporary directory or null if it hasn't been
    * created
    */ 
   public File getTempDirectory()
   {
      return tmpDir;
   }

   /**
    * Call when the Pass application is ready to exit.
    * This deletes the temporary directory if it exists
    */ 
   public void closeDown()
   {
      if (tmpDir != null && tmpDir.exists())
      {
         if (!deleteDir(tmpDir))
         {
            pass.error(getMessageWithDefault("error.cant_rm_temp_dir",
             "Unable to delete temporary directory\n{0}",
             tmpDir.getAbsolutePath()));
         }
      }
   }

   /**
    * Deletes the given directory and its contents. Recursively
    * descends all sub directories. Use with care!
    * @return true if deletion was successful
    */ 
   public static boolean deleteDir(File dir)
   {
      if (!dir.isDirectory())
      {
         return dir.delete();
      }

      File[] fileList = dir.listFiles();

      for (File file : fileList)
      {
         if (!deleteDir(file))
         {
            return false;
         }
      }

      return dir.delete();
   }

   /**
    * Creates a buffered reader using Pass's default encoding.
    */ 
   public BufferedReader newBufferedReader(File file) throws IOException
   {
      return Files.newBufferedReader(file.toPath(), 
         Charset.forName(pass.getEncoding()));
   }

   /**
    * Creates an input stream reader using Pass's default encoding.
    */ 
   public InputStreamReader newInputStreamReader(File file) throws IOException
   {
      return new InputStreamReader(
        new FileInputStream(file), Charset.forName(pass.getEncoding()));
   }

   /**
    * Checks if the given file is permitted for the given
    * assignment. The student isn't permitted to upload assignment resource
    * files or result files or non-allowed binaries.
    * @param data the assignment data
    * @param file the file to check
    * @throws InvalidFileException if file isn't allowed
    */ 
   public void checkFileName(AssignmentData data, File file)
      throws InvalidFileException
   {
      if (data.isAllowedBinary(file))
      {
         return;
      }

      String baseName = file.getName();

      if (data.hasResourceBaseName(baseName))
      {
         throw new InvalidFileException(
            getMessageWithDefault("error.resource_filename_conflict",
             "File ''{0}'' conflicts with assignment resource file ''{1}''.",
             file.getAbsolutePath(), baseName));
      }

      if (data.hasResultFile(baseName))
      {
         throw new InvalidFileException(
            getMessageWithDefault("error.result_filename_conflict",
             "File ''{0}'' conflicts with assignment result file ''{1}''.",
             file.getAbsolutePath(), baseName));
      }

      if (baseName.equals("a.out") || isBannedFile(baseName))
      {
         throw new InvalidFileException(
           getMessageWithDefault("error.forbidden_file",
            "File ''{0}'' forbidden (executable or archive files should not be uploaded).",
            baseName));
      }
   }

   /**
    * Tests if the given file is banned.
    * @return true if the file is banned
    */ 
   public boolean isBannedFile(File file)
   {
      return isBannedFile(file.getName());
   }

   /**
    * Tests if the given file is banned. Executables, classes,
    * object files and archives are banned. The purpose of PASS is
    * to compile the code using the designated compiler flags (which
    * may require conformance to a specified standard), and to test
    * the code. This ensures that the binaries created via PASS
    * match the code submitted. These files should therefore be unnecessary,
    * and allowing them runs the risk of incorporating
    * malware in the PDF attachments.
    * @return true if the file is banned
    */ 
   public boolean isBannedFile(String filename)
   {
      int idx = filename.lastIndexOf(".");

      if (idx == -1) return false;

      String ext = filename.substring(idx+1).toLowerCase();

      return ext.equals("zip") || ext.equals("exe") || ext.equals("tar")
       || ext.equals("gz") || ext.equals("tgz") || ext.equals("jar")
       || ext.equals("a") || ext.equals("ar") || ext.equals("iso")
       || ext.equals("bz2") || ext.equals("lz") || ext.equals("lz4")
       || ext.equals("xz") || ext.equals("7z") || ext.equals("s7z")
       || ext.equals("cab") || ext.equals("class") || ext.equals("o");
   }

   /**
    * Checks if the given file is a required file.
    * @param path the file to test
    * @param assignment the assignment data
    * @param basePath the base path (null if no relative path)
    */ 
   public boolean isRequiredFile(Path path, AssignmentData assignment, Path basePath)
   {
      String filename;

      if (basePath == null)
      {
         filename = path.toFile().getName();
      }
      else
      {
         Path relPath = basePath.relativize(path);
         filename = relPath.toString();
      }

      return assignment.hasFile(filename);
   }

   /**
    * Gets the checksum for the given file.
    * @param file the file
    * @return the checksum
    */
   public String getCheckSum(File file)
     throws IOException, NoSuchAlgorithmException
   {
      return config.getCheckSum(Files.readAllBytes(file.toPath()));
   }

   private HashMap<String,File> applications = null;
   private Pass pass;
   private boolean isWindows=false;
   private boolean agreeRequired = true;

   private CharSequence listingSettings;
   private CharSequence geometrySettings;
   private CharSequence fontSpecSettings, fontEncSettings;
   private String fontSpecOptions, fontEncOptions;
   private long maxOutputSetting = 10240L;
   private int verbMaxCharsPerLine = 80, verbTabCharCount=8;
   private boolean luaLaTeXBraces=false;

   private HashMap<String,String> processEnvMap;

   private HashMap<String,Object> dictionary;

   private AssignmentProcessConfig config;

   private File tmpDir = null;
}
