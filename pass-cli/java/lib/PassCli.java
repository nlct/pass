package com.dickimawbooks.passcli.lib;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.InvalidPathException;
import java.nio.charset.Charset;
import java.util.*;
import java.net.URL;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.MessageFormat;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import com.dickimawbooks.passlib.*;

public class PassCli implements Pass
{
   public PassCli() throws IOException
   {
      Locale locale = Locale.getDefault();

      passTools = new PassTools(this, locale);
      passTools.loadDictionary("passcli", locale);

      files = new Vector<PassFile>();
   }

   public boolean isSubmittedDateEnabled()
   {
      return false;
   }

   public void setSubmittedDate(String dateString) 
     throws InvalidSyntaxException,UnsupportedSettingException
   {
      if (!isSubmittedDateEnabled())
      {
         throw new UnsupportedSettingException(
           getMessageWithDefault("error.submission_date_not_supported",
            "Submission date setting not supported for {0}.",
            getApplicationName()));
      }

      try
      {
         submittedDate = ISO_DATETIME_FORMAT.parse(dateString);
      }
      catch (ParseException e)
      {
         throw new InvalidSyntaxException(
          getMessageWithDefault("error.invalid_datestamp", 
            "Invalid date-time stamp {0}. Required format: {1}.",
            dateString, ISO_DATETIME_FORMAT.toPattern()), e);
      }
   }

   public Date getSubmittedDate()
   {
      return isSubmittedDateEnabled() ? submittedDate : null;
   }

   public void addFile(PassCliFile passFile)
   {
      files.add(passFile);
   }

   public File resolve(String name) throws IOException
   {
      File file;

      String tried = "";

      if (sourceDirectory == null)
      {
         file = new File(name);
         tried = file.getAbsolutePath();
      }
      else
      {
         try
         {
            file = sourceDirectory.resolve(name).toFile();
            tried = file.getAbsolutePath();
         }
         catch (InvalidPathException e)
         {
            error(e);
            file = new File(name);
            tried = file.getAbsolutePath();
         }
      }

      if (!file.exists() && basePath != null)
      {
         file = basePath.resolve(name).toFile();
         tried = String.format("%s%n%s", tried, file.getAbsolutePath());
      }

      if (!file.exists())
      {
         throw new FileNotFoundException(passTools.getMessageWithDefault(
          "error.cant_find_file", "Can''t find file ''{0}''. Tried:\n{1}",
          file, tried));
      }

      return file;
   }

   public void addFile(String filename, String language) throws IOException
   {
      String listingLanguage = AssignmentData.UNKNOWN_LANGUAGE;

      for (String l : AssignmentData.LISTING_LANGUAGES)
      {
         if (l.equals(language))
         {
            listingLanguage = l;
            break;
         }
      }

      files.add(new PassCliFile(resolve(filename), listingLanguage));
   }

   public Vector<PassFile> getFiles()
   {
      return files;
   }

   private void readXML() throws SAXException,IOException,UnknownIdentifierException
   {
      Vector<AssignmentData> assignments = passTools.loadAssignments(course);

      for (AssignmentData assign : assignments)
      {
         if (assign.getLabel().equals(assignmentLabel))
         {
            assignmentData = assign;
            break;
         }
      }

      if (assignmentData == null)
      {
         throw new UnknownIdentifierException(
           getMessageWithDefault("error.cant_find_assignment",
            "Can''t find assignment ''{0}''.",
            assignmentLabel));
      }

      // Check for any unknown languages

      for (int i = 0; i < files.size(); i++)
      {
         PassCliFile passfile = (PassCliFile)files.get(i);

         String language = passfile.getLanguage();

         if (language.equals(AssignmentData.UNKNOWN_LANGUAGE))
         {
            String listingLanguage = assignmentData.getListingLanguage(
             passfile.getExtension());

            if (listingLanguage.equals(AssignmentData.UNKNOWN_LANGUAGE))
            {
               throw new UnknownIdentifierException(
                 getMessageWithDefault("error.unknown_file_language", 
                   "Unknown language for file ''{0}''.",
                   passfile.getFilename()));
            }
            else
            {
               passfile.setLanguage(listingLanguage);
            }
         }
      }
   }

   public Path getBasePath()
   {
      return basePath;
   }

   public void openTranscript(Charset charset) throws IOException
   {
      if (transcriptName == null) return;

      if (transcriptWriter != null)
      {
         transcriptWriter.close();
      }

      if (charset == null)
      {
         transcriptWriter = new PrintWriter(transcriptName);
      }
      else
      {
         transcriptWriter = new PrintWriter(transcriptName, charset.name());
      }
   }

