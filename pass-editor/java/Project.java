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

import java.util.Vector;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.Charset;

import javax.swing.JOptionPane;

import org.xml.sax.SAXException;

import com.dickimawbooks.passlib.*;

public class Project
{
   protected Project(PassEditor gui)
   {
      this(gui, false);
   }

   protected Project(PassEditor gui, boolean promptOverwrite)
   {
      this.gui = gui;
      files = new Vector<ProjectFile>();
      students = new Vector<Student>();
      folders = null;
      this.promptOverwrite = promptOverwrite;
   }

   public Project(PassEditor gui, Path basePath)
   {
      this(gui);
      this.basePath = basePath;
   }

   public Project(PassEditor gui, Path basePath, boolean promptOverwrite)
   {
      this(gui, promptOverwrite);
      this.basePath = basePath;
   }

   public void setCourse(Course course)
   {
      this.course = course;
   }

   public void setAssignment(AssignmentData assignment)
   {
      this.assignment = assignment;
   }

   public void setBase(Path basePath)
   {
      this.basePath = basePath;
   }

   public Path getBase()
   {
      return basePath;
   }

   public boolean isBase(Path path)
   {
      return basePath.equals(path);
   }

   public boolean isBase(File file)
   {
      return (file == null || file.toPath().equals(basePath));
   }

   public File getPdfFile()
   {
      return pdfFile;
   }

   public void setPdfFile(File file)
   {
      pdfFile = file;
   }

   public boolean pdfFileExists()
   {
      return pdfFile != null && pdfFile.exists();
   }

   public void setConfirmed(boolean agree)
   {
      this.agree = agree;
   }

   public boolean isConfirmed()
   {
      return agree;
   }

   public void addStudent(Student student)
   {
      students.add(student);
   }

   public void removeStudent(Student student)
   {
      students.remove(student);
   }

   public void clearStudents()
   {
      students.clear();
   }

   public Student getStudent()
   {
      return students.isEmpty() ? null : students.firstElement();
   }

   public Vector<Student> getStudents()
   {
      return students;
   }

   public Course getCourse()
   {
      return course;
   }

   public URL getCourseURL()
   {
      return course == null ? null : course.getURL();
   }

   public String getCourseCode()
   {
      return course == null ? null : course.getCode();
   }

   public AssignmentData getAssignment()
   {
      return assignment;
   }

   public String getAssignmentTitle()
   {
      return assignment == null ? null : assignment.getTitle();
   }

   public void addFolder(File file)
   {
      if (folders == null)
      {
         folders = new Vector<File>();
      }

      if (!folders.contains(file))
      {
         folders.add(file);
      }
   }

   public Vector<File> getFolders()
   {
      return folders;
   }

   public void removeFolder(File dir)
   {
      if (folders != null)
      {
         folders.remove(dir);
      }
   }

   public void addFile(ProjectFile projectFile)
   {
      if (!files.contains(projectFile))
      {
         files.add(projectFile);
      }
   }

   public void removeFile(ProjectFile projectFile)
   {
      if (!files.remove(projectFile))
      {
         return;
      }

      File dir = projectFile.getFile().getParentFile();

      if (isBase(dir) || (folders != null && folders.contains(dir)))
      {
         return;
      }

      addFolder(dir);
   }

   public void fileMove(Path src, Path target)
   {
      if (!src.isAbsolute())
      {
         src = basePath.resolve(src);
      }

      File srcFile = src.toFile();

      if (!target.isAbsolute())
      {
         target = basePath.resolve(target);
      }

      File targetFile = target.toFile();

      for (ProjectFile pf : files)
      {
         if (pf.getFile().equals(srcFile))
         {
            pf.setFile(targetFile);
            return;
         }
      }

      files.add(new ProjectFile(targetFile));
   }

   public Vector<ProjectFile> getFiles()
   {
      return files;
   }

   protected String getDefaultProjectFileName()
   {
      return String.format("%s-%s.passed", course.getCode(),
        assignment.getLabel());
   }

   public String getProjectFileName()
   {
      return projectFileName == null ? getDefaultProjectFileName() : projectFileName;
   }

   public void setProjectFileName(String filename)
   {
      projectFileName = filename;
   }

   private void printSetting(PrintWriter out, String key, Object value)
      throws IOException
   {
      if (value != null)
      {
         out.format("%s=%s%n", key, value.toString());
      }
   }

