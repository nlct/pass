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

import java.util.zip.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.TimeUnit;

import java.text.SimpleDateFormat;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.charset.MalformedInputException;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class is used to process the student's assignment and create
 * the PDF that they then need to submit. 
 * <strong>It's important for students to understand that PASS
 * doesn't submit their work for them. It simply creates a PDF file
 * that represents their work that they can then submit.</strong>
 *
 * The PDF contains pretty-printed listings of their source code
 * (with required files listed first in the order specified in the
 * assignment XML file, if applicable), a zip attachment
 * containing the source code, and (optionally) the result of
 * compiling their code (compiler errors and warnings), any content
 * written to STDOUT or STDERR when invoking their application, any
 * files created by their application (shown verbatim if text files,
 * embedded images if image files or just 
 * as attachments otherwise), and any reports accompanying the
 * submission (as attachments and, if the report is a PDF file and pdfpages has
 * been enabled, embedded). Any missing required files will have a
 * message written in the PDF indicating that the file hadn't been
 * provided.
 *
 * The PASS application name and version number is
 * included after the title, along with the operating system name,
 * version and architecture.
 *
 * The student needs to agree to read
 * the PDF before submitting their work to ensure that it
 * accurately represents their work. Otherwise, students don't
 * bother to look at the PDF, send it off, and then, when
 * they lose marks, they complain that Pass went wrong and didn't produce a
 * correct PDF. Usually it's not Pass that's wrong (but
 * occasionally it is, in which case the issue needs to be flagged),
 * but is more often due to the student not setting the correct compiler
 * flags when they compiled and tested their application in the
 * IDE or because the student didn't notice that PASS had been
 * instructed to use a different input file to the one the
 * student used to test their application, which had been hard-coded
 * for the specific test file.
 */

public class AssignmentProcess
{
   /**
    * Creates a new process object.
    * @param main the Pass application
    */ 
   public AssignmentProcess(Pass main)
   {
      this.main = main;
      this.config = main.getPassTools().getConfig();

      temporaryFiles = new Vector<File>();
   }

   /**
    * Creates a new process object. It's helpful for GUI versions of
    * PASS to have a progress bar, which can be updated with a
    * ProgressListener.
    * @param main the Pass application
    * @param listener the progress listener which may be null if
    * not required
    */ 
   public AssignmentProcess(Pass main, ProgressListener listener)
   {
      this(main);
      this.config = main.getPassTools().getConfig();
      progressListener = listener;
   }

   /**
    * Sets the progress listener. This will override the listener
    * supplied with the constructor.
    * @param listener the new progress listener which may be null if
    * not required
    */ 
   public void setProgressListener(ProgressListener listener)
   {
      progressListener = listener;
   }

   /**
    * Increments the progress if there's a non-null progress
    * listener.
    */ 
   private void incProgress()
   {
      if (progressListener != null)
      {
         progressListener.updateProgress((100*(++currentProgress))/maxProgress);
      }
   }

   /**
    * Sets the indeterminate state of the progress listener if not
    * null.
    * @param state true if progress should be in an indeterminate
    * state
    */ 
   public void setIndeterminateProgress(boolean state)
   {
      if (progressListener != null)
      {
         progressListener.setIndeterminate(state);
      }
   }

   /**
    * Adds a file to the list of temporary files.
    * Note that this doesn't create the file. The file may be a
    * directory.
    * @param parent the parent directory
    * @param name the file (or sub-directory) name
    * @return the temporary file
    */ 
   public File newTemporaryFile(File parent, String name)
   {
      File file = new File(parent, name);
      addTemporaryFile(file);
      return file;
   }

   /**
    * Creates a new temporary file and adds it to the list of
    * temporary files.
    * @param prefix the file prefix
    * @param suffix the file suffix
    * @param parent the parent directory
    * @return the new file
    */ 
   public File createTemporaryFile(String prefix, String suffix, File parent)
    throws IOException
   {
      File file = File.createTempFile(prefix, suffix, parent);
      addTemporaryFile(file);
      return file;
   }

   /**
    * Adds a file to the list of temporary files. The file may be a
    * directory.
    */ 
   public void addTemporaryFile(File file)
   {
      temporaryFiles.add(file);
   }

   /**
    * Deletes all identified temporary files and directories.
    */ 
   public void removeTemporaryFiles()
   {
      for (File file : temporaryFiles)
      {
         if (file.exists())
         {
            if (file.isDirectory())
            {
               main.getPassTools().deleteDir(file);
            }
            else
            {
               file.delete();
            }
         }
      }
   }