   public void closeDown()
   {
      passTools.closeDown();

      if (transcriptWriter != null)
      {
         transcriptWriter.close();
      }
   }

   public PassTools getPassTools()
   {
      return passTools;
   }

   public AssignmentData getAssignment()
   {
      return assignmentData;
   }

   public boolean usePdfPages()
   {
      return pdfPages != null;
   }

   public String getPdfPagesOptions()
   {
      return pdfPages;
   }

   public String getApplicationName()
   {
      return APP_NAME;
   }

   public String getApplicationVersion()
   {
      return APP_VERSION;
   }

   public String getApplicationDate()
   {
      return APP_DATE;
   }

   public boolean isConfirmed()
   {
      return agree;
   }

   public boolean isGroupProject()
   {
      return students.size() > 1;
   }

   public Vector<Student> getProjectTeam()
   {
      return students;
   }

   public Student getStudent()
   {
      return students.firstElement();
   }

   public String getDefaultBaseName()
   {
      return String.format("%s-%s",
         getAssignment().getLabel(), getStudent().getRegNumber());
   }

   public void setFileEncoding(String name) throws UnknownIdentifierException
   {
      String lcname = name.toLowerCase();

      if (lcname.equals("ascii") || lcname.equals("us-ascii"))
      {
         fileEncodingName = ENCODING_ASCII;
      }
      else if (lcname.equals("utf8") || lcname.equals("utf-8"))
      {
         fileEncodingName = ENCODING_UTF8;
      }
      else if (lcname.equals("latin1") || lcname.equals("latin-1") 
         || lcname.equals("latin 1"))
      {
         fileEncodingName = ENCODING_LATIN1;
      }
      else
      {
         throw new UnknownIdentifierException(
           getMessageWithDefault("error.unknown_encoding",
             "Unknown encoding name ''{0}''.",
              name));
      }
   }

   public String getEncoding()
   {
      return fileEncodingName;
   }

   public Charset getCharset() throws InvalidSyntaxException
   {
      try
      {
         return Charset.forName(fileEncodingName);
      }
      catch (Exception e)
      {
         throw new InvalidSyntaxException(e.getMessage(), e);
      }
   }

   public boolean isASCII()
   {
      return fileEncodingName.equals(ENCODING_ASCII);
   }

   public boolean isLatin1()
   {
      return fileEncodingName.equals(ENCODING_LATIN1);
   }

   public boolean isUTF8()
   {
      return fileEncodingName.equals(ENCODING_UTF8);
   }

   public void setTimeOut(long val)
   {
      if (val <= 0)
      {
         throw new IllegalArgumentException(
           getMessageWithDefault("error.invalid_timeout",
           "Invalid timeout value: {0}.",
           val));
      }

      timeout = val;
   }

   public void setTimeOut(String val) throws InvalidSyntaxException
   {
      try
      {
         timeout = Long.parseLong(val);
      }
      catch (NumberFormatException e)
      {
         throw new InvalidSyntaxException(
           getMessageWithDefault("error.invalid_timeout",
            "Invalid timeout value: {0}.",
            val), e);
      }
   }

   public long getTimeOut()
   {
      return timeout;
   }

   public void message(int messageType, char c)
   {
      if (messageType <= verboseLevel)
      {
         if (messageType == MESSAGE_TYPE_ERROR
          || messageType == MESSAGE_TYPE_WARNING)
         {
            System.err.print(c);
         }
         else
         {
            System.out.print(c);
         }
      }
   }

   public void message(int messageType, String message)
   {
      if (messageType <= verboseLevel)
      {
         if (messageType == MESSAGE_TYPE_ERROR
          || messageType == MESSAGE_TYPE_WARNING)
         {
            System.err.print(message);
         }
         else
         {
            System.out.print(message);
         }
      }
   }

   public void messageLn(int messageType, String message)
   {
      if (messageType <= verboseLevel)
      {
         if (messageType == MESSAGE_TYPE_ERROR
          || messageType == MESSAGE_TYPE_WARNING)
         {
            System.err.println(message);
         }
         else
         {
            System.out.println(message);
         }
      }
   }

   public boolean isDebugMode()
   {
      return verboseLevel >= MESSAGES_DEBUG; 
   }

   public void debug(String msg)
   {
      messageLn(MESSAGE_TYPE_DEBUG, msg);

      if (verboseLevel >= MESSAGES_DEBUG)
      {
         transcriptMessage(String.format("DEBUG: %s", msg));
      }
   }