   public void save() throws IOException
   {
      File file = new File(basePath.toFile(), getProjectFileName());

      if (promptOverwrite && file.exists())
      {
         switch (gui.confirm(
                  gui.getMessage("confirm.file_exists",file.getName()),
                  gui.getMessage("confirm.overwrite.title"),
                 JOptionPane.YES_NO_CANCEL_OPTION))
         {
            case JOptionPane.YES_OPTION:
            break;
            case JOptionPane.NO_OPTION:
               File newFile = gui.showSaveDialog();

               if (newFile != null)
               {
                  setProjectFileName(newFile.getName());
                  save();
                  return;
               }

            // fall through
            default:
               throw new FileAlreadyExistsException(file.toString());
         }
      }

      promptOverwrite = false;

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(file, CHARSET_NAME);

         printSetting(writer, "version", FILE_VERSIONS[CURRENT_FILE_VERSION_INDEX]);
         printSetting(writer, "agree", agree);
         printSetting(writer, "course", course == null ? null : course.getCode());
         printSetting(writer, "assignment", assignment == null ? null :
           assignment.getLabel());

         if (pdfFile != null)
         {
            if (pdfFile.isAbsolute())
            {
               printSetting(writer, "pdf", basePath.relativize(pdfFile.toPath()));
            }
            else
            {
               printSetting(writer, "pdf", pdfFile);
            }
         }

         for (ProjectFile f : files)
         {
            printSetting(writer, "file", f.exportData(basePath));
         }

         if (folders != null)
         {
            for (File f : folders)
            {
               if (f.isAbsolute())
               {
                  printSetting(writer, "folder", basePath.relativize(f.toPath()));
               }
               else
               {
                  printSetting(writer, "folder", f);
               }
            }
         }

         for (Student student : students)
         {
            printSetting(writer, "student", 
              String.format("%s\t%s", student.getUserName(), student.getRegNumber()));
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

   private static String[] readSetting(BufferedReader in, File file)
     throws IOException
   {
      String line = null;

      while ((line = in.readLine()) != null)
      {
         if (!line.isEmpty())
         {
            String[] split = line.split("=", 2);

            if (split.length == 2 && !split[0].isEmpty())
            {
               return split;
            }
            else
            {
               throw new InvalidFormatException(file, line);
            }
         }
      }

      return null;
   }

   public static Project load(PassEditor gui, File file) 
      throws IOException,SAXException
   {
      BufferedReader reader = null;
      File parent = file.getParentFile();

      if (parent == null)
      {
         parent = file.getAbsoluteFile().getParentFile();

         if (parent == null)
         {
            throw new IOException("No parent found for file "+file);
         }
      }

      Path basePath = parent.toPath();

      Project project = new Project(gui, basePath);

      String courseCode = null;
      String assignmentLabel = null;
      int versionIdx = CURRENT_FILE_VERSION_INDEX;
      Vector<String> fileData = new Vector<String>();

      try
      {
         reader = Files.newBufferedReader(file.toPath(), 
             Charset.forName(CHARSET_NAME));

         String[] split;

         while ((split = readSetting(reader, file)) != null)
         {
            if (split[0].equals("version"))
            {
               int idx = -1;

               for (int i = 0; i < FILE_VERSIONS.length; i++)
               {
                  if (FILE_VERSIONS[i].equals(split[1]))
                  {
                     idx = i;
                     break;
                  }
               }

               if (idx == -1)
               {
                  throw new InvalidFormatException(file, split[0], split[1]);
               }

               versionIdx = idx;
            }
            else if (split[0].equals("agree"))
            {
               project.setConfirmed(Boolean.parseBoolean(split[1]));
            }
            else if (split[0].equals("course"))
            {
               courseCode = split[1];
            }
            else if (split[0].equals("assignment"))
            {
               assignmentLabel = split[1];
            }
            else if (split[0].equals("pdf"))
            {
               File f = new File(split[1]);

               if (!f.isAbsolute())
               {
                  f = basePath.resolve(f.toPath()).toFile();
               }

               project.setPdfFile(f);
            }
            else if (split[0].equals("file"))
            {
               fileData.add(split[1]);
            }
            else if (split[0].equals("folder"))
            {
               File f = new File(split[1]);

               if (!f.isAbsolute())
               {
                  Path path = basePath.resolve(f.toPath());
                  f = path.toFile();
               }

               project.addFolder(f);
            }
            else if (split[0].equals("student"))
            {
               String[] s = split[1].split("\t");

               if (s.length != 2)
               {
                  throw new InvalidFormatException(file, split[0], split[1]);
               }

               project.addStudent(new Student(s[0], s[1]));
            }
            else
            {
               throw new InvalidFormatException(file, split[0]);
            }
         }
      }
      finally
      {
         if (reader != null)
         {
            reader.close();
         }
      }

      if (courseCode == null)
      {
         throw new InvalidFormatException(file, "course", "");
      }

      if (assignmentLabel == null)
      {
         throw new InvalidFormatException(file, "assignment", "");
      }

      project.setCourse(gui.getCourse(courseCode));
      project.setAssignment(gui.getAssignment(project.course, assignmentLabel));

      for (String dataLine : fileData)
      {
         project.addFile(ProjectFile.importData(dataLine, project));
      }

      return project;
   }

   private Course course;
   private AssignmentData assignment;
   private Path basePath;
   private Vector<ProjectFile> files;// additional files
   private Vector<File> folders; // additional directories (that may be empty)
   private String projectFileName = null;
   private boolean promptOverwrite = false;
   private boolean agree = false;
   private PassEditor gui;
   private Vector<Student> students;
   private File pdfFile;

   public static final String CHARSET_NAME = "UTF-8";
   public static final int CURRENT_FILE_VERSION_INDEX = 0;
   public static final String[] FILE_VERSIONS = new String[] {"1.0"};
}