   /**
    * Creates the PDF representing the student's work that the
    * student can then submit. This creates the LaTeX source code,
    * and, if applicable, fetches resource files, compiles and runs
    * the student's application. Finally, the document is built
    * using LuaLaTeX if UTF-8 encoding requested or PDFLaTeX for
    * ASCII or Latin-1. A rerun is needed to ensure that the table
    * of contents is correct.
    * @return the PDF file
    * @throws IOException if an I/O error occurs
    * @throws InterruptedException if any of the sub-processes were
    * interrupted (for example, timeout or user aborted)
    * @throws URISyntaxException an invalid URI was supplied
    * @throws AgreementRequiredException student agreement is required 
    * but hasn't been given
    */ 
   public File createPdf() 
     throws IOException,
     InterruptedException,
     URISyntaxException,
     AgreementRequiredException
   {
      setIndeterminateProgress(false);

      Date now = new Date();

      // For server version where job may be queued
      Date submittedDate = main.getSubmittedDate();

      PassTools passTools = main.getPassTools();

      if (passTools.isAgreeRequired() && !main.isConfirmed())
      {
         throw new AgreementRequiredException(passTools);
      }

      alwaysFetchResources = true;

      maxOutput = passTools.getMaxOutputSetting();

      Vector<PassFile> fileFields = main.getFiles();
      StringBuilder fileWarnings = null;

      // Make sure that all selected files exist and that there are
      // no duplicate filenames.

      for (int i = fileFields.size()-1; i >= 0; i--)
      {
         File file = fileFields.get(i).getFile();
         String msg = null;

         if (!file.exists())
         {
            msg = passTools.getMessageWithDefault(
               "document.file_doesnt_exist_ignoring",
               "File `\\file{\\detokenize{{0}}}'' doesn''t exist: ignoring.",
               String.format("\\file{\\detokenize{%s}}", file.getName()));

            main.warning(passTools.getMessageWithDefault(
               "warning.file_doesnt_exist_ignoring",
               "File `{0}'' doesn''t exist: ignoring.",
               file.getName()));

            fileFields.remove(i);
         }
         else
         {
            for (int j = i-1; j >= 0; j--)
            {
               if (file.equals(fileFields.get(j).getFile()))
               {
                  msg = passTools.getMessageWithDefault(
                     "document.ignoring_duplicate_file",
                     "Ignoring duplicate file `\\file{\\detokenize{{0}}}''.",
                      String.format("\\file{\\detokenize{%s}}", file.getName()));

                  main.warning(passTools.getMessageWithDefault(
                     "warning.ignoring_duplicate_file",
                     "Ignoring duplicate file ''{0}''.",
                      file.getName()));

                  fileFields.remove(i);

                  break;
               }
            }
         }

         if (msg != null)
         {
            if (fileWarnings == null)
            {
               fileWarnings = new StringBuilder(msg);
            }
            else
            {
               fileWarnings.insert(0, msg + " ");
            }
         }
      }

      basePath = main.getBasePath();
      timeout = main.getTimeOut();

      main.transcriptMessage(passTools.getMessageWithDefault(
        "message.timeout", "Timeout: {0,number}s.", timeout)
      );

      AssignmentData data = main.getAssignment();

      String encoding = main.getEncoding();
      String engine;

      if (encoding.equals(Pass.ENCODING_UTF8))
      {
         engine = "lualatex";
         isLua = true;
      }
      else
      {
         engine = "pdflatex";
         isLua = false;
         isASCII = main.getEncoding().equals(Pass.ENCODING_ASCII);
      }

      latexPath = passTools.findApplication(engine).getAbsolutePath();

      String mainFile = data.getMainFile();
      RequiredPassFile mainFilePanel = null;

      String label = data.getLabel();
      PrintWriter out = null;

      pdfFile = null;

      blackboardId="??";
      String student="??";
      String studentHeader="??";

      if (main.isGroupProject())
      {
         Vector<Student> students = main.getProjectTeam();

         StringBuilder builder = new StringBuilder();

         for (int i = 0; i < students.size(); i++)
         {
            Student s = students.get(i);

            builder.append(config.getAuthor(s));

            if (i == 0)
            {
               blackboardId = s.getBlackboardId();
               studentHeader = passTools.getMessageWithDefault(
                    "document.author_etal",
                    "{0} \\emph{et al}",
                     builder);
               builder.append("\\\\");
            }
         }

         student = builder.toString();
      }
      else
      {
         Student s = main.getStudent();

         blackboardId = s.getBlackboardId();
         student = config.getAuthor(s);
         studentHeader = student;
      }

      boolean usePdfPages = config.usePdfPages();

      maxProgress = 2*fileFields.size()+4;
      currentProgress = 0;

      try
      {
         File dir = passTools.createTempDirectory();
         addTemporaryFile(dir);

         main.transcriptMessage(passTools.getMessageWithDefault(
            "message.temp_dir",
            "Temporary directory: {0}.",
            dir));

         jobname = dir.getName();

         texDir = dir;
         File texFile = new File(dir, jobname+".tex");
         File auxFile = new File(dir, jobname+".aux");
         File logFile = new File(dir, jobname+".log");
         File outFile = new File(dir, jobname+".out");
         File tocFile = new File(dir, jobname+".toc");
         pdfFile = new File(dir, jobname+".pdf");
         File zipFile = null;

         // Create a zip file containing all the submitted source
         // code files. This adds the original selected files. They
         // haven't been copied to the temporary directory yet.

         if (!fileFields.isEmpty())
         {
            zipFile = new File(dir, main.getDefaultBaseName()+".zip");

            createZipFile(label, zipFile, fileFields);
         }

         // Start writing the LaTeX code

         out = new PrintWriter(texFile);

         out.println("\\batchmode");
         out.println("\\documentclass{article}");

         CharSequence fontSettings;

         if (encoding.equals(Pass.ENCODING_UTF8))
         {
            out.println("\\usepackage{luatex85}");

            String fontSpecOptions = passTools.getFontSpecOptions();
        
            if (fontSpecOptions == null)
            {
               out.println("\\usepackage{fontspec}");
            }
            else
            {
               out.format("\\usepackage[%s]{fontspec}%n", fontSpecOptions);
            }

            fontSettings = passTools.getFontSpecSettings();
         }
         else
         {
            String fontEncOptions = passTools.getFontEncOptions();

            if (fontEncOptions == null)
            {
               out.println("\\usepackage[T1]{fontenc}");
            }
            else
            {
               out.format("\\usepackage[%s]{fontenc}%n", fontEncOptions);
            }

            fontSettings = passTools.getFontEncSettings();

            if (encoding.equals(Pass.ENCODING_LATIN1))
            {
               out.println("\\usepackage[latin1]{inputenc}");
            }
         }

         if (fontSettings == null)
         {
            out.println("\\usepackage{lmodern}");
         }
         else
         {
            out.println(fontSettings.toString());
         }

         out.println("\\usepackage{geometry}");
         out.println("\\usepackage{graphicx}");
         out.println("\\usepackage{upquote}");
         out.println("\\usepackage{verbatim}");
         out.println("\\usepackage{listings}");
         out.println("\\usepackage{attachfile}");

         if (usePdfPages)
         {
            out.println("\\usepackage{pdfpages}");
         }

         out.println("\\usepackage{hyperref}");
         out.println("\\newcommand{\\file}[1]{\\texorpdfstring{\\texttt{#1}}{#1}}");
         out.println("\\newcommand{\\warning}[1]{\\textbf{\\color{red}#1}}");

         out.println("\\hypersetup{hidelinks,%");
         out.println(String.format("pdftitle={%s},%%", data.getTitle()));
         out.println(String.format("pdfauthor={%s},%%", blackboardId));
         out.format(String.format("pdfsubject={%s}}%n",
          passTools.getMessageWithDefault(
            "document.subject", "{0} Assignment Submission", 
            data.getCourse().getCode())));

         try
         {
            String checksum = "";

            if (zipFile != null)
            {
               checksum = config.getCheckSum(zipFile);
            }

            out.print("\\pdfinfo{");

            String pdfDate = PDF_DATE_FORMAT.format(now);
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);

            int zoneOffset = cal.get(Calendar.ZONE_OFFSET)
                                 + cal.get(Calendar.DST_OFFSET);

            if (zoneOffset == 0)
            {
               pdfDate += "Z";
            }
            else
            {
               // convert from milliseconds to seconds
               zoneOffset /= 10000;
               pdfDate = String.format("%s%s%02d'%02d'", pdfDate, 
                  zoneOffset >= 0 ? "+": "", zoneOffset/360, zoneOffset%360);
            }

            out.format("/CreationDate (%s)%n", pdfDate);

            // Encrypted metadata for pass-checker

            byte[] keyValue = config.createRandomKey();

            if (!checksum.isEmpty())
            {
               out.print("/DataCheckA (");
               writeBytes(out, config.encrypt(checksum.getBytes(), keyValue)); 
               out.println(")");
            }

            out.print("/DataCheckB (");
            writeBytes(out, config.encrypt(getDateStamp(now), keyValue)); 
            out.println(")");

            out.print("/DataCheckC (");
            writeBytes(out,
              config.encrypt(main.getApplicationVersion().getBytes(), keyValue)); 
            out.println(")");

            out.print("/DataCheckD (");
            writeBytes(out, config.encrypt(getDateStamp(data.getDueCalendarDate()), 
               keyValue)); 
            out.println(")");

            out.print("/DataCheckE (");
            writeBytes(out, config.encrypt(blackboardId.getBytes(), keyValue)); 
            out.println(")");

            out.print("/DataCheckF (");
            writeBytes(out, config.encrypt_1_16(keyValue)); 
            out.println(")");

            out.print("/DataCheckG (");
            writeBytes(out,
              config.encrypt(main.getApplicationName().getBytes(), keyValue)); 
            out.println(")");

            if (submittedDate != null)
            {
               out.print("/DataCheckH (");
               writeBytes(out,
                 config.encrypt(getDateStamp(submittedDate), keyValue)); 
               out.println(")");
            }

            out.println("}");

         }
         catch (Exception e)
         {
            main.error(e);
         }

         CharSequence geometry = passTools.getGeometrySettings();

         if (geometry == null || geometry.length() == 0)
         {
            out.println("\\geometry{a4paper,margin=1in}");
         }
         else
         {
            out.format("\\geometry{a4paper,%s}%n", geometry);
         }

         out.println("\\makeatletter");
         out.println("\\newcommand{\\ps@pass}{%");
         out.println(String.format(" \\def\\@oddhead{\\rightmark\\hfill %s}%%",
          studentHeader));
         out.println(" \\def\\@oddfoot{\\reset@font\\hfil\\thepage\\hfil}%");
         out.println(" \\let\\@evenhead\\@oddhead");
         out.println(" \\let\\@evenfoot\\@oddfoot");
         out.println(" \\let\\@mkboth\\markboth");
         out.println(" \\def\\sectionmark##1{\\markright{##1}}%");
         out.println("}");
         out.println("\\makeatother");

         out.println("\\pagestyle{pass}");

         out.println("\\newlength\\imgwidth");
         out.println("\\newlength\\imgheight");
         out.println("\\newsavebox\\imgsbox");
         out.println("\\newcommand\\includeimg[1]{%");
         out.println(" \\sbox{\\imgsbox}{\\includegraphics{#1}}%");
         out.println(" \\settowidth{\\imgwidth}{\\usebox\\imgsbox}%");
         out.println(" \\settoheight{\\imgheight}{\\usebox\\imgsbox}%");
         out.println(" \\ifdim\\imgwidth<\\imgheight");
         out.println("   \\ifdim\\imgwidth>\\linewidth");
         out.println("     \\resizebox{\\linewidth}{!}{\\usebox\\imgsbox}%");
         out.println("   \\else");
         out.println("     \\usebox\\imgsbox");
         out.println("   \\fi");
         out.println(" \\else");
         out.println("   \\dimen0=0.9\\textheight");
         out.println("   \\ifdim\\imgheight>\\dimen0");
         out.println("     \\resizebox{!}{\\dimen0}{\\usebox\\imgsbox}%");
         out.println("   \\else");
         out.println("     \\usebox\\imgsbox");
         out.println("   \\fi");
         out.println(" \\fi");
         out.println("}");

         out.println("\\lstset{%");
         out.println(" basicstyle=\\ttfamily,");
         out.println(" numbers=left,");
         out.println(" numberstyle=\\tiny,");
         out.println(" stepnumber=2,");
         out.println(" showstringspaces=false,");
         out.print(" breaklines");

         CharSequence listingSettings = passTools.getListingSettings();

         if (listingSettings != null)
         {
            out.println(",");
            out.print(listingSettings);
         }

         out.println("}");

         out.println("\\setcounter{secnumdepth}{-1}");

         out.println(String.format("\\title{%s}", data.getTitle()));

         out.println(String.format("\\author{%s}", student));

         if (submittedDate == null)
         {
            out.println(String.format("\\date{%s}", 
              config.formatDocDate(now)));
         }
         else
         {
            out.println(String.format("\\date{%s}", 
              config.formatDocDate(submittedDate)));
         }

         config.writeExtraPreambleCode(out);

         out.println("\\begin{document}");
         out.println("\\maketitle");
         out.print("\\noindent ");

         out.print(passTools.getMessageWithDefault(
          "document.version_details", 
          "PDF prepared using {0} version {1} running on {2} {3} ({4}).",
          main.getApplicationName(), main.getApplicationVersion(), 
          String.format("\\verb!%s!", System.getProperty("os.name").replaceAll("!", "")),
          String.format("\\verb!%s!", System.getProperty("os.version").replaceAll("!", "")),
          String.format("\\verb!%s!", System.getProperty("os.arch").replaceAll("!", ""))));

         out.format("%n%n\\medskip \\noindent %s %s%n%n\\bigskip ", 
            main.isConfirmed() ? CHECK_BOX : EMPTY_CHECK_BOX,
            passTools.getConfirmText()
         );

         if (zipFile != null)
         {
            out.println("\\attachfile");
            out.println("[mimetype=application/zip,");
            out.println(String.format("description={%s},",
              passTools.getMessageWithDefault(
                "document.source_code", 
                "Source code {0}.", 
                zipFile.getName())));
            out.println(String.format("author={%s},", blackboardId));
            out.println(String.format("size={%d}", zipFile.length()));
            out.println(String.format("]{%s}", zipFile.getName()));
         }
         else
         {
            out.format("\\warning{%s}%n", passTools.getMessageWithDefault(
              "document.no_source_code", "No source code files provided."));
         }

         out.println("\\tableofcontents");

         // Copy each source file to the temporary directory and add
         // the LaTeX code to input the file.

         // Separate binary files
         Vector<PassFile> reports = new Vector<PassFile>();
         Vector<AllowedBinaryFile> binaries = new Vector<AllowedBinaryFile>();

         for (PassFile field : fileFields)
         {
            String language = field.getLanguage();

            if (language == null
             || language.equals(AssignmentData.BINARY)
             || language.equals(AssignmentData.UNKNOWN_LANGUAGE))
            {
               if (field instanceof AllowedBinaryFile)
               {
                  binaries.add((AllowedBinaryFile)field);
                  continue;
               }
               else
               {
                  File file = field.getFile();

                  AllowedBinaryFilter filter 
                     = data.getAllowedBinaryFilter(file);

                  if (filter != null)
                  {
                     binaries.add(new AllowedBinaryFile(file, filter));
                     continue;
                  }
               }
            }
            else if (language.equals("PDF") || language.equals("WORD"))
            {
               reports.add(field);
               continue;
            }

            incProgress();

            if (field instanceof RequiredPassFile)
            {
               // If this is a required file, check if it's the main
               // file.

               RequiredPassFile reqFP = (RequiredPassFile)field;

               if (reqFP.getRequiredName().equals(mainFile))
               {
                  mainFilePanel = reqFP;
               }
            }

            copyAndInput(out, dir, field, "section");
         }

         if (fileWarnings != null)
         {
            out.format("\\par\\warning{%s}", fileWarnings.toString());
         }

         if (!binaries.isEmpty())
         {
            out.format("\\section{%s}%n",
               passTools.getChoiceMessage("document.binary.section", binaries.size()));

            for (PassFile file : binaries)
            {
               incProgress();

               copyAndInput(out, dir, file, "subsection");
            }
         }

         for (PassFile file : reports)
         {
            incProgress();

            copyAndInput(out, dir, file, "section");
         }

         incProgress();

         setIndeterminateProgress(true);

         int exitCode = buildCompileRun(out, dir, fileFields, mainFilePanel, data);

         setIndeterminateProgress(false);
         out.println("\\end{document}");
         out.close();
         out = null;

         incProgress();
         exitCode = latex(texFile);

         incProgress();

         if (exitCode == 0)
         {
            exitCode = latex(texFile);
         }
         else
         {
            main.warning(passTools.getMessageWithDefault(
              "warning.something_went_wrong",
              "Something went wrong while compiling the PDF."));

            main.debug(String.format(
              "Process failed with exit code %d:%n%s '%s'%nin directory %s",
              exitCode, engine, texFile.getName(), texFile.getParent()));
         }

         parseLaTeXLog(logFile);

         incProgress();
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }
      }

      return pdfFile;
   }

   /**
    * Copies source code file and writes LaTeX code to include it.
    * @param out the output stream
    * @param dir the directory to copy the files to
    * @param field the source code file
    * @throws IOException if I/O error occurs
    */ 
   private void copyAndInput(PrintWriter out, File dir, PassFile field, String section)
   throws IOException
   {
      PassTools passTools = main.getPassTools();
      AssignmentData data = main.getAssignment();

      File src = field.getFile();

      // Scrub any awkward characters in the filename that
      // will cause a problem for LaTeX. This will cause a
      // problem for source code files that need to be
      // compiled, but awkward characters are likely to cause
      // the compiler problems as well.

      String originalName = src.getName();
      String filename = getScrubbedFileName(dir, originalName);

      String language = field.getLanguage();

      if (!(language == null || language.equals("PDF") || language.equals("DOC")
            || language.equals(AssignmentData.BINARY)))
      {
         out.println("\\clearpage");
      }

      Path srcPath = src.toPath();
      StringBuilder subPath = new StringBuilder();

      Path destPath = getDestination(srcPath, filename, dir, subPath);
      File dest = destPath.toFile();

      // Start a new section for each file

      out.println(String.format("\\%s{\\file{%s%s}}", section, subPath, filename));

      if (!filename.equals(originalName))
      {
         main.warning(getPassTools().getMessageWithDefault(
          "warning.filename_scrubbed",
          "Filename scrubbed (one or more forbidden characters found). Original name: {0}",
           originalName));

         out.println(passTools.getMessageWithDefault(
           "document.filename_scrubbed",
           "Filename scrubbed (one or more forbidden characters found). Original name:")
         );

         createAndWriteVerbatim(out, originalName);
      }

      // Copy file to temporary directory and write the LaTeX code
      // to include it.

      try
      {
         if (filename.isEmpty() || !src.exists())
         {
            main.warning(passTools.getMessageWithDefault(
              "warning.cant_find_src",
              "Can''t find file ''{0}'' [src={1}].",
              filename, src));

            out.println(passTools.getMessageWithDefault(
              "document.file_not_found", 
              "File not found."));
         }
         else if (passTools.isBannedFile(filename))
         {
            main.warning(passTools.getMessageWithDefault(
              "warning.forbidden_file",
              "Forbidden file ''{0}''.", filename));

            out.println(passTools.getMessageWithDefault(
              "document.omitting_forbidden_file",
              "Omitting forbidden file."));
         }
         else
         {
            main.debug("Copying "+srcPath+" -> "+destPath);

            try
            {
               Files.copy(srcPath, destPath);
            }
            catch (Exception e)
            {
               main.error(e);
            }

            boolean addBraces = (isLua && !filename.contains("."));

            if (!dest.exists())
            {
               out.format("\\warning{%s}%n",
                 passTools.getMessage("error.file_not_found.copy_failed",
                    filename));
            }
            else if (language == null || AssignmentData.BINARY.equals(language))
            {
               out.println(passTools.getMessage("document.binary", src.length()));
               out.println();

               String mimetype = null;
               boolean showListing = false;

               if (field instanceof AllowedBinaryFile)
               {
                  mimetype = ((AllowedBinaryFile)field).getMimeType();
                  showListing = ((AllowedBinaryFile)field).showListing();
               }

               if (mimetype == null)
               {
                  out.format("\\warning{%s}%n",
                    passTools.getMessage("document.binary_no_mimetype"));
               }
               else
               {
                  String contentType = null;

                  try
                  {
                     contentType = Files.probeContentType(destPath);
                  }
                  catch (IOException e)
                  {
                     main.error(e);
                  }

                  if (contentType == null)
                  {
                     out.format("\\warning{%s}%n",
                       passTools.getMessage("document.binary_probe_failed", mimetype));
                  }
                  else if (!contentType.equals(mimetype))
                  {
                     out.format("\\warning{%s}%n",
                       passTools.getMessage("document.binary_probe_not_matched",
                          mimetype, contentType));
                  }
                  else if (showListing)
                  {
                     if (mimetype.startsWith("image/"))
                     {
                        out.println(String.format(
                           "\\includeimg{%s%s}", subPath, filename));
                     }
                     else
                     {
                        out.format("\\warning{%s}%n",
                          passTools.getMessage("document.binary_not_supported",
                             mimetype));
                     }
                  }
               }
            }
            else if (language.equals(AssignmentData.UNKNOWN_LANGUAGE)
             || language.equals(AssignmentData.PLAIN_TEXT))
            {
               createAndWriteVerbatim(out, src);
            }
            else if (language.equals("PDF"))
            {
               out.println("\\attachfile");
               out.println("[mimetype=application/pdf,");
               out.println(String.format("description={%s},",
                  passTools.getMessageWithDefault(
                   "document.pdf", "PDF document {0}.", filename)));
               out.println(String.format("author={%s},", blackboardId));
               out.println(String.format("size={%d}", dest.length()));
               out.println(String.format("]{%s%s}", subPath, filename));

               out.println(passTools.getMessage("document.binary", src.length()));
               out.println();

               // The student has identified the file as a PDF
               // file but have they made a mistake?

               String contentType = null;

               try
               {
                  contentType = Files.probeContentType(destPath);
               }
               catch (IOException e)
               {
                  main.error(e);
               }

               boolean incPdf = (config.usePdfPages() && !filename.contains(" "));

               if (contentType == null)
               {
                  out.format("\\warning{%s}%n",
                    passTools.getMessage("document.pdf_probe_failed"));

                  incPdf = false;
               }
               else if (!contentType.equals("application/pdf"))
               {
                  out.format("\\warning{%s}%n",
                    passTools.getMessage("document.pdf_probe_not_matched",
                      contentType));

                  incPdf = false;
               }

               if (!filename.endsWith(".pdf"))
               {
                  main.warning(passTools.getMessage(
                    "warning.ext_not.pdf", filename));

                  out.format("\\warning{%s}%n",
                    passTools.getMessage("document.not_pdf_ext"));

                  incPdf = false;
               }

               if (incPdf)
               {
                  String pdfPagesOptions = config.getPdfPagesOptions();

                  out.println(passTools.getMessageWithDefault(
                    "document.pdf_starts_nextpage",
                    "(Included PDF starts on next page.)"));

                  if (pdfPagesOptions == null || pdfPagesOptions.isEmpty())
                  {
                     out.format("\\includepdf[pages=-]{%s%s}",
                       subPath, filename);
                  }
                  else
                  {
                     out.format("\\includepdf[pages=-,%s]{%s%s}",
                       pdfPagesOptions, subPath, filename);
                  }

                  out.println();
               }
            }
            else if (language.equals("DOC"))
            {
               boolean ext_warn = false;

               String docContentType = AssignmentData.getMimeType(language, filename);

               out.println("\\attachfile");
               out.print("[mimetype=");

               out.print(docContentType);
               out.println(",");

               if (!(filename.endsWith(".docx") || filename.endsWith(".doc")))
               {
                  ext_warn = true;
               }

               out.println(String.format("description={%s},",
                  passTools.getMessageWithDefault(
                    "document.word", 
                    "Word document {0}.", 
                    filename)));
               out.println(String.format("author={%s},", blackboardId));
               out.println(String.format("size={%d}", dest.length()));
               out.println(String.format("]{%s%s}", subPath, filename));

               out.println(passTools.getMessage("document.binary", src.length()));

               // The student has identified the file as a
               // Word document, but have they made a mistake?

               String contentType = null;

               try
               {
                  contentType = Files.probeContentType(destPath);
               }
               catch (IOException e)
               {
                  main.error(e);
               }

               if (contentType == null)
               {
                  out.format("\\warning{%s}%n",
                    passTools.getMessage("document.doc_probe_failed"));
               }
               else if (!contentType.equals(docContentType))
               {
                  out.format("\\warning{%s}%n",
                    passTools.getMessage("document.doc_probe_not_matched",
                      docContentType, contentType));
               }

               if (ext_warn)
               {
                  main.warning(passTools.getMessage(
                       "warning.ext_not.doc", filename));

                  out.format("\\warning{%s}%n",
                    passTools.getMessage("document.not_doc_ext"));
               }
            }
            else if (language.equals(data.getMainLanguage()))
            {
               String variant = data.getLanguageVariant();

               if (variant == null)
               {
                  out.print(String.format(
                    "\\lstinputlisting[language={%s}",
                    language));
               }
               else
               {
                  out.print(String.format(
                    "\\lstinputlisting[language={[%s]%s}",
                    variant, language));
               }

               if (addBraces)
               {
                  out.println(String.format("]{{%s%s}}", subPath, filename));
               }
               else
               {
                  out.println(String.format("]{%s%s}", subPath, filename));
               }
            }
            else
            {
               out.print(String.format(
                 "\\lstinputlisting[language={%s}",
                 language));

               if (addBraces)
               {
                  out.println(String.format("]{{%s%s}}", subPath, filename));
               }
               else
               {
                  out.println(String.format("]{%s%s}", subPath, filename));
               }

            }
         }
      }
      catch (FileAlreadyExistsException e)
      {
         out.println(passTools.getMessageWithDefault(
          "document.filename_conflict",
          "File name conflict."));
      }
   }

   /**
    * Gets the destination path and creates sub-directories in
    * preparation to copying a file. Doesn't actually copy the file.
    * @param srcPath the source path
    * @param filename the destination filename
    * @param dir the directory file will be copied to
    * @param subPath the sub path name using / divider (required by
    * LaTeX) may be null if not required
    * @return the destination path
    * @throws IOException if I/O error occurs
    */ 
   protected Path getDestination(Path srcPath, 
     String filename, File dir, StringBuilder subPath)
   throws IOException
   {
      PassTools passTools = main.getPassTools();

      Path destPath;
      File dest;

      if (basePath == null)
      {
         dest = new File(dir, filename);
         destPath = dest.toPath();
      }
      else
      {
         Path relPath = null;

         try
         {
            relPath = basePath.relativize(srcPath);
         }
         catch (IllegalArgumentException e)
         {
            throw new IOException(passTools.getMessageWithDefault(
              "error.cant_relativize",
              "Can''t relativize source path ''{0}'' against base path ''{1}''.",
              srcPath, basePath), e);
         }

         destPath = dir.toPath().resolve(relPath);
         dest = new File(destPath.toFile().getParentFile(), filename);

         Files.createDirectories(dest.getParentFile().toPath());

         if (subPath != null)
         {
            for (int i = 0, n = relPath.getNameCount()-1; i < n; i++)
            {
               String name = relPath.getName(i).toString();

               Matcher m = FORBIDDEN_PATTERN.matcher(name);

               if (m.find())
               {
                  throw new IOException(passTools.getMessageWithDefault(
                  "error.illegal_char_in_dirname",
                  "Illegal character ''{0}'' found in directory name ''{1}''.",
                   m.group(), name));
               }

               subPath.append(name);
               subPath.append('/');
            }
         }
      }

      return destPath;
   }

   /**
    * Gets a file name suitable for use in a LaTeX document.
    * Problematic characters aren't permitted in filenames. 
    * @param dir the directory containing the file
    * @param name the file name
    * @return a file name suitable for use with LaTeX
    */ 
   private String getScrubbedFileName(File dir, String name)
   {
      return getScrubbedFileName(dir, name, false);
   }

   /**
    * Gets a file name suitable for use in a LaTeX document.
    * Problematic characters aren't permitted in filenames. 
    * Spaces in PDF filenames aren't permitted with <code>\includepdf</code>
    * so they will be replaced with a hyphen.
    * @param dir the directory containing the file
    * @param name the file name
    * @param extended if true use the extended forbidden pattern
    * FORBIDDEN_PATTERN_EXTENDED otherwise use the forbidden pattern
    * FORBIDDEN_PATTERN
    * @return a file name suitable for use with LaTeX
    */ 
   private String getScrubbedFileName(File dir, String name, boolean extended)
   {
      if (config.usePdfPages() && name.endsWith(".pdf"))
      {
         name = name.replaceAll(" ", "-");
      }

      Pattern pattern = (extended ? FORBIDDEN_PATTERN_EXTENDED : FORBIDDEN_PATTERN);

      Matcher m = pattern.matcher(name);

      if (m.find())
      {
         int idx = name.lastIndexOf(".");
         String ext = "";

         if (idx > -1)
         {
            ext = name.substring(idx);

            m = pattern.matcher(ext);

            if (m.find())
            {
               ext = "";
            }
         }

         String base = main.getDefaultBaseName()+"-file";

         File file = new File(dir, base+ext);
         idx = 0;

         while (file.exists())
         {
            idx++;
            file = new File(dir, base+idx+ext);
         }

         return file.getName();
      }

      return name;
   }

   /**
    * Creates a zip file containing all the supplied files.
    * @param base the base path
    * @param zipFile the zip file
    * @param fileFields the list of files
    * @throws IOException if I/O error occurs
    */ 
   private void createZipFile(String base, File zipFile, 
     Vector<PassFile> fileFields) 
     throws IOException
   {
      ZipOutputStream out = null;
      byte[] buffer = new byte[MAX_INPUT_BYTES];

      try
      {
         main.debug(String.format("Creating %s", zipFile));

         out = new ZipOutputStream(new FileOutputStream(zipFile));

         for (PassFile comp : fileFields)
         {
            incProgress();

            File file = comp.getFile();

            if (file != null && file.exists())
            {
               writeZipEntry(out, base, file, buffer);
            }
         }
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }
      }
   }

   /**
    * Writes an entry in the zip file.
    * @param out the zip output stream
    * @param base the base path
    * @param file the file to add to the archive
    * @param buffer the buffer
    * @throws IOException if I/O error occurs
    */ 
   private void writeZipEntry(ZipOutputStream out,
     String base, File file, byte[] buffer)
     throws IOException
   {
      FileInputStream in = null;

      try
      {
         String name = file.getName();

         if (basePath != null)
         {
            Path relPath = basePath.relativize(file.toPath());

            for (int i = relPath.getNameCount()-2; i >= 0; i--)
            {
               name = String.format("%s/%s", relPath.getName(i), name);
            }
         }

         String zipEntryName = String.format("%s/%s", base, name);

         main.debug(String.format("Adding %s", zipEntryName));

         ZipEntry entry = new ZipEntry(zipEntryName);

         out.putNextEntry(entry);

         in = new FileInputStream(file);

         int bytes = -1;

         while ((bytes = in.read(buffer)) != -1)
         {
            out.write(buffer, 0, bytes);
         }

         in.close();
         in = null;

         out.closeEntry();
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
    * Writes each byte in hexadecimal notation.
    * @param out the output stream
    * @param data the byte data
    * @throws IOException if I/O error occurs
    */ 
   private void writeBytes(PrintWriter out, byte[] data) throws IOException
   {
      for (byte b : data)
      {
         out.format("%02X", b);
      }
   }

   /**
    * Gets the timestamp as a byte array.
    * @param cal the timestamp
    * @return the byte array
    * @throws IOException if I/O error occurs
    */ 
   private byte[] getDateStamp(Calendar cal) throws IOException
   {
      return getDateStamp(cal.getTime());
   }

   /**
    * Gets the timestamp as a byte array.
    * @param date the timestamp
    * @return the byte array
    * @throws IOException if I/O error occurs
    */ 
   private byte[] getDateStamp(Date date) throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeLong(date.getTime());
      return baos.toByteArray();
   }

   /**
    * Runs LaTeX (PDFLaTeX or LuaLaTeX) on the given file.
    * @param texFile the LaTeX file
    * @return the process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    */ 
   private int latex(File texFile)
     throws IOException,InterruptedException
   {
      ProcessBuilder builder = new ProcessBuilder(latexPath,
       texFile.getName());

      File dir = texFile.getParentFile();
      builder.directory(dir);

      if (latexPath.contains(" "))
      {
         main.debug(String.format("Running '%s' %s%nin directory %s",
            latexPath, texFile.getName(), dir));
      }
      else
      {
         main.debug(String.format("Running %s %s%nin directory %s",
            latexPath, texFile.getName(), dir));
      }

      File log = File.createTempFile("PASS_tex-output-", ".txt", dir);

      return runProcess(timeout*1000L, builder, log);
   }

   /**
    * Creates a buffered reader using the designated encoding.
    * @param file the input file
    * @return the BufferedReader
    * @throws IOException if I/O error occurs
    */ 
   private BufferedReader newBufferedReader(File file) throws IOException
   {
      return main.getPassTools().newBufferedReader(file);
   }

   /**
    * Gets the PassTools object associated with the PASS application.
    * @return the PassTools object
    */ 
   protected PassTools getPassTools()
   {
      return main.getPassTools();
   }

   /**
    * Parses the LaTeX log file for warnings and possible invalid encoding messages.
    * This will issue warnings via Pass.warning(String) if there are
    * any problems.
    * @param logFile the log file create by LaTeX
    * @throws IOException if I/O error occurs
    */ 
   private void parseLaTeXLog(File logFile) throws IOException
   {
      BufferedReader in = null;
      boolean binary = false;

      try
      {
         in = newBufferedReader(logFile);

         String line;

         while ((line = in.readLine()) != null)
         {
            if (line.contains("Text line contains an invalid character")
             || line.contains("String contains an invalid utf-8 sequence"))
            {
               main.warning(line);
               binary = true;
            }
            else if (line.startsWith("Missing") || line.startsWith("! ")
               || (!line.startsWith("Package rerunfilecheck") 
                       && line.contains("Warning")))
            {
               main.warning(line);
            }
         }
      }
      catch (MalformedInputException e)
      {
         binary = true;
         throw e;
      }
      finally
      {
         if (binary)
         {
            main.warning(
               getPassTools().getMessageWithDefault(
               "warning.possible_binary",
               "A binary file may have been added where a text file was expected (or wrong encoding used)."));
         }

         if (in != null)
         {
            in.close();
         }
      }
   }

   /**
    * Runs a process with timeout.
    * @param timeout the timeout value in milliseconds
    * @param builder the process builder
    * @param outFile the output file (captures STDOUT)
    * @param errFile the output file (captures STDERR)
    * @param inFile the input file (supplies STDIN)
    * @return the process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    */ 
   private int runProcess(long timeout, ProcessBuilder builder, File outFile,
    File errFile, File inFile)
   throws IOException,InterruptedException
   {
      main.getPassTools().addEnvironmentVariablesToProcess(builder);

      currentTask = new PassTask(main, timeout, builder,
        outFile, errFile, inFile);

      int exitCode = currentTask.performProcess();
      currentTask = null;

      return exitCode;
   }

   /**
    * Interrupts the current task.
    * @return true if current task was interrupted or false if there
    * was no current task to interrupt
    */ 
   public boolean interrupt()
   {
      if (currentTask != null)
      {
         currentTask.interrupt();
         currentTask = null;
         return true;
      }

      return false;
   }

   /**
    * Runs a process with timeout.
    * @param timeout the timeout value in milliseconds
    * @param builder the process builder
    * @param log the log file
    * @return the process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    */ 
   public int runProcess(long timeout, ProcessBuilder builder, File log)
     throws IOException,InterruptedException
   {
      main.debug("PASS log file: "+log);

      main.getPassTools().addEnvironmentVariablesToProcess(builder);

      currentTask = new PassTask(main, timeout, builder, log);

      int exitCode = currentTask.performProcess();

      verbose(log);

      currentTask = null;

      return exitCode;
   }

   /**
    * Verbose message containing the contents of a file.
    * @param file the file containing the message
    */ 
   public void verbose(File file)
   {
      if (!file.exists())
      {
         main.debug("No such file: "+file);

         return;
      }

      BufferedReader in = null;

      try
      {
         in = newBufferedReader(file);

         String line;

         while ((line = in.readLine()) != null)
         {
            main.verbose(line);
         }
      }
      catch (IOException e)
      {
         main.error(e);
      }
      finally
      {
         if (in != null)
         {
            try 
            {
               in.close();
            }
            catch (IOException e)
            {
               main.error(e);
            }
         }
      }
   }

   /**
    * Executes a process and waits for result.
    * Uses the default timeout setting.
    * @param process the process
    * @throws InterruptedException if interruption occurs
    * @throws InterruptedByTimeoutException if the process timed-out
    */ 
   private int waitFor(Process process)
     throws InterruptedException,InterruptedByTimeoutException
   {
      if (!process.waitFor(timeout, TimeUnit.SECONDS))
      {
         process.destroy();
         main.error(getPassTools().getMessageWithDefault(
            "error.process_timedout",
            "Processed timed out after {0,number}s.", timeout));

         throw new InterruptedByTimeoutException();
      }

      return process.exitValue();
   }

   /**
    * Tests the student's application without creating a PDF. Used
    * by PassEditor for a work in progress.
    * @param out the output file
    * @param dir the directory containing the source code
    * @return process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    * @throws URISyntaxException if an invalid URI is encountered
    */ 
   public int runTestsNoPDF(PrintWriter out, File dir)
     throws IOException,InterruptedException,URISyntaxException
   {
      texDir = dir;
      maxOutput = main.getPassTools().getMaxOutputSetting();
      Vector<PassFile> fileFields = main.getFiles();
      AssignmentData data = main.getAssignment();
      String mainFile = data.getMainFile();
      RequiredPassFile mainFilePanel = null;
      alwaysFetchResources = false;

      basePath = main.getBasePath();
      timeout = main.getTimeOut();
      jobname = dir.getName();

      String encoding = main.getEncoding();
      isLua = encoding.equals(Pass.ENCODING_UTF8);

      maxProgress = 3;
      currentProgress = 0;

      for (PassFile field : fileFields)
      {
         if (field instanceof RequiredPassFile)
         {
            RequiredPassFile reqFP = (RequiredPassFile)field;

            if (reqFP.getRequiredName().equals(mainFile))
            {
               mainFilePanel = reqFP;
               break;
            }
         }
      }

      return buildCompileRun(out, dir, fileFields, mainFilePanel, data, true);
   }

   /**
    * Performs build and testing processes.
    * @param out the print writer
    * @param dir the directory containing the source code
    * @param fileFields the student's files
    * @param mainFilePanel the main file
    * @param data the assignment data
    * @return process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    */ 
   private int buildCompileRun(PrintWriter out, File dir,
     Vector<PassFile> fileFields, RequiredPassFile mainFilePanel,
     AssignmentData data)
     throws IOException,InterruptedException,URISyntaxException
   {
      return buildCompileRun(out, dir, fileFields, mainFilePanel, data, false);
   }

   /**
    * Performs build and testing processes.
    * @param out the print writer
    * @param dir the directory containing the source code
    * @param fileFields the student's files
    * @param mainFilePanel the main file
    * @param data the assignment data
    * @param noPdf true if no PDF is being created
    * @return process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    */ 
   private int buildCompileRun(PrintWriter out, File dir,
     Vector<PassFile> fileFields, RequiredPassFile mainFilePanel,
     AssignmentData data, boolean noPdf)
     throws IOException,InterruptedException,URISyntaxException
   {
      int exitCode = 0;
      URL buildURL;

      if (noPdf)
      {
         runTest = main.getAssignment().isNoPdfRunTestOn();
         buildURL = data.getNoPdfBuildScript();
      }
      else
      {
         runTest = main.getAssignment().isRunTestOn();
         buildURL = data.getBuildScript();
      }

      PassTools passTools = getPassTools();

      if (buildURL != null)
      {
         out.println("\\clearpage");
         out.format("\\section{%s}%n", 
           passTools.getMessageWithDefault("document.build_script", "Build Script"));

         // fetch build script

         try
         {
            exitCode = runBuildScript(out, dir, buildURL, data);
         }
         catch (IOException e)
         {
            main.warning(passTools.getMessageWithDefault(
              "warning.build_script_failed",
              "Something went wrong trying to run build script ''{0}'': {1} {2}",
              buildURL, e.getClass().getSimpleName(), e.getMessage()));
         }
      }
      else if (data.isCompileTestOn() && mainFilePanel != null)
      {
         out.println("\\clearpage");
         out.format("\\section{%s}%n", 
           passTools.getMessageWithDefault("document.application", "Application"));

         exitCode = testApplication(out, dir, fileFields, mainFilePanel, data);

         switch (exitCode)
         {
            case 0: break;
            case EXIT_CANCELLED: break;
            case EXIT_TIMEDOUT: break;
            default:
              if (noPdf)
              {
                 main.warning(passTools.getMessageWithDefault(
                   "warning.application_test_failed.no_pdf",
                   "Something went wrong while testing the application."));
              }
              else
              {
                 main.warning(passTools.getMessageWithDefault(
                   "warning.application_test_failed",
                   "Something went wrong while testing the application. Please check the PDF file."));
              }
         }
      }
      else if (!data.isCompileTestOn())
      {
         main.verbose(passTools.getMessageWithDefault("message.compile_off",
            "Compile setting off."));
      }
      else
      {
         main.warning(passTools.getMessageWithDefault(
          "warning.no_main_file",
          "Can''t compile: no main file."));
      }

      return exitCode;
   }

   /**
    * Tests the student's application.
    * @param writer the print writer
    * @param dir the directory containing the source code
    * @param fileFields the student's files
    * @param mainFilePanel the main file
    * @param data the assignment data
    * @return process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    */
   private int testApplication(PrintWriter writer, File dir,
     Vector<PassFile> fileFields, RequiredPassFile mainFilePanel,
     AssignmentData data)
     throws IOException,InterruptedException,URISyntaxException
   {
      String language = mainFilePanel.getLanguage();

      int exitCode = EXIT_UNSET;

      if (language == null)
      {
         String msg = getPassTools().getMessageWithDefault(
           "document.no_autobuild_support",
           "Unable to automatically compile and test language `{0}''.",
           mainFilePanel.getFile().getName());

         main.debug(msg);
         writer.println(msg);

         return 0;
      }
      else if (language.equals("Java"))
      {
         File file = mainFilePanel.getFile();

         String mainClass = file.getName();

         int idx = mainClass.lastIndexOf(".");

         if (idx > -1)
         {
            mainClass = mainClass.substring(0, idx);
         }

         exitCode = testJavaApplication(writer, dir, fileFields, mainClass, 
            data);
      }
      else if (language.equals("C++"))
      {
         exitCode = testCppApplication(writer, dir, fileFields, 
            mainFilePanel.getFile().getName(), data);
      }
      else if (language.equals("C"))
      {
         exitCode = testCApplication(writer, dir, fileFields, 
            mainFilePanel.getFile().getName(), data);
      }
      else if (language.equals("Perl"))
      {
         exitCode = testPerlScript(writer, dir, 
            mainFilePanel.getFile().getName());
      }
      else if (language.equals("Lua"))
      {
         exitCode = testLuaScript(writer, dir, 
            mainFilePanel.getFile().getName());
      }
      else if (language.equals("bash"))
      {
         exitCode = testBashScript(writer, dir, 
            mainFilePanel.getFile().getName());
      }
      else
      {
         String msg = getPassTools().getMessageWithDefault(
           "document.no_autobuild_support",
           "Unable to automatically compile and test language `{0}''.",
           language);

         main.debug(msg);
         writer.println(msg);

         return 0;
      }

      return exitCode;
   }


   /**
    * Runs the build script supplied at the given URL.
    * @param writer the print writer
    * @param dir the directory containing the source code
    * @param buildURL location of the build script
    * @param data the assignment data
    * @return process exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    * @throws URISyntaxException if an invalid URI is encountered
    */
   private int runBuildScript(PrintWriter writer, File dir,
     URL buildURL, AssignmentData data)
     throws IOException,InterruptedException,URISyntaxException
   {
      Path buildScript = copyResource(buildURL, dir, true, writer);

      copyResourceFiles(dir);

      File buildFile = buildScript.toFile();

      PassTools passTools = main.getPassTools();

      String filename = buildFile.getName();

      String ext = null;

      int idx = filename.lastIndexOf(".");

      Vector<String> invokerArgs = new Vector<String>();

      if (idx > 0)
      {
         ext = filename.substring(idx);

         if (ext.equals("bat") || ext.equals("com"))
         {
            invokerArgs.add(filename);
         }
         else if (ext.equals("lua"))
         {
            invokerArgs.add(passTools.findApplication("lua", "texlua").getAbsolutePath());
            invokerArgs.add(filename);
         }
         else if (ext.equals("pl"))
         {
            invokerArgs.add(passTools.findApplication("perl").getAbsolutePath());
            invokerArgs.add(filename);
         }
         else if (ext.equals("sh") || ext.equals("php"))
         {
            invokerArgs.add(passTools.findApplication(ext).getAbsolutePath());
            invokerArgs.add(filename);
         }
         else if (ext.equals("py"))
         {
            invokerArgs.add(passTools.findApplication("python").getAbsolutePath());
            invokerArgs.add(filename);
         }
         else if (ext.equals("mk") || ext.equals("make"))
         {
            invokerArgs.add(passTools.findApplication("make").getAbsolutePath());
            invokerArgs.add("-f");
            invokerArgs.add(filename);
         }
      }

      if (invokerArgs.isEmpty() && filename.toLowerCase().equals("makefile"))
      {
         invokerArgs.add(passTools.findApplication("make").getAbsolutePath());
         invokerArgs.add("-f");
         invokerArgs.add(filename);
      }

      // Check if the build script start with #!

      if (invokerArgs.isEmpty())
      {
         BufferedReader in = null;

         try
         {
            in = newBufferedReader(buildFile);

            String line = in.readLine();

            if (line != null && line.startsWith("#!"))
            {
               String[] split = line.substring(2).split(" +");

               for (int i = 0; i < split.length; i++)
               {
                  invokerArgs.add(split[i]);
               }

               invokerArgs.add(filename);
            }
         }
         catch (IOException e)
         {
            main.warning(String.format("%s: %s", filename, e.getMessage()));
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }
         }
      }

      if (invokerArgs.isEmpty())
      {
         try
         {
            if (!buildFile.setExecutable(true))
            {
               main.warning(getPassTools().getMessageWithDefault(
                 "warning.cant_chmod_exe",
                 "Unable to make ''{0}'' executable",
                 buildFile));
            }
         }
         catch (SecurityException e)
         {
            main.warning(getPassTools().getMessageWithDefault(
             "warning.cant_chmod_exe.with_message",
             "Unable to make ''{0}'' executable: {1}",
             buildFile, e.getMessage()));
         }

         invokerArgs.add(filename);
      }

      int exitCode = runApplication(writer, dir, invokerArgs);

      findResultFiles(writer, dir);

      return exitCode;
   }

   /**
    * Fetches and copies all the assignment resource files and puts
    * copies in the given directory.
    * @param dir the directory in which to put the files
    * @throws IOException if I/O error occurs
    */ 
   private void copyResourceFiles(File dir) 
     throws IOException
   {
      AssignmentData data = main.getAssignment();

      int n = data.resourceFileCount();

      for (int i = 0; i < n; i++)
      {
         ResourceFile rf = data.getResourceFile(i);
         URI uri = rf.getUri();
         URL file = uri.toURL();

         copyResource(file, dir);
      }
   }

   /**
    * Fetches a file from the given URL and puts
    * a copy in the given directory. The URL must be the actual location of the file,
    * not a redirect.
    * @param url the URL of the file
    * @param dir the directory in which to put the files
    * @return the path to the copied file
    * @throws IOException if I/O error occurs
    */ 
   private Path copyResource(URL url, File dir)
     throws IOException
   {
      return copyResource(url, dir, false, null);
   }

   /**
    * Fetches a file from the given URL, puts
    * a copy in the given directory and writes the corresponding
    * LaTeX code. The filename will be scrubbed of problematic
    * characters. The URL must be the actual location of the file,
    * not a redirect.
    * @param url the URL of the file
    * @param dir the directory in which to put the files
    * @param extended if true use the extended forbidden pattern
    * FORBIDDEN_PATTERN_EXTENDED otherwise use the forbidden pattern
    * FORBIDDEN_PATTERN
    * @param writer the print writer for the LaTeX code
    * @return the path to the copied file
    * @throws IOException if I/O error occurs
    */ 
   private Path copyResource(URL url, File dir, boolean extended, PrintWriter writer)
     throws IOException
   {
      String name = url.getPath();

      int idx = name.lastIndexOf("/");

      if (idx > 0)
      {
         name = name.substring(idx+1);
      }

      String scrubbedName = getScrubbedFileName(dir, name, extended);

      if (!scrubbedName.equals(name))
      {
         main.warning(getPassTools().getMessageWithDefault(
           "warning.filename_scrubbed",
           "Filename scrubbed (one or more forbidden characters found). Original name: {0}",
            name));

         if (writer != null)
         {
            writer.println(getPassTools().getMessageWithDefault(
             "document.filename_scrubbed",
             "Filename scrubbed (one or more forbidden characters found). Original name:"));

            createAndWriteVerbatim(writer, name);

            writer.println(getPassTools().getMessageWithDefault(
             "document.new_name",
             "New name: {0}", 
             String.format("\\texttt{%s}", scrubbedName)));
         }

         name = scrubbedName;
      }

      Path result = (new File(dir, name)).toPath();

      if (!alwaysFetchResources)
      {
         if (Files.exists(result))
         {
            return result;
         }
      }

      int status = main.getPassTools().testHttpURLConnection(url);

      if (status > 299)
      {
         throw new InputResourceException(
           getPassTools().getMessageWithDefault(
           "error.http_status",
           "Unable to access ''{0}''. Status code: {1,number,integer}.",
            url, status));
      }

      InputStream in = null;

      try
      {
         in = url.openStream();

         main.debug("Fetching file "+url);

         Files.copy(in, result, StandardCopyOption.REPLACE_EXISTING);
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      return result;
   }

   /**
    * Adds all the compiler arguments required by the assignment
    * specification.
    * @param data the assignment specification data
    * @param args the list to which the arguments should be added
    */ 
   private void addAssignmentComplierArgs(AssignmentData data,
      Vector<String> args)
   {
      args.addAll(data.getCompilerArgs());
   }

   /**
    * Tests the student's application for a Java project.
    * This method first compiles the code and then, if the
    * compilation step was successful,
    * runs the application.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param fileFields the list of source code files that make up
    * the project
    * @param mainClass the name of the class that contains the
    * "main" method
    * @param data the assignment specifications
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    * @throws URISyntaxException if an invalid URI is encountered
    */ 
   private int testJavaApplication(PrintWriter writer,
     File dir, Vector<PassFile> fileFields, String mainClass,
     AssignmentData data)
     throws IOException,InterruptedException,URISyntaxException
   {
      File classes = newTemporaryFile(dir, jobname+"classes");
      classes.mkdir();

      Vector<String> args = new Vector<String>();

      PassTools passTools = main.getPassTools();

      args.add(passTools.getJavaCompilerInvoker().getName());
      passTools.addJavaCompilerArgs(args);
      addAssignmentComplierArgs(data, args);
      args.add("-d");
      args.add(classes.getName());

      for (PassFile panel : fileFields)
      {
         String lang = panel.getLanguage();

         if ("Java".equals(lang))
         {
            addFileArg(args, panel.getFile());
         }
         else
         {
            // Copy any plain text files or allowed binary files in to
            // classes directory.

            if (AssignmentData.PLAIN_TEXT.equals(lang)
              || AssignmentData.BINARY.equals(lang))
            {
               File srcFile = panel.getFile();
               Path srcPath = srcFile.toPath();

               Path destPath = getDestination(srcPath,
                srcFile.getName(), classes, null);

               main.debug("Copying "+srcPath+" -> "+destPath);

               Files.copy(srcPath, destPath);
            }
         }
      }

      int exitCode = runCompiler(writer, dir, classes, args);

      if (exitCode == 0)
      {
         if (runTest)
         {
            exitCode = runJavaApplication(writer, classes, mainClass);
         }
         else
         {
            main.verbose(getPassTools().getMessageWithDefault(
              "message.run_off",
              "Run application setting is off."));
         }

         findResultFiles(writer, dir, classes.getName());
      }

      return exitCode;
   }

   /**
    * Adds a file name to the list of arguments.
    * This will just add the file name if there's no base path set,
    * otherwise it will try to use a path that's relative to the base path.
    * (Absolute paths can be undesirable.)
    * @param args the list of command line arguments to which the
    * file should be added
    * @param file the file to add
    */ 
   private void addFileArg(Vector<String> args, File file)
   {
      if (basePath == null)
      {
         String name = file.getName();
         args.add(name);
      }
      else
      {
         try
         {
            Path p = file.toPath();
            Path relPath = basePath.relativize(p);
            args.add(relPath.toString());
         }
         catch (IllegalArgumentException e)
         {
            main.error(e);
         }
      }
   }

   /**
    * Tests the student's application for a Java project.
    * This method first runs the application (called if the code
    * was successfully compiled).
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param mainClass the name of the class that contains the
    * "main" method
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    */ 
   private int runJavaApplication(PrintWriter writer, File dir, 
      String mainClass)
   throws IOException,InterruptedException
   {
      PassTools passTools = main.getPassTools();

      // Find the main class

      File clsFile = passTools.findFile(dir, mainClass+".class");

      if (clsFile != null)
      {
         Path relPath = dir.toPath().relativize(clsFile.toPath());

         StringBuilder builder = new StringBuilder();

         for (int i = 0, n = relPath.getNameCount()-1; i < n; i++)
         {
            builder.append(relPath.getName(i)+".");
         }

         mainClass = builder.toString()+mainClass;
      }

      String invoker = passTools.getJavaInvoker().getName();

      
      Vector<String> argList = new Vector<String>();
      argList.add(invoker);
      argList.addAll(main.getAssignment().getInvokerArgs());
      argList.add(mainClass);

      return runApplication(writer, dir, argList);
   }

   /**
    * Adds the arguments required by a process that needs to run
    * make.
    * @param args the list of command line arguments
    * @param makefile the Makefile
    */ 
   private void getMakeFileArgs(Vector<String> args, File makefile)
   {
      args.add("make");

      String name = makefile.getName();

      if (!name.equals("Makefile"))
      {
         if (makefile.isDirectory())
         {
            args.add("-C");
         }
         else
         {
            args.add("-f");
         }

         addFileArg(args, makefile);
      }
   }

   /**
    * Tests the student's application for a C++ project.
    * This method first compiles the code and then, if the
    * compilation step was successful,
    * runs the application.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param fileFields the list of source code files that make up
    * the project
    * @param mainFile the main file
    * @param data the assignment specifications
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    * @throws URISyntaxException if an invalid URI is encountered
    */ 
   private int testCppApplication(PrintWriter writer,
     File dir, Vector<PassFile> fileFields, String mainFile,
     AssignmentData data)
     throws IOException,InterruptedException,URISyntaxException
   {
      Vector<String> args = new Vector<String>();

      PassTools passTools = main.getPassTools();

      String outputName = passTools.getCppOutputName();

      args.add(passTools.getCppCompilerInvoker().getName());
      passTools.addCppCompilerArgs(args);
      addAssignmentComplierArgs(data, args);

      for (PassFile panel : fileFields)
      {
         if ("C++".equals(panel.getLanguage()))
         {
            File file = panel.getFile();
            String name = file.getName();

            // omit header files
            if (!name.endsWith(".h") && !name.endsWith(".hh")
              && !name.endsWith(".hpp") && !name.endsWith(".H"))
            {
               addFileArg(args, file);
            }
         }
         else if ("make".equals(panel.getLanguage()))
         {
            args.clear();
            getMakeFileArgs(args, panel.getFile());
            outputName = "a.out";
            break;
         }
      }

      int exitCode = runCompiler(writer, dir, args);

      if (exitCode == 0)
      {
         exitCode = runOutput(writer, dir, outputName);
      }

      return exitCode;
   }

   /**
    * Tests the student's application for a C or C++ project.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param outputName the name of the executable file
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    */ 
   private int runOutput(PrintWriter writer, File dir, String outputName)
   throws IOException,InterruptedException
   {
      int exitCode = 1;

      if (runTest)
      {
         File file = new File(dir, outputName);

         boolean aout = false;

         if (!file.exists() && outputName.equals("a.out"))
         {
            aout = true;
            file = new File(dir, "a.exe");
         }

         if (file.exists())
         {
            exitCode = runApplication(writer, dir, file.getAbsolutePath());
         }
         else
         {
            writer.println(getPassTools().getMessageWithDefault(
              "error.no_exe", "Executable doesn't exist:"));

            createAndWriteVerbatim(writer, file.getAbsolutePath());

            exitCode = 1;

            if (aout)
            {
               main.error(getPassTools().getMessageWithDefault(
               "error.aout_not_made",
               "Something''s gone wrong.\nThe compiler didn''t create the executable file.\nTried both ''a.out'' and ''a.exe'' in directory:\n{0}", dir));
            }
            else
            {
               main.error(getPassTools().getMessageWithDefault(
                  "error.exe_not_made",
                  "Something''s gone wrong.\nThe compiler didn''t create the executable file:\n{0}", file));
            }
         }
      }
      else
      {
         main.verbose(getPassTools().getMessageWithDefault(
           "message.run_off", "Run application setting is off."));
      }

      findResultFiles(writer, dir);

      return exitCode;
   }

   /**
    * Tests the student's application for a C project.
    * This method first compiles the code and then, if the
    * compilation step was successful,
    * runs the application.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param fileFields the list of source code files that make up
    * the project
    * @param mainFile the main file
    * @param data the assignment specifications
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    * @throws URISyntaxException if an invalid URI is encountered
    */ 
   private int testCApplication(PrintWriter writer,
     File dir, Vector<PassFile> fileFields, String mainFile,
     AssignmentData data)
     throws IOException,InterruptedException,URISyntaxException
   {
      Vector<String> args = new Vector<String>();

      PassTools passTools = main.getPassTools();

      String outputName = passTools.getCOutputName();
      args.add(passTools.getCCompilerInvoker().getName());
      passTools.addCCompilerArgs(args);
      addAssignmentComplierArgs(data, args);

      for (PassFile panel : fileFields)
      {
         if ("C".equals(panel.getLanguage()))
         {
            File file = panel.getFile();
            String name = file.getName();

            // omit header files
            if (!name.endsWith(".h") && !name.endsWith(".H"))
            {
               addFileArg(args, file);
            }
         }
         else if ("make".equals(panel.getLanguage()))
         {
            args.clear();
            getMakeFileArgs(args, panel.getFile());
            outputName = "a.out";
            break;
         }
      }

      int exitCode = runCompiler(writer, dir, args);

      if (exitCode == 0)
      {
         exitCode = runOutput(writer, dir, outputName);
      }

      return exitCode;
   }

   /**
    * Tests the student's application for a Perl project.
    * There's no compiler in this case, so this method just
    * runs the Perl script.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param mainFile the main file
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    */ 
   private int testPerlScript(PrintWriter writer, File dir, String mainFile)
     throws IOException,InterruptedException
   {
      Vector<String> args = new Vector<String>();

      PassTools passTools = main.getPassTools();

      args.add(passTools.getPerlInvoker().getName());

      passTools.addPerlArgs(args);

      args.addAll(main.getAssignment().getInvokerArgs());

      File file = new File(mainFile);

      addFileArg(args, file);

      int exitCode = EXIT_UNSET;

      if (runTest)
      {
         exitCode = runApplication(writer, dir, args);
      }
      else
      {
         main.verbose(passTools.getMessageWithDefault(
           "message.run_off",
           "Run application setting is off."));
      }

      findResultFiles(writer, dir);

      return exitCode;
   }

   /**
    * Tests the student's application for a Lua project.
    * There's no compiler in this case, so this method just
    * runs the Lua script.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param mainFile the main file
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    */ 
   private int testLuaScript(PrintWriter writer, File dir, String mainFile)
     throws IOException,InterruptedException
   {
      Vector<String> args = new Vector<String>();

      args.add(main.getPassTools().getLuaInvoker().getName());

      args.addAll(main.getAssignment().getInvokerArgs());

      File file = new File(mainFile);

      addFileArg(args, file);

      int exitCode = EXIT_UNSET;

      if (runTest)
      {
         exitCode = runApplication(writer, dir, args);
      }
      else
      {
         main.verbose(getPassTools().getMessageWithDefault(
           "message.run_off",
           "Run application setting is off."));
      }

      findResultFiles(writer, dir);

      return exitCode;
   }

   /**
    * Tests the student's application for a Bash project.
    * There's no compiler in this case, so this method just
    * runs the Bash script.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param mainFile the main file
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    */ 
   private int testBashScript(PrintWriter writer, File dir, String mainFile)
     throws IOException,InterruptedException
   {
      PassTools passTools = main.getPassTools();

      File file = new File(dir, mainFile);

      Path path = file.toPath();

      Vector<String> args = new Vector<String>();
      Vector<String> invokerArgs = main.getAssignment().getInvokerArgs();

      if (!Files.isExecutable(path) || !invokerArgs.isEmpty())
      {
         args.add(passTools.getBashInvoker().getName());

         args.addAll(invokerArgs);
      }

      addFileArg(args, file);

      int exitCode = EXIT_UNSET;

      if (runTest)
      {
         exitCode = runApplication(writer, dir, args);
      }
      else
      {
         main.verbose(passTools.getMessageWithDefault(
           "message.run_off",
           "Run application setting is off."));
      }

      findResultFiles(writer, dir);

      return exitCode;
   }

   /**
    * Runs the compiler.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param args the process arguments, which should include
    * the compiler invocation at the start
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    * @throws URISyntaxException if an invalid URI is encountered
    */ 
   private int runCompiler(PrintWriter writer, File dir, Vector<String> args)
     throws IOException,InterruptedException,URISyntaxException
   {
      return runCompiler(writer, dir, dir, args);
   }

   /**
    * Runs the compiler.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param resourcesDir the directory the resource files should be
    * copied to
    * @param args the process arguments, which should include
    * the compiler invocation at the start
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    * @throws URISyntaxException if an invalid URI is encountered
    */ 
   private int runCompiler(PrintWriter writer, File dir, File resourcesDir,
       Vector<String> args)
     throws IOException,InterruptedException,URISyntaxException
   {
      copyResourceFiles(resourcesDir);

      BufferedReader in = null;

      int exitCode = EXIT_UNSET;

      try
      {
         writer.format("\\subsection{%s}%n",
          getPassTools().getMessageWithDefault(
            "document.compiler_invocation", "Compiler Invocation"));

         writer.println(String.format(
          "\\begin{lstlisting}[numbers=none,language={%s}]",
          File.separatorChar == '\\' ? "command.com" : "bash"));

         Pattern pattern = Pattern.compile(".*\\s.*", Pattern.DOTALL);

         for (String arg : args)
         {
            if (pattern.matcher(arg).matches())
            {
               writer.print(String.format("'%s' ", 
                 arg.replaceAll("'", "\\\\'")));
            }
            else
            {
               writer.print(String.format("%s ", arg));
            }
         }

         writer.println();

         writer.println("\\end{lstlisting}");

         ProcessBuilder builder = new ProcessBuilder(args);
         builder.directory(dir);

         main.debugNoLn("Running ");

         for (int i = 0, n = args.size(); i < n; i++)
         {
            String arg = args.get(i);

            if (arg.contains(" "))
            {
               main.debugNoLn(String.format("'%s' ", arg));
            }
            else
            {
               main.debugNoLn(String.format("%s ", arg));
            }
         }

         main.debugNoLn(String.format("%nin directory %s%n", dir));

         compilerLog = createTemporaryFile("PASS_compiler-messages-", ".txt", dir);

         String interrupted = null;

         try
         {
            exitCode = runProcess(timeout*1000L, builder, compilerLog);
         }
         catch (java.nio.channels.InterruptedByTimeoutException e)
         {
            interrupted = getPassTools().getMessage("error.process_timedout", timeout);
            exitCode = EXIT_TIMEDOUT;
         }
         catch (java.util.concurrent.CancellationException e)
         {
            interrupted = getPassTools().getMessage("error.process_cancelled");
            exitCode = EXIT_CANCELLED;
         }

         if (interrupted != null)
         {
            writer.format("\\par\\warning{%s}%n", interrupted);
            main.warning(interrupted);
         }

         StringBuilder inBuilder = new StringBuilder();

         long msgLength = compilerLog.length();
         String truncMsg = null;

         if (msgLength > maxOutput)
         {
            truncMsg = getPassTools().getMessageWithDefault(
              "document.compiler_output_truncated",
              "Compiler output size ({0,number} bytes) exceeds maximum setting ({1,number} bytes). Truncating with [...]",
              msgLength, maxOutput);
            main.debug(truncMsg);
         }

         in = null;

         try
         {
            in = newBufferedReader(compilerLog);

            String line;

            while ((line = in.readLine()) != null)
            {
               if (isLua)
               {
                  if (truncMsg != null && inBuilder.length()+line.length() > maxOutput)
                  {
                     String substr = line.substring(0, 
                        (int)(inBuilder.length()+line.length()-maxOutput));

                     inBuilder.append(substr);
                     inBuilder.append(String.format("[...]%n"));
                     main.debug(substr+"[...]");
                     break;
                  }

                  inBuilder.append(String.format("%s%n", line));
               }
               else
               {
                  int n = line.length();
                  boolean truncate = false;

                  if (truncMsg != null && inBuilder.length()+n > maxOutput)
                  {
                     n = (int)(inBuilder.length() + n - maxOutput);
                     truncate = true;
                  }
   
                  for (int i = 0; i < n; )
                  {
                     int cp = line.codePointAt(i);
                     i += Character.charCount(cp);

                     if (cp == 0x2018)
                     {
                        inBuilder.appendCodePoint('`');
                     }
                     else if (cp == 0x2019)
                     {
                        inBuilder.appendCodePoint('\'');
                     }
                     else
                     {
                        inBuilder.appendCodePoint(cp);
                     }
                  }

                  if (truncate)
                  {
                     inBuilder.append(String.format("[...]%n"));
                     break;
                  }

                  inBuilder.append(String.format("%n"));
               }

               main.debug(line);
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
               in = null;
            }
         }

         writer.format("\\subsection{%s}%n",
           getPassTools().getMessageWithDefault(
              "document.compiler_messages", "Compiler Messages"));

         if (inBuilder.length() == 0)
         {
            writer.println(getPassTools().getMessageWithDefault(
               "document.none", "None."));
         }
         else
         {
            if (truncMsg != null)
            {
               writer.format("\\warning{%s}%n", truncMsg);
            }

            createAndWriteVerbatim(writer, inBuilder);
         }

         if (exitCode != 0)
         {
            String msg = getPassTools().getMessageWithDefault(
              "document.compiler_exitcode", "Compiler returned exit code {0,number}.", 
              exitCode);

            main.debug(msg);
            writer.println(msg);
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      return exitCode;
   }

   /**
    * Runs the application.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param args the process arguments, which should include
    * the application invocation at the start
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    */ 
   private int runApplication(PrintWriter writer, File dir, 
     String... args)
   throws IOException,InterruptedException
   {
      Vector<String> argList = new Vector<String>();

      for (String arg : args)
      {
         argList.add(arg);
      }

      return runApplication(writer, dir, argList);
   }

   /**
    * Runs the application.
    * @param writer the writer for the LaTeX code
    * @param dir the directory the source code files are in
    * @param argList the process arguments, which should include
    * the application invocation at the start
    * @return the exit code
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if an interruption occurs
    */ 
   private int runApplication(PrintWriter writer, File dir, 
     Vector<String> argList)
   throws IOException,InterruptedException
   {
      AssignmentData data = main.getAssignment();

      Vector<String> inputList = data.getInputs();

      argList.addAll(data.getArgs());

      writer.format("\\subsection{%s}%n", 
        getPassTools().getMessageWithDefault("document.application_invocation", 
           "Application Invocation"));

      StringBuilder strBuilder = new StringBuilder();

      Pattern pattern = Pattern.compile(".*\\s.*", Pattern.DOTALL);

      for (int i = 0, n = argList.size(); i < n; i++)
      {
         String arg = argList.get(i);

         if (!arg.startsWith("-"))
         {
            File file = new File(arg);

            if (file.exists() && file.isAbsolute())
            {
               arg = file.getName();
            }
         }

         if (pattern.matcher(arg).matches())
         {
            strBuilder.append(String.format("'%s' ", 
              arg.replaceAll("'", "\\\\'")));
         }
         else
         {
            strBuilder.append(String.format("%s ", arg));
         }
      }

      createAndWriteVerbatim(writer, strBuilder);

      ProcessBuilder builder = new ProcessBuilder(argList);
      builder.directory(dir);

      main.debugNoLn("Running ");

      for (int i = 0, n = argList.size(); i < n; i++)
      {
         String arg = argList.get(i);

         if (pattern.matcher(arg).matches())
         {
            main.debugNoLn(String.format("'%s' ", argList.get(i)));
         }
         else
         {
            main.debugNoLn(String.format("%s ", argList.get(i)));
         }
      }

      main.debugNoLn(String.format("%nin directory %s%n", dir));

      inFile = createTemporaryFile("PASS_process-in-", ".txt", texDir);
      outFile = createTemporaryFile("PASS_process-out-", ".txt", texDir);
      errFile = createTemporaryFile("PASS_process-err-", ".txt", texDir);

      main.debug("PASS in file: "+inFile);
      main.debug("PASS out file: "+outFile);
      main.debug("PASS error file: "+errFile);

      PrintWriter out = null;

      try
      {
         out = new PrintWriter(inFile);

         for (String input : inputList)
         {
            out.println(input);
         }
      }
      finally
      {
         if (out != null)
         {
            out.close();
            out=null;
         }
      }

      int exitCode = EXIT_UNSET;
      String interrupted = null;

      try
      {
         exitCode = runProcess(timeout*1000L, builder, outFile,
           errFile, inFile);
      }
      catch (java.nio.channels.InterruptedByTimeoutException e)
      {
         interrupted = getPassTools().getMessage("error.process_timedout", timeout);
         exitCode = EXIT_TIMEDOUT;
      }
      catch (java.util.concurrent.CancellationException e)
      {
         interrupted = getPassTools().getMessage("error.process_cancelled");
         exitCode = EXIT_CANCELLED;
      }

      if (interrupted != null)
      {
         writer.format("\\par\\warning{%s}%n", interrupted);
         main.warning(interrupted);
      }

      long msgLength = outFile.length();

      String truncMsg = null;

      if (msgLength > maxOutput)
      {
         truncMsg = getPassTools().getMessageWithDefault(
           "document.output_truncated",
           "Output size ({0,number} bytes) exceeds maximum setting ({1,number} bytes). Truncating with [...]",
           msgLength, maxOutput);
      }

      String line;
      StringBuilder inBuilder = new StringBuilder();

      BufferedReader in = null;

      try
      {
         in = newBufferedReader(outFile);

         while ((line = in.readLine()) != null)
         {
            if (truncMsg != null && inBuilder.length()+line.length() > maxOutput)
            {
               String substr = line.substring(0, 
                 (int)(inBuilder.length()+line.length()-maxOutput));

               inBuilder.append(substr);
               inBuilder.append(String.format("[...]%n"));
               main.debug(substr+"[...]");
               break;
            }

            inBuilder.append(String.format("%s%n", line));
            main.debug(line);
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
            in = null;
         }
      }

      StringBuilder errBuilder = new StringBuilder();

      try
      {
         in = newBufferedReader(errFile);

         while ((line = in.readLine()) != null)
         {
            errBuilder.append(String.format("%s%n", line));
            main.debug(line);
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
            in = null;
         }
      }

      writer.format("\\subsection{%s}%n", 
        getPassTools().getMessageWithDefault(
           "document.stdout_messages",
           "Messages to STDOUT"));

      if (inBuilder.length() == 0)
      {
         writer.println(getPassTools().getMessageWithDefault(
           "document.none", "None."));
      }
      else
      {
         if (truncMsg != null)
         {
            writer.format("\\warning{%s}%n", truncMsg);
            main.warning(truncMsg);
         }

         createAndWriteVerbatim(writer, inBuilder);
      }

      writer.format("\\subsection{%s}%n", 
       getPassTools().getMessageWithDefault(
         "document.stderr_messages", 
         "Messages to STDERR"));

      if (errBuilder.length() == 0)
      {
         writer.println(getPassTools().getMessageWithDefault(
            "document.none", "None."));
      }
      else
      {
         createAndWriteVerbatim(writer, errBuilder);
      }

      return exitCode;
   }

   /**
    * Gets the file used for STDIN. A process that needs to read
    * from STDIN will read from this file.
    * @return the file used for STDIN
    */ 
   public File getStdinFile()
   {
      return inFile;
   }

   /**
    * Gets the file used for STDOUT. A process that needs to write
    * to STDOUT will write to this file.
    * @return the file used for STDOUT
    */ 
   public File getStdoutFile()
   {
      return outFile;
   }

   /**
    * Gets the file used for STDERR. A process that needs to write
    * to STDERR will write to this file.
    * @return the file used for STDERR
    */ 
   public File getStderrFile()
   {
      return errFile;
   }

   /**
    * Gets the file used to save compiler messages.
    * @return the file containing compiler messages
    */ 
   public File getCompilerMessagesFile()
   {
      return compilerLog;
   }

   /**
    * Gets the directory containing the result files.
    * @return the results directory
    */ 
   public File getResultsDir()
   {
      return resultsDir;
   }

   /**
    * Gets the PDF file. This may or may not exist, depending on
    * whether the process was successful.
    * @return the PDF file
    */ 
   public File getPdfFile()
   {
      return pdfFile;
   }

   /**
    * Copies all the results files, if found, to the given directory.
    * @param destPath the destination directory
    * @param options copy options
    * @throws IOException if I/O error occurred while copying
    * @throws InvalidPathException if destination is invalid
    */ 
   public void copyResultFiles(Path destPath, CopyOption... options) 
     throws IOException,InvalidPathException
   {
      Vector<ResultFile> files = main.getAssignment().getResultFiles();

      if (files.isEmpty() || resultsDir == null) return;

      Path sourcePath = resultsDir.toPath();

      main.debug(String.format(
      "Copying result files from source path '%s' to destination path '%s'", 
        sourcePath, destPath));

      for (ResultFile result : files)
      {
         String filename = result.getName();

         Path src = sourcePath.resolve(filename);
         Path target = destPath.resolve(filename);

         if (!src.equals(target))
         {
            main.debug(String.format("Copying %s -> %s", src, target));
            Files.copy(src, target, options);
         }
      }
   }

   /**
    * Searches for the result files. If the student's application
    * needs to create one or more files, these files need to be
    * located and added to the PDF.
    * @param writer the write for the LaTeX document
    * @param dir the directory to search
    */
   public void findResultFiles(PrintWriter writer, File dir)
     throws IOException,InvalidPathException
   {
      findResultFiles(writer, dir, ".");
   }

   /**
    * Searches for the result files. If the student's application
    * needs to create one or more files, these files need to be
    * located and added to the PDF.
    * @param writer the write for the LaTeX document
    * @param dir the directory to search
    * @param resultsDirName the path name should be relative to this
    * directory
    */
   public void findResultFiles(PrintWriter writer, File dir, String resultsDirName)
     throws IOException,InvalidPathException
   {
      Vector<ResultFile> files = main.getAssignment().getResultFiles();

      if (files.isEmpty()) return;

      resultsDir = dir;

      if (!resultsDirName.equals("."))
      {
         Path sourcePath = dir.toPath().resolve(resultsDirName);
         resultsDir = sourcePath.toFile();
      }

      writer.format("\\section{%s}%n", 
       getPassTools().getMessageWithDefault("document.result_files", "Result Files"));

      for (ResultFile result : files)
      {
         String filename = result.getName();

         File file = new File(resultsDir, filename);

         writer.println(String.format("\\subsection{\\file{%s}}", filename));

         if (file.exists())
         {
            String mimetype = result.getMimeType();

            writer.println("\\attachfile");
            writer.println(String.format("[mimetype=%s,", mimetype));
            writer.println(String.format("description={%s},",
              getPassTools().getMessageWithDefault(
                "document.result_file", "Result file {0}", filename)));
            writer.println(String.format("author={%s},", blackboardId));
            writer.println(String.format("size={%d}", file.length()));
            writer.println(String.format("]{%s/%s}", resultsDirName, filename));

            writer.println();

            if (!result.showListing())
            {
               // skip listing
            }
            else if (mimetype.startsWith("text/"))
            {
               createAndWriteVerbatim(writer, file);
            }
            else if (mimetype.startsWith("image/"))
            {
               writer.println(String.format(
                  "\\includeimg{%s/%s}", resultsDirName, filename));
            }
         }
         else
         {
            writer.format("\\warning{%s}%n", getPassTools().getMessageWithDefault(
              "document.missing", "Missing"));
            main.warning(getPassTools().getMessageWithDefault(
              "warning.missing_result_file",
              "Your project application failed to create expected result file ''{0}''.",
              file.getName()));
         }
      }
   }

   /**
    * Creates a new file name for a verbatim file.
    * @return the new file name
    */ 
   protected String newVerbFileName()
   {
      return String.format("%s-verb%d.tex", jobname, ++verbCount);
   }

   /**
    * Creates a new verbatim file. This creates a temporary file to
    * store the provided content which needs to be included verbatim
    * in the LaTeX document with <code>\verbatiminput</code>.
    * This is designed for messages or text output files so it
    * shouldn't be pretty-printed but instead uses
    * writeVerbatim(PrintWriter,CharSequence)
    * to implement line wrapping, replace TABs and convert
    * problematic binary content.
    * @param writer the writer for the LaTeX source code
    * @param text the verbatim text
    * @throws IOException if I/O error occurs
    */ 
   protected void createAndWriteVerbatim(PrintWriter writer, CharSequence text)
     throws IOException
   {
      String verbFilename = newVerbFileName();
      File verbFile = newTemporaryFile(texDir, verbFilename);

      PrintWriter verbWriter = null;

      try
      {
         verbWriter = new PrintWriter(verbFile, main.getEncoding());
         writeVerbatim(verbWriter, text);
      }
      finally
      {
         if (verbWriter != null)
         {
            verbWriter.close();
         }
      }

      if (verbFile.exists())
      {
         writer.println("\\verbatiminput{"+verbFilename+"}");
      }
      else
      {
         writer.println(getPassTools().getMessageWithDefault(
           "document.verbatim_failed",
           "Failed to create verbatim file {0}", verbFilename));
         main.debug("Failed to create verbatim file "+verbFile);
      }
   }

   /**
    * Creates a new verbatim file. This creates a temporary file to
    * store the content of the given file which needs to be included verbatim
    * in the LaTeX document with <code>\verbatiminput</code>.
    * This is designed for messages or text output files so it
    * shouldn't be pretty-printed but instead uses
    * writeVerbatim(PrintWriter,File)
    * to implement line wrapping, replace TABs and convert
    * problematic binary content.
    * @param writer the writer for the LaTeX source code
    * @param src the file containing the verbatim content
    * @throws IOException if I/O error occurs
    */ 
   protected void createAndWriteVerbatim(PrintWriter writer, File src)
     throws IOException
   {
      String verbFilename = newVerbFileName();
      File verbFile = newTemporaryFile(texDir, verbFilename);

      PrintWriter verbWriter = null;

      try
      {
         verbWriter = new PrintWriter(verbFile, main.getEncoding());
         writeVerbatim(verbWriter, src);
      }
      finally
      {
         if (verbWriter != null)
         {
            verbWriter.close();
         }
      }

      if (verbFile.exists())
      {
         writer.println("\\verbatiminput{"+verbFilename+"}");
      }
      else
      {
         writer.println(getPassTools().getMessageWithDefault(
           "document.verbatim_failed",
           "Failed to create verbatim file {0}", verbFilename));

         main.debug("Failed to create verbatim file "+verbFile);
      }
   }

   /**
    * Creates a new verbatim file. This creates a temporary file to
    * store the content of the given reader which needs to be included verbatim
    * in the LaTeX document with <code>\verbatiminput</code>.
    * This is designed for messages or text output files so it
    * shouldn't be pretty-printed but instead uses
    * writeVerbatim(PrintWriter,Reader)
    * to implement line wrapping, replace TABs and convert
    * problematic binary content.
    * @param writer the writer for the LaTeX source code
    * @param src the input source containing the verbatim content
    * @throws IOException if I/O error occurs
    */ 
   protected void createAndWriteVerbatim(PrintWriter writer, Reader src)
     throws IOException
   {
      String verbFilename = newVerbFileName();
      File verbFile = newTemporaryFile(texDir, verbFilename);

      PrintWriter verbWriter = null;

      try
      {
         verbWriter = new PrintWriter(verbFile, main.getEncoding());
         writeVerbatim(verbWriter, src);
      }
      finally
      {
         if (verbWriter != null)
         {
            verbWriter.close();
         }
      }

      if (verbFile.exists())
      {
         writer.println("\\verbatiminput{"+verbFilename+"}");
      }
      else
      {
         writer.println(getPassTools().getMessageWithDefault(
           "document.verbatim_failed",
           "Failed to create verbatim file {0}", verbFilename));

         main.debug("Failed to create verbatim file "+verbFile);
      }
   }

   /**
    * Writes the given source as verbatim text, implementing line
    * wrapping, TAB substitution and binary/invalid character markup.
    * @param writer the writer
    * @param src the content that needs to be converted to verbatim
    * @throws IOException if I/O error occurs
    */ 
   public void writeVerbatim(PrintWriter writer, CharSequence str) throws IOException
   {
      if (str.length() == 0) return;

      writeVerbatim(writer, new StringReader(str.toString()));
   }

   /**
    * Writes the given source as verbatim text, implementing line
    * wrapping, TAB substitution and binary/invalid character markup.
    * @param writer the writer
    * @param file the file containing the content that needs to be converted to verbatim
    * @throws IOException if I/O error occurs
    */ 
   public void writeVerbatim(PrintWriter writer, File file) throws IOException
   {
      if (file.length() == 0) return;

      writeVerbatim(writer, main.getPassTools().newInputStreamReader(file));
   }

   /**
    * Writes the given source as verbatim text, implementing line
    * wrapping, TAB substitution and binary/invalid character markup.
    * @param writer the writer
    * @param reader the input source containing the content that needs to be converted to verbatim
    * @throws IOException if I/O error occurs
    */ 
   public void writeVerbatim(PrintWriter writer, Reader reader)
   throws IOException
   {
      BufferedReader in = null;
      PassTools passTools = main.getPassTools();

      int maxChars = passTools.getVerbMaxCharsPerLine();
      int tabCount = passTools.getVerbTabCharCount();

      try
      {
         in = new BufferedReader(reader);

         String line;

         while ((line = in.readLine()) != null)
         {
            for (int i = 0, j = 0, n = line.length(); i < n; )
            {
               int cp = line.codePointAt(i);
               i += Character.charCount(cp);

               j++;

               if (j > maxChars)
               {
                  writer.println();
                  j = 1;
               }

               if (cp == '\t')
               {
                  for (int k = (j-1)%tabCount; k < tabCount; k++)
                  {
                     writer.print(' ');
                     j++;

                     if (j > maxChars)
                     {
                        writer.println();
                        j = 1;
                     }
                  }

               }
               else if (Character.isISOControl(cp) 
                         && !(cp == 0x0A || cp == 0x0C || cp == 0x0D))
               {
                  String str = String.format("[0x%X]", cp);
                  writer.print(str);
                  j += str.length()-1;
                  main.warning(getPassTools().getMessageWithDefault(
                    "warning.control_char_found",
                    "Control character U+{0} detected", 
                    String.format("%X", cp)));
               }
               else if (isASCII && cp > 0x7F)
               {
                  main.warning(getPassTools().getMessageWithDefault(
                   "warning.non_ascii",
                   "ASCII mode set but non-ASCII character U+{0} detected",
                   String.format("%X", cp)));

                  if (cp == 0x2018 || cp == 0x2018) // left or single quote
                  {
                     writer.print('\'');
                  }
                  else if (cp == 0x201C || cp == 0x201D) // left or right double quote
                  {
                     writer.print('"');
                  }
                  else if (cp >= 0x200 && cp <= 2015) // hyphens or dashes
                  {
                     writer.print('-');
                  }
                  else
                  {
                     String str = String.format("[0x%X]", cp);
                     writer.print(str);
                     j += str.length()-1;
                  }
               }
               else if (cp <= Character.MAX_VALUE)
               {
                  writer.print((char)cp);
               }
               else
               {
                  writer.print(new String(Character.toChars(cp)));
               }
            }

            writer.println();
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
    * Counter to ensure unique filenames for the verbatim files.
    */ 
   private int verbCount=0;

   /**
    * The Pass application.
    */ 
   private Pass main;

   /**
    * The base path for the project files.
    */ 
   private Path basePath = null;

   /**
    * The LaTeX job name.
    */ 
   private String jobname;

   /**
    * The directory containing the LaTeX source.
    */ 
   private File texDir;

   /**
    * The directory containing the result files.
    */ 
   private File resultsDir;

   /**
    * The student's user name.
    */ 
   private String blackboardId;

   /**
    * The LaTeX invocation name. Defaults to "pdflatex" but will be
    * changed to "lualatex" if LuaLaTeX should be used.
    */ 
   private String latexPath = "pdflatex";

   /**
    * If true, indicates that LuaLaTeX should be used.
    */ 
   private boolean isLua = false;

   /**
    * If true, indicates that all supplied files have been
    * identified as ASCII.
    */ 
   private boolean isASCII=false;

   /**
    * List of all temporary files that need to be deleted on exit.
    */ 
   private Vector<File> temporaryFiles;

   private File inFile, outFile, errFile, compilerLog, pdfFile;

   private int maxProgress;
   private int currentProgress=0;

   private long timeout;

   private PassTask currentTask = null;

   /**
    * Maximum number of characters to write to the LaTeX document
    * for any block of output.
    */ 
   private long maxOutput=Long.MAX_VALUE;

   /**
    * If true, always fetch resource files, even if it means
    * overwriting existing files with the same name.
    * PassEditor sets this to false for trial runs.
    */ 
   private boolean alwaysFetchResources = true;

   private boolean runTest;

   private ProgressListener progressListener;

   /**
    * Maximum buffer size.
    */ 
   private static final int MAX_INPUT_BYTES=1024;

   /**
    * Format for PDF dates. This format is part of the PDF
    * specification.
    */ 
   private static final SimpleDateFormat PDF_DATE_FORMAT
     = new SimpleDateFormat("'D:'yyyyMMddHHmmss");

   /**
    * Pattern match for forbidden characters that need to be scrubbed.
    */ 
   private static final Pattern FORBIDDEN_PATTERN = 
     Pattern.compile("([^a-zA-Z0-9@+=\\-\\.])");

   /**
    * Stricter pattern match for forbidden characters that need to be scrubbed.
    */ 
   private static final Pattern FORBIDDEN_PATTERN_EXTENDED = 
     Pattern.compile("([^a-zA-Z0-9@+=\\-\\._~: ])");

   /**
    * LaTeX code to draw a box with a tick in it.
    */ 
   private static final String CHECK_BOX = 
     "{\\fontfamily {mvs}\\fontencoding {U}\\fontseries {m}\\fontshape {n}\\selectfont\\char 86}";

   /**
    * LaTeX code to draw an empty box without a tick in it.
    */ 
   private static final String EMPTY_CHECK_BOX = 
     "{\\fontfamily {mvs}\\fontencoding {U}\\fontseries {m}\\fontshape {n}\\selectfont\\char 79}";

   /**
    * Encryption keys and localisation settings.
    */ 
   private AssignmentProcessConfig config;

   /**
    * Abnormal exit code values. (That hopefully won't be produced
    * by any of the sub-processes.)
    */ 
   public static final int EXIT_CANCELLED=-1000, EXIT_TIMEDOUT=-2000, EXIT_UNSET=-3000;
}
