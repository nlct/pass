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
package com.dickimawbooks.passeditor;

import java.io.File;
import java.nio.file.Path;

import com.dickimawbooks.passlib.*;

public class ProjectFile implements PassFile
{
   public ProjectFile(File file)
   {
      setFile(file);
      language = AssignmentData.PLAIN_TEXT;
   }

   public ProjectFile(File file, String language)
   {
      setFile(file);
      setLanguage(language);
   }

   public ProjectFile(File file, AllowedBinaryFilter filter)
   {
      setFile(file);
      this.filter = filter;
      this.language = AssignmentData.BINARY;
   }

   protected ProjectFile(File file, String language, AllowedBinaryFilter filter)
   {
      setFile(file);

      if (filter != null)
      {
         this.language = AssignmentData.BINARY;
      }
      else
      {
         setLanguage(language);
      }

      this.filter = filter;
   }

   public boolean exists()
   {
      return file.exists();
   }

   @Override
   public File getFile()
   {
      return file;
   }

   @Override
   public String getLanguage()
   {
      return language;
   }

   public void setFile(File file)
   {
      this.file = file;
   }

   public void setLanguage(String language)
   {
      if (language == null || language.isEmpty())
      {
         this.language = AssignmentData.PLAIN_TEXT;
      }
      else
      {
         this.language = language;
      }
   }

   @Override
   public boolean equals(Object other)
   {
      if (this == other) return true;

      if (other == null || !(other instanceof ProjectFile))
      {
         return false;
      }

      ProjectFile pf = (ProjectFile)other;

      return pf.file.equals(file);
   }

   public Path getRelativePath(Project project)
   {
      return getRelativePath(project.getBase());
   }

   public Path getRelativePath(Path basePath)
   {
      Path path = file.toPath();

      if (path.isAbsolute())
      {
         path = basePath.relativize(path);
      }

      return path;
   }

   public String exportData(Path basePath)
   {
      Path path = getRelativePath(basePath);

      if (language == null || language.equals(AssignmentData.BINARY))
      {
         return String.format("%s\t%s", path, getMimeType());
      }
      else
      {
         return String.format("%s\t%s", path, language);
      }
   }

   public static ProjectFile importData(String dataLine, Project project)
   throws UnsupportedFileException
   {
      Path basePath = project.getBase();
      AssignmentData assignData = project.getAssignment();

      String[] split = dataLine.split("\t", 2);

      File f = new File(split[0]);

      if (!f.isAbsolute())
      {
         Path path = basePath.resolve(f.toPath());

         f = path.toFile();
      }

      String language = null;
      AllowedBinaryFilter filter = assignData.getAllowedBinaryFilter(f);

      if (split.length == 2)
      {
         if ("PDF".equals(split[1])
          || AssignmentData.MIME_PDF.equals(split[1]))
         {
            filter = new AllowedBinaryFilter("application/pdf", true,
              false, "PDF", "pdf");

            language = "PDF";
         }
         else if ("DOC".equals(split[1])
                 || AssignmentData.MIME_DOC.equals(split[1])
                 || AssignmentData.MIME_DOCX.equals(split[1]))
         {
            filter = new AllowedBinaryFilter(
              AssignmentData.getMimeType(language, split[0]), false,
              false, "DOC", "doc,docx");

            language = "DOC";
         }
         else if (AssignmentData.isText(split[1]))
         {
            language = split[1];
         }
         else if (filter == null || !filter.getMimeType().equals(split[1])
                 && !split[1].equals(AssignmentData.BINARY))
         {
            filter = assignData.getAllowedBinaryFilter(null, split[1]);

            if (filter == null)
            {
               throw new UnsupportedFileException(f, split[1]);
            }
         }
      }

      return new ProjectFile(f, language, filter);
   }

   public String getMimeType()
   {
      if (filter == null)
      {
         return AssignmentData.getMimeType(language, file.getName());
      }
      else
      {
         return filter.getMimeType();
      }
   }

   public boolean isText()
   {
      if (filter == null)
      {
         return !(language == null
           || language.equals(AssignmentData.BINARY)
           || language.equals("PDF")
           || language.equals("DOC"));
      }
      else
      {
         return filter.getMimeType().startsWith("text/");
      }
   }

   @Override
   public String toString()
   {
      if (filter != null)
      {
         return String.format("%s (%s)", file.toString(), getMimeType());
      }
      else
      {
         return String.format("%s (%s)", file.toString(), language);
      }
   }

   private File file;
   private String language;
   private AllowedBinaryFilter filter;
}
