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

import java.util.Date;
import java.util.Calendar;
import java.io.File;

/**
 * Metadata obtained from assignment PDF.
 */
public class AssignmentMetaData
{
   public AssignmentMetaData(PassChecker passChecker)
   {
      this.passChecker = passChecker;
   }

   /**
    * Sets the PDF author.
    * @param author the author name found in the standard PDF author
    * info
    */ 
   public void setPdfAuthor(String author)
   {
      pdfAuthor = author;
   }

   /**
    * Gets the PDF author.
    * @return the author name found in the standard PDF author
    * info
    */ 
   public String getPdfAuthor()
   {
      return pdfAuthor;
   }

   /**
    * Sets the decrypted author.
    * @param author the decrypted author name obtained from the encrypted data
    */ 
   public void setDecryptedAuthor(String author)
   {
      decryptedAuthor = author;
   }

   /**
    * Gets the decrypted author.
    * @return the decrypted author name obtained from the encrypted data
    */ 
   public String getDecryptedAuthor()
   {
      return decryptedAuthor;
   }

   /**
    * Sets the zip file attachment's checksum.
    * @param checksum the zip file attachment's checksum
    */ 
   public void setZipCheckSum(String checksum)
   {
      zipCheckSum = checksum;
   }

   /**
    * Gets the zip file attachment's checksum.
    * @return the zip file attachment's checksum
    */ 
   public String getZipCheckSum()
   {
      return zipCheckSum;
   }

   /**
    * Sets the zip file attachment's decrypted checksum.
    * The zip checksum is calculated by PASS using
    * AssignmentProcessConfig.getCheckSum(File) which uses SHA-256
    * with base 64 encoding so the supplied checksum should be in
    * that format.
    * @param checksum the zip file attachment's decrypted checksum obtained
    * from the encrypted data
    */ 
   public void setDecryptedCheckSum(String checksum)
   {
      decryptedCheckSum = checksum;
   }

   /**
    * Gets the zip file attachment's decrypted checksum.
    * @return the zip file attachment's decrypted checksum obtained
    * from the encrypted data
    */ 
   public String getDecryptedCheckSum()
   {
      return decryptedCheckSum;
   }

   /**
    * Sets the PDF creation date.
    * @param cal the date found in the standard PDF creation date
    * info
    */ 
   public void setPdfCreationDate(Calendar cal)
   {
      pdfCreationDate = (cal == null ? null : cal.getTime());
   }

   /**
    * Sets the PDF creation date.
    * @param date the date found in the standard PDF creation date
    * info
    */ 
   public void setPdfCreationDate(Date date)
   {
      pdfCreationDate = date;
   }

   /**
    * Gets the PDF creation date.
    * @return the date found in the standard PDF creation date
    * info
    */ 
   public Date getPdfCreationDate()
   {
      return pdfCreationDate;
   }

   /**
    * Sets the PDF modification date.
    * @param cal the date found in the standard PDF modification date
    * info
    */ 
   public void setPdfModDate(Calendar cal)
   {
      pdfModDate = (cal == null ? null : cal.getTime());
   }

   /**
    * Sets the PDF modification date.
    * @param date the date found in the standard PDF modification date
    * info
    */ 
   public void setPdfModDate(Date date)
   {
      pdfModDate = date;
   }

   /**
    * Gets the PDF modification date.
    * @return the date found in the standard PDF modification date
    * info
    */ 
   public Date getPdfModDate()
   {
      return pdfModDate;
   }

   /**
    * Sets the decrypted timestamp. This should correspond to the
    * creation date.
    * @param checksum the decrypted timestamp obtained
    * from the encrypted data
    */ 
   public void setDecryptedDate(Date date)
   {
      decryptedDate = date;
   }

   /**
    * Gets the decrypted timestamp.
    * @return the decrypted timestamp obtained
    * from the encrypted data
    */ 
   public Date getDecryptedDate()
   {
      return decryptedDate;
   }

   /**
    * Sets the decrypted due date.
    * @param checksum the decrypted due date obtained
    * from the encrypted data
    */ 
   public void setDecryptedDueDate(Date date)
   {
      decryptedDueDate = date;
   }

   /**
    * Gets the decrypted due date.
    * @return the decrypted due date obtained
    * from the encrypted data
    */ 
   public Date getDecryptedDueDate()
   {
      return decryptedDueDate;
   }

   /**
    * Sets the decrypted submission date. This will only be
    * available with PDF files created by Server PASS.
    * @param checksum the decrypted submission date obtained
    * from the encrypted data
    */ 
   public void setDecryptedSubmissionDate(Date date)
   {
      decryptedSubmissionDate = date;
   }

   /**
    * Gets the decrypted submission date.
    * @return the decrypted submission date obtained
    * from the encrypted data
    */ 
   public Date getDecryptedSubmissionDate()
   {
      return decryptedSubmissionDate;
   }

   /**
    * Sets the PDF file this data was obtained from. 
    * @param file the PDF file
    */ 
   public void setPdfFile(File file)
   {
      pdfFile = file;
   }

   /**
    * Gets the PDF file this data was obtained from. 
    * @return the PDF file
    */ 
   public File getPdfFile()
   {
      return pdfFile;
   }

   /**
    * Sets the decrypted PASS version.
    * @param version the decrypted version obtained
    * from the encrypted data
    */ 
   public void setDecryptedVersion(String version)
   {
      decryptedVersion = version;
   }