   public void debugNoLn(String msg)
   {
      message(MESSAGE_TYPE_DEBUG, msg);

      if (verboseLevel >= MESSAGES_DEBUG)
      {
         transcriptMessageNoLn(msg);
      }
   }

   public void verboseCodePoint(int cp)
   {
      if (cp <= Character.MAX_VALUE)
      {
         verbose((char)cp);
      }
      else
      {
         verbose(new String(Character.toChars(cp)));
      }
   }

   public void verbose(char c)
   {
      message(MESSAGE_TYPE_DETAIL, c);

      transcriptMessage(c);
   }

   public void verbose(String msg)
   {
      messageLn(MESSAGE_TYPE_DETAIL, msg);

      transcriptMessage(msg);
   }

   public void warning(String msg)
   {
      messageLn(MESSAGE_TYPE_WARNING, msg);

      transcriptMessage(getMessageWithDefault("message.warning",
         "WARNING: {0}", msg));
   }

   public void info(String msg)
   {
      messageLn(MESSAGE_TYPE_INFO, msg);

      transcriptMessage(msg);
   }

   public void error(String msg)
   {
      messageLn(MESSAGE_TYPE_ERROR, msg);

      transcriptMessage(getMessageWithDefault("message.error",
        "ERROR: {0}", msg));
   }

   public void error(Throwable throwable)
   {
      error(throwable, false);
   }

   public void error(Throwable throwable, boolean forceShowStackTrace)
   {
      error(String.format("%s: %s", throwable.getClass().getSimpleName(), 
       throwable.getMessage()));

      if (verboseLevel >= MESSAGES_DEBUG || forceShowStackTrace)
      {
         throwable.printStackTrace();

         if (transcriptWriter != null)
         {
            throwable.printStackTrace(transcriptWriter);
         }
      }
   }

   public void fatalError(Throwable throwable, int exitCode)
   {
      if (transcriptWriter == null)
      {
         System.err.format("%s: %s%n",
           throwable.getClass().getSimpleName(), throwable.getMessage());

         throwable.printStackTrace();
      }
      else
      {
         error(throwable, true);
      }

      closeDown();

      System.exit(exitCode);
   }

   public void transcriptMessage(String msg)
   {
      if (transcriptWriter != null)
      {
         transcriptWriter.println(msg);
      }
   }

   public void transcriptMessageNoLn(String msg)
   {
      if (transcriptWriter != null)
      {
         transcriptWriter.print(msg);
      }
   }

   public void transcriptMessage(char c)
   {
      if (transcriptWriter != null)
      {
         transcriptWriter.print(c);
      }
   }

   public String getMessageWithDefault(String key, String defFmt, Object... params)
   {
      if (passTools == null)
      {
         return MessageFormat.format(defFmt, params);
      }

      return passTools.getMessageWithDefault(key, defFmt, params);
   }

   public int getMessageLevel()
   {
      return verboseLevel;
   }

   public void setMessageLevel(String id)
     throws UnknownIdentifierException
   {
      try
      {
         int level = Integer.parseInt(id);

         switch (level)
         {
            case MESSAGES_SILENT:
            case MESSAGES_ERRORS_ONLY:
            case MESSAGES_ERRORS_AND_WARNINGS_ONLY:
            case MESSAGES_ERRORS_AND_WARNINGS_AND_INFO:
            case MESSAGES_VERBOSE:
            case MESSAGES_DEBUG:
               verboseLevel = level;
            return;
            default:
               throw new InvalidSyntaxException(
                 getMessageWithDefault("error.unknown_message_level", 
                   "Unknown message level {0}.", id));
         }
      }
      catch (NumberFormatException e)
      {// do nothing, assume text ID
      }

      for (int i = 0; i < MESSAGE_LEVEL_LABELS.length; i++)
      {
         if (id.equals(MESSAGE_LEVEL_LABELS[i]))
         {
            verboseLevel = i;
            return;
         }
      }

      throw new UnknownIdentifierException(
        getMessageWithDefault("error.unknown_message_level", 
          "Unknown message level {0}.", id));
   }

   public void version()
   {
      System.out.println(
        getMessageWithDefault("message.about", 
         "{0}\n\nversion {1} ({2})\n\n{3}\n{4}\n\nPASS Lib v{5} ({6})",
         getApplicationName(), 
         getApplicationVersion(), getApplicationDate(),
         COPYRIGHT_OWNER, ABOUT_URL, PASSLIB_VERSION, PASSLIB_VERSION_DATE));
   }

