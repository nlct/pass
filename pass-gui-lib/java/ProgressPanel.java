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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.JProgressBar;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;

/**
 * A panel that contains a progress bar, a time elapsed field, and an abort button.
 */
public class ProgressPanel extends JPanel implements ActionListener
{
   /**
    * Creates a new instance.
    * @param gui the Pass GUI application
    */ 
   public ProgressPanel(PassGui gui)
   {
      super(new BorderLayout());
      this.gui = gui;

      setBorder(BorderFactory.createLoweredSoftBevelBorder());

      infoField = new JLabel(gui.getMessage("process.subtask_aborted"));
      infoField.setVisible(false);

      progressBar = new JProgressBar();

      add(progressBar, "Center");

      JPanel panel = new JPanel();
      add(panel, "West");

      panel.add(gui.createJButton(
       gui.getToolIcon("general/Stop", gui.getMessage("process.abort"), true),
       "abort", this, this, null, false, gui.getToolTipMessage("process.abort"), true));

      panel.add(infoField);

      timerLabel = new JLabel("00:00");
      add(timerLabel, "East");

      timerListener = new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {
            time += delay;

            int seconds = (int)(time/1000L);
            int minutes = seconds/60;
            seconds = seconds%60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
         }
      };
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null) return;

      if (command.equals("abort"))
      {
         abortCurrentTask();
      }
   }

   /**
    * Aborts the current task.
    * Prompts the user for confirmation.
    * @return false if user didn't confirm otherwise true
    */ 
   public boolean abortCurrentTask()
   {
      if (task == null || task.isDone())
      {
         endProgress();
         return true;
      }

      if (gui.confirm(gui.getMessage("confirm.abort"),
                gui.getMessage("confirm.abort.title"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
      {
         if (task.interrupt())
         {
            endProgress();
            gui.finished(false, null);
         }
         else
         {
            infoField.setVisible(true);
         }

         return true;
      }

      return false;
   }

   /**
    * Sets the progress bar to the given value.
    * @param value the progress value
    */ 
   public void setValue(int value)
   {
      progressBar.setValue(value);
   }

   /**
    * Gets the progress bar value.
    * @return the progress value
    */ 
   public int getValue()
   {
      return progressBar.getValue();
   }

   /**
    * Indicates whether or not the progress is indeterminate.
    * @return true if indeterminate
    */ 
   public boolean isIndeterminate()
   {
      return progressBar.isIndeterminate();
   }

   /**
    * Sets whether or not the progress is indeterminate.
    * @param state true if indeterminate
    */ 
   public void setIndeterminate(boolean state)
   {
      progressBar.setIndeterminate(state);
   }

   /**
    * Called when a new task starts. Resets the time, hides the info
    * field and sets this panel to visible.
    * @param task the new task
    */ 
   public void startProgress(AssignmentProcessWorker task)
   {
      this.task = task;
      timerLabel.setText("00:00");

      infoField.setVisible(false);
      setVisible(true);
      time = 0L;

      if (timer == null)
      {
         timer = new Timer(delay, timerListener);
         timer.start();
      }
      else
      {
         timer.restart();
      }
   }

   /**
    * Called when the task has stopped. Stops the timer and sets the
    * panel to not visible.
    */ 
   public void endProgress()
   {
      setVisible(false);
      stopTimer();
      task = null;
   }

   /**
    * Stops the timer.
    */ 
   public void stopTimer()
   {
      if (timer != null && timer.isRunning())
      {
         timer.stop();
      }
   }

   private PassGui gui;
   private JProgressBar progressBar;
   private JLabel timerLabel;
   private Timer timer;
   private ActionListener timerListener;
   private long time = 0;
   private static final int delay = 1000; // milliseconds
   private AssignmentProcessWorker task;
   private JLabel infoField;
}