   /**
    * Gets the decrypted PASS version.
    * @return the decrypted version obtained
    * from the encrypted data
    */ 
   public String getDecryptedVersion()
   {
      return decryptedVersion;
   }

   /**
    * Sets the decrypted PASS application name.
    * @param version the decrypted application name obtained
    * from the encrypted data
    */ 
   public void setDecryptedApplicationName(String name)
   {
      decryptedApplicationName = name;
   }

   /**
    * Gets the decrypted PASS application name.
    * @return the decrypted application name obtained
    * from the encrypted data
    */ 
   public String getDecryptedApplicationName()
   {
      return decryptedApplicationName;
   }

   /**
    * Clears the notes.
    */ 
   public void clearInfo()
   {
      info = null;
   }

   /**
    * Sets the notes.
    */ 
   public void setInfo(String text)
   {
      if (text == null)
      {
         info = null;
      }
      else
      {
         info = new StringBuilder(text);
      }
   }

   /**
    * Appends information to the notes.
    * @param text extra information
    */ 
   public void appendInfo(String text)
   {
      if (info == null)
      {
         info = new StringBuilder(text);
      }
      else
      {
         info.append(String.format("%n%s", text));
      }
   }

   public String getInfo()
   {
      return info == null ? null : info.toString();
   }

   public void validate()
   {
      if (decryptedAuthor == null)
      {
         appendInfo(passChecker.getMessage("info.invalid_or_missing_author"));
      }

      if (pdfAuthor == null)
      {
         appendInfo(passChecker.getMessage("info.missing_pdf_author"));
      }

      if (pdfAuthor != null && decryptedAuthor != null 
           && !pdfAuthor.equals(decryptedAuthor))
      {
         appendInfo(passChecker.getMessage("info.mismatched_author"));
      }

      if (decryptedCheckSum == null)
      {
         appendInfo(passChecker.getMessage("info.invalid_or_missing_checksum"));
      }
      else if (zipCheckSum == null || zipCheckSum.isEmpty())
      {
         appendInfo(passChecker.getMessage("info.checksum_not_calculated"));
      }
      else if (!decryptedCheckSum.equals(zipCheckSum))
      {
         String msg = passChecker.getMessage("info.mismatched_checksum");
         passChecker.warning(String.format("%s: %s", pdfFile.getName(), msg));
         appendInfo(msg);
      }

      if (decryptedVersion == null)
      {
         appendInfo(passChecker.getMessage("info.invalid_or_missing_version"));
      }

      if (decryptedDate == null)
      {
         appendInfo(passChecker.getMessage("info.invalid_or_missing_date"));
      }

      if (pdfCreationDate == null)
      {
         appendInfo(passChecker.getMessage("info.missing_pdf_creation_date"));
      }

      if (pdfModDate == null)
      {
         appendInfo(passChecker.getMessage("info.missing_pdf_mod_date"));
      }

      if (decryptedDueDate == null)
      {
         appendInfo(passChecker.getMessage("info.invalid_or_missing_due_date"));
      }

      if (decryptedDate != null)
      {
         long secs = decryptedDate.getTime()/1000;

         if (pdfCreationDate != null)
         {
            long creationSecs = pdfCreationDate.getTime()/1000;

            if (secs != creationSecs)
            {
               appendInfo(passChecker.getMessage("info.mismatched_creation_date"));
            }
         }

         if (pdfModDate != null)
         {
            long modSecs = pdfModDate.getTime()/1000;
            int diff = passChecker.getMaxTimeDiff();

            if (modSecs <= secs)
            {
               appendInfo(passChecker.getMessage("info.mod_le_creation_date"));
            }
            else if (modSecs - secs > diff)
            {
               appendInfo(passChecker.getMessage("info.mod_gt_creation_date",
                diff));
            }
         }

         if (decryptedDueDate != null)
         {
            long dueSecs = decryptedDueDate.getTime()/1000;

            long submissionSecs = secs;

            if (decryptedSubmissionDate != null)
            {
               if (!decryptedApplicationName.equals("pass-cli-server"))
               {
                   appendInfo(passChecker.getMessage(
                     "info.submission_date_not_server_pass", "pass-cli-server"));
               }
               else
               {
                  submissionSecs = decryptedSubmissionDate.getTime()/1000;
               }
            }

            if (dueSecs < submissionSecs)
            {
               appendInfo(passChecker.getMessage("info.late_submission"));
            }
         }
      }

      if (decryptedSubmissionDate != null && decryptedAuthor != null)
      {
         jobData = passChecker.getJobData(pdfFile, decryptedAuthor,
            decryptedSubmissionDate);
      }
   }

   /**
    * Gets the job ID or -1 if not set.
    * @return the job ID or -1
    */ 
   public int getJobID()
   {
      return jobData == null ? -1 : jobData.getJobID();
   }

   private String pdfAuthor, decryptedAuthor;
   private String zipCheckSum, decryptedCheckSum;
   private String decryptedVersion, decryptedApplicationName;
   private Date pdfCreationDate, pdfModDate, decryptedDate, decryptedDueDate,
     decryptedSubmissionDate;

   private StringBuilder info = null;

   private File pdfFile;

   private ServerJobData jobData = null;

   private PassChecker passChecker;
}
