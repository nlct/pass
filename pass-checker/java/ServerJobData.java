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

import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.charset.Charset;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Submission data exported from Server Pass.
 * Note that it's possible for the checksum to be empty, which
 * indicates a fatal error occurred and the PDF wasn't created.
 */
public class ServerJobData
{
   private ServerJobData()
   {
   }

   public ServerJobData(int id, String author, Date submissionDate, String md5)
   {
      jobId = id;
      uploader = author;
      date = submissionDate;
      checksum = md5;
   }

   /**
    * Parses TSV file exported from Server Pass.
    * @param pass the main PassChecker class
    * @param file the TSV file
    * @return list of job data
    * @throws IOException if I/O error occurs
    */ 
   public static Vector<ServerJobData> parse(PassChecker pass, File file)
   throws IOException
   {
      Vector<ServerJobData> list = new Vector<ServerJobData>();

      parse(list, pass, file);

      return list;
   }

   /**
    * Parses TSV file exported from Server Pass and adds to
    * existing list.
    * @param list list of job data to add to
    * @param pass the main PassChecker class
    * @param file the TSV file
    * @throws IOException if I/O error occurs
    */ 
   public static void parse(Vector<ServerJobData> list,
      PassChecker pass, File file)
   throws IOException
   {
      BufferedReader in = null;

      try
      {
         in = Files.newBufferedReader(file.toPath(),
            Charset.forName(pass.getEncoding()));

         String line;
         int lineNum = 0;

         while ((line = in.readLine()) != null)
         {
            lineNum++;

            if (lineNum == 1 || line.isEmpty()) continue;

            String[] split = line.split("\t");

            if (split.length != 8)
            {
               throw new FileFormatException(
                 pass.getMessage("error.parse_tsv.invalid_line", 8, split.length),
                 file, lineNum);
            }

            ServerJobData data = new ServerJobData();

            try
            {
               data.jobId = Integer.parseInt(split[0]);
            }
            catch (NumberFormatException e)
            {
               throw new FileFormatException(
                 pass.getMessage("error.parse_tsv.invalid_jobid", split[0]),
                 file, lineNum, e);
            }

            try
            {
               data.date = ISO_DATETIME_FORMAT.parse(split[1]);
            }
            catch (ParseException e)
            {
               throw new FileFormatException(
                 pass.getMessage("error.parse_tsv.invalid_date", split[1]),
                 file, lineNum, e);
            }

            data.uploader = split[5];

            if (!data.uploader.equals(split[6]))
            {
               data.projectGroup = split[6].split(",");
            }

            data.checksum = split[7];

            list.add(data);
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
    * Gets the submission ID.
    * @return the submission ID
    */ 
   public int getJobID()
   {
      return jobId;
   }

   /**
    * Gets the submission date.
    * @return the submission date
    */ 
   public Date getDate()
   {
      return date;
   }

   /**
    * Gets the username of the uploader.
    * @return the uploader's username
    */ 
   public String getUploader()
   {
      return uploader;
   }

   /**
    * Gets the MD5 checksum.
    * @return the checksum
    */ 
   public String getCheckSum()
   {
      return checksum;
   }

   /**
    * Gets the job that matches the given PDF file.
    * It's possible for multiple files to have the same MD5 checksum,
    * so this method checks for a match on the checksum, author and
    * submission date.
    *
    * The Server Pass frontend should always list the uploader first,
    * so for a group project the uploader should be the first username
    * in the list and therefore should match the username supplied in
    * the author information.
    *
    * A warning is issued if the checksum matches but the author
    * doesn't. This could simply be that it's a coincidental match
    * so all jobs have to be tested to find if there's an exact
    * match.
    *
    * If no exact match is found, job ID -1 is returned. This may simply
    * mean that the data wasn't included in the exported file. Check
    * the search criteria and also the submission timestamp in case
    * the project was uploaded after the data was exported.
    *
    * @param list the list obtained from exporting the data from
    * Server Pass
    * @param pass the main PassChecker class
    * @param pdfFile the PDF file to match
    * @param author the username to match
    * @param submissionDate the submission date
    * @return the matching job or job with ID = -1 if no match
    * @throws IOException if I/O exception occurs while computing
    * the checksum
    * @throws NoSuchAlgorithmException if invalid checksum
    * algorithm (shouldn't happen)
   */
   public static ServerJobData getSubmission(Vector<ServerJobData> list,
     PassChecker pass, File pdfFile, String author, Date submissionDate)
   throws IOException,NoSuchAlgorithmException
   {
      byte[] b = Files.readAllBytes(pdfFile.toPath());
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hash = md.digest(b);
      String md5 = java.util.HexFormat.of().formatHex(hash);// Java 17+

      Vector<ServerJobData> matchGroupMember = null;
      Vector<ServerJobData> noUserMatch = null;
      Vector<ServerJobData> noDateMatch = null;

      for (ServerJobData job : list)
      {
         if (md5.equals(job.checksum))
         {
            boolean partialMatch = false;

            if (author.equals(job.uploader))
            {
               if (submissionDate.equals(job.date))
               {
                  return job;
               }
               else
               {
                  if (noDateMatch == null)
                  {
                     noDateMatch = new Vector<ServerJobData>();
                  }

                  noDateMatch.add(job);
                  partialMatch = true;
               }
            }

            if (!partialMatch && job.projectGroup != null)
            {
               for (String member : job.projectGroup)
               {
                  if (author.equals(member))
                  {
                     if (matchGroupMember == null)
                     {
                        matchGroupMember = new Vector<ServerJobData>();
                     }

                     matchGroupMember.add(job);
                     partialMatch = true;

                     break;
                  }
               }
            }

            if (!partialMatch)
            {
               if (noUserMatch == null)
               {
                  noUserMatch = new Vector<ServerJobData>();
               }

               noUserMatch.add(job);
            }
         }
      }

      if (noDateMatch != null)
      {
         StringBuilder builder = null;

         for (ServerJobData job : noDateMatch)
         {
            if (builder == null)
            {
               builder = new StringBuilder();
               builder.append(job.jobId);
            }
            else
            {
               builder.append(", ");
               builder.append(job.jobId);
            }
         }

         pass.warning(pass.getMessage("warning.checksum_match_not_date",
           pdfFile.getName(), 
           pass.getPassTools().getChoiceMessage("warning.checksum_matched", 
            noDateMatch.size()),
           builder
         ));
      }

      if (matchGroupMember != null)
      {
         StringBuilder builder = null;

         for (ServerJobData job : matchGroupMember)
         {
            if (builder == null)
            {
               builder = new StringBuilder();
               builder.append(job.jobId);
            }
            else
            {
               builder.append(", ");
               builder.append(job.jobId);
            }
         }

         pass.warning(pass.getMessage("warning.checksum_match_on_group_member",
           pdfFile.getName(), 
           pass.getPassTools().getChoiceMessage("warning.checksum_matched", 
            matchGroupMember.size()),
           builder
         ));
      }

      if (noUserMatch != null)
      {
         StringBuilder builder = null;

         for (ServerJobData job : noUserMatch)
         {
            if (builder == null)
            {
               builder = new StringBuilder();
               builder.append(job.jobId);
            }
            else
            {
               builder.append(", ");
               builder.append(job.jobId);
            }
         }

         pass.warning(pass.getMessage("warning.checksum_match_not_user",
           pdfFile.getName(), 
           pass.getPassTools().getChoiceMessage("warning.checksum_matched", 
            noUserMatch.size()),
           builder
         ));
      }

      return new ServerJobData(-1, author, submissionDate, md5);
   }

   private int jobId;// submission ID
   private Date date;// upload time
   private String uploader;// uploader's username
   private String checksum;// PDF MD5 checksum
   private String[] projectGroup=null;// group members or null for solo

   public static final SimpleDateFormat ISO_DATETIME_FORMAT
     = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssSSSZ");
}
