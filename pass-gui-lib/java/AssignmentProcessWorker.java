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
package com.dickimawbooks.passguilib;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.SwingWorker;

import com.dickimawbooks.passlib.*;

/**
 * SwingWorker used to process assignment in a separate thread.
 */ 
public class AssignmentProcessWorker extends SwingWorker<File,Void>
   implements ProgressListener
{
   /**
    * Create a new instance.
    * @param main the main Pass GUI application
    */ 
   public AssignmentProcessWorker(PassGui main)
   {
      this.main = main;
      addPropertyChangeListener(main);

      assignmentProcess = new AssignmentProcess(main, this);
   }

   @Override
   public void setIndeterminate(boolean state)
   {
      main.setIndeterminateProgress(state);
   }

   @Override
   public void updateProgress(int progress)
   {
      setProgress(progress);
   }

   @Override
   public File doInBackground()
     throws IOException,
     InterruptedException,
     URISyntaxException,
     AgreementRequiredException
   {
      return assignmentProcess.createPdf();
   }

   @Override
   protected void done()
   {
      File tmpPdfFile = null;
      boolean success = false;

      try
      {
         tmpPdfFile = get();

         if (tmpPdfFile != null && tmpPdfFile.exists())
         {
            success = true;
         }
      }
      catch (Exception e)
      {
         main.error(e);
      }

      main.finished(success, tmpPdfFile);
   }

   /**
    * Gets the assignment process object.
    * @return the assignment process
    */ 
   public AssignmentProcess getProcess()
   {
      return assignmentProcess;
   }

   /**
    * Interrupts the current process if it's running.
    * @return false if the task couldn't be cancelled, typically
    * because the task has already been completed, or true otherwise
    */ 
   public boolean interrupt()
   {
      if (assignmentProcess != null && !assignmentProcess.interrupt())
      {
         return cancel(true);
      }

      return false;
   }

   protected PassGui main;

   protected AssignmentProcess assignmentProcess;
}
