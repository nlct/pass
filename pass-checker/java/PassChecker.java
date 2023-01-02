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
package com.dickimawbooks.passchecker;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Vector;
import java.util.Date;
import java.util.Locale;
import java.util.Base64;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.security.InvalidKeyException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.cos.COSInputStream;

import com.dickimawbooks.passlib.*;

/**
 * Checks one or more PDF files for PASS encrypted metadata.
 */
public class PassChecker extends Vector<AssignmentMetaData>
   implements MessageSystem,Pass
{
   public PassChecker() throws IOException
   {
      super();
      passTools = new PassTools(this);
      passTools.loadDictionary("passchecker", Locale.getDefault());

      // I had plans to implement a GUI option in which case 
      // it would've had a text component for messages.
      messageSystem = this;
   }

   /**
    * Sets the tolerance for differences in timestamps.
    * Don't make this 0 as there will always be a small difference.
    * The tolerance should take into account the expected time taken
    * to compile and test the source code.
    * @param seconds the maximum time difference in seconds
    */ 
   public void setMaxTimeDiff(int seconds)
   {
      maxTimeDiff = seconds;
   }

   /**
    * Gets the tolerance for differences in timestamps.
    * @return the maximum time difference in seconds
    */
   public int getMaxTimeDiff()
   {
      return maxTimeDiff;
   }

   /**
    * Sets the output file name. If null, writes output to STDOUT.
    * @param name the output file name or null if not required
    */ 
   public void setOutFileName(String name)
   {
      outfilename = name;
   }

   /**
    * Parses submission data exported from Server PASS.
    * @param name TSV filename
    */ 
   public void parseSubmissions(String name)
   throws IOException
   {
      if (serverData == null)
      {
         serverData = ServerJobData.parse(this, new File(name));
      }
      else
      {
         ServerJobData.parse(serverData, this, new File(name));
      }
   }

   public ServerJobData getJobData(File pdfFile, String author, Date date)
   {
      if (serverData == null) return null;

      ServerJobData job = null;

      try
      {
         job = ServerJobData.getSubmission(serverData, this, pdfFile, author, date);

         if (job.getJobID() == -1)
         {
            warning(getMessage("warning.no_job", pdfFile.getName(), 
              author, ServerJobData.ISO_DATETIME_FORMAT.format(date),
              job.getCheckSum()));
         }
      }
      catch (Exception e)
      {
         error(e);
      }

      return job;
   }

   /**
    * Sets the debugging level
    */ 
   public void setDebugLevel(int level)
   {
      debugLevel = level;
   }

   @Override
   public boolean isDebugMode()
   {
      return debugLevel > 0;
   }

   @Override
   public void debug(String message)
   {
      debug(message, null);
   }

   @Override
   public void debugNoLn(String message)
   {
      if (debugLevel > 0)
      {
         System.err.print(message);
      }
   }

   public void debug(String message, Throwable throwable)
   {
      messageSystem.debugMessage(message, throwable);
   }

   @Override
   public void debugMessage(String message, Throwable throwable)
   {
      if (debugLevel > 0)
      {
         if (message != null)
         {
            System.err.println(message);
         }

         if (throwable != null)
         {
            throwable.printStackTrace();
         }
      }
   }

   @Override
   public void warning(String message)
   {
      warning(message, null);
   }

   public void warning(String message, Throwable throwable)
   {
      messageSystem.warningMessage(message, throwable);
   }

   @Override
   public void warningMessage(String message, Throwable throwable)
   {
      if (message != null)
      {
         System.err.println(getMessage("warning", message));
      }

      if (throwable != null)
      {
         String msg = throwable.getMessage();

         if (msg != null)
         {
            System.err.println(getMessage("warning", msg));
         }

         if (debugLevel > 0)
         {
            throwable.printStackTrace();
         }
      }
   }

   @Override
   public void error(String message)
   {
      error(message, null);
   }

   @Override
   public void error(Throwable throwable)
   {
      errorMessage(null, throwable);
   }

   public void error(String message, Throwable throwable)
   {
      messageSystem.errorMessage(message, throwable);
   }

   @Override
   public void errorMessage(String message, Throwable throwable)
   {
      if (message != null)
      {
         System.err.println(getMessage("error", message));
      }

      if (throwable != null)
      {
         String msg = throwable.getMessage();

         if (msg != null)
         {
            System.err.println(getMessage("error", msg));
         }

         if (debugLevel > 0)
         {
            throwable.printStackTrace();
         }
      }
   }

   public String getMessage(String label, Object... params)
   {
      return passTools.getMessage(label, params);
   }

   @Override
   public PassTools getPassTools()
   {
      return passTools;
   }

   @Override
   public String getApplicationName()
   {
      return NAME;
   }

   @Override
   public String getApplicationVersion()
   {
      return VERSION_DATE;
   }

   // Other Pass methods not required but need to be defined.
   @Override
   public Date getSubmittedDate() { return null; }

   @Override
   public long getTimeOut() { return 360L; }

   @Override
   public void setTimeOut(long timeout) { }

   @Override
   public Vector<PassFile> getFiles() { return null; }

   @Override
   public Path getBasePath() { return null; }

   @Override
   public AssignmentData getAssignment() { return null; }

   @Override
   public String getEncoding() { return "UTF-8"; }

   @Override
   public boolean isGroupProject() { return false; }

   @Override
   public Vector<Student> getProjectTeam() { return null; }

   @Override
   public Student getStudent() { return null; }

   @Override
   public boolean isConfirmed() { return true; }

   @Override
   public void transcriptMessage(String msg) {}

   @Override
   public void verboseCodePoint(int cp) {}

   @Override
   public void verbose(String msg) {}

   public void processPDF(String filename)
     throws IOException,
            NoSuchAlgorithmException
   {
      AssignmentMetaData data = parsePDF(new File(filename));

      add(data);
   }

   /**
    * Process a PDF file.
    * @param file the PDF file
    * @throws IOException if I/O error occurs
    * @throws NoSuchAlgorithmException if unrecognised encryption
    * algorithm
    */ 
   public void processPDF(File file)
     throws IOException,
            NoSuchAlgorithmException
   {
      AssignmentMetaData data = parsePDF(file);

      String decryptedCheckSum = data.getDecryptedCheckSum();
      String checkSum = data.getZipCheckSum();

      if (flagIdenticalCheckSums && !(decryptedCheckSum == null && checkSum == null))
      {
         for (AssignmentMetaData d : this)
         {
            if (decryptedCheckSum.equals(d.getDecryptedCheckSum()))
            {
               data.appendInfo(getMessage("message.decrypted_checksum_identical",
                  d.getPdfFile().getName()));
               d.appendInfo(getMessage("message.decrypted_checksum_identical",
                  data.getPdfFile().getName()));
            }

            if (checkSum.equals(d.getZipCheckSum()))
            {
               data.appendInfo(getMessage("message.calculated_checksum_identical",
                 d.getPdfFile().getName()));
               d.appendInfo(getMessage("message.calculated_checksum_identical",
                 data.getPdfFile().getName()));
            }
         }
      }

      add(data);
   }

   /**
    * Parses metadata from PDF file.
    * @param file the PDF file
    * @return the metadata
    * @throws IOException if I/O error occurs
    * @throws NoSuchAlgorithmException if unrecognised encryption
    * algorithm
    */ 
   private AssignmentMetaData parsePDF(File file) 
     throws IOException,
            NoSuchAlgorithmException
   {
      PDDocument document = null;
      AssignmentMetaData data = new AssignmentMetaData(this);
      data.setPdfFile(file);

      AssignmentProcessConfig config = passTools.getConfig();

      try
      {
         document = PDDocument.load(file);
   
         PDDocumentInformation pdd = document.getDocumentInformation();
   
         data.setPdfCreationDate(pdd.getCreationDate());
         data.setPdfModDate(pdd.getModificationDate());
         data.setPdfAuthor(pdd.getAuthor());

         byte[] keyValue = null;

         try
         {
            String keyData = pdd.getCustomMetadataValue("DataCheckF");

            if (keyData != null)
            {
               keyValue = config.decrypt_1_16(keyData);
            }
         }
         catch (Exception e)
         {
            debug("Failed to decrypt key", e);
            data.appendInfo(getMessage("error.invalid_key"));
         }

         try
         {
            data.setDecryptedCheckSum(new String(
              decrypt(pdd.getCustomMetadataValue("DataCheckA"), keyValue)));
         }
         catch (Exception e)
         {
            debug("Failed to decrypt checksum.", e);
            data.appendInfo(getMessage("error.invalid_checksum"));
         }

         try
         {
            data.setDecryptedDate(getDateStamp(
               decrypt(pdd.getCustomMetadataValue("DataCheckB"), keyValue)));
         }
         catch (Exception e)
         {
            debug("Failed to decrypt date stamp.", e);
            data.appendInfo(getMessage("error.invalid_datestamp"));
         }

         try
         {
            data.setDecryptedVersion(new String(
               decrypt(pdd.getCustomMetadataValue("DataCheckC"), keyValue)));
         }
         catch (Exception e)
         {
            debug("Failed to decrypt version.", e);
            data.appendInfo(getMessage("error.invalid_version"));
         }

         if (data.getDecryptedCheckSum() == null
              && data.getDecryptedVersion() == null)
         {
            String val = pdd.getCustomMetadataValue("CheckSum");

            if (val != null)
            {
               data.appendInfo("Old version of PASS probably used.");
               data.setDecryptedCheckSum(val);
            }
         }

         try
         {
            data.setDecryptedDueDate(getDateStamp(
               decrypt(pdd.getCustomMetadataValue("DataCheckD"), keyValue)));
         }
         catch (Exception e)
         {
            debug("Failed to decrypt due date.", e);
            data.appendInfo(getMessage("error.invalid_duedate"));
         }

         try
         {
            data.setDecryptedAuthor(new String(
               decrypt(pdd.getCustomMetadataValue("DataCheckE"), keyValue)));
         }
         catch (Exception e)
         {
            debug("Failed to decrypt author.", e);
            data.appendInfo(getMessage("error.invalid_author"));
         }
   
         try
         {
            String val = pdd.getCustomMetadataValue("DataCheckG");

            if (val == null)
            {
               data.appendInfo(getMessage("error.invalid_no_appname"));
            }
            else
            {
               data.setDecryptedApplicationName(new String(
                  decrypt(val, keyValue)));
            }
         }
         catch (Exception e)
         {
            debug("Failed to decrypt application name.", e);
            data.appendInfo(getMessage("error.invalid_appname"));
         }

         try
         {
            String val = pdd.getCustomMetadataValue("DataCheckH");

            if (val != null)
            {
               data.setDecryptedSubmissionDate(getDateStamp(
                  decrypt(val, keyValue)));
            }
         }
         catch (Exception e)
         {
            debug("Failed to decrypt date stamp.", e);
            data.appendInfo(getMessage("error.invalid_datestamp"));
         }

         PDPage page = document.getPage(0);
   
         int attachmentCount = 0;
   
         for (PDAnnotation annotation : page.getAnnotations())
         {
            if (annotation instanceof PDAnnotationFileAttachment
                 && data.getZipCheckSum() == null)
            {
               attachmentCount++;
   
               PDAnnotationFileAttachment attachment =
                 (PDAnnotationFileAttachment)annotation;
   
               PDFileSpecification fileSpec = attachment.getFile();

               debug(String.format("Page 1: found attachment '%s'",
                   attachment.getAttachmentName()));
   
               if (fileSpec instanceof PDComplexFileSpecification)
               {
                  PDComplexFileSpecification complexFS =
                    (PDComplexFileSpecification)fileSpec;
   
                  PDEmbeddedFile embeddedFile = complexFS.getEmbeddedFile();
   
                  int size = embeddedFile.getSize();
   
                  String fileType = embeddedFile.getSubtype();
   
                  if ("application/zip".equals(fileType))
                  {
                     COSInputStream in = null;
   
                     try
                     {
                        in = embeddedFile.createInputStream();
   
                        byte[] array = readBytes(in, size,
                           file.getName(), attachment.getFile().getFile());
   
                        if (array != null)
                        {
                           data.setZipCheckSum(passTools.getConfig().getCheckSum(array));
                        }
                        else
                        {
                         // set to empty to indicate that the attachment is present
                         // but the checksum hasn't been calculated
                           data.setZipCheckSum("");
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
                  else
                  {
                     warning(getMessage("warning.page_mimetype", 
                       file.getName(), 1, fileType));
                  }
               }
            }
         }

         if (attachmentCount == 0)
         {
            error(getMessage("error.no_attachments", file.getName(), 1));
         }
         else if (attachmentCount > 1)
         {
            error(getMessage("error.multiple_attachments", 
              file.getName(), attachmentCount, 1));
         }

         if (data.getZipCheckSum() == null)
         { 
            error(getMessage("error.zip_attachment_no_checksum", file.getName()));
            data.appendInfo(getMessage("info.zip_attachment_no_checksum"));
         }
      }
      finally
      {
         if (document != null)
         {
            document.close();
         }
      }

      data.validate();

      return data;
   }

   /**
    * Reads bytes from input stream for zip attachment.
    * The size should match the declared size. If not, it's likely
    * that some corruption or tampering has occurred.
    * If there is a size mismatch then the actual byte array will
    * only be returned if the flag identical setting is on,
    * otherwise null will be returned if the size is incorrect.
    * @param in the input stream
    * @param size the declared size
    * @param pdfName the name of the PDF file
    * @param zipName the name of the zip attachment
    * @return the byte array or null
    */ 
   private byte[] readBytes(InputStream in, int size, String pdfName, String zipName)
   throws IOException
   {
      byte[] array = new byte[size];
   
      int result = in.read(array);

      if (result == -1)
      {// input stream has ended

         warning(getMessage(
            "warning.attach_empty", pdfName, zipName, size));

         return null;
      }
      else if (result < size)
      {
         // actual size less than declared size

         warning(getMessage(
            "warning.attach_size_less", pdfName, zipName, size, result));

         if (flagIdenticalCheckSums)
         {
            byte[] newArray = new byte[result];

            for (int i = 0; i < result; i++)
            {
               newArray[i] = array[i];
            }

            return newArray;
         }
         else
         {
            return null;
         }
      }

      int b = in.read();

      if (b == -1)
      {// all bytes have been read
         return array;
      }

      // larger than declared size

      if (!flagIdenticalCheckSums)
      {
         // Don't bother to read anything else if flag identical
         // checksums isn't on. The size mismatch is a sufficient
         // alert.

         warning(getMessage(
            "warning.attach_size_more", pdfName, zipName, size));

         return null;
      }

      Vector<Byte> byteList = new Vector<Byte>(size+1);

      for (int i = 0; i < array.length; i++)
      {
         byteList.add(Byte.valueOf(array[i]));
      }

      while (b != -1)
      {
         byteList.add(Byte.valueOf((byte)b));

         b = in.read();
      }

      int actualSize = byteList.size();

      warning(getMessage(
         "warning.attach_size_larger", pdfName, zipName, size, actualSize));

      array = new byte[actualSize];

      for (int i = 0; i < actualSize; i++)
      {
         array[i] = byteList.get(i).byteValue();
      }

      return array;
   }

   /**
    * Decrypts a string. The string needs to be converted to binary
    * data using AssignmentProcessConfig.toData(String).
    * @param String string
    * @param keyValue encryption key
    * @return decrypted data
    */ 
   private byte[] decrypt(String string, byte[] keyValue)
     throws NoSuchAlgorithmException,
            InvalidKeyException,
            IllegalBlockSizeException,
            NoSuchPaddingException,
            BadPaddingException,
            IllegalArgumentException
   {
      debug("Decrypting: "+string);

      if (string == null)
      {
         throw new IllegalArgumentException(getMessage("error.missing_value"));
      }

      AssignmentProcessConfig config = passTools.getConfig();

      return config.decrypt(config.toData(string), keyValue);
   }

   /**
    * Converts decrypted time data into a timestamp.
    * @param data the decrypted time data
    * @return the timestamp
    * @throws IOException if I/O error occurs
    */ 
   private Date getDateStamp(byte[] data) throws IOException
   {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      DataInputStream dis = new DataInputStream(bais);
      return new Date(dis.readLong());
   }

   /**
    * Gets the formatted date.
    * @param date the date (may be null)
    * @return the formatted date or em-dash if null
    */ 
   public static String format(Date date)
   {
      return date == null ? "\u2015" : DATE_FORMAT.format(date);
   }

   /**
    * Gets the formatted filename.
    * @param file the file (may be null)
    * @return the formatted filename or em-dash if null
    */ 
   public static String format(File file)
   {
      return file == null ? "\u2015" : file.getName();
   }

   /**
    * Gets the text or em-dash if null.
    * @param text the text (may be null)
    * @return the text or em-dash if null
    */ 
   public static String format(String text)
   {
      return text == null ? "\u2015" : text;
   }

   /**
    * Gets the formatted numeric ID.
    * @param id the ID or -1 for none
    * @return the ID or em-dash if -1
    */ 
   public static String format(int id)
   {
      return id == -1 ? "\u2015" : ""+id;
   }

   /**
    * Runs PASS Checker in batch mode. This method is retained 
    * to allow for the possibility of adding GUI
    * support in the future.
    * @param files the list of PDF files to process
    * @throws IOException if I/O exception occurs
    */ 
   private void runBatch(Vector<String> files) throws IOException
   {
      if (files.size() == 0)
      {
         System.err.println(getMessage("error.syntax.no_filenames", "--help"));
         System.exit(1);
      }

      for (String filename : files)
      {
         try
         {
            processPDF(filename);
         }
         catch (Exception e)
         {
            error(getMessage("error.process_failed", filename), e);
            AssignmentMetaData data = new AssignmentMetaData(this);
            data.setPdfFile(new File(filename));
            String msg = e.getMessage();

            if (msg == null)
            {
               msg = getMessage("error.parsing_failed");
            }

            data.setInfo(msg);
            add(data);
         }
      }

      PrintWriter writer = null;

      try
      {
         if (outfilename != null)
         {
            writer = new PrintWriter(outfilename);
         }

         String header;

         if (serverData == null)
         {
            header = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
               getMessage("header.filename"),
               getMessage("header.author"),
               getMessage("header.author_check"),
               getMessage("header.date_check"),
               getMessage("header.creation_date"),
               getMessage("header.mod_date"),
               getMessage("header.pass_version"),
               getMessage("header.application"),
               getMessage("header.submission_date"),
               getMessage("header.notes")
            );
         }
         else
         {
            header = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
               getMessage("header.filename"),
               getMessage("header.author"),
               getMessage("header.author_check"),
               getMessage("header.date_check"),
               getMessage("header.creation_date"),
               getMessage("header.mod_date"),
               getMessage("header.pass_version"),
               getMessage("header.application"),
               getMessage("header.submission_date"),
               getMessage("header.submission_id"),
               getMessage("header.notes")
            );
         }
   
         if (writer == null)
         {
            System.out.println(header);
         }
         else
         {
            writer.println(header);
         }
   
         for (AssignmentMetaData data : this)
         {
            String info = data.getInfo();

            String row;

            if (serverData == null)
            {
               row = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                  format(data.getPdfFile()),
                  format(data.getPdfAuthor()),
                  format(data.getDecryptedAuthor()),
                  format(data.getDecryptedDate()),
                  format(data.getPdfCreationDate()),
                  format(data.getPdfModDate()),
                  format(data.getDecryptedVersion()),
                  format(data.getDecryptedApplicationName()),
                  format(data.getDecryptedSubmissionDate()),
                  info == null ? "" : String.format("\"%s\"", info)
               );
            }
            else
            {
               row = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                  format(data.getPdfFile()),
                  format(data.getPdfAuthor()),
                  format(data.getDecryptedAuthor()),
                  format(data.getDecryptedDate()),
                  format(data.getPdfCreationDate()),
                  format(data.getPdfModDate()),
                  format(data.getDecryptedVersion()),
                  format(data.getDecryptedApplicationName()),
                  format(data.getDecryptedSubmissionDate()),
                  format(data.getJobID()),
                  info == null ? "" : String.format("\"%s\"", info)
               );
            }

            if (writer == null)
            {
               System.out.println(row);
            }
            else
            {
               writer.println(row);
            }
         }
      }
      finally
      {
         if (writer != null)
         {
            writer.close();
         }
      }
   }

   /**
    * Not implemented.
    */ 
   private void createAndShowGui(final Vector<String> files)
   {
      System.out.println("GUI mode not yet implemented.");
   }

   /**
    * Writes version details to STDOUT.
    */ 
   public void version()
   {
      System.out.format("%s %s (%s)%n", NAME, VERSION, VERSION_DATE);
      System.out.format("Copyright %d-%s Nicola Talbot%n", COPYRIGHT_START_YEAR,
         VERSION_DATE.substring(0, 4));
      System.out.println("Apache License. Version 2.0, January 2004");
      System.exit(0);
   }

   /**
    * Writes help to STDOUT.
    */ 
   public void help()
   {
      System.out.format("Usage: %s [<option>]+ <PDF file>+", INVOKER_NAME);
      System.out.println();
      System.out.println(getMessage("syntax.filenames"));
      System.out.println();
      System.out.println(getMessage("syntax.options"));
      System.out.println(getMessage("syntax.help", "--help", "-h"));
      System.out.println(getMessage("syntax.version", "--version", "-v"));
      System.out.println(getMessage("syntax.debug", "--debug"));

      System.out.println();
      System.out.println(getMessage("syntax.out", "--out", "-o"));

      System.out.println();
      System.out.println(getMessage("syntax.job", "--job", "-j"));

      System.out.println();
      System.out.println(getMessage("syntax.max_time_diff", "--max-time-diff",
         getMaxTimeDiff()));

      System.out.println();
      System.out.println(getMessage("syntax.flag.identical.checksums",
       "--flag-identical-checksums", "-c"));
      System.out.println(getMessage("syntax.noflag.identical.checksums",
       "--noflag-identical-checksums", "-k"));

      System.exit(0);
   }

   /**
    * Parse command line arguments.
    */ 
   public void parseArgs(String[] args)
   throws IllegalArgumentException,IOException
   {
      boolean gui = false;
      Vector<String> files = new Vector<String>();

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--gui") || args[i].equals("-g"))
         {
            gui = true;
         }
         else if (args[i].equals("--batch"))
         {
            gui = false;
         }
         else if (args[i].equals("--debug"))
         {
            setDebugLevel(1);
         }
         else if (args[i].equals("--flag-identical-checksums")
             || args[i].equals("-c"))
         {
            flagIdenticalCheckSums = true;
         }
         else if (args[i].equals("--noflag-identical-checksums")
             || args[i].equals("-k"))
         {
            flagIdenticalCheckSums = false;
         }
         else if (args[i].equals("--max-time-diff") || args[i].equals("-m"))
         {
            String opt = args[i];
            i++;

            if (i == args.length)
            {
               throw new IllegalArgumentException(
                 getMessage("error.syntax.missing_arg", opt));
            }

            try
            {
               setMaxTimeDiff(Integer.parseInt(args[i]));
            }
            catch (NumberFormatException e)
            {
               throw new IllegalArgumentException(
                 getMessage("error.syntax.invalid_arg", opt,
                 e.getMessage()));
            }
         }
         else if (args[i].equals("--out") || args[i].equals("-o"))
         {
            String opt = args[i];
            i++;

            if (i == args.length)
            {
               throw new IllegalArgumentException(
                 getMessage("error.syntax.missing_arg", opt));
            }

            setOutFileName(args[i]);
         }
         else if (args[i].equals("--job") || args[i].equals("-j"))
         {
            String opt = args[i];
            i++;

            if (i == args.length)
            {
               throw new IllegalArgumentException(
                 getMessage("error.syntax.missing_arg", opt));
            }

            parseSubmissions(args[i]);
         }
         else if (args[i].equals("--help") || args[i].equals("-h"))
         {
            help();
         }
         else if (args[i].equals("--version") || args[i].equals("-v"))
         {
            version();
         }
         else if (args[i].startsWith("-"))
         {
            throw new IllegalArgumentException(getMessage("error.syntax.unknown",
               args[i], "--help"));
         }
         else
         {
            files.add(args[i]);
         }
      }

      if (gui)
      {
         createAndShowGui(files);
      }
      else
      {
         runBatch(files);
      }
   }

   public static void main(String[] args)
   {
      PassChecker passChecker = null;

      try
      {
         passChecker = new PassChecker();

         passChecker.parseArgs(args);
      }
      catch (IllegalArgumentException e)
      {
         if (passChecker != null)
         {
            passChecker.error(e);
         }
         else if (e.getMessage() == null)
         {
            e.printStackTrace();
         }
         else
         {
            System.out.println(e.getMessage());
         }

         System.exit(1);
      }
      catch (IOException e)
      {
         if (passChecker != null)
         {
            passChecker.error(e);
         }
         else if (e.getMessage() == null)
         {
            e.printStackTrace();
         }
         else
         {
            System.out.println(e.getMessage());
         }

         System.exit(2);
      }
   }

   private String outfilename=null;
   private MessageSystem messageSystem;
   private int debugLevel = 0;
   private int maxTimeDiff = 10;
   private boolean flagIdenticalCheckSums=false;

   private Vector<ServerJobData> serverData;

   public static final String NAME="PASS Checker";
   public static final String INVOKER_NAME="pass-checker";
   public static final String VERSION="1.4.1";
   public static final String VERSION_DATE="2022-12-05";
   public static final int COPYRIGHT_START_YEAR=2018;

   public static final SimpleDateFormat DATE_FORMAT
      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   private PassTools passTools;
}