   public void copyrightAndLicence()
   {
      String year = getApplicationDate().substring(0, 4);

      if (!year.equals(COPYRIGHT_START_YEAR))
      {
         year = String.format("%s-%s", COPYRIGHT_START_YEAR, year);
      }

      System.out.println();
      System.out.println(passTools.getMessage(
        "message.licence", "Nicola L. C. Talbot", year));
      System.out.println();
      System.out.println("https://www.dickimaw-books.com/");
   }

   public void printWrap(String msg)
   {
      System.out.println(passTools.stringWrap(msg, 70, String.format("%n")));
   }

   public void printWrapMessage(String key, Object... params)
   {
      printWrap(passTools.getMessage(key, params));
   }

   public void printMessage(String key, Object... params)
   {
      System.out.print(passTools.getMessage(key, params));
   }

   public void printlnMessage(String key, Object... params)
   {
      System.out.println(passTools.getMessage(key, params));
   }

   public void help()
   {
      System.out.println();
      printlnMessage("syntax.summary", getApplicationName());
      System.out.println();

      printlnMessage("syntax.available_settings");

      System.out.println();
      printlnMessage("syntax.course_assignment");
      System.out.println();

      printWrapMessage("syntax.course", "--course", "-c");
      printWrapMessage("syntax.assignment", "--assignment", "-a");

      System.out.println();
      printlnMessage("syntax.student_identification");
      System.out.println();

      printWrapMessage("syntax.user_id", "--user-id", "-u");
      printWrapMessage("syntax.reg_num", "--student-number", "-n");
      printWrapMessage("syntax.group_project.note",
         "--user-id", "--student-number", "--student");

      System.out.println();
      printWrapMessage("syntax.student", "--student");

      System.out.println();
      printWrapMessage(agree ? "syntax.agree.default" : "syntax.agree",
        "--agree", "-Y");

      printWrapMessage(agree ? "syntax.no_agree" : "syntax.no_agree.default",
        "--no-agree", "-N");

      System.out.println();
      printlnMessage("syntax.latex_settings");
      System.out.println();

      printWrapMessage("syntax.pdfpages", "--pdfpages", "-p", pdfPages);
      printWrapMessage("syntax.nopdfpages", "--nopdfpages");

      System.out.println();
      printlnMessage("syntax.file_settings");
      System.out.println();

      printWrapMessage("syntax.encoding", "--encoding", "--from-file");

      System.out.println();
      printWrapMessage("syntax.pdf_result", "--pdf-result", "-r");

      System.out.println();
      printWrapMessage("syntax.base_path", "--base-path", "-b");
      System.out.println();
      printWrapMessage("syntax.file", "--file", "-f", "--directory");
      System.out.println();

      printWrapMessage("syntax.project_encoding", "--project-encoding", "-e");

      System.out.print("\t");
      printlnMessage(isASCII() ? "syntax.project_encoding.ascii.default" : 
        "syntax.project_encoding.ascii");

      System.out.print("\t");
      printlnMessage(isLatin1() ? "syntax.project_encoding.latin1.default" : 
        "syntax.project_encoding.latin1");

      System.out.format("\t");
      printlnMessage(isUTF8() ? "syntax.project_encoding.utf8.default" : 
        "syntax.project_encoding.utf8");

      printWrapMessage("syntax.project_encoding.note");

      System.out.println();
      printlnMessage("syntax.general");
      System.out.println();

      if (isSubmittedDateEnabled())
      {
         printWrapMessage("syntax.submission_timestamp", "--submission-timestamp");
      }

      printWrapMessage("syntax.from_file", "--from-file", "-F");
      printWrapMessage("syntax.directory", "--directory", "-d", "--file");

      System.out.println();
      printWrapMessage("syntax.message", "--message", "-m");

      for (int i = 0; i < MESSAGE_LEVEL_LABELS.length; i++)
      {
         System.out.format("\t%d\t'%s'%n", i, MESSAGE_LEVEL_LABELS[i]);
      }

      System.out.println();
      printWrapMessage("syntax.silent",
         "--silent", "-q", "--message ", MESSAGES_SILENT);
      System.out.println();

      printWrapMessage("syntax.debug", "--debug", "--message", MESSAGES_DEBUG);
      System.out.println();

      printWrapMessage("syntax.transcript", "--transcript", "-l", "--file-encoding");

      System.out.println();
      printWrapMessage("syntax.timeout", "--timeout", timeout);

      printWrapMessage("syntax.help", "--help", "-h");
      printWrapMessage("syntax.version", "--version", "-V");
   }

