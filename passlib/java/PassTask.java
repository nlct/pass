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

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.CancellationException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.Timer;

/**
 * A task that needs to run as a process. The output can be logged in a
 * file. Optionally, there can also be a file that contains lines
 * that should be passed to the process via STDIN and a separate
 * file for STDERR.
 */

public class PassTask
{
   /**
    * Creates a new instance. The output file may be null, but if it
    * isn't, both STDOUT and STDERR messages will be written to it.
    * @param pass the Pass application
    * @param timeout the process's timeout (in milliseconds)
    * @param processBuilder the process builder that needs to be
    * started
    * @param outputFile the output file to send STDOUT and STDERR
    * messages (may be null)
    */ 
   public PassTask(Pass pass, long timeout, ProcessBuilder processBuilder,
       File outputFile)
   {
      this(pass, timeout, processBuilder, outputFile, null, null);
   }

   /**
    * Creates a new instance. The output file may be null, but if it
    * isn't, the STDOUT messages will be written to it.
    * @param pass the Pass application
    * @param timeout the process's timeout (in milliseconds)
    * @param processBuilder the process builder that needs to be
    * started
    * @param outputFile the output file to send STDOUT messages (may be null)
    * @param errFile the output file to send STDERR messages (if null, 
    * they will be redirected to outputFile if that isn't null)
    * @param inFile the input file containing STDIN messages (may be null)
    */ 
   public PassTask(Pass pass, long timeout, ProcessBuilder processBuilder, 
     File outputFile, File errFile, File inFile)
   {
      this.pass = pass;
      this.timeout = timeout;
      this.processBuilder = processBuilder;
      this.outputFile = outputFile;
      this.errFile = errFile;
      this.inFile = inFile;
   }

   /**
    * Starts the process with the require timeout.
    * @return the exit code 
    * @throws IOException if I/O error occurs
    * @throws InterruptedException if interruption occurs
    */ 
   public int performProcess() throws IOException,InterruptedException
   {
      int exitCode = -1;

      BufferedInputStream stdOutReader = null;

      try
      {
         if (outputFile != null)
         {
            if (errFile == null)
            {
               processBuilder.redirectErrorStream(true);
            }

            processBuilder.redirectOutput(outputFile);

            stdOutReader = new BufferedInputStream(new FileInputStream(outputFile));
         }

         if (errFile != null)
         {
            processBuilder.redirectError(errFile);
         }

         if (inFile != null)
         {
            processBuilder.redirectInput(inFile);
         }

         processStatus=STATUS_OK;

         Timer timer = new Timer((int)timeout, new ActionListener()
         {
            public void actionPerformed(ActionEvent evt)
            {
               processStatus=STATUS_TIMEOUT;
            }
         });

         timer.setRepeats(false);
         timer.start();

         Process p = processBuilder.start();

         while (p.isAlive())
         {
            if (stdOutReader != null && stdOutReader.available() > 0)
            {
               int c = stdOutReader.read();

/*
               if (c != -1)
               {
                  pass.verboseCodePoint(c);
               }
*/
            }
            else
            {
               Thread.sleep(SLEEP_INTERVAL);
            }

            if (processStatus > 0)
            {
               p.destroy();

               if (processStatus == STATUS_TIMEOUT)
               {
                  throw new InterruptedByTimeoutException();
               }
               else
               {
                  throw new CancellationException();
               }
            }
         }

         exitCode = p.exitValue();

         if (stdOutReader != null)
         {
            int c;

            while ((c = stdOutReader.read()) != -1)
            {
/*
               pass.verboseCodePoint(c);
*/
            }
         }
      }
      finally
      {
         if (stdOutReader != null)
         {
            stdOutReader.close();
         }
      }

      return exitCode;
   }

   /**
    * Signals that the current process should be cancelled.
    * May be called by a button provided for the user to cancel.
    * Doesn't have an instant effect but depends on the sleep
    * interval.
    */ 
   public void interrupt()
   {
      processStatus = STATUS_CANCELLED;
   }

   public static final int STATUS_OK=0, STATUS_TIMEOUT=1, STATUS_CANCELLED=2;
   public static final int SLEEP_INTERVAL=100;// milliseconds
   private volatile int processStatus=STATUS_OK;
   private long timeout;// milliseconds
   private ProcessBuilder processBuilder;
   private File outputFile = null;
   private File errFile = null;
   private File inFile = null;

   private Pass pass;
}
