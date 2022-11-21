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
import java.time.LocalDateTime;
import java.text.ParseException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.File;

/**
 * Assignment data. This consist of:
 * <ul>
 * <li>a label identifier;
 * <li>the assignment title;
 * <li>the associated course;
 * <li>the submission date;
 * <li>the programming language and optionally the language variant
 * (the variant is just for the listings package);
 * <li>a list of required files (that the student must submit) which
 * may be empty if the student is permitted their own file naming
 * scheme;
 * <li>the file in the list of required files that has the "main"
 * method (needed for Java assignments);
 * <li>an optional list of resource files (these are files supplied by the
 * lecturer that the student's programme has to input);
 * <li>an optional list of command line arguments (these are arguments that
 * PASS has to supply to the student's programme when it's invoked)
 * <li>an optional list of lines that should be supplied to the
 * student's application via STDIN;
 * <li>an optional list of files that the student's application must
 * create;
 * <li>the list of command line arguments to pass to the compiler;
 * <li>the list of command line arguments to pass to the invoker
 * (java, perl etc);
 * <li>whether or not PASS should compile the source code (this step
 * will always be skipped for non-compiled languages);
 * <li>whether or not PASS should run the student's application to
 * test it;
 * <li>an optional script to use to build and run the application
 * instead of using PASS's default action;
 * <li>an optional list of basenames (without the pdf or doc
 * extension) for any reports that should
 * accompany the submission.
 * </ul>
 */
public class AssignmentData
{
   public AssignmentData(Course course)
   {
      this.course = course;
      title = "Untitled";
      fileList = new Vector<String>();
      resourceList = new Vector<ResourceFile>();
      allowedBinaryFilters = new Vector<AllowedBinaryFilter>();
      resultList = new Vector<ResultFile>();
      inputList = new Vector<String>();
      argList = new Vector<String>();
      compilerArgs = new Vector<String>();
      invokerArgs = new Vector<String>();
      reports = new Vector<String>();
   }

   /**
    * Gets the associated course.
    * @return the course 
    */ 
   public Course getCourse()
   {
      return course;
   }

   /**
    * Sets the assignment's title.
    * @param title the title
    */ 
   public void setTitle(String title)
   {
      this.title = title;
   }

   /**
    * Gets the assignment's title.
    * @return the title
    */ 
   public String getTitle()
   {
      return title;
   }

   /**
    * Adds the basename (without the pdf or doc extension) of a required report.
    * @param name the basename
    */ 
   public void addReport(String name)
   {
      reports.add(name);
   }

   /**
    * Gets the list of required report basenames.
    * @return list of basenames (without pdf or doc extensions)
    */ 
   public Vector<String> getReports()
   {
      return reports;
   }

   /**
    * Adds a required result file to the list.
    * This is a file that the student's application must create.
    * @param file the required result file
    */ 
   public void addResultFile(ResultFile file)
   {
      resultList.add(file);
   }

   /**
    * Adds a plain text resource file to the list.
    * This is a file supplied by the lecturer that the student's
    * application must input. It should be located in a place that
    * can be read from but can't be edited by the student.
    * Note that redirects aren't followed so the reference must be
    * the actual location. This method assumes that the file is
    * plain text.
    * @param href the URI of the resource file
    * @throws URISyntaxException if the href is invalid
    */ 
   public void addResourceFile(String href)
     throws URISyntaxException
   {
      addResourceFile(new URI(href));
   }

   /**
    * Adds a resource file to the list.
    * @param href the URI of the resource file
    * @param mimetype the file's mime type
    * @throws URISyntaxException if the href is invalid
    */ 
   public void addResourceFile(String href, String mimetype)
     throws URISyntaxException
   {
      addResourceFile(new URI(href), mimetype);
   }

   /**
    * Adds a plain text resource file to the list.
    * @param uri the location of the resource file
    */
   public void addResourceFile(URI uri)
   {
      resourceList.add(new ResourceFile(uri));
   }

   /**
    * Adds a resource file to the list.
    * @param uri the location of the resource file
    * @param mimetype the file's mime type
    */
   public void addResourceFile(URI uri, String mimetype)
   {
      resourceList.add(new ResourceFile(uri, mimetype));
   }

   /**
    * Adds a required source file. The student is required to
    * include a file with this name in their submission.
    * The listings order will match the order in which files are
    * added.
    * @param fileName the required source file's name
    */ 
   public void addFile(String fileName)
   {
      addFile(fileName, null);
   }

