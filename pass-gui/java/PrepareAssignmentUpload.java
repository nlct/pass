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
package com.dickimawbooks.passgui;

import java.io.*;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.util.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.lang.reflect.InvocationTargetException;

import java.awt.event.*;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.Desktop;
import java.awt.Cursor;
import java.awt.Insets;
import javax.swing.*;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import com.dickimawbooks.passlib.*;
import com.dickimawbooks.passguilib.*;

/**
 * The main Pass GUI.
 */
public class PrepareAssignmentUpload extends JFrame
  implements ActionListener,PassGui,HyperlinkListener
{
   /**
    * Creates a new instance.
    * @param debug true if debug mode should be on
    */ 
   public PrepareAssignmentUpload(int debug)
     throws SAXException,IOException
   {
      super(LONG_TITLE);
      this.debugLevel = debug;

      passGuiTools = new PassGuiTools(this);

      ImageIcon ic = getLogoIcon();

      if (ic != null)
      {
         setIconImage(ic.getImage());
      }

      messageArea = passGuiTools.createMessageArea("", 10, 40);
      messageAreaSp = new JScrollPane(messageArea);

      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            quit();
         }
      });

      Cursor orgCursor = getCursor();
      setCursor(new Cursor(Cursor.WAIT_CURSOR));

      Locale locale = Locale.getDefault();
      passTools = new PassTools(this, locale);
      passTools.loadDictionary("passguilib", locale);
      passTools.loadDictionary("progassignsys", locale);

      try
      {
         loadProperties();

         String lookandfeel = properties.getProperty("lookandfeel");

         if (lookandfeel != null)
         {
            LookAndFeel current = UIManager.getLookAndFeel();

            if (!lookandfeel.equals(current.getClass().getName()))
            {
               UIManager.setLookAndFeel(lookandfeel);
               SwingUtilities.updateComponentTreeUI(this);
               SwingUtilities.updateComponentTreeUI(messageAreaSp);
            }
         }

         properties.setProperty("lookandfeel", 
           UIManager.getLookAndFeel().getClass().getName());
      }
      catch (Exception e)
      {
         error(e);
      }

      try
      {
         readXML();

         initGUI();
      }
      catch (Exception e)
      {
         fatalError(e);
      }
      finally
      {
         setCursor(orgCursor);
      }

      setLocationRelativeTo(null);

      if (helpFrame != null)
      {
         helpFrame.setSize(getSize());
         helpFrame.setLocationRelativeTo(null);
      }

      transFrame.setSize(getSize());

      setVisible(true);
   }

   /**
    * Gets the logo.
    * @return the logo image icon
    */ 
   public ImageIcon getLogoIcon()
   {
      return getIcon("pass-logo-32x32.png");
   }

   /**
    * Gets an icon with the given name.
    * @return the icon or null if not found
    */ 
   public ImageIcon getIcon(String name)
   {
      String filename = "/icons/"+name;

      java.net.URL imgURL = getClass().getResource(filename);

      if (imgURL != null)
      {
         return new ImageIcon(imgURL);
      }

      debug("Can't find icon "+filename);
      return null;
   }

   /**
    * Indicates if debug mode is on.
    * If true, the debugging courses will be available.
    * @return true if debug mode on
    */ 
   @Override
   public boolean isDebugMode()
   {
      return debugLevel > 0;
   }

   /**
    * Gets the debugging level.
    * @return 0 if debug mode off otherwise the debugging level
    */ 
   public int getDebugLevel()
   {
      return debugLevel;
   }

   /**
    * Writes a message to the transcript and STDOUT if debugging on.
    * A newline will be appended to the message.
    * @param message the message
    */ 
   @Override
   public void debug(String message)
   {
      if (debugLevel > 0)
      {
         transcriptMessage(message);
         System.out.println(message);
      }
   }

   /**
    * Writes a message to the transcript and STDOUT if debugging on.
    * Doesn't append a newline.
    * @param message the message
    */ 
   @Override
   public void debugNoLn(String message)
   {
      if (debugLevel > 0)
      {
         transcriptMessageNoLn(message);
         System.out.print(message);
      }
   }

   /**
    * Close down and exit the application.
    * Removes temporary files, saves user preferences and exits.
    */ 
   private void quit()
   {
      try
      {
         passTools.closeDown();

         saveProperties();
      }
      catch (Exception e)
      {
         error(e);
      }

      System.exit(0);
   }

   /**
    * Gets the property file used to store the user preferences.
    * The file is determined as follows:
    * <ul>
    * <li>If the environment variable <code>PASSPROP</code> is set,
    * that value is used, unless it's a directory.
    * <li>If the value <code>PASSPROP</code> is a path to a directory then 
    * the file will be within that directory with the filename given
    * by DEFAULT_PROP_NAME.
    * <li>Otherwise the file will be in the user's home directory
    * with the filename given by DEFAULT_PROP_NAME.
    * </ul>
    * @return the property file
    */ 
   private File getPropertyFile()
   {
      try
      {
         String filename = System.getenv("PASSPROP");

         if (filename != null && !filename.isEmpty())
         {
            File file = new File(filename);

            if (file.isDirectory())
            {
               file = new File(file, DEFAULT_PROP_NAME);
            }

            return file;
         }
      }
      catch (SecurityException e)
      {
      }

      File home = new File(System.getProperty("user.home", "."));

      return new File(home, DEFAULT_PROP_NAME);
   }

   /**
    * Gets the submitted date. Not enabled for this application.
    * @return null
    */ 
   @Override
   public Date getSubmittedDate()
   {
      return null;
   }

   /**
    * Gets the user's preferred startup directory.
    * The project files should be somewhere on this path to make it
    * easier to find them.
    * @return the startup directory
    */ 
   public File getStartUpDirectory()
   {
      String prop = properties.getProperty("startup.setting", "last");

      if (prop.equals("home"))
      {
         return new File(System.getProperty("user.home", "."));
      }

      if (prop.equals("last"))
      {
         return getLastDirectory();
      }

      prop = properties.getProperty("startup.dir");

      if (prop == null)
      {
         prop = System.getProperty("user.home", ".");
         properties.setProperty("startup.dir", prop);
      }

      return new File(prop);
   }

   /**
    * Gets a keystroke defined in the properties.
    * @param prop the property
    * @return the keystroke or null if not found
    */ 
   public KeyStroke getKeyStrokeProperty(String prop)
   {
      return getKeyStrokeProperty(prop, null);
   }

   /**
    * Gets a keystroke defined in the properties.
    * @param prop the property
    * @param defVal default if not found
    * @return the keystroke
    */ 
   public KeyStroke getKeyStrokeProperty(String prop, KeyStroke defVal)
   {
      String val = properties.getProperty(prop);

      if (val == null || val.isEmpty())
      {
         return defVal;
      }

      KeyStroke keyStroke = KeyStroke.getKeyStroke(val);

      if (keyStroke == null)
      {
         debug(String.format("Invalid value '%s' for keystroke property '%s'",
           val, prop));

         return defVal;
      }

      return keyStroke;
   }

   /**
    * Gets the user's preferred startup property value.
    * @return the startup directory property value or null if not
    * set
    */ 
   public String getStartupDirectoryProperty()
   {
      return properties.getProperty("startup.dir");
   }

   /**
    * Sets the look &amp; feel for the GUI.
    * @param lookandfeel the L&amp;F name
    */ 
   public void setUI(String lookandfeel) 
    throws ClassNotFoundException,
           InstantiationException,
           IllegalAccessException,
           UnsupportedLookAndFeelException
   {
      UIManager.setLookAndFeel(lookandfeel);
      SwingUtilities.updateComponentTreeUI(this);
      SwingUtilities.updateComponentTreeUI(messageAreaSp);

      if (helpFrame != null)
      {
         SwingUtilities.updateComponentTreeUI(helpFrame);
      }

      if (transFrame != null)
      {
         SwingUtilities.updateComponentTreeUI(transFrame);
      }

      if (propertiesDialog != null)
      {
         SwingUtilities.updateComponentTreeUI(propertiesDialog);
      }

      properties.setProperty("lookandfeel", lookandfeel);
   }

   /**
    * Gets the course.
    * This will Fetch the course information from the resources.xml 
    * file if not already loaded. If more than one course is listed,
    * the student will be prompted to select the correct one.
    */ 
   public Course fetchCourse() throws IOException,SAXException
   {
      if (course != null)
      {
         return course;
      }

      URL resourceURL = getClass().getResource("/resources.xml");

      Vector<Course> data = passTools.loadCourseData(resourceURL);

      if (!passTools.isAgreeRequired())
      {
         error(passTools.getMessage("error.agree_not_permitted", 
           SHORT_TITLE, resourceURL));
         System.exit(1);
      }

      int n = data.size();

      if (n == 0)
      {
         error(passTools.getMessage("error.no_course_data", resourceURL));
         System.exit(1);
      }

      if (n == 1)
      {
         course = data.firstElement();
      }
      else
      {
         String defCourse = properties.getProperty("default.course");
         Course defValue = null;

         Course[] values = new Course[n];

         for (int i = 0; i < n; i++)
         {
            values[i] = data.get(i);

            if (values[i].getCode().equals(defCourse))
            {
               defValue = values[i];
            }
         }

         Object result = JOptionPane.showInputDialog(this, 
           passTools.getMessage("message.select_course"),
           passTools.getMessage("message.select_course.title"),
            JOptionPane.QUESTION_MESSAGE, null, 
            values, defValue);

         if (result == null)
         {
            System.exit(0);
         }

         course = (Course)result;
      }

      return course;
   }

   /**
    * Gets the startup directory property setting.
    * @return the startup directory setting data
    */ 
   public StartupDirType getStartupDirType()
   {
      String prop = properties.getProperty("startup.setting");

      if ("home".equals(prop))
      {
         return StartupDirType.HOME;
      }

      if ("custom".equals(prop))
      {
         return StartupDirType.CUSTOM;
      }

      return StartupDirType.LAST;
   } 

   /**
    * Sets the startup directory property setting to home.
    */ 
   public void setStartUpDirectoryHome()
   {
      properties.setProperty("startup.setting", "home");
   }

   /**
    * Sets the startup directory property setting to last.
    */ 
   public void setStartUpDirectoryLast()
   {
      properties.setProperty("startup.last", "home");
   }

   /**
    * Sets the startup directory.
    * @param dir the directory
    * @param type the setting type
    */ 
   public void setStartUpDirectory(File dir, StartupDirType type)
   {
      setStartUpDirectory(dir.getAbsolutePath(), type);
   }

   /**
    * Sets the startup directory.
    * @param dirname the directory path
    * @param type the setting type
    */ 
   public void setStartUpDirectory(String dirname, StartupDirType type)
   {
      properties.setProperty("startup.dir", dirname);

      switch (type)
      {
         case LAST:
           properties.setProperty("startup.setting", "last");
         break;
         case HOME:
           properties.setProperty("startup.setting", "home");
         break;
         case CUSTOM:
           properties.setProperty("startup.setting", "custom");
         break;
      }
   }

   /**
    * Gets the last directory saved in the user settings.
    * @return the directory
    */ 
   public File getLastDirectory()
   {
      String prop = properties.getProperty("startup.last",
         System.getProperty("user.home", "."));

      return new File(prop);
   }

   /**
    * Sets the last directory. This will be saved in the user
    * settings on exit.
    * @param dir the directory
    */ 
   public void setLastDirectory(File dir)
   {
      properties.setProperty("startup.last", dir.getAbsolutePath());
   }

   /**
    * Gets the username saved in the user settings.
    * @return the username or an empty string if not saved
    */ 
   public String getStudentIdProperty()
   {
      return properties.getProperty("student.id", "");
   }

   /**
    * Sets the username. This will be saved in the user
    * settings on exit.
    * @param name the student's username
    */ 
   public void setStudentIdProperty(String name)
   {
      properties.setProperty("student.id", name);
   }

   /**
    * Gets the registration number saved in the user settings.
    * @return the registration number or an empty string if not saved
    */ 
   public String getStudentNumberProperty()
   {
      return properties.getProperty("student.number", "");
   }

   /**
    * Sets the registration number. This will be saved in the user
    * settings on exit.
    * @param name the student's registration number
    */ 
   public void setStudentNumberProperty(String value)
   {
      properties.setProperty("student.number", value);
   }

   /**
    * Sets the user's preferred encoding default.
    * @param value the encoding
    */ 
   public void setEncodingProperty(String value)
   {
      properties.setProperty("src.encoding", value);
   }

   /**
    * Gets the user's preferred encoding default.
    * @param value the preferred encoding or the default
    */ 
   public String getEncodingProperty()
   {
      String val = properties.getProperty("src.encoding", 
        ENCODING_UTF8);

      if (val.equals(ENCODING_UTF8) || val.equals(ENCODING_ASCII)
           || val.equals(ENCODING_LATIN1))
      {
         return val;
      }

      if (val.equals("ASCII"))
      {
         return ENCODING_ASCII;
      }
      else if (val.equals("Latin 1") || val.equals("Latin-1"))
      {
         return ENCODING_LATIN1;
      }
      else if (val.equals("UTF8") || val.equals("utf8"))
      {
         return ENCODING_UTF8;
      }

      debug(String.format("Unknown encoding '%s'", val));

      return ENCODING_ASCII;
   }

   @Override
   public void setTimeOut(long value)
   {
      timeout = value;
      disableTimeOutProp = true;
   }

   /**
    * Sets the user's timeout preference.
    * @param value the timeout in seconds
    */ 
   public void setTimeOutProperty(long value)
   {
      properties.setProperty("timeout", ""+value);
      timeout = value;
   }

   @Override
   public long getTimeOut()
   {
      return timeout;
   }

   /**
    * Gets the user's timeout preference.
    * @return the timeout in seconds
    */ 
   public long getTimeOutProperty()
   {
      String val = properties.getProperty("timeout");

      if (val == null)
      {
         return timeout;
      }

      try
      {
         return Long.parseLong(val);
      }
      catch (NumberFormatException e)
      {
         debug("Invalid timeout property '"+val+"'");
         setTimeOutProperty(timeout);
         return timeout;
      }
   }

   /**
    * Gets the maximum number of files to search.
    * @return maximum
    */ 
   @Override
   public int getFileSearchMax()
   {
      String val = properties.getProperty("searchmax");

      if (val == null)
      {
         return FILE_SEARCH_MAX;
      }

      try
      {
         return Integer.parseInt(val);
      }
      catch (NumberFormatException e)
      {
         debug("Invalid searchmax property '"+val+"'");
         setFileSearchMax(FILE_SEARCH_MAX);

         return FILE_SEARCH_MAX;
      }
   }

   /**
    * Sets the maximum number of files to search.
    * @param value the maximum
    */ 
   public void setFileSearchMax(int val)
   {
      properties.setProperty("searchmax", ""+val);
   }

   /**
    * Loads the file containing the user's preferences.
    * @throws IOException if I/O error occurs
    */ 
   private void loadProperties() throws IOException
   {
      File file = getPropertyFile();
      properties = new Properties();

      if (file == null || !file.exists())
      {
         return;
      }

      BufferedReader in = null;

      try
      {
         in = new BufferedReader(new FileReader(file));

         properties.load(in);

         timeout = getTimeOutProperty();
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
    * Saves the file containing the user's preferences.
    * @throws IOException if I/O error occurs
    */ 
   private void saveProperties() throws IOException
   {
      setLastDirectory(dirSelector.getCurrentDirectory());

      String studentId = studentIdField.getText();
      setStudentIdProperty(studentId);

      String studentNumber = studentNumberField.getText();

      if (!studentNumber.isEmpty())
      {
         setStudentNumberProperty(studentNumber);
      }

      setEncodingProperty(encodingBox.getSelectedItem().toString());

      File file = getPropertyFile();

      if (file != null)
      {
         PrintWriter writer = null;

         try
         {
            writer = new PrintWriter(file);

            properties.store(writer, String.format("%s Properties", SHORT_TITLE));
         }
         finally
         {
            if (writer != null)
            {
               writer.close();
            }
         }
      }
   }

   /**
    * Update the main window's title.
    * The title is either just the application name or the
    * application name followed by the name of the last component in
    * the main panel.
    */ 
   public void updateTitle()
   {
      String name = mainPanel.getComponent(currentPanel-1).getName();

      if (name == null)
      {
         setTitle(String.format("%s", LONG_TITLE));
      }
      else
      {
         setTitle(String.format("%s - %s", LONG_TITLE, name));
      }
   }

   /**
    * Gets the default text for the top area.
    */ 
   private String getDefaultTopText()
   {
      return passTools.getMessage("message.select_assignment_for_course", 
        course.getCode());
   }

   /**
    * Sets the cursor to the wait cursor for this window and the
    * transcript window. 
    */ 
   public void setWaitCursor()
   {
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      transFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
   }

   /**
    * Sets the cursor to the default cursor for this window and the
    * transcript window. 
    */ 
   public void restoreCursor()
   {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      transFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   }

   /**
    * Writes a character in the transcript. 
    */ 
   @Override
   public void verboseCodePoint(int cp)
   {
      if (transFrame == null) return;

      if (cp <= Character.MAX_VALUE)
      {
         transFrame.messageNoLn((char)cp);
      }
      else
      {
         transFrame.messageNoLn(new String(Character.toChars(cp)));
      }
   }

   /**
    * Writes a message in the transcript. 
    * Verbose mode is always on for Pass GUI.
    */ 
   @Override
   public void verbose(String msg)
   {
      transcriptMessage(msg);
   }

   /**
    * Writes a character in the transcript. 
    */ 
   public void transcriptMessage(char c)
   {
      if (transFrame != null)
      {
         transFrame.messageNoLn(c);
      }
   }

   /**
    * Writes a message followed by newline in the transcript. 
    */ 
   @Override
   public void transcriptMessage(String message)
   {
      if (transFrame != null)
      {
         transFrame.message(message);
      }
   }

   /**
    * Writes a message in the transcript without appending newline. 
    */ 
   public void transcriptMessageNoLn(String message)
   {
      if (transFrame != null)
      {
         transFrame.messageNoLn(message);
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("previous".equals(action))
      {
         if (fileSearcher != null)
         {
            if (confirm(getMessage("message.abort_search"),
              getMessage("message.abort_search.title"), 
              JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
               cancelFileSearch();
            }
            else
            {
               return;
            }
         }

         if (currentPanel > CONFIRM_PANEL)
         {
            layout.previous(mainPanel);
            currentPanel--;

            prevButton.setEnabled(currentPanel > CONFIRM_PANEL);
            enableNextButton(true);

            if (currentPanel == ASSIGNMENT_PANEL)
            {
               topField.setText(getDefaultTopText());
            }
            else if (currentPanel == CONFIRM_PANEL)
            {
               topField.setText(course.getCode());
            }
         }

         updateTitle();
      }
      else if ("next".equals(action))
      {
         if (fileSearcher != null)
         {
            if (confirm(getMessage("message.abort_search"),
              getMessage("message.abort_search.title"), 
              JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
               cancelFileSearch();
            }
            else
            {
               return;
            }
         }

         if (currentPanel < maxPanels)
         {
            if (currentPanel == CONFIRM_PANEL)
            {
               if (!confirmBox.isSelected())
               {
                  return;
               }

               topField.setText(getDefaultTopText());
            }
            else if (currentPanel == ASSIGNMENT_PANEL)
            {
               if (groupProjectButton.isSelected())
               {
                  int numStudents = 0;

                  TableModel model = groupProjectTable.getModel();

                  for (int row = 0; row < model.getRowCount(); row++)
                  {
                     String id = model.getValueAt(row, 0).toString();
                     String num = model.getValueAt(row, 1).toString();

                     boolean numFound = !num.isEmpty();
                     boolean idFound = !id.isEmpty();

                     if (idFound && numFound)
                     {
                        if (!passTools.isValidRegNum(num))
                        {
                           error(passTools.getMessage("error.invalid_input",
                             passTools.getConfig().getRegNumText(), num));
                           return;
                        }

                        if (!passTools.isValidUserName(id))
                        {
                           error(passTools.getMessage("error.invalid_input",
                            passTools.getConfig().getUserNameText(), id));
                           return;
                        }

                        numStudents++;
                     }
                     else if (idFound && !numFound)
                     {
                        error(passTools.getMessage("error.missing_input_for",
                          passTools.getConfig().getUserNameText(), id));
                        return;
                     }
                     else if (!idFound && numFound)
                     {
                        error(passTools.getMessage("error.missing_input_for",
                          passTools.getConfig().getRegNumText(), num));
                        return;
                     }
                  }

                  if (numStudents < 2)
                  {
                     error(passTools.getMessage("error.group_project_min", 2));
                     return;
                  }
               }
               else
               {
                  if (studentNumberField.getText().isEmpty())
                  {
                     error(passTools.getMessage("error.missing_input",
                        passTools.getConfig().getRegNumText()));
                     return;
                  }

                  if (studentIdField.getText().isEmpty())
                  {
                     error(passTools.getMessage("error.missing_input", 
                       passTools.getConfig().getUserNameText()));
                     return;
                  }
               }

               transcriptMessage(passTools.getMessage("message.assignment",
                 assignmentBox.getSelectedItem(), dueField.getText()));

               if (getAssignment().isRelativePathsDefaultOn())
               {
                  relativizeBox.setSelected(true);
               }

               createRequiredComponents();
            }
            else if (currentPanel == DIR_PANEL)
            {
               if (getAssignment().isRelativePathsDefaultOn())
               {
                  if (!relativizeBox.isSelected())
                  {
                     error(getMessage("error.relpath_not_set",
                       getMessage("message.use_rel_paths")));
                     return;
                  }
                  else if (getBasePath() == null)
                  {
                     error(getMessage("error.relpath_base_not_set"));
                     return;
                  }
               }

               transcriptMessage(passTools.getMessage(
                 "message.base_directory",
                 dirField.getText(), useRelativePaths()));

               updateFileComponents();
            }
            else if (currentPanel == FILE_LIST_PANEL)
            {
               try
               {
                  String msg = checkSuppliedFiles();

                  if (!msg.isEmpty())
                  {

                     if (confirm(passTools.getMessage(
                      "message.file_problems.confirm", msg),
                      passTools.getMessage("message.confirm_continue.title"),
                       JOptionPane.YES_NO_OPTION) 
                        != JOptionPane.YES_OPTION)
                     {
                        return;
                     }
                  }
               }
               catch (InvalidFileException e)
               {
                  error(e.getMessage());
                  return;
               }
            }

            layout.next(mainPanel);
            currentPanel++;

            enableNextButton(currentPanel < maxPanels, prevButton);
            prevButton.setEnabled(true);

            if (currentPanel == DIR_PANEL)
            {
               topField.setText(passTools.getMessage("message.assignment",
                 assignmentBox.getSelectedItem(),
                 dueField.getText()));
            }
            else if (currentPanel == PROCESS_PANEL)
            {
               enableNextButton(false);
               nextButton.setVisible(false);
               prevButton.setVisible(false);

               transFrame.setVisible(true);

               currentProcess = new AssignmentProcessWorker(this);
               progressPanel.startProgress(currentProcess);
               currentProcess.execute();
            }
         }

         updateTitle();
      }
      else if ("groupproject".equals(action))
      {
         if (groupProjectButton.isSelected())
         {
            idCardLayout.show(idPanel, "groupproject");

            TableModel model = groupProjectTable.getModel();

            for (int i = 0; i < model.getRowCount(); i++)
            {
               if (model.getValueAt(i, 0).toString().isEmpty())
               {
                  groupProjectTable.changeSelection(i, 0, false, false);
                  groupProjectTable.editCellAt(i, 0);
                  groupProjectTable.getEditorComponent().requestFocusInWindow();

                  break;
               }
            }
         }
         else
         {
            idCardLayout.show(idPanel, "soloproject");
         }
      }
      else if ("addstudent".equals(action))
      {
         DefaultTableModel model = (DefaultTableModel)groupProjectTable.getModel();

         int idx = groupProjectTable.getSelectionModel().getMaxSelectionIndex();

         if (idx == -1 || idx == model.getRowCount()-1)
         {
            model.addRow(new Object[] {"", ""});
         }
         else
         {
            model.insertRow(idx, new Object[] {"", ""});
         }
      }
      else if ("removestudent".equals(action))
      {
         DefaultTableModel model = (DefaultTableModel)groupProjectTable.getModel();

         for (int i = model.getRowCount()-1; i >= 0; i--)
         {
            if (groupProjectTable.isRowSelected(i))
            {
               model.removeRow(i);
            }
         }
      }
      else if ("movestudentup".equals(action))
      {
         DefaultTableModel model = (DefaultTableModel)groupProjectTable.getModel();

         for (int i = 1; i < model.getRowCount(); i++)
         {
            if (groupProjectTable.isRowSelected(i))
            {
               model.moveRow(i, i, i-1);
            }
         }
      }
      else if ("movestudentdown".equals(action))
      {
         DefaultTableModel model = (DefaultTableModel)groupProjectTable.getModel();

         for (int i = model.getRowCount()-2; i >= 0; i--)
         {
            if (groupProjectTable.isRowSelected(i))
            {
               model.moveRow(i, i, i+1);
            }
         }
      }
      else if ("confirmuse".equals(action))
      {
         enableNextButton(confirmBox.isSelected());
      }
      else if ("choosedir".equals(action))
      {
         if (dirSelector.showDialog(mainPanel, 
                passTools.getMessage("filefield.select"))
           == JFileChooser.APPROVE_OPTION)
         {
            File file = dirSelector.getSelectedFile();
            dirField.setText(file.toString());
            fileSelector.setCurrentDirectory(file);
         }
      }
      else if ("addfile".equals(action))
      {
         AdditionalFilePanel comp = new AdditionalFilePanel(this, fileSelector);
         additionalFileComp.add(comp);
         revalidate();

         JScrollBar bar = fileListSp.getVerticalScrollBar();
         bar.setValue(bar.getMaximum());

         comp.getTextComponent().requestFocusInWindow();
      }
      else if ("addbinaryfile".equals(action))
      {
         if (allowedBinaryFilters != null)
         {
            try
            {
               FileTextField comp = addBinaryFileComponent();
               revalidate();

               JScrollBar bar = fileListSp.getVerticalScrollBar();
               bar.setValue(bar.getMaximum());

               comp.getTextComponent().requestFocusInWindow();
            }
            catch (IOException e)
            {// shouldn't happen
               error(e);
            }
         }
      }
      else if ("quit".equals(action))
      {
         quit();
      }
      else if ("encoding".equals(action))
      {
         encodingInfo.setText(encodingInfoList[encodingBox.getSelectedIndex()]);
      }
      else if ("openpdf".equals(action))
      {
         if (savedFile != null)
         {
            try
            {
               passGuiTools.openPdf(savedFile);
            }
            catch (Throwable e)
            {
               error(e);
            }
         }
         else
         {// shouldn't happen
            error(getMessage("error.file_not_found"));
         }
      }
      else if ("openlog".equals(action))
      {
         if (tmpPdfFile != null)
         {
            File logFile = getLogFile();

            try
            {
               passGuiTools.openEditor(logFile);
            }
            catch (Throwable e)
            {
               error(e);
            }
         }
         else
         {// shouldn't happen
            error(getMessage("error.file_not_found"));
         }
      }
      else if ("save".equals(action))
      {
         try
         {
            savedFile = copyFile(tmpPdfFile, 
              new File(getDefaultPdfName()), getPdfFileChooser());

            if (savedFile != null)
            {
               debug("Saved "+savedFile);
               topField.setText(String.format("%s%n%s",
                  topField.getText(),
                  passTools.getMessage("message.file_saved", 
                    savedFile.getAbsolutePath())));
               saveButton.setVisible(false);
               openButton.setVisible(Desktop.isDesktopSupported());
               getRootPane().setDefaultButton(openButton);
            }
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if ("savetex".equals(action))
      {
         try
         {
            String name = tmpPdfFile.getName();

            name = name.substring(0, name.lastIndexOf("."))+".tex";
            File tmpTeXFile = new File(tmpPdfFile.getParentFile(), name);

            File file = copyFile(tmpTeXFile, new File(getDefaultTeXName()),
              getTeXFileChooser());

            if (file != null)
            {
               debug("Saved "+file);
            }
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if ("settings".equals(action))
      {
         propertiesDialog.display();
      }
      else if ("handbook".equals(action))
      {
         helpFrame.setVisible(true);
      }
      else if (action.equals("about"))
      {
         JOptionPane.showMessageDialog(this, 
           passTools.getMessage("message.about",
            FULL_TITLE, VERSION, VERSION_DATE, COPYRIGHT_OWNER, ABOUT_URL,
            PASSLIB_VERSION, PASSLIB_VERSION_DATE), 
           passTools.getMessage("message.about.title"),
           JOptionPane.INFORMATION_MESSAGE);
      }
      else if (action.equals("licence"))
      {
         JOptionPane.showMessageDialog(this, licenceComp, 
           passTools.getMessage("message.licence.title"),
           JOptionPane.PLAIN_MESSAGE);
      }
      else if (action.equals("transcript"))
      {
         transFrame.setLocationRelativeTo(this);
         transFrame.setVisible(true);
      }
      else if (action.equals("abortsearch"))
      {
         cancelFileSearch();
      }
      else
      {
         System.err.println(String.format("Unknown action '%s'", action));
      }
   }

   /**
    * Sets whether or not the next button is enabled.
    * @param enable if true, enable next button otherwise disable it
    */ 
   public void enableNextButton(boolean enable)
   {
      enableNextButton(enable, null);
   }

   /**
    * Sets whether or not the next button is enabled. If the next
    * button is enabled, it will be set as the default button
    * otherwise the other button will be set as the default
    * @param enable if true, enable next button otherwise disable it
    * @param other the other button that should be set as the
    * default if the next button isn't enabled
    */ 
   public void enableNextButton(boolean enable, JButton other)
   {
      nextButton.setEnabled(enable);

      if (nextButton.isEnabled())
      {
         getRootPane().setDefaultButton(nextButton);
      }
      else
      {
         getRootPane().setDefaultButton(other);
      }
   }

   /**
    * Gets the LaTeX log file.
    * @return the LaTeX log file or null if not yet processed
    */ 
   public File getLogFile()
   {
      if (tmpPdfFile == null) return null;

      String name = tmpPdfFile.getName();
      name = name.substring(0, name.lastIndexOf("."))+".log";
      return new File(tmpPdfFile.getParentFile(), name);
   }

   /**
    * Copies a file to a location chosen by the user.
    * This method will open a file chooser dialog for the user to
    * specify the destination.
    * @param src the source file to copy
    * @param dest the suggested destination
    * @return the actual destination or null if action cancelled
    */ 
   public File copyFile(File src, File dest)
     throws IOException
   {
      String name = dest.getName();
      int idx = name.lastIndexOf(".");

      String ext = "";

      if (idx > -1)
      {
         ext = name.substring(idx+1).toLowerCase();
      }

      if (ext.equals("pdf"))
      {
         return copyFile(src, dest, getPdfFileChooser());
      }
      else
      {
         return copyFile(src, dest, getTeXFileChooser());
      }
   }

   /**
    * Copies a file to a location chosen by the user.
    * This method will open the given file chooser for the user to
    * specify the destination.
    * @param src the source file to copy
    * @param dest the suggested destination
    * @param fileSelector the file chooser
    * @return the actual destination or null if action cancelled
    */ 
   public File copyFile(File src, File dest, JFileChooser fileSelector)
     throws IOException
   {
      return copyFile(src, dest, this, fileSelector);
   }

   /**
    * Copies a file to a location chosen by the user.
    * This method will open the given file chooser for the user to
    * specify the destination.
    * @param src the source file to copy
    * @param dest the suggested destination
    * @param parent the parent frame for the file chooser
    * @param fileSelector the file chooser
    * @return the actual destination or null if action cancelled
    */ 
   public File copyFile(File src, File dest,
      JFrame parent, JFileChooser fileSelector)
     throws IOException
   {
      fileSelector.setSelectedFile(dest);

      if (fileSelector.showSaveDialog(parent)
             == JFileChooser.APPROVE_OPTION)
      {
         dest = fileSelector.getSelectedFile();

         if (dest.exists())
         {
            switch (confirm(parent,
                 passTools.getMessage("message.confirm_overwrite",
                    dest.getAbsolutePath()),
                 passTools.getMessage("message.confirm_overwrite.title"),
                 JOptionPane.YES_NO_CANCEL_OPTION,
                 JOptionPane.WARNING_MESSAGE))
            {
               case JOptionPane.YES_OPTION:
               break;
               case JOptionPane.NO_OPTION:
                 return copyFile(src, dest, parent, fileSelector);
               default:
                 return null;
            }
         }

         Path target = Files.copy(src.toPath(), dest.toPath(),
             StandardCopyOption.REPLACE_EXISTING);

         return target.toFile();
      }

      return null;
   }

   /**
    * Gets the default filename for the PDF file.
    * @return the default PDF filename
    */
   public String getDefaultPdfName()
   {
      return String.format("%s.pdf", getDefaultBaseName());
   }

   /**
    * Gets the default filename for the LaTeX file.
    * @return the default LaTeX filename
    */
   public String getDefaultTeXName()
   {
      return String.format("%s.tex", getDefaultBaseName());
   }

   /**
    * Gets the default basename for the PDF and zip files.
    * @return the default basename
    */
   public String getDefaultBaseName()
   {
      return passTools.getConfig().getDefaultBaseName(getAssignment(),
        getStudent());
   }

   @Override
   public PassTools getPassTools()
   {
      return passTools;
   }

   @Override
   public PassGuiTools getPassGuiTools()
   {
      return passGuiTools;
   }

   @Override
   public String getMessage(String label, Object... params)
   {
      return passTools.getMessage(label, params);
   }

   @Override
   public int getMnemonic(String label)
   {
      return passTools.getMnemonic(label, -1);
   }

   @Override
   public String getToolTipMessage(String label, Object... params)
   {
      if (!label.endsWith(".tooltip"))
      {
         label += ".tooltip";
      }

      return passTools.getMessageWithDefault(label, null, params);
   }

   @Override
   public void propertyChange(PropertyChangeEvent evt)
   {
      if ("progress".equals(evt.getPropertyName()))
      {
         progressPanel.setValue((Integer)evt.getNewValue());
      }
   }

   @Override
   public void setIndeterminateProgress(boolean state)
   {
      progressPanel.setIndeterminate(state);
   }

   /**
    * Creates a non-editable text area messages with line wrapping.
    * @param text the text to go in the new text area
    * @return a new text area
    */ 
   private JTextArea createTextArea(String text)
   {
      JTextArea textArea = passGuiTools.createMessageArea(text);

      textArea.setOpaque(false);

      return textArea;
   }

   /**
    * Creates a non-editable editor pane for messages formatted in HTML.
    * @param text the HTML text to go in the new editor pane
    * @return a new editor pane
    */ 
   private JEditorPane createHTMLArea(String text)
   {
      JEditorPane pane = new JEditorPane();
      pane.setEditable(false);
      HTMLDocument htmlDoc = new HTMLDocument();
      HTMLEditorKit editorKit = new HTMLEditorKit();
      pane.setEditorKit(editorKit);
      pane.setText(text);

      pane.addHyperlinkListener(this);

      return pane;
   }

   @Override
   public void hyperlinkUpdate(HyperlinkEvent evt)
   {
      if (HyperlinkEvent.EventType.ACTIVATED.equals(evt.getEventType()))
      {
         try
         {
            passGuiTools.browse(evt.getURL().toURI());
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }
   }

   @Override
   public boolean isConfirmed()
   {
      return confirmBox.isSelected();
   }

   /**
    * Creates a component containing the confirmation checkbox.
    * @return the new component
    */ 
   private JComponent createConfirmComp()
   {
      Box box = Box.createVerticalBox();
      box.setAlignmentX(0);
      box.setName(passTools.getMessage("message.confirm_agree.title"));

      JEditorPane textArea = createHTMLArea(
       passTools.getMessage("message.confirm_blurb", SHORT_TITLE, DICKIMAW_PASS_FAQ)
      );
      textArea.setAlignmentX(0);
      box.add(textArea);

      confirmBox = new ConfirmCheckBox(this);
      confirmBox.setAlignmentX(0);
      confirmBox.setActionCommand("confirmuse");
      box.add(confirmBox);

      confirmBox.addActionListener(this);

      box.add(Box.createVerticalGlue());

      return box;
   }

   /**
    * Creates a component with the assignment selector.
    * @return the new component
    */ 
   private JComponent createAssignmentComp()
   {
      JComponent box = new JPanel(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      box.setAlignmentX(0);
      box.setName(passTools.getMessage("message.select_assignment.title"));

      gbc.gridx=0;
      gbc.gridy=0;
      gbc.anchor=GridBagConstraints.LINE_START;

      JLabel label = createJLabel("message.select_assignment");
      label.setAlignmentX(0);
      box.add(label, gbc);

      assignmentBox = new JComboBox<AssignmentData>(assignments);
      assignmentBox.setAlignmentX(0);

      gbc.gridx++;
      box.add(assignmentBox, gbc);
      label.setLabelFor(assignmentBox);

      JComponent panel = Box.createHorizontalBox();
      panel.setAlignmentX(0);
      gbc.gridx++;
      box.add(panel, gbc);

      label = createJLabel("message.assignment_due");
      label.setAlignmentX(0);
      panel.add(label);

      dueField = new JTextField();
      dueField.setAlignmentX(0);
      dueField.setEditable(false);
      panel.add(dueField);

      LocalDateTime now = LocalDateTime.now();

      for (int i = 0, n = assignments.size(); i < n; i++)
      {
         AssignmentData assignment = assignments.get(i);
         LocalDateTime due = assignment.getDueDate();

         assignmentBox.setSelectedIndex(i);
         dueField.setText(assignment.formatDueDate());

         if (due.isAfter(now))
         {
            break;
         }
      }

      assignmentBox.addItemListener(new ItemListener()
      {
         public void itemStateChanged(ItemEvent e)
         {
            AssignmentData data = 
              (AssignmentData)assignmentBox.getSelectedItem();
            dueField.setText(data.formatDueDate());
         }
      });

      gbc.gridx++;
      gbc.weightx=1;
      box.add(Box.createHorizontalStrut(20));

      gbc.weightx=0;
      gbc.gridx=0;
      gbc.gridwidth=3;
      gbc.gridy++;

      groupProjectButton = createJCheckBox("message.group_project");
      groupProjectButton.setActionCommand("groupproject");
      groupProjectButton.addActionListener(this);
      box.add(groupProjectButton, gbc);

      gbc.gridy++;

      idCardLayout = new CardLayout();
      idPanel = new JPanel(idCardLayout);

      JComponent soloPanel = createSoloStudentIdPanel();
      idPanel.add(soloPanel);
      idCardLayout.addLayoutComponent(soloPanel, "soloproject");

      JComponent groupPanel = createGroupProjectPanel();
      idPanel.add(groupPanel);
      idCardLayout.addLayoutComponent(groupPanel, "groupproject");

      idCardLayout.first(idPanel);

      box.add(idPanel, gbc);

      gbc.gridwidth=3;
      gbc.gridy++;
      gbc.gridx=0;

      panel = Box.createHorizontalBox();
      panel.setAlignmentX(0);
      box.add(panel, gbc);

      JLabel encodingLabel = createJLabel("message.file_encoding");
      panel.add(encodingLabel);

      encodingBox = new JComboBox<String>(ENCODING_VALUES);
      encodingBox.setActionCommand("encoding");
      encodingLabel.setLabelFor(encodingBox);
      panel.add(encodingBox);

      encodingBox.setSelectedItem(getEncodingProperty());
      int index = encodingBox.getSelectedIndex();

      if (index == -1)
      {
         index = 0;
         encodingBox.setSelectedIndex(0);
      }

      encodingInfoList = new String[]
       {passTools.getMessage("encoding_info.ascii"),
        passTools.getMessage("encoding_info.latin1"),
        passTools.getMessage("encoding_info.utf8")
      };

      encodingInfo = new JTextField(encodingInfoList[index]);
      encodingInfo.setEditable(false);
      encodingInfo.setOpaque(false);
      panel.add(encodingInfo);

      encodingBox.addActionListener(this);

      gbc.gridy++;
      gbc.weighty=1;
      box.add(Box.createVerticalStrut(20), gbc);

      return box;
   }

   /**
    * Gets the default username. This first tries the user settings
    * and then tries the user name according to the JVM, which is
    * likely to be the same if they are using a lab computer.
    */ 
   private String getDefaultBlackboardId()
   {
      String blackboardId = getStudentIdProperty();

      if (blackboardId == null || blackboardId.isEmpty())
      {
         String username = System.getProperty("user.name");

         if (username != null && passTools.isValidUserName(username))
         {
            blackboardId = username;
         }
      }

      return blackboardId == null ? "" : blackboardId;
   }

   /**
    * Creates a component containing solo student details.
    * @return the new component 
    */ 
   private JComponent createSoloStudentIdPanel()
   {
      JComponent box = new JPanel(new GridBagLayout());
      box.setName("soloproject");
      GridBagConstraints gbc = new GridBagConstraints();
      box.setAlignmentX(0);

      gbc.gridx=0;
      gbc.gridy=0;
      gbc.anchor=GridBagConstraints.LINE_START;

      String blackboardId = getDefaultBlackboardId();

      studentIdField = new JTextField(new PlainDocument()
       {
          public void insertString(int offs, String str, AttributeSet a)
             throws BadLocationException
          {
             super.insertString(offs, str.replaceAll("[^A-Za-z0-9]", ""), a);
          }
       },
       blackboardId, 20);

      JLabel nameLabel = passGuiTools.createUserNameLabel(studentIdField);

      nameLabel.setAlignmentX(0);
      box.add(nameLabel, gbc);

      studentIdField.setAlignmentX(0);
      gbc.gridx++;
      box.add(studentIdField, gbc);

      String requiredText = passTools.getMessage("message.required");

      gbc.gridx++;
      box.add(new JLabel(requiredText), gbc);

      studentNumberField = new JTextField(new PlainDocument()
       {
          public void insertString(int offs, String str, AttributeSet a)
             throws BadLocationException
          {
             super.insertString(offs, str.replaceAll("[^A-Za-z0-9]", ""), a);
          }
       },
       getStudentNumberProperty(), 20);
      studentNumberField.setAlignmentX(0);

      JLabel numLabel = passGuiTools.createRegNumLabel(studentNumberField);
      numLabel.setAlignmentX(0);

      gbc.gridx=0;
      gbc.gridy++;
      box.add(numLabel, gbc);

      gbc.gridx++;
      box.add(studentNumberField, gbc);

      gbc.gridx++;
      box.add(new JLabel(requiredText), gbc);

      return box;
   }

   /**
    * Creates a component containing the table of students in a
    * group project.
    * @return the new component 
    */ 
   private JComponent createGroupProjectPanel()
   {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
      panel.setName("groupproject");

      TableModel model = new DefaultTableModel(new Object[][] 
       {new Object[]{getDefaultBlackboardId(), getStudentNumberProperty()}, 
        new Object[] {"", ""},
        new Object[]{"", ""}},
       new Object[]{
        passTools.getConfig().getUserNameTitle(), 
        passTools.getConfig().getRegNumTitle()
       });

      groupProjectTable = new JTable(model);

      int prefHeight = groupProjectTable.getPreferredSize().height;
      Dimension dim = groupProjectTable.getPreferredScrollableViewportSize();
      dim.height = prefHeight;

      groupProjectTable.setPreferredScrollableViewportSize(dim);

      panel.add(new JScrollPane(groupProjectTable));

      JPanel box = new JPanel(new GridLayout(2, 2));
      panel.add(box);

      JButton addStudentButton = createJButton(
        "table/RowInsertAfter", "addstudent");
      box.add(addStudentButton);

      removeStudentButton = createJButton(
        "table/RowDelete", "removestudent");
      removeStudentButton.setEnabled(false);
      box.add(removeStudentButton);

      studentUpButton = createJButton(
        "navigation/Up", "movestudentup");
      studentUpButton.setEnabled(false);
      box.add(studentUpButton);

      studentDownButton = createJButton(
        "navigation/Down", "movestudentdown");
      studentDownButton.setEnabled(false);
      box.add(studentDownButton);

      groupProjectTable.getSelectionModel().addListSelectionListener(
         new ListSelectionListener()
         {
            public void valueChanged(ListSelectionEvent evt)
            {
               if (!evt.getValueIsAdjusting())
               {
                  Object source = evt.getSource();

                  if (source instanceof ListSelectionModel)
                  {
                     if (((ListSelectionModel)source).isSelectionEmpty())
                     {
                        removeStudentButton.setEnabled(false);
                     }
                     else
                     {
                        removeStudentButton.setEnabled(true);

                        studentUpButton.setEnabled(
                         !groupProjectTable.isRowSelected(0));
                        studentDownButton.setEnabled(
                         !groupProjectTable.isRowSelected( 
                           groupProjectTable.getRowCount()-1));
                     }
                  }
               }
            }
         }
      );

      model.addTableModelListener(new TableModelListener()
      {
         public void tableChanged(TableModelEvent evt)
         {
            if (evt.getType() == TableModelEvent.UPDATE)
            {
               int column = evt.getColumn();
               int row = evt.getFirstRow();

               if (column != -1 && row != -1 && row == evt.getLastRow())
               {
                  String text = model.getValueAt(row, column).toString().trim();

                  if (!text.isEmpty())
                  {
                     if (column == 0 && !passTools.isValidUserName(text))
                     {
                        error(passTools.getMessage("error.invalid_input", 
                          passTools.getConfig().getUserNameText(), text));
                     }
                     else if (column == 1 && !passTools.isValidRegNum(text))
                     {
                        error(passTools.getMessage("error.invalid_input", 
                          passTools.getConfig().getRegNumText(), text));
                     }
                  }
               }
            }
         }
      });

      return panel;
   }

   @Override
   public String getEncoding()
   {
      return encodingBox.getSelectedItem().toString();
   }

   /**
    * Indicates whether or not paths should be relative to the base
    * directory. If true, the relative structure will be retained
    * when copying the files over to the temporary directory.
    * @return true if relative path structure required
    */ 
   public boolean useRelativePaths()
   {
      return relativizeBox.isEnabled() && relativizeBox.isSelected();
   }

   /**
    * Gets the base path if relative paths required.
    * @return the base path or null if no base path or no relative
    * structure required
    */ 
   @Override
   public Path getBasePath()
   {
      String dir = dirField.getText();

      if (!useRelativePaths() || dir.isEmpty())
      {
         return null;
      }

      return new File(dir).toPath();
   }

   /**
    * Creates the component containing the base directory and
    * relative path checkbox.
    * @return the new component
    */ 
   private JComponent createDirectoryComp()
   {
      Box box = Box.createVerticalBox();
      box.setName(passTools.getMessage("message.select_code_dir"));

      JPanel panel = new JPanel();
      box.add(panel);

      JLabel label = createJLabel("message.code_dir");
      panel.add(label);

      dirField = new JTextField(20);
      panel.add(dirField);
      label.setLabelFor(dirField);

      panel.add(createJButton("general/Open", "choosedir"));

      relativizeBox = createJCheckBox(false, "message.use_rel_paths");
      relativizeBox.setEnabled(false);
      box.add(relativizeBox);

      dirField.getDocument().addDocumentListener(new DocumentListener()
      {
         public void changedUpdate(DocumentEvent e)
         {
            if (dirField.getText().isEmpty())
            {
               relativizeBox.setSelected(false);
               relativizeBox.setEnabled(false);
            }
            else
            {
               relativizeBox.setEnabled(true);
            }
         }

         public void insertUpdate(DocumentEvent e)
         {
            relativizeBox.setEnabled(!dirField.getText().isEmpty());
         }

         public void removeUpdate(DocumentEvent e)
         {
            if (dirField.getText().isEmpty())
            {
               relativizeBox.setSelected(false);
               relativizeBox.setEnabled(false);
            }
            else
            {
               relativizeBox.setEnabled(true);
            }
         }

      });

      JTextArea info = createTextArea(passTools.getMessage(
        "message.code_dir.note", 
        SHORT_TITLE, relativizeBox.getText()));
      box.add(info);

      return box;
   }

   /**
    * Creates the component containing the file list.
    * @return the new component
    */ 
   private JComponent createFileListComp()
   {
      Box box = Box.createVerticalBox();
      box.setAlignmentX(0);

      fileListSp = new JScrollPane(box);
      fileListSp.setName(passTools.getMessage("message.project_files"));

      JTextArea tabMessage = createTextArea(passTools.getMessage(
        "message.file_format.note", AssignmentData.PLAIN_TEXT));
      tabMessage.setAlignmentX(0);
      box.add(tabMessage);

      fileSearchPanel = new JPanel(new BorderLayout());
      fileSearchPanel.setAlignmentX(0);
      fileSearchPanel.setAlignmentY(0);

      JLabel fileSearchLabel = new JLabel(getMessage("message.file_search"));
      fileSearchLabel.setAlignmentX(0);
      fileSearchLabel.setAlignmentY(0);
      fileSearchLabel.setVerticalAlignment(SwingConstants.TOP);

      fileSearchPanel.add(fileSearchLabel, BorderLayout.WEST);

      fileSearcherInfo = passGuiTools.createMessageArea("", 2, 40);
      fileSearcherInfo.setAlignmentX(0);
      fileSearcherInfo.setAlignmentY(0);
      fileSearchPanel.add(fileSearcherInfo, BorderLayout.CENTER);

      fileSearchAbortButton = createJButton(
        getToolIcon("general/Stop", getMessage("process.abort"), true),
        "abortsearch", this, null, null, false, 
        getToolTipMessage("process.abort"), true
       );

      fileSearchPanel.add(fileSearchAbortButton, BorderLayout.EAST);

      box.add(fileSearchPanel);

      requiredFilesLabel = new JLabel(passTools.getMessage("message.required_files"));
      requiredFilesLabel.setAlignmentX(0);
      box.add(requiredFilesLabel);

      requiredFileComp = Box.createVerticalBox();
      requiredFileComp.setAlignmentX(0);
      box.add(requiredFileComp);

      resourceFileComp = Box.createVerticalBox();
      resourceFileComp.setAlignmentX(0);
      resourceFileComp.setBorder(
        BorderFactory.createTitledBorder(
          passTools.getMessage("message.resource_files")));
      box.add(resourceFileComp);
      resourceFileComp.setVisible(false);

      JLabel label = new JLabel(passTools.getMessage("message.optional_files"));
      label.setAlignmentX(0);
      box.add(label);

      additionalFileComp = Box.createVerticalBox();
      additionalFileComp.setAlignmentX(0);
      box.add(additionalFileComp);

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
      box.add(panel);
      panel.setAlignmentX(0);

      panel.add(createJButton("table/RowInsertAfter", "addfile"));

      binaryLabel = new JLabel(passTools.getMessage("message.binary_files"));
      binaryLabel.setAlignmentX(0);
      box.add(binaryLabel);

      binaryFileComp = Box.createVerticalBox();
      binaryFileComp.setAlignmentX(0);
      box.add(binaryFileComp);

      panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
      box.add(panel);
      panel.setAlignmentX(0);

      binaryAddButton = createJButton("table/RowInsertAfter", "addbinaryfile");
      panel.add(binaryAddButton);

      return fileListSp;
   }

   /**
    * Creates the component containing the process information.
    * @return the new component
    */ 
   private JComponent createProcessComp()
   {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setName(passTools.getMessage("message.processing"));

      progressPanel = new ProgressPanel(this);
      panel.add(progressPanel, "North");

      doneField = new JTextField(passTools.getMessage("message.tick"));
      doneField.setHorizontalAlignment(JTextField.CENTER);
      doneField.setVisible(false);
      doneField.setEditable(false);
      doneField.setForeground(Color.GREEN);
      doneField.setBorder(BorderFactory.createEmptyBorder());
      doneField.setFont(doneField.getFont().deriveFont(Font.BOLD, 100.0f));
      panel.add(doneField, "Center");

      panel.add(passGuiTools.createMessageArea(
        getMessage("message.finished_instructions", getApplicationName())), "South");

      return panel;
   }

   /**
    * Creates a new menu item. The menu text and mnemonic are
    * obtained from the dictionary. The label should be in the form 
    * "menu.<em>action</em>".
    * @param action the action command and label suffix
    * @return the new menu item
    */ 
   private JMenuItem createJMenuItem(String action)
   {
      return createJMenuItem("menu", action);
   }

   /**
    * Creates a new menu item. The menu text and mnemonic are
    * obtained from the dictionary. The label should be in the form 
    * "<em>label</em>.<em>action</em>".
    * @param parent the label prefix
    * @param action the action command and label suffix
    * @return the new menu item
    */ 
   private JMenuItem createJMenuItem(String parent, String action)
   {
      return passGuiTools.createJMenuItem(parent, action, this, 
        getKeyStrokeProperty(action));
   }

   /**
    * Creates a new menu item. The menu text and mnemonic are
    * obtained from the dictionary. The label should be in the form 
    * "<em>label</em>.<em>action</em>".
    * @param parent the label prefix
    * @param action the action command and label suffix
    * @param keyStroke the accelerator
    * @return the new menu item
    */ 
   private JMenuItem createJMenuItem(String parent, String action,
      KeyStroke keyStroke)
   {
      return passGuiTools.createJMenuItem(parent, action, this, 
        keyStroke);
   }

   /**
    * Creates a new menu item. 
    * @param text the menu item text
    * @param mnemonic the mnemonic code point or -1 if none
    * @param action the action command
    * @return the new menu item
    */ 
   private JMenuItem createJMenuItem(String text, int mnemonic, String action)
   {
      return passGuiTools.createJMenuItem(text, mnemonic, this, action,
        null, null);
   }

   /**
    * Creates a new menu item. The menu text and mnemonic are
    * obtained from the dictionary.
    * @param label identifying the localised text
    * @return the new menu item
    */ 
   private JMenu createJMenu(String label)
   {
      JMenu menu = new JMenu(passTools.getMessage(label));
      int mnemonic = passTools.getMnemonic(label, -1);

      if (mnemonic != -1)
      {
         menu.setMnemonic(mnemonic);
      }

      return menu;
   }

   /**
    * Initialises the GUI.
    */
   private void initGUI()
   {
      File dir = getStartUpDirectory();

      dirSelector = new JFileChooser();
      dirSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      if (dir != null)
      {
         dirSelector.setCurrentDirectory(dir);
      }

      fileSelector = new JFileChooser();

      if (dir != null)
      {
         fileSelector.setCurrentDirectory(dir);
      }

      pdfFilter = 
       new FileNameExtensionFilter(passTools.getMessage("filefilter.pdf"), "pdf");

      texFilter = 
       new FileNameExtensionFilter(passTools.getMessage("filefilter.tex"), "tex");

      JMenuBar mBar = new JMenuBar();
      setJMenuBar(mBar);

      JMenu optionsM = createJMenu("menu.options.title");
      mBar.add(optionsM);

      JMenu helpM = createJMenu("menu.help.title");
      mBar.add(helpM);

      String year = String.format("%d-%s",
           START_COPYRIGHT_YEAR, VERSION_DATE.substring(0, 4));

      JTextArea licenceArea = passGuiTools.createMessageArea(
       passTools.getMessage("message.licence", year, COPYRIGHT_OWNER), 10, 40);

      licenceComp = new JScrollPane(licenceArea);

      propertiesDialog = new ApplicationProperties(this, disableTimeOutProp);

      optionsM.add(createJMenuItem("menu.options", "settings"));

      transFrame = new TranscriptFrame(this);

      transFrame.message(passTools.getMessage("message.course", course.getCode()));

      optionsM.add(createJMenuItem("menu.options", "transcript"));

      optionsM.add(createJMenuItem("menu.options", "quit",
       getKeyStrokeProperty("quit", 
        KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK))));

      try
      {
         helpFrame = new HelpFrame(this);
         helpM.add(createJMenuItem("menu.help", "handbook",
           getKeyStrokeProperty("quit", 
             KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0))));
      }
      catch (IOException e)
      {
         error(e);
      }

      helpM.add(createJMenuItem("menu.help", "licence"));
      helpM.add(createJMenuItem("menu.help", "about"));

      layout = new CardLayout();

      mainPanel = new JPanel(layout);
      getContentPane().add(mainPanel, "Center");

      JPanel buttonPanel = new JPanel();
      getContentPane().add(buttonPanel, "South");

      topField = passGuiTools.createMessageArea(course.getCode(), 4, 60);
      topField.setBorder(BorderFactory.createEtchedBorder());
      getContentPane().add(new JScrollPane(topField), "North");

      prevButton = createJButton("navigation/Back", "previous");
      prevButton.setEnabled(false);
      buttonPanel.add(prevButton);

      nextButton = createJButton("navigation/Forward", "next");
      nextButton.setEnabled(false);
      buttonPanel.add(nextButton);

      saveTeXButton = createJButton("general/Save", "savetex");
      saveTeXButton.setVisible(false);
      buttonPanel.add(saveTeXButton);

      openLogButton = createJButton("general/Open", "openlog");
      openLogButton.setVisible(false);
      buttonPanel.add(openLogButton);

      saveButton = createJButton("general/Save", "save");
      saveButton.setVisible(false);
      buttonPanel.add(saveButton);

      openButton = createJButton("general/Open", "openpdf");
      openButton.setVisible(false);
      buttonPanel.add(openButton);

      exitButton = createJButton("quit");
      exitButton.setVisible(false);
      buttonPanel.add(exitButton);

      JComponent confirmComp = createConfirmComp();

      mainPanel.add(new JScrollPane(confirmComp));
      maxPanels++;

      JComponent assignmentComp = createAssignmentComp();
      JScrollPane sp = new JScrollPane(assignmentComp);
      Dimension dim = assignmentComp.getPreferredSize();

      mainPanel.add(sp);
      maxPanels++;

      JComponent dirComp = createDirectoryComp();

      mainPanel.add(new JScrollPane(dirComp));
      maxPanels++;

      confirmComp.setPreferredSize(dim);
      dirComp.setPreferredSize(dim);

      pack();

      mainPanel.add(createFileListComp());
      maxPanels++;

      mainPanel.add(createProcessComp());
      maxPanels++;

      updateTitle();
   }

   @Override
   public void warning(String msg)
   {
      transcriptMessage(msg);

      if (warning == null)
      {
         warning = msg;
      }
      else
      {
         warning = String.format("%s%n%s", warning, msg);
      }
   }

   @Override
   public void finished(boolean successful, File file)
   {
      tmpPdfFile = file;
      savedFile = null;

      progressPanel.endProgress();

      if (tmpPdfFile == null)
      {
         AssignmentProcess p = currentProcess.getProcess();

         if (p != null)
         {
            tmpPdfFile = p.getPdfFile();
         }
      }

      if (tmpPdfFile != null && tmpPdfFile.exists())
      {
         try
         {
            savedFile = copyFile(tmpPdfFile, new File(getDefaultPdfName()));

            if (savedFile != null)
            {
               debug("Saved "+savedFile);
            }
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else
      {
         tmpPdfFile = null;
      }

      if (successful)
      {
         if (savedFile != null)
         {
            topField.setText(passTools.getMessage("message.finished.saved", 
              savedFile.getAbsolutePath()));
         }
         else
         {
            topField.setText(passTools.getMessage(
              "message.finished.not_saved",
              saveButton.getText(),
              exitButton.getText()));
         }

         if (warning != null)
         {
            topField.setText(String.format("%s%n%s", 
              topField.getText(), warning));

            doneField.setText("!");
            doneField.setForeground(Color.ORANGE);
            setTitle(passTools.getMessage("message.completed.warning", LONG_TITLE));
            openLogButton.setVisible(true);
         }
         else
         {
            setTitle(passTools.getMessage("message.completed", LONG_TITLE));
         }
      }
      else if (tmpPdfFile != null)
      {
         if (savedFile != null)
         {
            topField.setText(
              passTools.getMessage("message.finished_with_errors.saved", 
              savedFile.getAbsolutePath()));
         }
         else
         {
            topField.setText(passTools.getMessage(
              "message.finished_with_errors.not_saved",
              saveButton.getText(),
              exitButton.getText()));
         }

         if (warning != null)
         {
            topField.setText(String.format("%s%n%s", 
              topField.getText(), warning));
         }

         File logFile = getLogFile();

         if (logFile != null && logFile.exists())
         {
            openLogButton.setVisible(true);
         }

         setTitle(passTools.getMessage("message.failed", LONG_TITLE));
         doneField.setText("!");
         doneField.setForeground(Color.RED);
      }
      else
      {
         File tmpDir = passTools.getTempDirectory();

         if (tmpDir == null)
         {
            topField.setText(passTools.getMessage("message.failed.not_tmpdir"));
         }
         else
         {
            topField.setText(passTools.getMessage(
              "message.failed.tmpdir",
              tmpDir.getAbsolutePath()));
         }

         if (warning != null)
         {
            topField.setText(String.format("%s%n%s", 
              topField.getText(), warning));
         }

         File logFile = getLogFile();

         if (logFile != null && logFile.exists())
         {
            openLogButton.setVisible(true);
         }

         setTitle(passTools.getMessage("message.failed", LONG_TITLE));
         doneField.setText("!");
         doneField.setForeground(Color.RED);
      }

      warning = null;

      doneField.setVisible(true);
      exitButton.setVisible(true);

      if (tmpPdfFile == null)
      {
         getRootPane().setDefaultButton(exitButton);
      }
      else if (savedFile == null)
      {
         saveButton.setVisible(true);
         getRootPane().setDefaultButton(saveButton);
      }
      else
      {
         openButton.setVisible(Desktop.isDesktopSupported());
         getRootPane().setDefaultButton(openButton);
      }
   }

   /**
    * Updates all the file components.
    * These need to be updated when the assignment is changed.
    */ 
   private void updateFileComponents()
   {
      updateFileComponent();

      try
      {
         updateResourceFileComponent();
      }
      catch (MalformedURLException e)
      {
         error(e);
      }

      additionalFileComp.removeAll();
      binaryFileComp.removeAll();

      updateBinaryCompVisibility();

      File dir = null;

      if (!dirField.getText().isEmpty())
      {
         dir = new File(dirField.getText());
      }

      if (dir != null && dir.exists())
      {
         fileSearchMessage("");
         fileSearchPanel.setVisible(true);

         fileSearcher = new FileSearcher(this, dir);
         fileSearcher.execute();
      }
   }

   public void cancelFileSearch()
   {
      if (fileSearcher != null)
      {
         fileSearcher.cancel(true);
         fileSearchMessage(getMessage("message.cancelling"));
      }

      fileSearchAbortButton.setVisible(false);
   }

   @Override
   public void fileSearchMessage(String msg)
   {
      fileSearcherInfo.setText(msg);
   }

   @Override
   public void fileSearchMessage(Exception exc)
   {
      fileSearchMessage(exc.getMessage());

      if (isDebugMode())
      {
         exc.printStackTrace();
      }
   }

   @Override
   public void fileSearchCompleted() throws IOException
   {
      if (additionalFileComp.getComponentCount() == 0)
      {
         additionalFileComp.add(new AdditionalFilePanel(this, fileSelector));
      }

      additionalFileComp.setMaximumSize(additionalFileComp.getPreferredSize());

      if (binaryFileComp.isVisible())
      {
         if (binaryFileComp.getComponentCount() == 0)
         {
            addBinaryFileComponent();
         }

         binaryFileComp.setMaximumSize(binaryFileComp.getPreferredSize());
      }

      fileSearcher = null;

      fileSearchAbortButton.setVisible(false);
   }

   /**
    * Updates the required file list component.
    * These need to be updated when the assignment is changed.
    * For example, if a student realises that they selected the
    * wrong assignment, they can go back and select the correct one.
    */ 
   private void updateFileComponent()
   {
      String dirName = dirField.getText();

      AssignmentData data = (AssignmentData)assignmentBox.getSelectedItem();

      File dir = dirName.isEmpty() ? null : new File(dirName);

      for (int i = 0; i < getRequiredFilesCount(); i++)
      {
         String name = data.getFile(i);

         if (dir == null)
         {
            fileFields[i].setFilename(name);
         }
         else
         {
            File file = new File(dir, name);

            if (file.exists())
            {
               fileFields[i].setFilename(file);
            }
            else
            {
               fileFields[i].setFilename(name);
            }
         }
      }
   }

   @Override
   public FileTextField setRequiredFileComponent(File file)
   throws IOException
   {
      for (int i = 0; i < fileFields.length; i++)
      {
         if (file.getName().equals(fileFields[i].getDefaultName()))
         {
            fileFields[i].setFilename(file);

            return fileFields[i];
         }
      }

      return null;
   }

   /**
    * Updates the resource file list component.
    * These need to be updated when the assignment is changed.
    * These aren't editable and will be fetched from their remote
    * location. The student's local copy should not be used.
    */ 
   private void updateResourceFileComponent()
     throws MalformedURLException
   {
      if (resourceFileFields == null) return;

      String dirName = dirField.getText();

      AssignmentData data = (AssignmentData)assignmentBox.getSelectedItem();

      File dir = dirName.isEmpty() ? null : new File(dirName);

      for (int i = 0; i < resourceFileFields.length; i++)
      {
         URI uri = data.getResourceFile(i).getUri();

         resourceFileFields[i].setFile(uri.toString());
      }
   }

   /**
    * Creates all the required file components and resource fields.
    * These need to be updated when the assignment is changed.
    */ 
   private void createRequiredComponents()
   {
      createRequiredFields();

      try
      {
         createResourceFields();
      }
      catch (MalformedURLException e)
      {
         error(e);
      }
   }

   /**
    * Gets the total number of required files.
    * @return the number of required files
    */ 
   public int getRequiredFilesCount()
   {
      return fileFields == null ? 0 : fileFields.length;
   }

   /**
    * Creates all the required file components.
    * These need to be updated when the assignment is changed.
    */ 
   private void createRequiredFields()
   {
      requiredFileComp.removeAll();

      AssignmentData data = (AssignmentData)assignmentBox.getSelectedItem();

      int n = data.fileCount();

      if (n == 0)
      {
         requiredFilesLabel.setText(passTools.getMessage("message.no_required_files"));
         fileFields = null;
         return;
      }

      requiredFilesLabel.setText(passTools.getMessage("message.required_files"));
      fileFields = new RequiredFilePanel[n];

      File dir = new File(dirField.getText());

      if (dir.exists())
      {
         String mainFileName = data.getMainFile();

         if (mainFileName != null)
         {
            File mainFile = passTools.findFile(dir, mainFileName);

            if (mainFile != null)
            {
               dir = mainFile.getParentFile();
            }
         }
      }

      Dimension maxDim = new Dimension();

      for (int i = 0; i < n; i++)
      {
         String filename = data.getFile(i);

         fileFields[i] = new RequiredFilePanel(this, dir, filename, fileSelector,
            getImageURL("general/Open"));
         requiredFileComp.add(fileFields[i]);

         Dimension dim = fileFields[i].getLabelPreferredSize();

         if (dim.width > maxDim.width)
         {
            maxDim.width = dim.width;
         }

         if (dim.height > maxDim.height)
         {
            maxDim.height = dim.height;
         }
      }

      for (int i = 0; i < n; i++)
      {
         fileFields[i].setLabelPreferredSize(maxDim);
      }

      requiredFileComp.setMaximumSize(requiredFileComp.getPreferredSize());
   }

   /**
    * Creates all the resource fields.
    * These need to be updated when the assignment is changed.
    */ 
   private void createResourceFields() throws MalformedURLException
   {
      resourceFileComp.removeAll();

      AssignmentData data = (AssignmentData)assignmentBox.getSelectedItem();

      int n = data.resourceFileCount();

      if (n == 0)
      {
         resourceFileFields = null;
         resourceFileComp.setVisible(false);
         return;
      }

      resourceFileComp.setVisible(true);

      resourceFileFields = new ResourceFilePanel[n];

      for (int i = 0; i < n; i++)
      {
         resourceFileFields[i] = new ResourceFilePanel(
               data.getResourceFile(i).getUri());
         resourceFileComp.add(resourceFileFields[i]);
      }

      resourceFileComp.setMaximumSize(resourceFileComp.getPreferredSize());
   }

   @Override
   public FileTextField addAdditionalFileComponent(File file)
   throws IOException
   {
      AdditionalFilePanel panel = new AdditionalFilePanel(
        file, this, fileSelector);

      additionalFileComp.add(panel);

      return panel;
   }

   /**
    * Removes an optional file field.
    */ 
   public void removeOptionalFilePanel(AdditionalFilePanel comp)
   {
      additionalFileComp.remove(comp);
      revalidate();
   }

   /**
    * Removes an binary file field.
    */ 
   public void removeBinaryFilePanel(BinaryFilePanel comp)
   {
      binaryFileComp.remove(comp);
      revalidate();
   }

   /**
    * Adds an empty binary file panel.
    */ 
   public BinaryFilePanel addBinaryFileComponent()
   throws IOException
   {
      return addBinaryFileComponent(null);
   }

   /**
    * Adds a binary file panel.
    * @param file the file to add to the panel
    */ 
   @Override
   public BinaryFilePanel addBinaryFileComponent(File file)
   throws IOException
   {
      BinaryFilePanel comp = new BinaryFilePanel(this, file, fileSelector,
         allowedBinaryFilters);

      binaryFileComp.add(comp);

      return comp;
   }

   /**
    * Updates the visibility of the binary files component.
    */ 
   private void updateBinaryCompVisibility()
   {
      Vector<AllowedBinaryFilter> binaryFilters
         = getAssignment().getAllowedBinaryFilters();

      boolean visible = (binaryFilters.size() > 0);

      if (visible)
      {
         allowedBinaryFilters = new javax.swing.filechooser.FileFilter[binaryFilters.size()];

         for (int i = 0; i < binaryFilters.size(); i++)
         {
            allowedBinaryFilters[i] = binaryFilters.get(i);
         }
      }
      else
      {
         allowedBinaryFilters = null;
      }

      binaryFileComp.setVisible(visible);
      binaryLabel.setVisible(visible);
      binaryAddButton.setVisible(visible);
   }

   /**
    * Checks all the selected files. This checks if all the required
    * files have been selected, and makes sure that all other
    * selected files are permitted. Some problems, such as missing
    * required files, just require a warning. It may be that the
    * student was unable to complete the assignment but want to
    * submit a partial solution. In which case, they need to be
    * warned but will be allowed to continue.
    * @return an empty string if no issues were encounter or a list
    * of warning messages
    * @throws InvalidFileException if a banned file is encountered
    * (thrown by PassTools.checkFileName(AssignmentData,File))
    */ 
   public String checkSuppliedFiles() throws InvalidFileException
   {
      StringBuilder builder = new StringBuilder();
      boolean relPathsRequired = false;
      Path basePath = getBasePath();
      AssignmentData data = getAssignment();

      if (fileFields != null)
      {
         for (RequiredFilePanel comp : fileFields)
         {
            String name = comp.getRequiredName();
            String filename = comp.getFilename();

            if (filename == null || filename.isEmpty())
            {
               builder.append(String.format("%n%s",
                  passTools.getMessage("error.required_file_missing", name)));
               continue;
            }

            File file = new File(filename);
            passTools.checkFileName(data, file);

            String language = comp.getLanguage();

            Path path = file.toPath();
            File requiredFile = new File(name);
            Path requiredPath = requiredFile.toPath();
            name = requiredFile.getName();
            Path parentPath = requiredPath.getParent();

            if (parentPath != null)
            {
               relPathsRequired = true;

               try
               {
                  if (basePath != null 
                       && !basePath.relativize(path).equals(requiredPath))
                  {
                     builder.append(String.format("%n%s",
                      passTools.getMessage("error.subpath_not_relative",
                         requiredPath, basePath)));
                  }
               }
               catch (IllegalArgumentException e)
               {
                  builder.append(String.format("%n%s",
                   passTools.getMessage("error.subpath_not_relative", 
                     requiredPath, basePath)));
               }
            }

            if (!file.exists())
            {
               builder.append(String.format("%n%s",
                 passTools.getMessage("error.file_doesnt_exist",
                   file.getAbsolutePath())));
            }
            else
            {
               if (!file.getName().equals(name))
               {
                  builder.append(String.format("%n%s", 
                    passTools.getMessage("error.misnamed_file", filename, name)));
               }

               if (parentPath != null && !path.endsWith(requiredPath))
               {
                  builder.append(String.format(
                   "%n%s", 
                    passTools.getMessage("error.file_not_in_subtree", 
                      filename, parentPath)));
               }
            }

            if (language.equals(AssignmentData.UNKNOWN_LANGUAGE))
            {
               builder.append(String.format("%n%s",
                  passTools.getMessage("error.no_file_lang", 
                    name, AssignmentData.PLAIN_TEXT)));
            }
         }
      }

      if (relPathsRequired)
      {

         if (basePath == null)
         {
            builder.append(String.format("%n%s",
              passTools.getMessage("error.relpath_required")));
         }
      }

      for (int i = 0, n = additionalFileComp.getComponentCount(); i < n; i++)
      {
         FilePanel comp = (FilePanel)additionalFileComp.getComponent(i);

         String language = comp.getLanguage();
         String filename = comp.getFilename();
         File file = (filename == null || filename.isEmpty() ? null
            : new File(filename));

         if (file != null)
         {
            passTools.checkFileName(data, file);

            if (!file.exists())
            {
               builder.append(String.format("%n%s",
                passTools.getMessage("error.file_doesnt_exist", 
                  file.getAbsolutePath())));
            }

            if (language.equals(AssignmentData.UNKNOWN_LANGUAGE))
            {
               builder.append(String.format("%n%s",
                  passTools.getMessage("error.no_lang_set", 
                    AssignmentData.PLAIN_TEXT, file.getName())));
            }
         }
      }

      if (binaryFileComp.isVisible())
      {
         for (int i = 0, n = binaryFileComp.getComponentCount(); i < n; i++)
         {
            FilePanel comp = (FilePanel)binaryFileComp.getComponent(i);

            File file = comp.getFile();

            if (file != null)
            {
               if (!file.exists())
               {
                  builder.append(String.format("%n%s",
                   passTools.getMessage("error.file_doesnt_exist", 
                     file.getAbsolutePath())));
               }

               if (!data.isAllowedBinary(file))
               {
                  builder.append(String.format("%n%s",
                   passTools.getMessage("error.not_allowed_binary", 
                     file.getAbsolutePath())));
               }
            }
         }
      }

      return builder.toString();
   }

   /**
    * Gets all the project files selected by the user.
    * @return list of project files
    */ 
   @Override
   public Vector<PassFile> getFiles()
   {
      int numOpt = additionalFileComp.getComponentCount();
      int requiredN = getRequiredFilesCount();
      int numBin = (binaryFileComp.isVisible() ? 
         binaryFileComp.getComponentCount() : 0);

      Vector<PassFile> files = new Vector<PassFile>(requiredN + numOpt + numBin);

      for (int i = 0; i < requiredN; i++)
      {
         if (fileFields[i].getFilename().isEmpty())
         {
            fileFields[i].setFilename(fileFields[i].getRequiredName());
         }

         files.add(fileFields[i]);
      }

      for (int i = 0; i < numOpt; i++)
      {
         FilePanel panel = (FilePanel)additionalFileComp.getComponent(i);

         if (!panel.getFilename().isEmpty())
         {
            files.add(panel);
         }
      }

      if (binaryFileComp.isVisible())
      {
         AssignmentData data = getAssignment();

         for (int i = 0; i < numBin; i++)
         {
            FilePanel panel = (FilePanel)binaryFileComp.getComponent(i);

            if (!panel.getFilename().isEmpty())
            {
               File file = panel.getFile();

               files.add(new AllowedBinaryFile(file,
                 data.getAllowedBinaryFilter(file)));
            }
         }
      }

      return files;
   }

   /**
    * Gets the selected assignment.
    * @return the selected assignment
    */ 
   @Override
   public AssignmentData getAssignment()
   {
      return (AssignmentData)assignmentBox.getSelectedItem();
   }

   /**
    * Gets the directory chooser.
    * @return the directory chooser
    */ 
   public JFileChooser getDirChooser()
   {
      return dirSelector;
   }

   /**
    * Gets the PDF file chooser.
    * @return the PDF file chooser
    */ 
   public JFileChooser getPdfFileChooser()
   {
      fileSelector.resetChoosableFileFilters();
      fileSelector.addChoosableFileFilter(pdfFilter);
      fileSelector.setFileFilter(pdfFilter);

      return fileSelector;
   }

   /**
    * Gets the LaTeX file chooser.
    * @return the LaTeX file chooser
    */ 
   public JFileChooser getTeXFileChooser()
   {
      fileSelector.resetChoosableFileFilters();
      fileSelector.addChoosableFileFilter(texFilter);
      fileSelector.setFileFilter(texFilter);

      return fileSelector;
   }

   @Override
   public boolean isGroupProject()
   {
      return groupProjectButton.isSelected();
   }

   @Override
   public Vector<Student> getProjectTeam()
   {
      TableModel model = groupProjectTable.getModel();

      Vector<Student> students = new Vector<Student>(model.getRowCount());

      for (int row = 0; row < model.getRowCount(); row++)
      {
         String id = model.getValueAt(row, 0).toString().trim();
         String num = model.getValueAt(row, 1).toString().trim();

         boolean numFound = !num.isEmpty();
         boolean idFound = !id.isEmpty();

         if (idFound && numFound)
         {
            students.add(new Student(id, num));
         }
      }

      return students;
   }

   @Override
   public Student getStudent()
   {
      return new Student(getStudentId(), getStudentNumber());
   }

   /**
    * Gets the value of the registration number field.
    * @return the student's registration number
    */ 
   public String getStudentNumber()
   {
      return studentNumberField.getText();
   }

   /**
    * Gets the value of the username field.
    * @return the student's username
    */ 
   public String getStudentId()
   {
      return studentIdField.getText();
   }

   @Override
   public String getApplicationName()
   {
      return SHORT_TITLE;
   }

   @Override
   public String getApplicationVersion()
   {
      return VERSION;
   }

   @Override
   public void error(Throwable e)
   {
      Throwable cause = e.getCause();

      String msg = e.getMessage();

      if (cause == null)
      {
         if (msg == null)
         {
            error(e.getClass().getSimpleName());
         }
         else
         {
            error(String.format("%s %s", e.getClass().getSimpleName(), msg));
         }
      }
      else if (msg == null)
      {
         error(String.format("%s (%s)", e.getClass().getSimpleName(),
            cause.getClass().getSimpleName()));
      }
      else
      {
         error(String.format("%s (%s) %s", e.getClass().getSimpleName(),
            cause.getClass().getSimpleName(), msg));
      }

      if (debugLevel > 0)
      {
         e.printStackTrace();
      }
   }

   @Override
   public void error(String msg)
   {
      transcriptMessage(msg);

      message(msg, 
        passTools == null ? "Error" : 
           passTools.getMessageWithDefault("error.title", "Error"),
        JOptionPane.ERROR_MESSAGE);
   }

   /**
    * Issues a fatal error. This will show the error and quit with
    * exit code 1. If debugging is enabled this will print the stack
    * trace.
    * @param e the error
    */ 
   public void fatalError(Throwable e)
   {
      fatalError(null, e);
   }

   /**
    * Issues a fatal error. This will show the error and quit with
    * exit code 1. If debugging is enabled this will print the stack
    * trace.
    * @param msg the error message
    * @param e the cause of the fatal error
    */ 
   public void fatalError(String msg, Throwable e)
   {
      String errMess;

      if (passTools == null)
      {
         errMess = String.format("Fatal Error: %s", e.getMessage());
      }
      else
      {
         errMess = passTools.getMessageWithDefault("error.fatal", "Fatal Error: {0}", 
          e.getMessage());
      }

      if (msg != null)
      {
         errMess = String.format("%s%n%s", errMess, msg);
      }

      error(errMess);

      if (debugLevel > 0)
      {
         e.printStackTrace();
      }

      System.exit(1);
   }

   /**
    * Shows a plain message in a dialog box.
    * @param msg the message text
    */
   public void message(String msg)
   {
      message(msg, passTools.getMessageWithDefault("message.title", "Message"),
        JOptionPane.PLAIN_MESSAGE);
   }

   /**
    * Shows a message in a dialog box.
    * @param msg the message text
    * @param title the title of the dialog box
    * @param messageType the JOptionPane message type
    */
   public void message(String msg, String title, int messageType)
   {
      messageArea.setText(msg);
      JOptionPane.showMessageDialog(this, messageAreaSp, title,
        messageType);
   }

   @Override
   public int confirm(String msg, String title, int options)
   {
      return confirm(this, msg, title, options, JOptionPane.QUESTION_MESSAGE);
   }

   /**
    * Shows a dialog requesting confirmation.
    * @param msg the message
    * @param title the title
    * @param option the JOptionPane options
    * @param msgType the JOptionPane message type
    */ 
   public int confirm(String msg, String title, 
     int options, int msgType)
   {
      return confirm(this, msg, title, options, msgType);
   }

   /**
    * Shows a dialog requesting confirmation.
    * @param parent the parent frame
    * @param msg the message
    * @param title the title
    * @param option the JOptionPane options
    * @param msgType the JOptionPane message type
    */ 
   public int confirm(JFrame parent, String msg, String title, 
     int options, int msgType)
   {
      messageArea.setText(msg);

      return JOptionPane.showConfirmDialog(parent,
                 messageAreaSp, title, options, msgType);
   }

   /**
    * Reads the assignments XML file for the selected course.
    * @throws SAXException if XML syntax error
    * @throws IOException if I/O error
    */ 
   private void readXML() throws SAXException,IOException
   {
      warning = null;

      assignments = passTools.loadAssignments(fetchCourse());

      if (warning != null)
      {
         message(warning, passTools.getMessageWithDefault("warning.title", "Warning"),
           JOptionPane.WARNING_MESSAGE);
         warning = null;
      }
   }

   /**
    * Gets the URL of an image. This assumes the Java L&amp;F
    * graphics repository is on the class path.
    * @param imageName the image name without "24.gif" suffix
    * relative to the "toolbarButtonGraphics" directory
    * @return the URL of the image
    */ 
   public URL getImageURL(String imageName)
   {
      return getClass().getResource(String.format(
        "/toolbarButtonGraphics/%s24.gif", imageName));
   }

   /**
    * Gets an icon for a button. 
    * @param name the image name without "24.gif" or "16.gif" suffix
    * relative to the "toolbarButtonGraphics" directory
    * @return the image icon or null if not found
    */
   public ImageIcon getToolIcon(String name)
   {
      return getToolIcon(name, null);
   }

   /**
    * Gets an icon for a button. 
    * @param name the image name without "24.gif" or "16.gif" suffix
    * relative to the "toolbarButtonGraphics" directory
    * @param description the icon description
    * @return the image icon or null if not found
    */
   public ImageIcon getToolIcon(String name, String description)
   {
      return getToolIcon(name, description, false);
   }

   /**
    * Gets an icon for a button. 
    * @param name the image name without "24.gif" or "16.gif" suffix
    * relative to the "toolbarButtonGraphics" directory
    * @param description the icon description
    * @param small if true use 16 otherwise use 24
    * @return the image icon or null if not found
    */
   @Override
   public ImageIcon getToolIcon(String name, String description, boolean small)
   {
      String iconPath;

      if (small)
      {
         iconPath = "/toolbarButtonGraphics/"+name+"16.gif";
      }
      else
      {
         iconPath = "/toolbarButtonGraphics/"+name+"24.gif";
      }

      URL imgUrl = getClass().getResource(iconPath);

      if (imgUrl != null)
      {
         if (description == null)
         {
            return new ImageIcon(imgUrl);
         }
         else
         {
            return new ImageIcon(imgUrl, description);
         }
      }
      else
      {
         debug("Can't find image "+iconPath);
      }

      return null;
   }

   /**
    * Creates a new button with the localised text.
    * @param action button action command
    */ 
   public JButton createJButton(String action)
   {
      String label = "button."+action;

      return createJButton(passTools.getMessage(label), 
           passTools.getMnemonic(label, -1), action);
   }

   /**
    * Creates a new button with the localised text.
    * @param action button action command
    * @param listener button action listener
    */ 
   public JButton createJButton(String action, ActionListener listener)
   {
      String label = "button."+action;

      return createJButton(passTools.getMessage(label), 
           passTools.getMnemonic(label, -1), action, listener);
   }

   /**
    * Creates a new button with the given text.
    * @param text the button text
    * @param mnemonic the button mnemonic
    * @param action button action command
    */ 
   public JButton createJButton(String text, int mnemonic, String action)
   {
      return createJButton(text, mnemonic, action, this);
   }

   /**
    * Creates a new button with the given text.
    * @param text the button text
    * @param mnemonic the button mnemonic
    * @param action button action command
    * @param listener button action listener
    */ 
   public JButton createJButton(String text, int mnemonic, String action, 
      ActionListener listener)
   {
      JButton button = new JButton(text);

      if (mnemonic != -1)
      {
         button.setMnemonic(mnemonic);
      }

      if (action != null)
      {
         button.setActionCommand(action);
         button.addActionListener(this);
      }

      button.setAlignmentX(0);

      return button;
   }

   /**
    * Creates a new button with the localised text and optionally icon.
    * @param imageName identifies the image to use for the icon, see
    * getImageURL(String)
    * @param action button action command
    */ 
   public JButton createJButton(String imageName, String action)
   {
      return createJButton(imageName, action, this);
   }

   /**
    * Creates a new button with the localised text and optionally icon.
    * The button text is obtained from the dictionary identified by
    * "button.<em>action</em>". The mnemonic is obtained from the
    * corresponding mnemonic label or -1 if not defined. The tooltip
    * text is identified by the label appended with ".tooltip" or
    * null if not provided.
    * @param imageName identifies the image to use for the icon, see
    * getImageURL(String)
    * @param action button action command
    * @param listener button action listener
    */ 
   public JButton createJButton(String imageName, String action, 
     ActionListener listener)
   {
      String label = "button."+action;

      return createJButton(passTools.getMessageWithDefault(label, null), 
           passTools.getMnemonic(label, -1), imageName, action, listener,
           passTools.getMessageWithDefault(label+".tooltip", null));
   }

   /**
    * Creates a new button with the given text and optionally icon.
    * @param text button text
    * @param mnemonic button mnemonic codepoint or -1 if none
    * @param imageName identifies the image to use for the icon, see
    * getImageURL(String)
    * @param action button action command
    */ 
   public JButton createJButton(String text, int mnemonic, String imageName,
      String action)
   {
      return createJButton(text, mnemonic, imageName, action, this);
   }

   /**
    * Creates a new button with the given text and optionally icon.
    * @param text button text
    * @param mnemonic button mnemonic codepoint or -1 if none
    * @param imageName identifies the image to use for the icon, see
    * getImageURL(String)
    * @param action button action command
    * @param listener button action listener
    */ 
   public JButton createJButton(String text, int mnemonic, String imageName,
      String action, ActionListener listener)
   {
      return createJButton(text, mnemonic, imageName, action, listener, null);
   }

   /**
    * Creates a new button with the given text and optionally icon.
    * @param text button text
    * @param mnemonic button mnemonic codepoint or -1 if none
    * @param imageName identifies the image to use for the icon, see
    * getImageURL(String)
    * @param action button action command
    * @param listener button action listener
    * @param tooltipText button tooltip text
    */ 
   public JButton createJButton(String text, int mnemonic, String imageName,
      String action, ActionListener listener, String tooltipText)
   {
      URL imageURL = getImageURL(imageName);

      Icon icon = null;

      if (imageURL != null)
      {
         icon = new ImageIcon(imageURL, text);
      }

      return createJButton(text, mnemonic, icon, action, listener, tooltipText);
   }

   /**
    * Creates a new button with the given text.
    * @param text button text
    * @param mnemonic button mnemonic codepoint or -1 if none
    * @param icon button icon or null if none
    * @param action button action command
    * @param listener button action listener
    * @param tooltipText button tooltip text
    */ 
   public JButton createJButton(String text, int mnemonic, Icon icon,
      String action, ActionListener listener, String tooltipText)
   {
      JButton button = passGuiTools.createJButton(text, mnemonic, icon,
       action, listener, tooltipText);

      button.setAlignmentX(0);

      return button;
   }

   /**
    * Creates a new button.
    * @param text button text if no image or the tooltip text
    * otherwise
    * @param imageName identifies the image to use for the icon, see
    * getImageURL(String)
    * @param action button action command
    */ 
   public JButton createJButton(String text, String imageName, String action)
   {
      return createJButton(text, imageName, action, this);
   }

   /**
    * Creates a new button.
    * @param text button text if no image or the tooltip text
    * otherwise
    * @param imageName identifies the image to use for the icon, see
    * getImageURL(String)
    * @param action button action command
    * @param listener button action listener
    */ 
   public JButton createJButton(String text, String imageName, String action,
     ActionListener listener)
   {
      URL imageURL = getImageURL(imageName);

      JButton button = new JButton();

      if (imageURL != null)
      {
         button.setIcon(new ImageIcon(imageURL, text));
         button.setToolTipText(text);
      }
      else
      {
         button.setText(text);
      }

      if (action != null)
      {
         button.setActionCommand(action);
      }

      if (listener != null)
      {
         button.addActionListener(listener);
      }

      button.setAlignmentX(0);

      return button;
   }

   /**
    * Creates a new button with just an icon and no text.
    * @param icon button icon or null if none
    * @param action button action command
    * @param listener button action listener
    * @param component parent or ancestor component of button (to
    * set default button, if applicable) may be null
    * @param keyStroke button accelerator or null if none
    * @param isDefault if true and component not null makes this
    * button the default for the root pane
    * @param toolTip button tooltip text or null if none
    * @param isCompact if true, insets will all be 0
    */ 
   @Override
   public JButton createJButton(Icon icon, String action,
     ActionListener listener, JComponent component, KeyStroke keyStroke,
     boolean isDefault, String toolTip, boolean isCompact)
   {
      return createJButton(null, -1, icon, action, listener, component, keyStroke,
       isDefault, toolTip, isCompact);
   }

   /**
    * Creates a new button. Note that this doesn't add the button to
    * the component. The component is used to obtain the root pane
    * to set the default button.
    * @param text button text
    * @param mnemonic button mnemonic codepoint or -1 if none
    * @param icon button icon or null if none
    * @param action button action command
    * @param listener button action listener
    * @param component parent or ancestor component of button (to
    * set default button, if applicable) may be null
    * @param keyStroke button accelerator or null if none
    * @param isDefault if true and component not null makes this
    * button the default for the root pane
    * @param toolTip button tooltip text or null if none
    * @param isCompact if true, insets will all be 0
    */ 
   public JButton createJButton(String text, int mnemonic, Icon icon, String action,
     ActionListener listener, JComponent component, KeyStroke keyStroke,
     boolean isDefault, String toolTip, boolean isCompact)
   {
      JButton button = createJButton(text, mnemonic, icon, action, listener, toolTip);

      if (isCompact)
      {
         button.setMargin(new Insets(0,0,0,0));
      }

      if (isDefault && component != null)
      {
         JRootPane rootPane;

         if (component instanceof JRootPane)
         {
            rootPane = (JRootPane)component;
         }
         else
         {
            rootPane = component.getRootPane();
         }

         rootPane.setDefaultButton(button);
      }

      return button;
   }

   /**
    * Creates a label with localised text.
    * @param propLabel the label identifying the dictionary message
    * @param params the message parameters
    */ 
   public JLabel createJLabel(String propLabel, Object... params)
   {
      return passGuiTools.createJLabel(propLabel, params);
   }

   /**
    * Creates a label with the given text and mnemonic.
    * @param mnemonic the mnemonic codepoint
    * @param text the label text
    */ 
   public JLabel createJLabel(int mnemonic, String text)
   {
      JLabel label = new JLabel(text);

      if (mnemonic != -1)
      {
         label.setDisplayedMnemonic(mnemonic);
      }

      return label;
   }

   /**
    * Creates a checkbox with localised text.
    * @param selected true if the checkbox should be selected by
    * default
    * @param propLabel the label identifying the dictionary message
    * @param params the message parameters
    */ 
   public JCheckBox createJCheckBox(boolean selected, 
      String propLabel, Object... params)
   {
      JCheckBox checkbox = createJCheckBox(propLabel, params);
      checkbox.setSelected(selected);
      return checkbox;
   }

   /**
    * Creates a checkbox with localised text.
    * @param propLabel the label identifying the dictionary message
    * @param params the message parameters
    */ 
   public JCheckBox createJCheckBox(String propLabel, Object... params)
   {
      JCheckBox checkbox = new JCheckBox(passTools.getMessage(propLabel, params));

      int mnemonic = passTools.getMnemonic(propLabel, -1);

      if (mnemonic != -1)
      {
         checkbox.setMnemonic(mnemonic);
      }

      return checkbox;
   }

   /**
    * Creates and starts up the GUI.
    * @param debug the debugging level (0 for off)
    */ 
   private static void createGUI(final int debug)
     throws InterruptedException,InvocationTargetException
   {
      try
      {
         UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
      }
      catch (Exception e)
      {
      }

      SwingUtilities.invokeAndWait(new Runnable()
      {
         public void run()
         {
            try
            {
               new PrepareAssignmentUpload(debug);
            }
            catch (Exception e)
            {
               e.printStackTrace();
               System.exit(1);
            }
         }
      });
   }

   public static void main(String[] args)
   {
      int debug = 0;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--debug") || args[i].equals("-debug"))
         {
            debug = 1;
         }
         else if (args[i].equals("--help") || args[i].equals("-help")
                 || args[i].equals("-h"))
         {
            System.err.println("Syntax: progassignsys [--debug]");
            System.exit(1);
         }
         else
         {
            System.err.println("Unknown option '"+args[i]+"'");
            System.exit(1);
         }
      }

      try
      {
         createGUI(debug);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         System.exit(1);
      }
   }

   private int debugLevel = 0;

   private Vector<AssignmentData> assignments;

   private File tmpPdfFile, savedFile;

   private JScrollPane fileListSp;

   private JButton prevButton, nextButton, exitButton, saveButton, openButton, saveTeXButton, openLogButton;
   private CardLayout layout;
   private JPanel mainPanel;
   private JTextField doneField;

   private JComboBox<AssignmentData> assignmentBox;
   private JTextField dueField;

   private JTextArea topField;

   private String warning = null;

   private JFileChooser dirSelector, fileSelector;
   private JTextField dirField;
   private JLabel requiredFilesLabel;

   private JCheckBox relativizeBox;

   private JComponent requiredFileComp;
   private JComponent resourceFileComp;
   private JComponent additionalFileComp;

   private JLabel binaryLabel;
   private JButton binaryAddButton;
   private JComponent binaryFileComp;
   private javax.swing.filechooser.FileFilter[] allowedBinaryFilters = null;

   private RequiredFilePanel[] fileFields;
   private ResourceFilePanel[] resourceFileFields;

   private JComponent fileSearchPanel;
   private JTextArea fileSearcherInfo;
   private JButton fileSearchAbortButton;
   private FileSearcher fileSearcher = null;

   public static final int FILE_SEARCH_MAX=100;

   private JTextField applicationField;

   private JTextField studentIdField;
   private JTextField studentNumberField;
   private JCheckBox groupProjectButton;

   private JComponent idPanel;
   private CardLayout idCardLayout;
   private JTable groupProjectTable;
   private JButton removeStudentButton, studentUpButton, studentDownButton;

   private JComboBox<String> encodingBox;
   private JTextField encodingInfo;

   private static final String[] ENCODING_VALUES = new String[]
    {ENCODING_ASCII, ENCODING_LATIN1, ENCODING_UTF8};

   private String[] encodingInfoList;

   private int currentPanel = 1;
   private int maxPanels = 0;

   private ProgressPanel progressPanel;

   private Properties properties;

   private ApplicationProperties propertiesDialog;

   private long timeout = 120L;
   private boolean disableTimeOutProp = false;

   private HelpFrame helpFrame;

   private TranscriptFrame transFrame;

   private JCheckBox confirmBox;

   private static final int CONFIRM_PANEL=1;
   private static final int ASSIGNMENT_PANEL=2;
   private static final int DIR_PANEL=3;
   private static final int FILE_LIST_PANEL=4;
   private static final int PROCESS_PANEL=5;

   private static final String FULL_TITLE="Preparing Programming Assignments for Submission System (PASS)";
   private static final String LONG_TITLE="Preparing Programming Assignments for Submission System";
   public static final String SHORT_TITLE="PASS GUI";

   public static final String DICKIMAW_PASS_FAQ="https://www.dickimaw-books.com/software/pass/faq.php";

   private static final String COPYRIGHT_OWNER="Nicola L.C. Talbot";
   private static final String ABOUT_URL="https://www.dickimaw-books.com/software/pass/";

   private static final int START_COPYRIGHT_YEAR=2016;
   public static final String VERSION="1.3.2";
   public static final String VERSION_DATE="2022-11-21";

   private JComponent licenceComp;

   private JTextArea messageArea;
   private JScrollPane messageAreaSp;

   private Course course=null;

   private PassTools passTools;

   private PassGuiTools passGuiTools;

   private AssignmentProcessWorker currentProcess;

   private FileNameExtensionFilter pdfFilter, texFilter;

   private static final String DEFAULT_PROP_NAME = ".progassignsys.prop";

}
