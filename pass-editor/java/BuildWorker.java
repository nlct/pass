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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URISyntaxException;

import com.dickimawbooks.passlib.*;
import com.dickimawbooks.passguilib.*;

public class BuildWorker extends AssignmentProcessWorker
{
   public BuildWorker(PassEditor gui)
   {
      super(gui);
      gui.setCurrentProcess(assignmentProcess);
   }

   public File doInBackground()
     throws IOException,
     InterruptedException,
     URISyntaxException,
     AgreementRequiredException
   {
      PassEditor gui = (PassEditor)main;
      gui.setIndeterminateProgress(true);

      gui.messageLn(gui.getMessage("message.build"));
      gui.enableTools(false);

      Project project = gui.getProject();
      PassTools passTools = main.getPassTools();

      // Does the project have any result files?

      AssignmentData assignment = project.getAssignment();
      File dir = project.getBase().toFile();

      StringWriter strWriter = new StringWriter();
      StringBuffer buffer = null;
      PrintWriter out = null;
      BufferedReader reader = null;

      exitCode = AssignmentProcess.EXIT_UNSET;

      try
      {
         out = new PrintWriter(strWriter);

         exitCode = assignmentProcess.runTestsNoPDF(out, dir);
      }
      finally
      {
         buffer = strWriter.getBuffer();

         if (out != null)
         {
            out.close();
         }

         if (reader != null)
         {
            reader.close();
         }

         gui.addResultFiles(assignment.getResultFiles(), true);
      }

      if (buffer != null)
      {
         StringBuilder msgs = new StringBuilder();

         String text = "\\begin{lstlisting}[numbers=none,language={bash}]";
         int startIdx = buffer.indexOf(text);
         int endIdx = 0;

         if (startIdx >= 0)
         {
            startIdx += text.length();
            text = "\\end{lstlisting}";

            endIdx = buffer.indexOf(text, startIdx);

            if (endIdx == -1)
            {
               endIdx = buffer.length();
            }

            gui.verbatim(buffer.substring(startIdx, endIdx));
         }
         else
         {
            startIdx = 0;
         }

         text = "\\verbatiminput{";

         startIdx = buffer.indexOf(text, endIdx);

         while (startIdx > 0)
         {
            startIdx += text.length();
            endIdx = buffer.indexOf("}", startIdx);

            if (endIdx == -1)
            {
               break;
            }

            String filename = buffer.substring(startIdx, endIdx);

            File file = new File(dir, filename);

            reader = passTools.newBufferedReader(file);
            int cp;

            msgs.setLength(0);

            while ((cp = reader.read()) != -1)
            {
               msgs.appendCodePoint(cp);
            }

            gui.buildMessages(msgs);

            reader = null;

            startIdx = buffer.indexOf(text, endIdx);
         }
      }

      Path basePath = project.getBase();
      File resultsDir = assignmentProcess.getResultsDir();

      if (resultsDir != null && !basePath.equals(resultsDir.toPath()))
      {
         assignmentProcess.copyResultFiles(basePath,
           StandardCopyOption.REPLACE_EXISTING);
      }

      return null;
   }

   protected void done()
   {
      try
      {
         get();

         switch (exitCode)
         {
            case AssignmentProcess.EXIT_UNSET:
            case AssignmentProcess.EXIT_TIMEDOUT:
            case AssignmentProcess.EXIT_CANCELLED:
            break;
            default:
             ((PassEditor)main).messageLn(main.getMessage("message.build_finished", exitCode));
         }
      }
      catch (Exception e)
      {
         main.error(e);
      }
      finally
      {
         main.setIndeterminateProgress(false);
         ((PassEditor)main).finishedBuild();
      }
   }

   private int exitCode;
}