   /**
    * Adds a required source file with a template.
    * The template file is for PassEditor and may be null if
    * not required.
    * @param fileName the required source file's name
    * @param templateURL the location of the template file which may
    * be null
    */ 
   public void addFile(String fileName, URL templateURL)
   {
      fileList.add(fileName);

      if (templateURL != null)
      {
         addTemplate(fileName, templateURL);
      }
   }

   /**
    * Adds a required source file and identifies it as the file with
    * the "main" method. The file extension determines the
    * assignment's programming language.
    * @param fileName the required source file's name
    */ 
   public void addMainFile(String fileName)
   {
      addMainFile(fileName, null);
   }

   /**
    * Adds a required source file and identifies it as the file with
    * the "main" method/function. The file extension determines the
    * assignment's programming language.
    * The template file is for PassEditor and may be null if
    * not required.
    * @param fileName the required source file's name
    * @param templateURL the location of the template file which may
    * be null
    */ 
   public void addMainFile(String fileName, URL templateURL)
   {
      fileList.add(fileName);
      mainFile = fileName;

      String ext = mainFile.substring(fileName.lastIndexOf(".")+1);

      language = getListingLanguage(ext);

      if (templateURL != null)
      {
         addTemplate(fileName, templateURL);
      }
   }

   /**
    * Gets the required source code file that was identified as
    * having the "main" method/function.
    */ 
   public String getMainFile()
   {
      return mainFile;
   }

   /**
    * Gets the default file extension for source code files.
    * This is the file extension of the source code file identified
    * as having a "main" method/function.
    * @return the extension of the main source code file if provided
    * or null otherwise
    */ 
   public String getDefaultExtension()
   {
      if (mainFile == null)
      {
         if (language != null)
         {
            if ("C++".equals(language))
            {
               return "cpp";
            }
            else if ("Perl".equals(language))
            {
               return "pl";
            }
            else if ("bash".equals(language))
            {
               return "sh";
            }
            else if ("command.com".equals(language))
            {
               return "bat";
            }
            else if ("Python".equals(language))
            {
               return "py";
            }
            else if ("Matlab".equals(language))
            {
               return "m";
            }
            else if ("make".equals(language))
            {
               return "mk";
            }
            else if ("Assembler".equals(language))
            {
               return "s";
            }
            else
            {
               return language.toLowerCase();
            }
         }

         return null;
      }

      int idx = mainFile.lastIndexOf(".");

      if (idx > 0)
      {
         return mainFile.substring(idx+1);
      }

      return mainFile;
   }

   /**
    * Gets the number of required source code files.
    * @return the number of required source code files
    */ 
   public int fileCount()
   {
      return fileList.size();
   }

   /**
    * Gets the required source code file with the given index.
    * @param index the index
    * @return the name of the required source code file at that
    * index
    */ 
   public String getFile(int index)
   {
      return fileList.get(index);
   }

   /**
    * Tests whether or not the given filename has been identified as
    * a required source code file. 
    * @param filename
    * @return true if the given filename has been identified as a
    * required source code file
    */ 
   public boolean hasFile(String filename)
   {
      return fileList.contains(filename);
   }

   /**
    * Gets the iterator over all required source code files.
    * @return the iterator
    */ 
   public Iterator<String> getFileIterator()
   {
      return fileList.iterator();
   }

   /**
    * Adds an allowed binary filter. 
    * @param filter the allowed filter to add
    */ 
   public void addAllowedBinary(AllowedBinaryFilter filter)
   {
      allowedBinaryFilters.add(filter);
   }

   /**
    * Gets all allowed binary filters.
    * @return list of all allowed binary filters
    */ 
   public Vector<AllowedBinaryFilter> getAllowedBinaryFilters()
   {
      return allowedBinaryFilters;
   }

   /**
    * Has allowed binary files.
    */ 
   public boolean hasAllowedBinaries()
   {
      return allowedBinaryFilters.size() > 0;
   }

   /**
    * Tests if the given file matches an allowed binary filter.
    * @param file the file to test
    * @return true if the file matches an allowed binary filter
    */ 
   public boolean isAllowedBinary(File file)
   {
      for (AllowedBinaryFilter filter : allowedBinaryFilters)
      {
         if (filter.accept(file))
         {
            return true;
         }
      }

      return false;
   }

   /**
    * Gets the allowed binary filter that matches the given file.
    * @param file the file to test
    * @return the allowed binary filter or null if no match
    */ 
   public AllowedBinaryFilter getAllowedBinaryFilter(File file)
   {
      return getAllowedBinaryFilter(file, null);
   }