   private void parseSettingsFile(String filename)
    throws UnknownIdentifierException,IOException,UnsupportedSettingException
   {
      BufferedReader in = null;

      try
      {
         in = Files.newBufferedReader(new File(filename).toPath(), getCharset());

         String line;
         int lineNum = 0;

         while ((line = in.readLine()) != null)
         {
            lineNum++;

            if (line.startsWith("#")) continue;

            String[] split = line.split(": *", 2);

            if (split.length < 2)
            {
               throw new InvalidSyntaxException(
                 getMessageWithDefault(
                   "error.parse.no_key_val",
                   "{0}:{1,number,integer}: expected ''<Key>: <Value>'' found ''{2}''.",
                   filename, lineNum, line));
            }

            String argName = split[0];
            String argValue = split[1];

            if (argName.equals("File"))
            {
               split = argValue.split(" *\t *", 2);

               if (split.length == 2)
               {
                  addFile(split[0], split[1]);
               }
               else
               {
                  addFile(new PassCliFile(resolve(argValue)));
               }
            }
            else if (argName.equals("Submission-timestamp"))
            {
               setSubmittedDate(argValue);
            }
            else if (argName.equals("Course"))
            {
               courseCode = argValue;
            }
            else if (argName.equals("Assignment"))
            {
               assignmentLabel = argValue;
            }
            else if (argName.equals("User-id"))
            {
               if (students != null)
               {
                  throw new InvalidSyntaxException(
                    getMessageWithDefault(
                    "error.parse.option_clash",
                    "{0}:{1,number,integer}: option clash - can''t use {2} with {3} & {4}.",
                     filename, lineNum, "Student", "Student-number", argName));
               }

               blackboardId = argValue.split(",");
            }
            else if (argName.equals("Student-number"))
            {
               if (students != null)
               {
                  throw new InvalidSyntaxException(
                    getMessageWithDefault(
                    "error.parse.option_clash",
                    "{0}:{1,number,integer}: option clash - can''t use {2} with {3} & {4}.",
                     filename, lineNum, "Student", argName, "User-id"));
               }

               studentNumber = argValue.split(",");
            }
            else if (argName.equals("Student"))
            {
               if (studentNumber != null || blackboardId != null)
               {
                  throw new InvalidSyntaxException(
                    getMessageWithDefault(
                    "error.parse.option_clash",
                    "{0}:{1,number,integer}: option clash - can''t use {2} with {3} & {4}.",
                     filename, lineNum, argName, "Student-number", "User-id"));
               }

               split = argValue.split(" *\t *", 2);

               if (split.length < 2)
               {
                  throw new InvalidSyntaxException(
                     getMessageWithDefault(
                      "error.parse.missing_second_val",
                      "{0}:{1,number,integer}: missing second value for {2}.",
                      filename, lineNum, argName));
               }

               if (students == null)
               {
                  students = new Vector<Student>();
               }

               students.add(new Student(split[0], split[1]));
            }
            else if (argName.equals("Agree"))
            {
               if (argValue.equals("true"))
               {
                  agree = true;
               }
               else if (argValue.equals("false"))
               {
                  agree = false;
               }
               else
               {
                  throw new UnknownIdentifierException(
                    getMessageWithDefault(
                     "error.parse.invalid_bool",
                     "{0}:{1,number,integer}: invalid ''{2}'' value ''{3}'' (''true'' or ''false'' expected).",
                      filename, lineNum, argName, argValue));
               }
            }
            else if (argName.equals("Pdfpages"))
            {
               pdfPages = argValue;
            }
            else if (argName.equals("Project-encoding"))
            {
               setFileEncoding(argValue);
            }
            else if (argName.equals("Base-path"))
            {
               basePath = new File(argValue).toPath();

               if (!Files.exists(basePath))
               {
                  throw new FileNotFoundException(passTools.getMessageWithDefault(
                    "error.no_such_dir", "No such directory ''{0}''.", argValue));
               }

               if (!Files.isDirectory(basePath))
               {
                  throw new IOException(passTools.getMessageWithDefault(
                    "error.not_a_dir", "Not a directory ''{0}''.", argValue));
               }
            }
            else if (argName.equals("Messages"))
            {
               setMessageLevel(argValue);
            }
            else if (argName.equals("Timeout"))
            {
               setTimeOut(argValue);
            }
            else if (argName.equals("Pdf-result"))
            {
               pdfResult = argValue;
            }
            else
            {
               throw new UnknownIdentifierException(
                getMessageWithDefault(
                  "error.parse.unknown_key",
                  "{0}:{1,number,integer}: unknown identifier ''{2}''.",
                  filename, lineNum, argName));
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

   public void parseArgs(String[] args) 
    throws UnknownIdentifierException,SAXException,IOException,
           UnsupportedSettingException,InvalidFileException
   {
      /*
       * First look for options that need to be processed first.
      */

      String fromFile = null;
      String encodingName = null;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--help") || args[i].equals("-h"))
         {
            version();
            help();
            System.exit(0);
         }
         else if (args[i].equals("--version") || args[i].equals("-V"))
         {
            version();
            copyrightAndLicence();
            System.exit(0);
         }
         else if (args[i].equals("--silent") || args[i].equals("-q"))
         {
            verboseLevel = MESSAGES_SILENT;
         }
         else if (args[i].equals("--debug"))
         {
            verboseLevel = MESSAGES_DEBUG;
         }
         else if (args[i].startsWith("-"))
         {
            String[] split = null;
            String argName = args[i];
            String argValue = null;

            if (args[i].startsWith("--"))
            {
               split = argName.split("=", 2);

               if (split.length == 2)
               {
                  argName = split[0];
                  argValue = split[1];
               }
            }

            int nextI = i+1;

            if (argValue == null && nextI < args.length 
                  && !args[nextI].startsWith("-"))
            {
               argValue = args[nextI];
            }

            if (argName.equals("--transcript") || argName.equals("-l"))
            {
               transcriptName = argValue;
            }
            else if (argName.equals("--encoding"))
            {
               encodingName = argValue;
            }
            else if (argName.equals("--messages") || argName.equals("-m"))
            {
               setMessageLevel(argValue);
            }
            else if (argName.equals("--from-file") || argName.equals("-F"))
            {
               fromFile = argValue;
            }
            else if (argName.equals("--directory") || argName.equals("-d"))
            {
               sourceDirectory = (new File(argValue)).getAbsoluteFile().toPath();

               if (!Files.exists(sourceDirectory))
               {
                  throw new FileNotFoundException(passTools.getMessageWithDefault(
                    "error.no_such_dir", "No such directory ''{0}''.", argValue));
               }

               if (!Files.isDirectory(sourceDirectory))
               {
                  throw new IOException(passTools.getMessageWithDefault(
                    "error.not_a_dir", "Not a directory ''{0}''.", argValue));
               }
            }
         }
      }

      Charset charset = null;

      if (encodingName != null)
      {
         try
         {
            charset = Charset.forName(encodingName);
         }
         catch (Exception e)
         {
            throw new InvalidSyntaxException(
              getMessageWithDefault(
                "error.unknown_encoding",
                "Unknown encoding ''{0}''.", encodingName), e);
         }
      }

      openTranscript(charset);

      if (fromFile != null)
      {
         parseSettingsFile(fromFile);
      }

      courseData = passTools.loadCourseData(
         getClass().getResource("/resources.xml"));

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--silent") || args[i].equals("-q")
             || args[i].equals("--debug"))
         {// already processed
         }
         else if (args[i].equals("--nopdfpages"))
         {
            pdfPages = null;
         }
         else if (args[i].equals("--agree") || args[i].equals("-Y"))
         {
            agree = true;
         }
         else if (args[i].equals("--no-agree") || args[i].equals("-N"))
         {
            agree = false;
         }
         else if (args[i].startsWith("-"))
         {
            String[] split = null;

            if (args[i].startsWith("--"))
            {
               split = args[i].split("=", 2);
            }

            String argName;
            String argValue=null;

            if (split != null && split.length == 2)
            {
               argName = split[0];
               argValue = split[1];
            }
            else
            {
               argName = args[i];
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                   getMessageWithDefault(
                    "error.missing_value_or_unknown",
                    "Missing value for ''{0}'' or unknown option.", argName));
               }

               argValue = args[i];
            }

            if (argName.equals("--file") || argName.equals("-f"))
            {
               if (i < args.length && !args[i+1].startsWith("-"))
               {
                  addFile(argValue, args[++i]);
               }
               else
               {
                  addFile(new PassCliFile(resolve(argValue)));
               }
            }
            else if (argName.equals("--transcript") || argName.equals("-l"))
            {
               // already processed
            }
            else if (argName.equals("--from-file") || argName.equals("-F"))
            {
               // already processed
            }
            else if (argName.equals("--directory") || argName.equals("-d"))
            {
               // already processed
            }
            else if (argName.equals("--submission-timestamp"))
            {
               setSubmittedDate(argValue);
            }
            else if (argName.equals("--course") || argName.equals("-c"))
            {
               courseCode = argValue;
            }
            else if (argName.equals("--assignment") || argName.equals("-a"))
            {
               assignmentLabel = argValue;
            }
            else if (argName.equals("--user-id") || argName.equals("-u"))
            {
               if (students != null)
               {
                  throw new InvalidSyntaxException(getMessageWithDefault(
                    "error.option_clash",
                    "Option clash - can''t use {0} with {1} & {2}.",
                    "--student", "--student-number", argName));
               }

               blackboardId = argValue.split(",");
            }
            else if (argName.equals("--student-number") || argName.equals("-n"))
            {
               if (students != null)
               {
                  throw new InvalidSyntaxException(getMessageWithDefault(
                    "error.option_clash",
                    "Option clash - can''t use {0} with {1} & {2}.",
                    "--student", argName, "user-id"));
               }

               studentNumber = argValue.split(",");
            }
            else if (argName.equals("--student") || argName.equals("-s"))
            {
               if (studentNumber != null || blackboardId != null)
               {
                  throw new InvalidSyntaxException(getMessageWithDefault(
                    "error.option_clash",
                    "Option clash - can''t use {0} with {1} & {2}.",
                    argName, "--student-number", "user-id"));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getMessageWithDefault(
                      "error.missing_second_val",
                      "Missing second value for {0} <id> <number>.",
                      argName));
               }

               if (students == null)
               {
                  students = new Vector<Student>();
               }

               students.add(new Student(argValue, args[i]));
            }
            else if (argName.equals("--pdfpages") || argName.equals("-p"))
            {
               pdfPages = argValue;
            }
            else if (argName.equals("--project-encoding") || argName.equals("-e"))
            {
               setFileEncoding(argValue);
            }
            else if (argName.equals("--base-path") || argName.equals("-b"))
            {
               basePath = new File(argValue).toPath();

               if (!Files.exists(basePath))
               {
                  throw new FileNotFoundException(passTools.getMessageWithDefault(
                    "error.no_such_dir", "No such directory ''{0}''.", argValue));
               }

               if (!Files.isDirectory(basePath))
               {
                  throw new IOException(passTools.getMessageWithDefault(
                    "error.not_a_dir", "Not a directory ''{0}''.", argValue));
               }
            }
            else if (argName.equals("--encoding"))
            {
               // already processed
            }
            else if (argName.equals("--messages") || argName.equals("-m"))
            {
               // already processed
            }
            else if (argName.equals("--timeout"))
            {
               setTimeOut(argValue);
            }
            else if (argName.equals("--pdf-result") || argName.equals("-r"))
            {
               pdfResult = argValue;
            }
            else
            {
               throw new InvalidSyntaxException(
                 getMessageWithDefault(
                  "error.unknown_option",
                  "Unknown option ''{0}''.", argName));
            }
         }
         else
         {
            throw new InvalidSyntaxException(
                 getMessageWithDefault(
                  "error.unknown_option",
                  "Unknown option ''{0}''.", args[i]));
         }
      }

      if (students == null)
      {
         if (studentNumber == null)
         {
            throw new InvalidSyntaxException(
              getMessageWithDefault("error.missing_regnums", 
                "Missing student number(s)"));
         }

         if (blackboardId == null)
         {
            throw new InvalidSyntaxException(
              getMessageWithDefault("error.missing_usernames",
                "Missing Blackboard ID(s)"));
         }

         if (studentNumber.length != blackboardId.length)
         {
            throw new InvalidSyntaxException(String.format(
              "Unequal lists of student numbers (%d) and Blackboard IDs (%d)",
                studentNumber.length, blackboardId.length));
         }

         students = new Vector<Student>(studentNumber.length);

         for (int i = 0; i < studentNumber.length; i++)
         {
            students.add(new Student(blackboardId[i], studentNumber[i]));
         }
      }

      if (courseCode == null)
      {
         throw new InvalidSyntaxException("Missing course code");
      }

      for (Course c : courseData)
      {
         if (c.getCode().equals(courseCode))
         {
            course = c;
            break;
         }
      }

      if (course == null)
      {
         throw new UnknownIdentifierException(
           String.format("Unknown course code '%s'", courseCode));
      }

      if (assignmentLabel == null)
      {
         throw new InvalidSyntaxException("Missing assignment label");
      }

      readXML();

      if (assignmentData == null)
      {
         throw new UnknownIdentifierException(
           String.format("Unknown assignment label '%s'", assignmentLabel));
      }

      if (files.size() == 0)
      {
         throw new InvalidSyntaxException("At least one file must be specified.");
      }

      if (pdfResult == null)
      {
         throw new InvalidSyntaxException("--pdf-result required");
      }

      for (int i = 0; i < files.size(); i++)
      {
         PassCliFile pf = (PassCliFile)files.get(i);

         passTools.checkFileName(assignmentData, pf.getFile());

         if (assignmentData.hasFile(pf.getFile().getName()))
         {
            files.set(i, new RequiredPassCliFile(pf));
         }
      }
   }

   public void process() 
   throws IOException,InterruptedException,URISyntaxException,
     AgreementRequiredException
   {
      info("Main language: "+assignmentData.getMainLanguage());

      String mainFile = assignmentData.getMainFile();

      if (mainFile != null)
      {
         info("Main file: "+mainFile);
      }

      AssignmentProcess process = new AssignmentProcess(this);

      File pdfFile = process.createPdf();

      if (pdfFile != null)
      {
         File dest = new File(pdfResult);

         info(String.format("Creating %s", dest));

         Files.copy(pdfFile.toPath(), dest.toPath(), 
            StandardCopyOption.REPLACE_EXISTING);
      }
   }

   public void run(String[] args)
   {
      if (args.length == 0)
      {
         System.err.println(String.format(
           "%s: no arguments provided. Use --help for help", getApplicationName()));
         System.exit(EXIT_SYNTAX);
      }

      try
      {
         parseArgs(args);
         process();
      }
      catch (SAXException e)
      {
         fatalError(e, EXIT_SAX);
      }
      catch (IOException e)
      {
         fatalError(e, EXIT_IO);
      }
      catch (UnknownIdentifierException e)
      {
         fatalError(e, EXIT_SYNTAX);
      }
      catch (UnsupportedSettingException e)
      {
         fatalError(e, EXIT_UNSUPPORTED_SETTING);
      }
      catch (InvalidFileException e)
      {
         fatalError(e, EXIT_INVALID_FILE);
      }
      catch (Throwable e)
      {
         fatalError(e, EXIT_OTHER);
      }

      closeDown();
   }

   private long timeout = 60L;
   private String courseCode = null;
   private String assignmentLabel = null;
   private String[] blackboardId = null;
   private String[] studentNumber = null;
   private String pdfPages = "pagecommand={\\thispagestyle{pass}}";
   private String fileEncodingName=ENCODING_UTF8;
   private Date submittedDate;

   private boolean agree=false;

   private Path basePath = null;
   private Path sourceDirectory = null;

   private Course course;
   private Vector<Student> students;
   private AssignmentData assignmentData;

   private Vector<PassFile> files;

   private Vector<Course> courseData;

   private PassTools passTools;

   private String transcriptName = null;
   private PrintWriter transcriptWriter = null;

   private String pdfResult = null;

   public static final SimpleDateFormat ISO_DATETIME_FORMAT
     = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssSSSZ");

   public static final int MESSAGE_TYPE_ERROR=1;
   public static final int MESSAGE_TYPE_WARNING=2;
   public static final int MESSAGE_TYPE_INFO=3;
   public static final int MESSAGE_TYPE_DETAIL=4;
   public static final int MESSAGE_TYPE_DEBUG=5;

   public static final int MESSAGES_SILENT=0;
   public static final int MESSAGES_ERRORS_ONLY=1;
   public static final int MESSAGES_ERRORS_AND_WARNINGS_ONLY=2;
   public static final int MESSAGES_ERRORS_AND_WARNINGS_AND_INFO=3;
   public static final int MESSAGES_VERBOSE=4;
   public static final int MESSAGES_DEBUG=5;

   public static final String[] MESSAGE_LEVEL_LABELS
    = new String[] {"silent", "errors", "errors and warnings", 
        "errors warnings and info", "verbose", "debug"};

   private int verboseLevel=MESSAGES_ERRORS_AND_WARNINGS_AND_INFO;

   public static final String APP_NAME="pass-cli";
   public static final String APP_VERSION="1.08";
   public static final String APP_DATE="2021-05-25";
   public static final String COPYRIGHT_START_YEAR="2020";

   private static final String COPYRIGHT_OWNER="Nicola L.C. Talbot";
   private static final String ABOUT_URL="https://www.dickimaw-books.com/software/pass/";

   public static final int EXIT_SYNTAX=1;
   public static final int EXIT_NO_COURSE_DATA=2;
   public static final int EXIT_IO=3;
   public static final int EXIT_SAX=4;
   public static final int EXIT_UNSUPPORTED_SETTING=5;
   public static final int EXIT_INVALID_FILE=6;
   public static final int EXIT_OTHER=100;
}