   /**
    * Gets the allowed binary filter that matches the given file or
    * mime type.
    * @param file the file to test
    * @param mimeType the mime type
    * @return the allowed binary filter or null if no match
    */ 
   public AllowedBinaryFilter getAllowedBinaryFilter(File file, String mimeType)
   {
      for (AllowedBinaryFilter filter : allowedBinaryFilters)
      {
         if ((file != null && filter.accept(file))
          || (mimeType != null && filter.getMimeType().equals(mimeType))
            )
         {
            return filter;
         }
      }

      return null;
   }

   /**
    * Gets the number of all resource files required by this assignment.
    * @return the number of resource files
    */ 
   public int resourceFileCount()
   {
      return resourceList.size();
   }

   /**
    * Gets the resource file at the given index.
    * @param index the index
    * @return the resource file at the index
    */ 
   public ResourceFile getResourceFile(int index)
   {
      return resourceList.get(index);
   }

   /**
    * Tests if the supplied basename matches the basename of any
    * required resource files. In this case the basename is the
    * filename without the path.
    * @param basename the basename
    * @return true if any of the supplied resource files matches the
    * basename
    */ 
   public boolean hasResourceBaseName(String basename)
   {
      for (ResourceFile rf : resourceList)
      {
         if (basename.equals(rf.getBaseName()))
         {
            return true;
         }
      }

      return false;
   }

   /**
    * Tests if the given URI matches any of the required resource
    * files.
    * @param uri the URI
    * @return true if the URI matches any identified resource files
    */ 
   public boolean hasResourceFile(URI uri)
   {
      for (ResourceFile rf : resourceList)
      {
         if (rf.getUri().equals(uri))
         {
            return true;
         }
      }

      return false;
   }

   /**
    * Gets the iterator over all resource files.
    */ 
   public Iterator<ResourceFile> getResourceFileIterator()
   {
      return resourceList.iterator();
   }

   /**
    * Gets the list of all required result files. These are the
    * files that the student's application should create.
    * @return the list of all required result files 
    */ 
   public Vector<ResultFile> getResultFiles()
   {
      return resultList;
   }

   /**
    * Tests if the given filename has been identified as a required
    * result file.
    * @param name the filename
    * @return true if the name matches any of the required result
    * filenames
    */ 
   public boolean hasResultFile(String name)
   {
      for (ResultFile f : resultList)
      {
         if (f.getName().equals(name))
         {
            return true;
         }
      }

      return false;
   }

   /**
    * Sets the date that this assignment is due.
    * @param source the date supplied as a string 
    * @throws ParseException if the supplied string is invalid
    */ 
   public void setDueDate(String source) throws ParseException
   {
      due = LocalDateTime.parse(source, AssignmentProcessConfig.DATE_TIME_PARSER);
   }

   /**
    * Gets the local date and time that the assignment is due by.
    * @return the local timestamp that the assignment is due
    */ 
   public LocalDateTime getDueDate()
   {
      return due;
   }

   /**
    * Gets the date that the assignment is due by.
    * @return the date that the assignment is due as a Calendar
    * object
    */ 
   public Calendar getDueCalendarDate()
   {
      Calendar cal = Calendar.getInstance();
      cal.set(due.getYear(), due.getMonthValue()-1, due.getDayOfMonth(), 
       due.getHour(), due.getMinute(), due.getSecond());

      return cal;
   }

   /**
    * Gets the due by date as a formatted string.
    * @return the formatted due by date
    */ 
   public String formatDueDate()
   {
      return AssignmentProcessConfig.DATE_TIME_FORMATTER.format(due);
   }

   /**
    * String representation of this object.
    * @return the assignment title
    */
   @Override 
   public String toString()
   {
      return title;
   }

   /**
    * Sets the language variant for the listings package.
    * @param variant a variant of the language recognised by 
    * listings.sty
    */ 
   public void setLanguageVariant(String variant)
   {
      this.variant = variant;
   }

   /**
    * Gets the language variant. If not set, this will return the
    * default variant.
    * @return the language variant to use with listings.sty or null
    * if no set and no default
    */ 
   public String getLanguageVariant()
   {
      if (variant != null)
      {
         return variant;
      }

      return getDefaultVariant();
   }

   /**
    * Gets the default language variant.
    * @return the default for the assignment's language or null if
    * no default variant available
    */ 
   public String getDefaultVariant()
   {
      if ("Lua".equals(language))
      {
         return "5.2";
      }

      return variant;
   }

   /**
    * Gets the main programming language.
    * @return the main programming language
    */ 
   public String getMainLanguage()
   {
      return language;
   }

   /**
    * Sets the main programming language.
    * @param lang the main programming language
    */ 
   public void setMainLanguage(String lang)
   {
      language = lang;
   }

   /**
    * Gets the language identifier for the given file
    * extension. In addition to the listings.sty language
    * identifiers, this may also return "PDF", "DOC", "Plain Text", 
    * "---" or the main language name if its lowercase value equals the
    * file extension or if there's no match.
    * @param ext the file extension
    * @return the language identifier or the main
    * language if no mapping available
    */ 
   public String getListingLanguage(String ext)
   {
      if ("java".equals(ext))
      {
         return "Java";
      }
      else if ("cpp".equals(ext) || "cp".equals(ext)
          || "cc".equals(ext) || "C".equals(ext)
          || "CPP".equals(ext) || "c++".equals(ext)
          || "cxx".equals(ext) || "hh".equals(ext)
          || "hpp".equals(ext) || "H".equals(ext))
      {
         return "C++";
      }
      else if ("h".equals(ext))
      {
         return "C++".equals(getMainLanguage()) ? "C++" : "C";
      }
      else if ("c".equals(ext))
      {
         return "C";
      }
      else if ("pl".equals(ext) || "perl".equals(ext))
      {
         return "Perl";
      }
      else if ("php".equals(ext))
      {
         return "PHP";
      }
      else if ("sh".equals(ext))
      {
         return "bash";
      }
      else if ("bat".equals(ext) || "com".equals(ext))
      {
         return "command.com";
      }
      else if ("html".equals(ext) || "xhtml".equals(ext)
            || "htm".equals(ext) || "shtml".equals(ext))
      {
         return "HTML";
      }
      else if ("py".equals(ext))
      {
         return "Python";
      }
      else if ("m".equals(ext))
      {
         return "Matlab";
      }
      else if ("Makefile".equals(ext) || "makefile".equals(ext)
               || "mk".equals(ext))
      {
         return "make";
      }
      else if ("s".equals(ext) || "S".equals(ext) || "asm".equals(ext))
      {
         return "Assembler";
      }
      else if ("pdf".equals(ext))
      {
         return "PDF";
      }
      else if ("doc".equals(ext) || "docx".equals(ext))
      {
         return "DOC";
      }
      else if ("txt".equals(ext) || "csv".equals(ext))
      {
         return PLAIN_TEXT;
      }

      for (int i = 0; i < LISTING_LANGUAGES.length; i++)
      {
         String name = LISTING_LANGUAGES[i].toLowerCase();

         if (ext.equals(name))
         {
            return LISTING_LANGUAGES[i];
         }
      }

      return language;
   }

   /**
    * Gets the mime type for the given language.
    * This assumes that the given language is source code, plain
    * text, PDF or DOC. The filename is used to distinguish between
    * ".doc" and ".docx". No content probe performed (which is
    * performed by AssignmentProcess).
    * @param language the language name
    * @param filename the filename
    * @return the mime type
    */
   public static String getMimeType(String language, String filename)
   {
      if ("PDF".equals(language))
      {
         return MIME_PDF;
      }
      else if ("DOC".equals(language))
      {
         if (filename.endsWith(".docx"))
         {
            return MIME_DOCX;
         }
         else
         {
            return MIME_DOC;
         }
      }
      else if (UNKNOWN_LANGUAGE.equals(language)
            || PLAIN_TEXT.equals(language))
      {
         return "text/plain";
      }
      else
      {
         return "text/x-source";
      }
   }

   /**
    * Determines if the given language should be considered 
    * as having text source.
    * @param language the language to test
    * @return true if the given language is identified in
    * LISTING_LANGUAGES, unless "PDF" or "DOC", otherwise returns
    * false
    */ 
   public static boolean isText(String language)
   {
      if ("PDF".equals(language) || "DOC".equals(language))
      {
         return false;
      }
      else
      {
         for (String l : LISTING_LANGUAGES)
         {
            if (l.equals(language))
            {
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Gets the assignment label. If no label has been set, this will
    * return the title with non-alphanumerics and any punctuation
    * that's not ".", "-" or "+" stripped.
    * @return the assignment label
    */ 
   public String getLabel()
   {
      return label == null ? title.replaceAll("[^a-zA-Z0-9\\.\\-\\+]","")
        : label;
   }

   /**
    * Sets the assignment's identifying label. An empty value will be treated as
    * null.
    * @param value the label
    */ 
   public void setLabel(String value)
   {
      label = "".equals(value) ? null : value;
   }

   /**
    * Adds a line of input that must be sent to the student's application via STDIN.
    * @param input a line of input
    */ 
   public void addInput(String input)
   {
      inputList.add(input);
   }

   /**
    * Gets the list of all lines of input that must be sent to the
    * student's application via STDIN.
    * @return list of all lines of input
    */ 
   public Vector<String> getInputs()
   {
      return inputList;
   }

   /**
    * Adds a command line argument that must be added to the
    * invocation of the student's application. Each argument must be
    * added separately.
    * @param input the command line argument
    */ 
   public void addArg(String input)
   {
      argList.add(input);
   }

   /**
    * Gets the list of all command line arguments that must be added
    * to the invocation of the student's application.
    * @return list of all command line arguments
    */ 
   public Vector<String> getArgs()
   {
      return argList;
   }

   /**
    * Adds a command line argument that must be passed to the
    * compiler. Each command line argument must be added separately.
    * @param arg the command line argument
    */ 
   public void addCompilerArg(String arg)
   {
      compilerArgs.add(arg);
   }

   /**
    * Gets the list of all command line arguments that must be
    * passed to the compiler.
    * @return list of all command line arguments
    */ 
   public Vector<String> getCompilerArgs()
   {
      return compilerArgs;
   }

   /**
    * Adds a command line argument that must be passed to the
    * invoker. For example, with a Java assignment, the invoker will
    * be "java" and this is an argument to pass to "java" (not to
    * the student's application).
    * Each command line argument must be added separately.
    * @param arg the command line argument
    */ 
   public void addInvokerArg(String arg)
   {
      invokerArgs.add(arg);
   }

   /**
    * Gets a list of all the command line arguments that must be
    * passed to the invoker.
    * @return the list of command line arguments
    */ 
   public Vector<String> getInvokerArgs()
   {
      return invokerArgs;
   }

   /**
    * Indicates if PASS should try running the student's application.
    * This will only be done if there are no errors at the compiler
    * stage, if applicable.
    * @return true if PASS should try running the student's
    * application
    */ 
   public boolean isRunTestOn()
   {
      return runTest;
   }

   /**
    * Indicates if noPDF test build should try running the student's application.
    * This will only be done if there are no errors at the compiler
    * stage, if applicable.
    * @return true if noPDF test build should try running the student's
    * application
    */ 
   public boolean isNoPdfRunTestOn()
   {
      return noPdfRunTest;
   }

   /**
    * Sets whether or not PASS should try running the student's
    * application.
    * @param doTest true if PASS should try running the application
    */ 
   public void setRunTest(boolean doTest)
   {
      runTest = doTest;
   }

   /**
    * Sets whether or not PASS should try running the student's
    * application.
    * @param doTest true if PASS should try running the application
    * @param doNoPdfTest true if the noPdf test build should try running the application
    */ 
   public void setRunTest(boolean doTest, boolean doNoPdfTest)
   {
      runTest = doTest;
      noPdfRunTest = doNoPdfTest;
   }

   /**
    * Sets whether or not the noPDF test build should try running the student's
    * application.
    * @param doNoPdfTest true if the noPdf test build should try running the application
    */ 
   public void setNoPdfRunTest(boolean doNoPdfTest)
   {
      noPdfRunTest = doNoPdfTest;
   }

   /**
    * Indicates if PASS should try compiling the student's code.
    * Only applicable for compiled languages.
    * @return true if PASS should try compiling the student's
    * assignment code
    */ 
   public boolean isCompileTestOn()
   {
      return compileTest;
   }

   /**
    * Sets whether or not PASS should try compiling the student's
    * application, if applicable.
    */ 
   public void setCompileTest(boolean doTest)
   {
      compileTest = doTest;
   }

   /**
    * Sets the build script to use to compile (if applicable) and
    * run the student's application. The supplied URL must be the
    * actual script. Redirects aren't permitted.
    * @param url the URL to fetch the build script from
    */ 
   public void setBuildScript(URL url)
   {
      buildScript = url;

      if (url != null)
      {
         runTest = false;
         compileTest = false;
      }
   }

   /**
    * Gets the location of the build script.
    * @param the URL of the build script or null if not set
    */ 
   public URL getBuildScript()
   {
      return buildScript;
   }

   /**
    * Sets the noPDF build script to use to compile (if applicable) and
    * run the student's application. The supplied URL must be the
    * actual script. Redirects aren't permitted.
    * @param url the URL to fetch the build script from
    */ 
   public void setNoPdfBuildScript(URL url)
   {
      noPdfBuildScript = url;
   }

   /**
    * Gets the location of the noPDF build script.
    * @param the URL of the build script or null if not set
    */ 
   public URL getNoPdfBuildScript()
   {
      return noPdfBuildScript;
   }

   /**
    * Gets the location of the template for the given filename.
    * @param name the filename identifying the template
    * @return the URL of the template or null if not set
    */ 
   public URL getTemplate(String name)
   {
      return templates == null ? null : templates.get(name);
   }

   /**
    * Adds a template for the given file. Templates are only used
    * with PassEditor. The URL must be the location of the template
    * file. Redirects aren't permitted.
    * @param fileName the file name
    * @param template the URL of the template
    */ 
   public void addTemplate(String fileName, URL template)
   {
      if (templates == null)
      {
         templates = new HashMap<String,URL>();
      }

      templates.put(fileName, template);
   }

   /**
    * Sets if the relative paths setting should be on by default.
    * This is initialised to true but can be changed by the
    * "relpath" attribute. The user may override this if the setting
    * is off by default.
    * @param defValue true if the setting should be on by default
    */ 
   public void setRelativePathsDefault(boolean defValue)
   {
      relPathsDefault = defValue;
   }

   /**
    * Gets the default relative paths setting.
    * @return true if the setting should be on by default
    */ 
   public boolean isRelativePathsDefaultOn()
   {
      return relPathsDefault;
   }

   private Course course;
   private String title;
   private String variant, language;
   private String label;
   private Vector<String> fileList, inputList, argList, compilerArgs, invokerArgs;
   private Vector<AllowedBinaryFilter> allowedBinaryFilters;
   private Vector<ResourceFile> resourceList;
   private Vector<ResultFile> resultList;
   private String mainFile;
   private LocalDateTime due;
   private boolean runTest=true, noPdfRunTest=true;
   private boolean compileTest=true;
   private URL buildScript = null;
   private URL noPdfBuildScript = null;
   private HashMap<String,URL> templates = null;
   private boolean relPathsDefault = false;

   private Vector<String> reports;

   public static final String UNKNOWN_LANGUAGE="---";

   public static final String PLAIN_TEXT="Plain Text";

   public static final String[] LISTING_LANGUAGES = new String[]
   {
      UNKNOWN_LANGUAGE,
      "PDF",
      "DOC",
      PLAIN_TEXT,
      "ABAP",
      "ACMscript",
      "ACM",
      "ACSL",
      "ADA",
      "Algol",
      "Ant",
      "Assembler",
      "Awk",
      "bash",
      "Basic",
      "C",
      "C++",
      "Caml",
      "CIL",
      "Clean",
      "Cobol",
      "Comal 80",
      "command.com",
      "Comsol",
      "csh",
      "Delphi",
      "Eiffel",
      "Elan",
      "erlang",
      "Euphoria",
      "Fortran",
      "GAP",
      "GCL",
      "Gnuplot",
      "hansl",
      "Haskell",
      "HTML",
      "IDL",
      "inform",
      "Java",
      "JVMIS",
      "ksh",
      "Lingo",
      "Lisp",
      "LLVM",
      "Logo",
      "Lua",
      "make",
      "Mathematica",
      "Matlab",
      "Mercury",
      "MetaPost",
      "Miranda",
      "Mizar",
      "ML",
      "Modula-2",
      "MuPAD",
      "NASTRAN",
      "Oberon-2",
      "OCL",
      "Octave",
      "Oz",
      "Pascal",
      "Perl",
      "PHP",
      "PL/I",
      "Plasm",
      "PostScript",
      "POV",
      "Prolog",
      "Promela",
      "PSTricks",
      "Python",
      "R",
      "Reduce",
      "Rexx",
      "RSL",
      "Ruby",
      "S",
      "SAS",
      "Scala",
      "Scilab",
      "sh",
      "SHELXL",
      "Simula",
      "SPARQL",
      "SQL",
      "tcl",
      "TeX",
      "VBScript",
      "Verilog",
      "VHDL",
      "VRML",
      "XML",
      "XSLT"
   };

   public static final String BINARY = "BINARY";

   public static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
   public static final String MIME_DOC = "application/msword";
   public static final String MIME_PDF = "application/pdf";
}
