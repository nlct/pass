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

import java.util.Date;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.Future;

import java.nio.file.*;
import java.nio.channels.InterruptedByTimeoutException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.plaf.FontUIResource;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.help.*;
import javax.help.event.*;

import com.dickimawbooks.passlib.*;
import com.dickimawbooks.passguilib.*;

class PassEditor extends JFrame 
 implements PassGui,
  ActionListener,
  TreeSelectionListener,
  ChangeListener,
  MouseListener,
  HyperlinkListener
{
   public PassEditor() throws IOException
   {
      super(APP_NAME);
      Locale locale = Locale.getDefault();
      passTools = new PassTools(this, locale);
      passTools.loadDictionary("passguilib", locale);
      passTools.loadDictionary("passeditor", locale);

      passGuiTools = new PassGuiTools(this);

      filePaneProperties = new FilePaneProperties();

      createListingLanguages();
   }

   protected void showStartUpFrame() throws IOException
   {
      StringBuilder msg = new StringBuilder(
        getMessage("message.initialising",
          APP_NAME, APP_VERSION, APP_DATE));
      msg.append("<br>");

      // Load properties so that L&F can be set before displaying
      // components
      msg.append(getMessage("message.loading_props"));
      msg.append("<br>");
      loadProperties();

      String lookandfeel = properties.getProperty("lookandfeel", 
        "javax.swing.plaf.nimbus.NimbusLookAndFeel");

      LookAndFeel current = UIManager.getLookAndFeel();

      if (!lookandfeel.equals(current.getClass().getName()))
      {
         try
         {
            UIManager.setLookAndFeel(lookandfeel);
            SwingUtilities.updateComponentTreeUI(this);
         }
         catch (Exception e)
         {
            msg.append(getMessage("error.lookandfeel_failed", 
              lookandfeel, e.getMessage()));
            msg.append("<br>");

            debug(e);

            properties.setProperty("lookandfeel", current.getClass().getName());
         }
      }

      updateWidgetFontSizes();

      // messages areas need to be set up before parsing in case 
      // errors need reporting.

      dialogMessageArea = new JTextArea(10, 40);
      dialogMessageArea.setEditable(false);
      dialogMessageArea.setLineWrap(true);
      dialogMessageArea.setWrapStyleWord(true);
      dialogMessageAreaSp = new JScrollPane(dialogMessageArea);

      messageArea = new JEditorPane("text/html", 
         String.format("<html><body id=\"main\"><p>%s</p></body></html>",
         msg.toString()));
      messageArea.setEditable(false);
      messageArea.setTransferHandler(new HtmlPlainTransferHandler());
      messageArea.addHyperlinkListener(this);

      messageAreaSp = new JScrollPane(messageArea);

      startUpFrame = new JFrame(getMessage("startup.title", APP_NAME));
      startUpFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      ImageIcon ic = getLogoIcon();

      if (ic != null)
      {
         startUpFrame.setIconImage(ic.getImage());
         setIconImage(ic.getImage());
      }

      startUpFrame.getContentPane().add(messageAreaSp, "Center");
      startUpFrame.pack();
      startUpFrame.setLocationRelativeTo(null);
      startUpFrame.setVisible(true);
   }

   protected void closeStartUpFrame()
   {
      if (startUpFrame != null)
      {
         startUpFrame.setVisible(false);
         startUpFrame.remove(messageAreaSp);
         startUpFrame = null;
      }
   }

   protected void createAndShowGUI()
   throws SAXException,IOException,URISyntaxException,HelpSetException
   {
      toolbarButtons = new Vector<AbstractButton>();
      toolbarSeparators = new Vector<JSeparator>();

      JEditorPane.registerEditorKitForContentType("text/x-source", 
        "com.dickimawbooks.passeditor.DefaultSourceEditorKit");

      editorFont = createEditorFont();
      manualFont = createManualFont();
      initialiseHelp();

      updateFontStyle();

      JPanel statusPanel = new JPanel(new BorderLayout());

      JPanel statusButtonPanel = new JPanel();
      statusPanel.add(statusButtonPanel, "West");

      statusButtonPanel.add(createJButton(
        getToolIcon("general/Remove", getMessage("messages.delmessages.icon"), true), 
        "delmessages",
        this, statusButtonPanel, null, false, 
          getMessage("messages.delmessages"), true));

      statusButtonPanel.add(createJButton(
        getToolIcon("general/Copy", getMessage("messages.copymessages.icon"), true), 
        "copymessages",
        this, statusButtonPanel, null, false, 
          getMessage("messages.copymessages"), true));

      progressPanel = new ProgressPanel(this);
      progressPanel.setVisible(false);
      statusPanel.add(progressPanel, "Center");

      infoField = new JLabel();
      infoField.setAlignmentX(0.0f);
      statusPanel.add(infoField, "East");

      getContentPane().add(statusPanel, "South");

      dirChooser = new JFileChooser();
      dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      pdfFileChooser = new JFileChooser();
      pdfFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

      FileNameExtensionFilter filter = new FileNameExtensionFilter(
       getMessage("filter.pdf"), "pdf");
      pdfFileChooser.addChoosableFileFilter(filter);
      pdfFileChooser.setFileFilter(filter);

      projectFileChooser = new JFileChooser();
      projectFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      projectFileChooser.addChoosableFileFilter(
        new FileNameExtensionFilter(getMessage("filter.passed"), "passed"));
      projectFileChooser.setAcceptAllFileFilterUsed(false);

      File lastDir = getStartUpDir();

      dirChooser.setCurrentDirectory(lastDir);
      projectFileChooser.setCurrentDirectory(lastDir);

      selectProjectDialog = new SelectProjectDialog(this, startUpFrame);
      selectProjectDialog.display();

      updateInfoField();

      messageLn(getMessage("message.creating_main"));

      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            quit();
         }
      });

      navigatorPopup = new JPopupMenu();
      editorPopup = new JPopupMenu();

      JMenuBar mbar = new JMenuBar();
      setJMenuBar(mbar);

      SlidingToolBar toolBar = new SlidingToolBar(this);

      getContentPane().add(toolBar, "North");

      JMenu fileM = createJMenu("file.title");
      mbar.add(fileM);

      saveItem = createJMenuItem("file", "save", toolBar, 
        "general/Save", 
          KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
      fileM.add(saveItem);

      saveAllItem = createJMenuItem("file", "save_all", toolBar, 
        "general/SaveAll", 
          KeyStroke.getKeyStroke(KeyEvent.VK_S, 
             InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

      fileM.add(saveAllItem);

      buildItem = createJMenuItem("file", "build", toolBar, 
        "development/Application",
          KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
      fileM.add(buildItem);

      runPassItem = createJMenuItem("file", "runpass", toolBar, 
        "development/ApplicationDeploy",
          KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
      fileM.add(runPassItem);

      openPdfItem = createJMenuItem("file", "openpdf", toolBar, 
        "general/Zoom",
       KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
      fileM.add(openPdfItem);

/*
      exportItem = createJMenuItem("Export PDF", 'E', "export", toolBar, 
        "general/Export");
      exportItem.setEnabled(false);
      fileM.add(exportItem);
*/

      fileM.add(createJMenuItem("file", "close",
          KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK)));

      fileM.add(createJMenuItem("file", "quit", 
          KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK)));

      addToolBarSeparator(toolBar, "file_edit");

      JMenu editM = createJMenu("edit.title");
      mbar.add(editM);

      undoItem = createJMenuItem("edit", "undo", toolBar,
       "general/Undo",
       KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
      editM.add(undoItem);

      editorUndoItem = createJMenuItem("edit", "undo");
      editorPopup.add(editorUndoItem);

      redoItem = createJMenuItem("edit", "redo", toolBar,
       "general/Redo",
       KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
      editM.add(redoItem);

      editorRedoItem = createJMenuItem("edit", "redo");
      editorPopup.add(editorRedoItem);

      editM.addSeparator();
      editorPopup.addSeparator();

      goToLineItem = createJMenuItem("edit", "gotoline",
       KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
      editM.add(goToLineItem);

      editorGoToLineItem = createJMenuItem("edit", "gotoline");
      editorPopup.add(editorGoToLineItem);

      findItem = createJMenuItem("edit", "find", toolBar,
       "general/Find",
       KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
      editM.add(findItem);

      editorFindItem = createJMenuItem("edit", "find");
      editorPopup.add(editorFindItem);

      findAgainItem = createJMenuItem("edit", "findagain", toolBar,
       "general/FindAgain",
       KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
      editM.add(findAgainItem);

      editorFindAgainItem = createJMenuItem("edit", "findagain");
      editorPopup.add(editorFindAgainItem);

      findPreviousItem = createJMenuItem("edit", "findprev", 
       KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK));
      editM.add(findPreviousItem);

      editorFindPreviousItem = createJMenuItem("edit", "findprev");
      editorPopup.add(editorFindPreviousItem);

      replaceItem = createJMenuItem("edit", "replace", toolBar,
       "general/Replace",
       KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
      editM.add(replaceItem);

      editorReplaceItem = createJMenuItem("edit", "replace");
      editorPopup.add(editorReplaceItem);

      enableFindNextPrev(false);

      editM.addSeparator();
      editorPopup.addSeparator();

      copyItem = createJMenuItem("edit", "copy", toolBar,
       "general/Copy", 
       KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
      editM.add(copyItem);

      editorCopyItem = createJMenuItem("edit", "copy");
      editorPopup.add(editorCopyItem);

      cutItem = createJMenuItem("edit", "cut", toolBar,
       "general/Cut", 
       KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
      editM.add(cutItem);

      editorCutItem = createJMenuItem("edit", "cut");
      editorPopup.add(editorCutItem);

      pasteItem = createJMenuItem("edit", "paste", toolBar,
       "general/Paste", 
       KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
      editM.add(pasteItem);

      editorPasteItem = createJMenuItem("edit", "Paste");
      editorPopup.add(editorPasteItem);

      editM.addSeparator();
      addToolBarSeparator(toolBar, "edit_pref");

      editM.add(createJMenuItem("edit", "incfont", toolBar,
       "general/ZoomIn",
       KeyStroke.getKeyStroke(KeyEvent.VK_GREATER, 
        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));

      editM.add(createJMenuItem("edit", "decfont", toolBar,
       "general/ZoomOut",
       KeyStroke.getKeyStroke(KeyEvent.VK_LESS, 
        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));

      editM.add(createJMenuItem("edit", "setproperties", toolBar,
       "general/Preferences"));

      addToolBarSeparator(toolBar, "pref_project");

      JMenu projectM = createJMenu("project.title");
      mbar.add(projectM);

      projectDetailsDialog = new ProjectDetailsDialog(this);

      projectM.add(createJMenuItem("project", "details", toolBar,
        "general/Information"));

      newFolderItem = createJMenuItem("project", "newfolder");
      projectM.add(newFolderItem);

      navigatorNewFolderItem = createJMenuItem("project", "newfolder",
        KeyStroke.getKeyStroke(KeyEvent.VK_N, 
          InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
      navigatorPopup.add(navigatorNewFolderItem);

      newFileItem = createJMenuItem("project", "newfile", toolBar,
        "general/New",
        KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
      projectM.add(newFileItem);

      navigatorNewFileItem = createJMenuItem("project", "newfile");
      navigatorPopup.add(navigatorNewFileItem);

      importFileItem = createJMenuItem("project", "import", toolBar,
        "general/Import",
        KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
      projectM.add(importFileItem);

      navigatorImportItem = createJMenuItem("project", "import");
      navigatorPopup.add(navigatorImportItem);

      importFileChooser = createImportFileChooser();

      deleteFileItem = createJMenuItem("project", "deletefile", toolBar,
        "general/Delete");
      projectM.add(deleteFileItem);
      deleteFileItem.setEnabled(false);

      navigatorDeleteItem = createJMenuItem("project", "deletefile");
      navigatorPopup.add(navigatorDeleteItem);
      navigatorDeleteItem.setEnabled(false);

      removeFromProjectItem = createJMenuItem("project", "removefromproject");
      projectM.add(removeFromProjectItem);
      removeFromProjectItem.setEnabled(false);

      navigatorRemoveFromProjectItem = createJMenuItem("project", "removefromproject");
      navigatorPopup.add(navigatorRemoveFromProjectItem);
      navigatorRemoveFromProjectItem.setEnabled(false);

      moveFileItem = createJMenuItem("project", "movefile");
      moveFileItem.setEnabled(false);
      projectM.add(moveFileItem);

      navigatorMoveItem = createJMenuItem("project", "movefile");
      navigatorMoveItem.setEnabled(false);
      navigatorPopup.add(navigatorMoveItem);

      renameFileItem = createJMenuItem("project", "renamefile",
       KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
      renameFileItem.setEnabled(false);
      projectM.add(renameFileItem);

      navigatorRenameItem = createJMenuItem("project", "renamefile");
      navigatorRenameItem.setEnabled(false);
      navigatorPopup.add(navigatorRenameItem);

      reloadFileItem = createJMenuItem("project", "reload", toolBar,
       "general/Refresh",
       KeyStroke.getKeyStroke(KeyEvent.VK_R, 
          InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
      reloadFileItem.setEnabled(false);
      projectM.add(reloadFileItem);

      navigatorReloadItem = createJMenuItem("project", "reload");
      navigatorReloadItem.setEnabled(false);
      navigatorPopup.add(navigatorReloadItem);

      navigatorModel = new DefaultTreeModel(new PathNode(this, null, null));
      navigatorTree = new JTree(navigatorModel);
      //navigatorTree.setFont(editorFont);
      navigatorTree.setShowsRootHandles(true);
      navigatorTree.addTreeSelectionListener(this);

      navigatorTree.addMouseListener(this);

      JMenu helpM = createJMenu("help.title");
      mbar.add(helpM);

      JMenuItem manualItem = createJMenuItem("help", "manual", toolBar, "general/Help",
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), csh);

      helpM.add(manualItem);

      String year = String.format("%d-%s",
           START_COPYRIGHT_YEAR, APP_DATE.substring(0, 4));

      JTextArea licenceArea = new JTextArea(
       passTools.getMessage("message.licence", year, COPYRIGHT_OWNER), 10, 40);
      licenceArea.setLineWrap(true);
      licenceArea.setWrapStyleWord(true);
      licenceArea.setEditable(false);
      licenceComp = new JScrollPane(licenceArea);
      licenceComp.setName(passTools.getMessage("message.licence.title"));

      helpM.add(createJMenuItem("help", "licence"));

      helpM.add(createJMenuItem("help", "about"));

      fileTabPane = new JTabbedPane();
      fileTabPane.setName(FILE_TABS_NAME);
      fileTabPane.addChangeListener(this);

      nodeTransferHandler = new NodeTransferHandler(this);
      fileTabPane.setTransferHandler(nodeTransferHandler);

      messageLn(getMessage("message.finding_files"));
      addFiles();

      enableTools(true);

      messageLn(getMessage("message.creating_directory_dialog"));

      fileDirectoryDialog = new FileDirectoryDialog(this);

      messageLn(getMessage("message.creating_goto"));

      goToLineDialog = new GoToLineDialog(this);

      messageLn(getMessage("message.creating_find"));

      findDialog = new FindDialog(this);

      messageLn(getMessage("message.creating_props"));

      propertiesDialog = new PropertiesDialog(this);

      compilerLogPane = new JTextArea();
      compilerLogPane.setEditable(false);

      stdErrPane = new JTextArea();
      stdErrPane.setEditable(false);

      stdOutPane = new JTextArea();
      stdOutPane.setEditable(false);

      logPane = new JTabbedPane();

      messageLn(getMessage("message.creating.main"));

      JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        new JScrollPane(navigatorTree), fileTabPane);

      messageLn(getMessage("message.screen_size"));

      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension dim = tk.getScreenSize();
      int w = dim.width*3/4;
      int h = dim.height*3/4;

      messageLn(getMessage("message.close_startup"));

      closeStartUpFrame();

      addToTabbedPane(logPane, messageAreaSp, "messages.title");
      addToTabbedPane(logPane, new JScrollPane(compilerLogPane), "messages.compiler");
      addToTabbedPane(logPane, new JScrollPane(stdErrPane), "messages.stderr");
      addToTabbedPane(logPane, new JScrollPane(stdOutPane), "messages.stdout");

      JSplitPane verSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        mainPanel, logPane);
      verSplit.setResizeWeight(0.8);

      getContentPane().add(verSplit, "Center");

      setSize(w, h);

      setLocationRelativeTo(null);

      setVisible(true);

      if (fileTabPane.getTabCount() > 0)
      {
         getFilePaneTab(0).requestFocusInWindow();
      }
      else
      {
         navigatorTree.requestFocusInWindow();
      }
   }

   public void addToTabbedPane(JTabbedPane tp, JComponent comp, String propLabel)
   {
      tp.addTab(getMessage(propLabel), comp);

      int mnemonic = getMnemonic(propLabel);

      if (mnemonic != -1)
      {
         tp.setMnemonicAt(tp.getTabCount()-1, mnemonic);
      }

      String tooltip = getToolTipMessage(propLabel);

      if (tooltip != null)
      {
         tp.setToolTipTextAt(tp.getTabCount()-1, tooltip);
      }
   }

   public FilePane getFilePaneTab(int idx)
   {
      JScrollPane sp = (JScrollPane)fileTabPane.getComponentAt(idx);
      return (FilePane)sp.getViewport().getView();
   }

   public FilePane getSelectedFilePaneTab()
   {
      JScrollPane sp = (JScrollPane)fileTabPane.getSelectedComponent();

      if (sp == null) return null;

      return (FilePane)sp.getViewport().getView();
   }

   protected JFileChooser createImportFileChooser()
   {
      JFileChooser chooser = new JFileChooser();

      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setMultiSelectionEnabled(true);

      AssignmentData assignment = project.getAssignment();

      String lang = assignment.getMainLanguage();

      addImportFilter(chooser, lang, AssignmentData.PLAIN_TEXT,
        "txt", "csv", "tsv", "tab");
      addImportFilter(chooser, lang, "Assembler", "s", "S", "asm");
      addImportFilter(chooser, lang, "Awk", "awk");
      addImportFilter(chooser, lang, "bash", "sh");
      addImportFilter(chooser, lang, "C", "c", "h");
      addImportFilter(chooser, lang, "C++", 
          "cpp", "cp", "cc", "C", "CPP", "c++", "cxx", "hh", "hpp", "h", "H");
      addImportFilter(chooser, lang, "Java", "java");
      addImportFilter(chooser, lang, "Lua", "lua");
      addImportFilter(chooser, lang, "Matlab", "m");
      addImportFilter(chooser, lang, "Perl", "pl", "perl");
      addImportFilter(chooser, lang, "PHP", "php");
      addImportFilter(chooser, lang, "Python", "py");

      Vector<AllowedBinaryFilter> binaryFilters = assignment.getAllowedBinaryFilters();

      for (AllowedBinaryFilter filter : binaryFilters)
      {
         chooser.addChoosableFileFilter(filter);
      }

      return chooser;
   }

   protected void addImportFilter(JFileChooser chooser, String lang,
      String description, String... extensions)
   {
      FileNameExtensionFilter filter = new FileNameExtensionFilter(
         description, extensions);
      chooser.addChoosableFileFilter(filter);

      if (description.equals(lang))
      {
         chooser.setFileFilter(filter);
      }
   }

   public boolean newProject() throws SAXException,IOException
   {
      dirChooser.setDialogTitle(getMessage("message.choose_project_dir"));

      if (dirChooser.showDialog(null, getMessage("button.select"))
            != JFileChooser.APPROVE_OPTION)
      {
         return false;
      }

      setLastDirectory(dirChooser.getCurrentDirectory());

      File dir = dirChooser.getSelectedFile();

      if (!dir.exists())
      {
         Files.createDirectory(dir.toPath());
      }

      if (!dir.isDirectory())
      {
         throw new NotDirectoryException(dir.toString());
      }

      File[] list = dir.listFiles();

      if (list.length > 0)
      {
         throw new DirectoryNotEmptyException(dir.toString());
      }

      project = new Project(this, dir.toPath());

      readXML();

      return project.getAssignment() != null;
   }

   public boolean importProject() throws SAXException,IOException
   {
      dirChooser.setDialogTitle(getMessage("message.select_project_dir"));

      if (dirChooser.showDialog(null, getMessage("button.select"))
            != JFileChooser.APPROVE_OPTION)
      {
         return false;
      }

      setLastDirectory(dirChooser.getCurrentDirectory());

      File dir = dirChooser.getSelectedFile();

      if (!dir.isDirectory())
      {
         throw new NotDirectoryException(dir.toString());
      }

      File[] list = dir.listFiles(new FilenameFilter()
      {
         @Override
         public boolean accept(File dir, String name)
         {
            return name.endsWith(".passed");
         }
      });

      boolean confirmOverwrite = false;

      if (list.length > 0)
      {
         int result = JOptionPane.showConfirmDialog(null,
           passTools.getChoiceMessage("message.passed_file", list.length),
           getMessage("confirm.title"),
           JOptionPane.YES_NO_CANCEL_OPTION, 
           JOptionPane.QUESTION_MESSAGE);

         if (result == JOptionPane.YES_OPTION)
         {
            projectFileChooser.setCurrentDirectory(dir);
            projectFileChooser.setSelectedFile(list[0]);

            return openProject();
         }
         else if (result != JOptionPane.NO_OPTION)
         {
            return false;
         }

         confirmOverwrite = true;
      }

      project = new Project(this, dir.toPath(), confirmOverwrite);

      readXML();

      return project.getAssignment() != null;
   }

   public boolean openProject() throws IOException,SAXException
   {
      if (projectFileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
      {
         return false;
      }

      setLastDirectory(projectFileChooser.getCurrentDirectory());

      project = Project.load(this, projectFileChooser.getSelectedFile());

      if (project.pdfFileExists())
      {
         completed = true;
      }

      return true;
   }

   public File showSaveDialog()
   {
      if (projectFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
      {
         return projectFileChooser.getSelectedFile();
      }

      return null;
   }

   public JLabel createJLabel(String propLabel)
   {
      JLabel label = new JLabel(getMessage(propLabel));

      int mnemonic = passTools.getMnemonic(propLabel, -1);

      if (mnemonic != -1)
      {
         label.setDisplayedMnemonic(mnemonic);
      }

      return label;
   }

   public JButton createHelpButton(String id)
   {
      return createHelpButton(id, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
   }

   public JButton createHelpButton(String id, KeyStroke keyStroke)
   {
      String text = getMessage("button.help");
      ImageIcon ic = getToolIcon("general/Help", text);

      JButton button = createJButton(text,
         getMnemonic("button.help"), ic, null, null, null, keyStroke, false);

      enableHelpOnButton(button, id, keyStroke);

      return button;
   }

   public void enableHelpOnButton(AbstractButton button, String id,
     KeyStroke keyStroke)
   {
      if (mainHelpBroker != null)
      {
         try
         {
            mainHelpBroker.enableHelpOnButton(button, id,
               mainHelpBroker.getHelpSet());

            csh = new CSH.DisplayHelpFromSource(mainHelpBroker);

            if (keyStroke != null)
            {
               button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
                put(keyStroke, button.getActionCommand());

               button.getActionMap().put(button.getActionCommand(),
                new AbstractAction(button.getActionCommand())
                {
                   public void actionPerformed(ActionEvent evt)
                   {
                      csh.actionPerformed(evt);
                   }
                }
                );
             }
         }
         catch (BadIDException e)
         {
            error(e);
         }
      }
      else
      {
         error(getMessage("error.no_helpset"));
      }
   }

   private void listContainerChildren(Container container)
   {
      listContainerChildren(container, "");
   }

   private void listContainerChildren(Container container, String lpad)
   {
      System.out.println(lpad+"Container: "+container);

      int n = container.getComponentCount();
      System.out.println(lpad+"Child count: "+n);

      for (int i = 0; i < n; i++)
      {
         Component comp = container.getComponent(i);

         if (comp instanceof Container)
         {
            listContainerChildren((Container)comp, lpad+"  ");
         }
         else
         {
            System.out.println(lpad+comp);
         }
      }
   }

   private Component findComponent(Container container, Class cl)
   {
      if (container == null)
      {
         return null;
      }

      int n = container.getComponentCount();

      for (int i = 0; i < n; i++)
      {
         Component comp = container.getComponent(i);

         if (cl.isInstance(comp))
         {
            return comp;
         }

         if (comp instanceof Container)
         {
            Component c = findComponent((Container)comp, cl);

            if (c != null) return c;
         }
      }

      return null;
   }

   private void initialiseHelp() throws IOException,HelpSetException
   {
      if (mainHelpBroker == null)
      {
         HelpSet mainHelpSet = null;

         URL hsURL = getHelpSetLocation("passeditor");

         mainHelpSet = new HelpSet(null, hsURL);

         TryMap map = (TryMap)mainHelpSet.getCombinedMap();
         map.add(new HelpSetIconMap(mainHelpSet));

         mainHelpBroker = mainHelpSet.createHelpBroker();

         if (mainHelpBroker instanceof DefaultHelpBroker)
         {
            WindowPresentation pres = 
              ((DefaultHelpBroker)mainHelpBroker).getWindowPresentation();

            Font f = pres.getFont();

            Window w = pres.getHelpWindow();
            w.setIconImages(getIconImages());

            try
            {
               jhelp = (JHelp)findComponent(w, Class.forName("javax.help.JHelp"));

               if (jhelp != null)
               {
                  jhelp.setFont(manualFont);

                  helpEditorPane = (JEditorPane)findComponent(jhelp.getContentViewer(),
                     Class.forName("javax.swing.JEditorPane"));

                  helpEditorPane.putClientProperty(
                     JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

                  helpEditorPane.setFont(manualFont);
               }
            }
            catch (ClassNotFoundException e)
            {
               debug(e);
            }
         }

         csh = new CSH.DisplayHelpFromSource(mainHelpBroker);
      }
   }

   private URL getHelpSetLocation(String appName)
     throws IOException
   {
      String helpsetLocation = "/manual/";

      String hsLocation;

      URL hsURL;

      if (helpLocaleId != null)
      {
         hsLocation = String.format("%s%s/%s.hs",
           helpsetLocation, helpLocaleId, appName);

         hsURL = getClass().getResource(hsLocation);

         if (hsURL == null)
         {
            warning("Can't find helpset for language '"+helpLocaleId+"'");
            helpLocaleId = null;
         }

         return hsURL;
      }

      Locale locale = Locale.getDefault();

      String localeId = locale.getLanguage()+"-"+locale.getCountry();

      helpLocaleId = localeId;

      hsLocation = String.format("%s%s/%s.hs",
        helpsetLocation, localeId, appName);

      hsURL = getClass().getResource(hsLocation);

      if (hsURL == null)
      {
         String tried = hsLocation;

         helpLocaleId = locale.getLanguage();

         hsLocation = String.format("%s%s/%s.hs",
           helpsetLocation, helpLocaleId, appName);

         hsURL = getClass().getResource(hsLocation);

         if (hsURL == null)
         {
            tried += "\n"+hsLocation;

            if (!localeId.equals("en"))
            {
               helpLocaleId = "en";

               hsLocation = String.format("%s%s/%s.hs",
                 helpsetLocation, helpLocaleId, appName, helpLocaleId);

               hsURL = getClass().getResource(hsLocation);

               if (hsURL == null)
               {
                  tried += "\n"+hsLocation;
               }
            }

            if (hsURL == null)
            {
               throw new IOException("Can't find helpset. Tried:\n"+tried);
            }
         }

      }

      return hsURL;
   }

   public String[] getAvailableHelpLanguages(String appName)
   {
      URL url = getClass().getResource("/manual/");

      File parent;

      try
      {
         parent = new File(url.toURI());
      }
      catch (URISyntaxException e)
      {
         // this shouldn't happen!

         debug(e);
         return new String[] {"en"};
      }

      //File[] files = parent.listFiles(directoryFilter);
      File[] files = parent.listFiles();

      if (files == null)
      {
         debug("no dictionaries found");
         return new String[] {"en"};
      }

      String[] lang = new String[files.length];

      for (int i = 0; i < files.length; i++)
      {
         lang[i] = files[i].getName();
      }

      return lang;
   }

   public String getHelpLocaleId()
   {
      return helpLocaleId;
   }

   public void setHelpLocaleId(String id)
   {
      helpLocaleId = id;
   }

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

   private JMenu createJMenu(String label, int mnemonic)
   {
      JMenu menu = new JMenu(label);

      if (mnemonic != -1)
      {
         menu.setMnemonic(mnemonic);
      }

      return menu;
   }

   private JMenuItem createJMenuItem(String parent, String action)
   {
      String propLabel = parent+"."+action;
      return createJMenuItem(passTools.getMessage(propLabel),
        passTools.getMnemonic(propLabel, -1), action);
   }

   private JMenuItem createJMenuItem(String parent, String action, 
     SlidingToolBar toolBar, String buttonIcon)
   {
      String propLabel = parent+"."+action;
      return createJMenuItem(passTools.getMessage(propLabel),
        passTools.getMnemonic(propLabel, -1), action, toolBar, buttonIcon);
   }

   private JMenuItem createJMenuItem(String parent, String action, 
      KeyStroke keyStroke)
   {
      String propLabel = parent+"."+action;

      return createJMenuItem(passTools.getMessage(propLabel),
        passTools.getMnemonic(propLabel, -1), action, keyStroke);
   }

   private JMenuItem createJMenuItem(String parent, String action, 
     SlidingToolBar toolBar, String buttonIcon, KeyStroke keyStroke)
   {
      String propLabel = parent+"."+action;
      return createJMenuItem(passTools.getMessage(propLabel),
        passTools.getMnemonic(propLabel, -1), action, toolBar, buttonIcon, keyStroke);
   }

   private JMenuItem createJMenuItem(String parent, String action, 
     SlidingToolBar toolBar, String buttonIcon, KeyStroke keyStroke,
     ActionListener listener)
   {
      String propLabel = parent+"."+action;
      String text = passTools.getMessage(propLabel);

      return createJMenuItem(text, passTools.getMnemonic(propLabel, -1), action, 
        toolBar, buttonIcon, text, keyStroke, listener);
   }

   private JMenuItem createJMenuItem(String label, int mnemonic, String action)
   {
      return createJMenuItem(label, mnemonic, action, null, null);
   }

   private JMenuItem createJMenuItem(String label, int mnemonic, String action,
     KeyStroke keyStroke)
   {
      return createJMenuItem(label, mnemonic, action, null, null, keyStroke);
   }

   private JMenuItem createJMenuItem(String label, int mnemonic, String action,
     SlidingToolBar toolBar, String buttonIcon)
   {
      return createJMenuItem(label, mnemonic, action, toolBar, buttonIcon, label);
   }

   private JMenuItem createJMenuItem(String label, int mnemonic, String action,
     SlidingToolBar toolBar, String buttonIcon, KeyStroke keyStroke)
   {
      return createJMenuItem(label, mnemonic, action, toolBar, buttonIcon, label, keyStroke);
   }

   private JMenuItem createJMenuItem(String label, int mnemonic, String action,
     SlidingToolBar toolBar, String buttonIcon, String altText)
   {
      return createJMenuItem(label, mnemonic, action, toolBar, buttonIcon, altText,
        null);
   }

   private JMenuItem createJMenuItem(String label, int mnemonic, String action,
     SlidingToolBar toolBar, String buttonIcon, String altText, KeyStroke keyStroke)
   {
      return createJMenuItem(label, mnemonic, action, toolBar, buttonIcon, 
         altText, keyStroke, this);
   }

   private JMenuItem createJMenuItem(String label, int mnemonic, String action,
     SlidingToolBar toolBar, String buttonIcon, String altText, KeyStroke keyStroke,
     ActionListener listener)
   {
      JButton button = null;

      if (toolBar != null)
      {
         ImageIcon ic = getToolIcon(buttonIcon, label, 
            !getBooleanProperty("toolbariconslarge", true));

         if (ic != null)
         {
            button = new JButton();

            if (listener != null)
            {
               if (action != null)
               {
                  button.setActionCommand(action);

                  toolbarButtons.add(button);
                  button.setVisible(getBooleanProperty("toolbaricon."+action, true));
               }

               button.addActionListener(listener);
            }

            button.setIcon(ic);

            if (altText != null)
            {
               button.setToolTipText(altText);
            }

            toolBar.addWidget(button);
         }
      }

      JMenuItem item;

      if (button == null)
      {
         item = new JMenuItem(label);
      }
      else
      {
         item = new MenuItemButton(label, button);
      }

      item.setMnemonic(mnemonic);

      if (listener != null)
      {
         if (action != null)
         {
            item.setActionCommand(action);
         }

         item.addActionListener(listener);

         if (keyStroke != null)
         {
            item.setAccelerator(keyStroke);
         }
      }

      return item;
   }

   private void addToolBarSeparator(SlidingToolBar toolBar, String sepname)
   {
      JSeparator sep = toolBar.addSeparator();

      sep.setName(sepname);
      toolbarSeparators.add(sep);
      sep.setVisible(getBooleanProperty("toolbarsep."+sepname, true));
   }

   public Vector<AbstractButton> getToolBarButtons()
   {
      return toolbarButtons;
   }

   public Vector<JSeparator> getToolBarSeparators()
   {
      return toolbarSeparators;
   }

   public JButton createJButton(String label, int mnemonic, String action,
     ActionListener listener)
   {
      return createJButton(label, mnemonic, action, listener, null, null, false);
   }

   public JButton createJButton(String label, int mnemonic, String action,
     ActionListener listener, JComponent component, KeyStroke keyStroke)
   {
      return createJButton(label, mnemonic, action, listener, component, 
       keyStroke, false);
   }

   public JButton createJButton(String label, int mnemonic, String action,
     ActionListener listener, JComponent component, KeyStroke keyStroke,
     boolean isDefault)
   {
      return createJButton(label, mnemonic, null, action, listener,
        component, keyStroke, isDefault);
   }

   public JButton createJButton(Icon icon, String action,
     ActionListener listener, JComponent component, KeyStroke keyStroke,
     boolean isDefault, String toolTip)
   {
      return createJButton(icon, action, listener, component, keyStroke,
       isDefault, toolTip, false);
   }

   public JButton createJButton(Icon icon, String action,
     ActionListener listener, JComponent component, KeyStroke keyStroke,
     boolean isDefault, String toolTip, boolean isCompact)
   {
      JButton button = createJButton(null, -1, icon, action, listener,
        component, keyStroke, isDefault, toolTip);

      if (isCompact)
      {
         //button.setContentAreaFilled(false);
         button.setMargin(new Insets(0,0,0,0));
      }

      return button;
   }

   public JButton createJButton(String label, int mnemonic, Icon icon, 
     String action, ActionListener listener, JComponent component, 
     KeyStroke keyStroke, boolean isDefault)
   {
      return createJButton(label, mnemonic, icon, action, listener, component,
         keyStroke, isDefault, null);
   }

   public JButton createJButton(String label, int mnemonic, Icon icon, 
     String action, ActionListener listener, JComponent component, 
     KeyStroke keyStroke, boolean isDefault, String toolTip)
   {
      JButton button;

      if (icon == null)
      {
         button = new JButton(label);
      }
      else if (label == null)
      {
         button = new JButton(icon);
      }
      else
      {
         button = new JButton(label, icon);
      }

      if (mnemonic != -1)
      {
         button.setMnemonic(mnemonic);
      }

      if (toolTip != null)
      {
         button.setToolTipText(toolTip);
      }

      if (action != null)
      {
         button.setActionCommand(action);
      }

      if (listener != null)
      {
         button.addActionListener(listener);

         if (keyStroke != null)
         {
            if (component == null)
            {
               component = getRootPane();
            }

            component.getInputMap().put(keyStroke, action);
            component.getActionMap().put(action, new ButtonAction(button, listener));
         }
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


   private void addFiles() throws IOException,URISyntaxException
   {
      PathNode rootNode = (PathNode)navigatorTree.getModel().getRoot();

      if (project.getFolders() != null)
      {
         for (File dir : project.getFolders())
         {
            addFolder(dir);
         }
      }

      File baseDir = getBasePath().toFile();

      Vector<File> folders = project.getFolders();

      AssignmentData assignment = project.getAssignment();

      for (Iterator<String> it = assignment.getFileIterator(); it.hasNext(); )
      {
         String filename = it.next();

         messageLn(getMessage("message.required_file", filename));

         String[] split = filename.split("/");

         PathNode parentNode = rootNode;

         File dir = baseDir;

         for (int i = 0; i < split.length-1; i++)
         {
            if (!split[i].equals("."))
            {
               dir = new File(dir, split[i]);

               if (dir.exists())
               {
                  PathNode childNode = (PathNode)parentNode.getChild(dir);

                  if (childNode == null)
                  {
                     childNode = new PathNode(this, parentNode, dir);
                     sortedNavigatorInsert(childNode, parentNode);
                  }

                  parentNode = childNode;
               }
               else
               {
                  dir.mkdir();

                  PathNode childNode = new PathNode(this, parentNode, dir);

                  sortedNavigatorInsert(childNode, parentNode);

                  parentNode = childNode;
               }
            }
         }

         File file = new File(dir, split[split.length-1]);
         URI original = null;

         URL template = assignment.getTemplate(filename);

         if (template != null)
         {
            original = template.toURI();
         }

         if (!file.exists())
         {
            if (template != null)
            {
               messageLn(getMessage("message.fetching", template));

               Path path = copyResource(template, baseDir, file.getName());
            }
            else
            {
               messageLn(getMessage("message.creating", file));

               file.createNewFile();
            }
         }

         int idx = filename.lastIndexOf(".");

         String lang = assignment.getListingLanguage(
           idx >= 0 ? filename.substring(0, idx) : filename);

         RequiredFileEditor editor 
            = new RequiredFileEditor(this, file, filename, lang, original);

         EditorNode editorNode = new EditorNode(editor);

         sortedNavigatorInsert(editorNode, parentNode);

         addFileTab(editorNode);
      }

      fileSearcher = new FileSearcher(this, baseDir);
      fileSearcher.execute();
   }

   /**
    * Does nothing as required files have already been found if they
    * exist.
    */ 
   @Override
   public FileTextField setRequiredFileComponent(File file)
   throws IOException
   {
      return null;
   }

   @Override
   public FileTextField addAdditionalFileComponent(File file)
   throws IOException
   {
      addFile(new ProjectFile(file), false);
      
      return null;
   }

   @Override
   public FileTextField addBinaryFileComponent(File file)
   throws IOException
   {
      AssignmentData assignment = project.getAssignment();

      addFile(
        new ProjectFile(file, assignment.getAllowedBinaryFilter(file)), false);

      return null;
   }

   @Override
   public void fileSearchCompleted() throws IOException
   {
      findSupplementaryFiles();

      project.save();

      fileSearcher = null;
   }

   protected void findSupplementaryFiles() throws IOException
   {
      PathNode rootNode = (PathNode)navigatorTree.getModel().getRoot();

      AssignmentData assignment = project.getAssignment();

      File baseDir = getBasePath().toFile();

      for (int i = 0, n = assignment.resourceFileCount(); i < n; i++)
      {
         ResourceFile rf = assignment.getResourceFile(i);

         String filename = rf.getBaseName();
         File file = new File(baseDir, filename);
         String mimetype = rf.getMimeType();

         URI uri = rf.getUri();

         if (file.exists())
         {
            messageLn(getMessage("message.detected_local", filename));
         }
         else
         {
            messageLn(getMessage("message.fetching", uri));

            Path path = copyResource(uri.toURL(), baseDir);

            file = path.toFile();
            filename = file.getName();
         }

         EditorNode editorNode = new EditorNode(
            new FilePane(this, file, filename, 
                  !mimetype.startsWith("text"), mimetype));

         sortedNavigatorInsert(editorNode, rootNode);

         addFileTab(editorNode);
      }

      addResultFiles(assignment.getResultFiles());

      for (ProjectFile file : project.getFiles())
      {
         if (!file.exists())
         {
            error(getMessage("error.project_file_not_found", file));
         }
         else
         {
            addFile(file, false);
         }
      }
   }

   @Override
   public void fileSearchMessage(String msg)
   {
      messageLn(msg);
   }

   @Override
   public void fileSearchMessage(Exception exc)
   {
      error(exc);
   }

   public void addResultFiles(Vector<ResultFile> resultFiles)
      throws IOException
   {
      addResultFiles(resultFiles, false);
   }

   public void addResultFiles(Vector<ResultFile> resultFiles, boolean reload)
      throws IOException
   {
      PathNode rootNode = (PathNode)navigatorTree.getModel().getRoot();
      File baseDir = getBasePath().toFile();

      for (ResultFile rf : resultFiles)
      {
         String filename = rf.getName();
         File file = new File(baseDir, filename);
         String mimetype = rf.getMimeType();

         if (file.exists())
         {
            NavigationTreeNode node = rootNode.getChild(file);

            if (node == null)
            {
               messageLn(getMessage("message.found_result_file", filename));

               EditorNode editorNode = new EditorNode(
                  new FilePane(this, file, filename, ProjectFileType.RESULT,
                     !mimetype.startsWith("text"), null, mimetype));

               sortedNavigatorInsert(editorNode, rootNode);

               addFileTab(editorNode);
            }
            else if (reload && node instanceof EditorNode)
            {
               ((EditorNode)node).getEditor().reload();
            }
         }
         else if (reload)
         {
            messageLn(getMessage("message.not_found_result_file", filename));
         }
      }

   }

   private int addFileTab(EditorNode editorNode)
   {
      return addFileTab(editorNode, false);
   }

   private int addFileTab(EditorNode editorNode, boolean selectTab)
   {
      fileTabPane.addTab(null, editorNode.getScrollPane());

      int idx = fileTabPane.getTabCount()-1;

      if (selectTab)
      {
         fileTabPane.setSelectedIndex(idx);
      }

      fileTabPane.setTabComponentAt(idx, editorNode.getTabLabelComponent());

      return idx;
   }

   private void insertFileTab(int idx, EditorNode editorNode)
   {
      insertFileTab(idx, editorNode, false);
   }

   private void insertFileTab(int idx, EditorNode editorNode, boolean selectTab)
   {
      fileTabPane.insertTab(null, null, editorNode.getScrollPane(), null, idx);

      if (selectTab)
      {
         fileTabPane.setSelectedIndex(idx);
      }

      fileTabPane.setTabComponentAt(idx, editorNode.getTabLabelComponent());
   }

   public void setSelectedFile(EditorNode node)
   {
      try
      {
         fileTabPane.setSelectedComponent(node.getScrollPane());
      }
      catch (IllegalArgumentException e)
      {// file not open
      }
   }

   public boolean movePathReference(Component comp, Transferable transfer)
   {
      int targetIdx = -1;
      int sourceIdx = -1;
      String sourceName = null;
      String targetName = null;
      FilePane srcPane = null;

      try
      {
         sourceName = (String)transfer.getTransferData(DataFlavor.stringFlavor);
      }
      catch (UnsupportedFlavorException | IOException e)
      {
         return false;
      }

      if (comp instanceof PathReference)
      {
         targetName = ((PathReference)comp).getName();
      }

      for (int i = 0, n = fileTabPane.getTabCount(); i < n; i++)
      {
         FilePane filePane = getFilePaneTab(i);
         String name = filePane.getName();

         if (name.equals(targetName))
         {
            targetIdx = i;
         }

         if (name.equals(sourceName))
         {
            sourceIdx = i;
            srcPane = filePane;
         }

         if (sourceIdx > -1 && (targetIdx > -1 || targetName == null))
         {
            break;
         }
      }

      if (sourceIdx == -1 || sourceIdx == targetIdx)
      {
         return false;
      }

      fileTabPane.remove(sourceIdx);

      if (targetIdx == -1)
      {
         addFileTab(srcPane.getNode());
      }
      else
      {
         insertFileTab(targetIdx, srcPane.getNode());
      }

      return true;
   }

   private Path copyResource(URL url, File dir) throws IOException
   {
      return copyResource(url, dir, null);
   }

   public long refetch(URL source, File dest) throws IOException
   {
      long result = 0L;

      int status = passTools.testHttpURLConnection(source);

      if (status > 299)
      {
         throw new InputResourceException(
           getMessage("error.http_status", source, status));
      }

      InputStream in = null;

      try
      {
         in = source.openStream();

         messageLn(getMessage("message.fetching", source));

         result = Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      return result;
   }

   private Path copyResource(URL url, File dir, String name) throws IOException
   {
      Path result = null;

      int status = passTools.testHttpURLConnection(url);

      if (status > 299)
      {
         throw new InputResourceException(
           getMessage("error.http_status", url, status));
      }

      InputStream in = null;

      try
      {
         if (name == null)
         {
            name = url.getPath();

            int idx = name.lastIndexOf("/");

            if (idx > 0)
            {
               name = name.substring(idx+1);
            }
         }

         in = url.openStream();

         messageLn(getMessage("message.fetching", url));

         result = (new File(dir, name)).toPath();

         Files.copy(in, result);
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      return result;
   }

   private void sortedNavigatorInsert(NavigationTreeNode childNode, 
      NavigationTreeNode parentNode)
   {
      String childName = childNode.toString();

      int n = parentNode.getChildCount();

      for (int i = 0; i < n; i++)
      {
         TreeNode node = parentNode.getChildAt(i);

         if (childName.compareTo(node.toString()) <= 0)
         {
            navigatorModel.insertNodeInto(childNode, parentNode, i);

            return;
         }
      }

      navigatorModel.insertNodeInto(childNode, parentNode, n);
   }

   public ImageIcon getLogoIcon()
   {
      return getIcon("pass-logo-32x32.png");
   }

   public ImageIcon getIcon(String name)
   {
      return getIcon("/icons", name);
   }

   public ImageIcon getIcon(String parent, String name)
   {
      String filename = parent+"/"+name;

      URL imgURL = getClass().getResource(filename);

      if (imgURL != null)
      {
         return new ImageIcon(imgURL);
      }

      debug("Can't find icon "+filename);
      return null;
   }

   public ImageIcon getToolIcon(String name)
   {
      return getToolIcon(name, null);
   }

   public ImageIcon getToolIcon(String name, String description)
   {
      return getToolIcon(name, description, false);
   }

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

   public NodeTransferHandler getNodeTransferHandler()
   {
      return nodeTransferHandler;
   }

   public boolean getBooleanProperty(String propName, boolean defValue)
   {
      String propValue = properties.getProperty(propName);

      if (propValue == null) return defValue;

      return Boolean.parseBoolean(propValue);
   }

   public void setBooleanProperty(String propName, boolean value)
   {
      properties.setProperty(propName, ""+value);
   }

   protected File getStartUpDir()
   {
      String prop = properties.getProperty("startup.setting", "home");
      String path = ".";

      if (prop.equals("home"))
      {
         path = System.getProperty("user.home", path);
      }
      else if (prop.equals("last"))
      {
         path = properties.getProperty("startup.last", path);
      }
      else if (prop.equals("custom"))
      {
         path = properties.getProperty("startup.dir", path);
      }

      return new File(path);
   }

   protected void setLastDirectory(File file)
   {
      if (file != null)
      {
         properties.setProperty("startup.last", file.getAbsolutePath());
      }
   }

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

   protected Font createEditorFont()
   {
      String value = properties.getProperty("editorfont", null);

      if (value == null)
      {
         return new Font(Font.MONOSPACED, Font.PLAIN, 12);
      }

      return Font.decode(value);
   }

   protected Font createManualFont()
   {
      String value = properties.getProperty("manualfont", null);

      if (value == null)
      {
         return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
      }

      return Font.decode(value);
   }

   public Font getEditorFont()
   {
      return editorFont;
   }

   public Font getManualFont()
   {
      return manualFont;
   }

   protected void updateFontStyle()
   {
      StyleSheet stylesheet = ((HTMLDocument)messageArea.getDocument()).getStyleSheet();
      String preFamily = String.format("pre { font-family: '%s', monospace; }", 
        editorFont.getFamily());

      stylesheet.addRule(preFamily);

      String preWeight;

      if (editorFont.isBold())
      {
         preWeight = "pre { font-weight: bold; }";
      }
      else
      {
         preWeight = "pre { font-weight: normal; }";
      }

      stylesheet.addRule(preWeight);

      String preStyle;

      if (editorFont.isItalic())
      {
         preStyle = "pre { font-style: italic; }";
      }
      else
      {
         preStyle = "pre { font-style: normal; }";
      }

      stylesheet.addRule(preStyle);

      String fontSize = String.format("body { font-size: %d; }", 
        editorFont.getSize());

      stylesheet.addRule(fontSize);
   }

   public void setEditorFont(Font f)
   {
      editorFont = f;

/*
      if (navigatorTree != null)
      {
         navigatorTree.setFont(editorFont);
      }
*/

      ((NavigationTreeNode)navigatorModel.getRoot()).updateEditorFont(editorFont);

      updateFontStyle();

      String style;

      if (f.isBold())
      {
         if (f.isItalic())
         {
            style = "BOLDITALIC";
         }
         else
         {
            style = "BOLD";
         }
      }
      else if (f.isItalic())
      {
         style = "BOLD";
      }
      else
      {
         style = "PLAIN";
      }

      properties.setProperty("editorfont", String.format("%s-%s-%d", 
         f.getFamily(), style, f.getSize()));
   }

   public void setManualFont(Font f)
   {
      manualFont = f;

      if (jhelp != null)
      {
         jhelp.setFont(manualFont);
         helpEditorPane.setFont(manualFont);
      }

      String style;

      if (f.isBold())
      {
         if (f.isItalic())
         {
            style = "BOLDITALIC";
         }
         else
         {
            style = "BOLD";
         }
      }
      else if (f.isItalic())
      {
         style = "BOLD";
      }
      else
      {
         style = "PLAIN";
      }

      properties.setProperty("manualfont", String.format("%s-%s-%d", 
         f.getFamily(), style, f.getSize()));
   }

   public void increaseFont()
   {
      addToFontSize(1.0f);
   }

   public void decreaseFont()
   {
      addToFontSize(-1.0f);
   }

   public void addToFontSize(float increment)
   {
      setEditorFontSize(editorFont.getSize2D()+increment);
      setManualFontSize(manualFont.getSize2D()+increment);

/*
      Object value = UIManager.get("defaultFont");
      float size = 12.0f;

      if (value instanceof FontUIResource)
      {
         size = ((FontUIResource)value).getSize();
      }
      else
      {
         String prop = properties.getProperty("widgetfontsize");

         if (prop != null)
         {
            size = Float.parseFloat(prop);
         }
      }

      updateWidgetFontSizes(size+increment);
*/
   }

   protected void updateWidgetFontSizes()
   {
      String prop = properties.getProperty("widgetfontsize");

      float size = 12;

      if (prop == null)
      {
         Object value = UIManager.get("defaultFont");

         if (value instanceof FontUIResource)
         {
            size = ((FontUIResource)value).getSize();
         }
      }
      else
      {
         try
         {
            size = Float.parseFloat(prop);
         }
         catch (NumberFormatException e)
         {
            debug(e);
         }
      }

      setWidgetFontSizes(size, true);
   }

   public void setWidgetFontSizes(float size, boolean update)
   {
      if (size < 1.0f)
      {
         return;
      }

      if (update)
      {
         java.util.Enumeration keys = UIManager.getDefaults().keys();

         while (keys.hasMoreElements())
         {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);

            if (value instanceof FontUIResource) 
            {
               FontUIResource fr = (FontUIResource)value;

               if (size != fr.getSize2D())
               {
                  UIManager.put(key, new FontUIResource(fr.deriveFont(size)));
               }
            }
         }
      }

      properties.setProperty("widgetfontsize", ""+size);
   }

   public float getWidgetFontSize()
   {
      String prop = properties.getProperty("widgetfontsize");

      if (prop == null)
      {
         return 12.0f;
      }

      try
      {
         return Float.parseFloat(prop);
      }
      catch (NumberFormatException e)
      {
         debug(e);
         return 12.0f;
      }
   }

   public void setEditorFontSize(float size)
   {
      if (size < 1.0f)
      {
         return;
      }

      setEditorFont(editorFont.deriveFont(size));
   }

   public void setManualFontSize(float size)
   {
      if (size < 1.0f)
      {
         return;
      }

      setManualFont(manualFont.deriveFont(size));
   }

   public void setUI(String name) 
     throws ClassNotFoundException,
     InstantiationException,
     IllegalAccessException,
     UnsupportedLookAndFeelException
   {
      UIManager.setLookAndFeel(name);
      SwingUtilities.updateComponentTreeUI(this);

      if (selectProjectDialog != null)
      {
         SwingUtilities.updateComponentTreeUI(selectProjectDialog);
      }

      if (fileDirectoryDialog != null)
      {
         SwingUtilities.updateComponentTreeUI(fileDirectoryDialog);
      }

      if (goToLineDialog != null)
      {
         SwingUtilities.updateComponentTreeUI(goToLineDialog);
      }

      if (propertiesDialog != null)
      {
         SwingUtilities.updateComponentTreeUI(propertiesDialog);
      }

      if (dirChooser != null)
      {
         SwingUtilities.updateComponentTreeUI(dirChooser);
      }

      if (projectFileChooser != null)
      {
         SwingUtilities.updateComponentTreeUI(projectFileChooser);
      }

      if (pdfFileChooser != null)
      {
         SwingUtilities.updateComponentTreeUI(pdfFileChooser);
      }

      if (importFileChooser != null)
      {
         SwingUtilities.updateComponentTreeUI(importFileChooser);
      }

      if (dialogMessageArea != null)
      {
         SwingUtilities.updateComponentTreeUI(dialogMessageArea);
      }

      if (progressPanel != null)
      {
         SwingUtilities.updateComponentTreeUI(progressPanel);
      }

      if (assignmentSelector != null)
      {
         SwingUtilities.updateComponentTreeUI(assignmentSelector);
      }

      if (projectDetailsDialog != null)
      {
         SwingUtilities.updateComponentTreeUI(projectDetailsDialog);
      }

      if (navigatorPopup != null)
      {
         SwingUtilities.updateComponentTreeUI(navigatorPopup);
      }

      if (editorPopup != null)
      {
         SwingUtilities.updateComponentTreeUI(editorPopup);
      }

      if (mainHelpBroker != null && mainHelpBroker instanceof DefaultHelpBroker)
      {
         WindowPresentation pres = ((DefaultHelpBroker)mainHelpBroker).getWindowPresentation();
         if (pres != null)
         {
            Window w = pres.getHelpWindow();

            if (w != null)
            {
               SwingUtilities.updateComponentTreeUI(w);
            }
         }
      }

      properties.setProperty("lookandfeel", name);
   }

   public FilePaneProperties getFilePaneProperties()
   {
      return filePaneProperties;
   }

   public void setFilePaneProperties(FilePaneProperties prop)
   {
      filePaneProperties.set(prop);
      properties.setProperty("editor.showlines", ""+prop.isShowLines());
      properties.setProperty("editor.showcolumns", ""+prop.isShowColumns());
      properties.setProperty("editor.showposition", ""+prop.isShowPosition());
      properties.setProperty("editor.locationformat", ""+prop.getLocationFormat());

      if (fileTabPane != null)
      {
         for (int i = 0; i < fileTabPane.getTabCount(); i++)
         {
            FilePane fp = getFilePaneTab(i);
            fp.getNode().updatePosition();
         }
      }
   }

   public String formatLocation(FilePane filePane)
   {
      return filePaneProperties.formatLocation(filePane);
   }

   public Properties getProperties()
   {
      return properties;
   }

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
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      String prop = properties.getProperty("editor.showlines");

      if (prop != null)
      {
         filePaneProperties.setShowLines(Boolean.parseBoolean(prop));
      }

      prop = properties.getProperty("editor.showcolumns");

      if (prop != null)
      {
         filePaneProperties.setShowColumns(Boolean.parseBoolean(prop));
      }

      prop = properties.getProperty("editor.showposition");

      if (prop != null)
      {
         filePaneProperties.setShowPosition(Boolean.parseBoolean(prop));
      }

      prop = properties.getProperty("editor.locationformat");

      if (prop != null)
      {
         try
         {
            filePaneProperties.setLocationFormat(
               FilePaneProperties.LocationFormat.parse(prop));
         }
         catch (IllegalArgumentException e)
         {
            throw new InvalidFormatException(file, "editor.locationformat", prop);
         }
      }
   }

   private void saveProperties() throws IOException
   {
      if (toolbarButtons != null)
      {
         for (AbstractButton button : toolbarButtons)
         {
            properties.setProperty("toolbaricon."+button.getActionCommand(), 
               ""+button.isVisible());
         }
      }

      if (toolbarSeparators != null)
      {
         for (JSeparator sep : toolbarSeparators)
         {
            properties.setProperty("toolbarsep."+sep.getName(), 
               ""+sep.isVisible());
         }
      }

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

   public void quit()
   {
      String msg = null;

      if (modified)
      {
         msg = getMessage("warning.unsaved");
      }
      else if (!project.pdfFileExists())
      {
         msg = getMessage("warning.no_pdf");
      }
      else if (!completed)
      {
         msg = getMessage("warning.pdf_not_updated");
      }

      if (msg != null 
           && confirm(String.format("%s%n%s", msg, getMessage("confirm.quit")), 
               getMessage("confirm.quit.title"), JOptionPane.YES_NO_OPTION)
               != JOptionPane.YES_OPTION)
      {
         return;
      }

      if (currentProcess != null)
      {
         currentProcess.removeTemporaryFiles();
      }

      try
      {
         saveProperties();
      }
      catch (IOException e)
      {
         error(e);
      }

      System.exit(0);
   }

   private boolean checkForPopupTrigger(MouseEvent evt)
   {
      if (evt.isPopupTrigger())
      {
         showPopup(evt.getComponent(), evt.getX(), evt.getY());

         return true;
      }

      return false;
   }

   private void showPopup(Component comp, int x, int y)
   {
      if (comp == navigatorTree)
      {
         navigatorPopup.show(comp, x, y);
      }
      else if (comp instanceof FilePane)
      {
         editorCopyItem.setEnabled(copyItem.isEnabled());
         editorCutItem.setEnabled(cutItem.isEnabled());
         editorUndoItem.setEnabled(undoItem.isEnabled());
         editorRedoItem.setEnabled(redoItem.isEnabled());
         editorGoToLineItem.setEnabled(goToLineItem.isEnabled());
         editorFindItem.setEnabled(findItem.isEnabled());
         editorFindAgainItem.setEnabled(findAgainItem.isEnabled());
         editorFindPreviousItem.setEnabled(findPreviousItem.isEnabled());
         editorReplaceItem.setEnabled(replaceItem.isEnabled());
         editorPopup.show(comp, x, y);
      }
   }

   @Override
   public void mouseClicked(MouseEvent evt)
   {
   }

   @Override
   public void mouseExited(MouseEvent evt)
   {
   }

   @Override
   public void mouseEntered(MouseEvent evt)
   {
   }

   @Override
   public void mousePressed(MouseEvent evt)
   {
      if (checkForPopupTrigger(evt))
      {
         evt.consume();
         return;
      }

      if (evt.getComponent() == navigatorTree)
      {
         if (evt.getClickCount() == 2)
         {
            NavigationTreeNode tail = (NavigationTreeNode)
                navigatorTree.getLastSelectedPathComponent();

            if (tail instanceof EditorNode)
            {
               open((EditorNode)tail);
            }
         }
      }
   }

   public void open(EditorNode editorNode)
   {
      JScrollPane comp = editorNode.getScrollPane();
      FilePane filePane = editorNode.getEditor();
      int idx = fileTabPane.indexOfComponent(comp);

      if (idx == -1)
      {
         addFileTab(editorNode, true);

         filePane.requestFocusInWindow();

         try
         {
            filePane.readFile();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else
      {
         fileTabPane.setSelectedIndex(idx);
         filePane.requestFocusInWindow();
      }
   }

   @Override
   public void mouseReleased(MouseEvent evt)
   {
      if (checkForPopupTrigger(evt))
      {
         evt.consume();
         return;
      }
   }

   @Override
   public void hyperlinkUpdate(HyperlinkEvent evt)
   {
      if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
      {
         URL url = evt.getURL();

         if (url != null)
         {
            try
            {
               URI uri = url.toURI();

               String scheme = uri.getScheme();

               if (scheme.equals("file"))
               {
                  Path path = Paths.get(uri.getPath());

                  NavigationTreeNode node = findNode(path.toFile());

                  if (node != null && node instanceof EditorNode)
                  {
                     open((EditorNode)node);

                     String query = uri.getQuery();

                     if (query != null)
                     {
                        String[] split = query.split("\\&");
                        String lineStr = null;
                        String colStr = null;

                        for (String s : split)
                        {
                           String[] param = s.split("=", 2);

                           if (param[0].equals("line"))
                           {
                              lineStr = param[1];
                           }
                           else if (param[0].equals("col"))
                           {
                              colStr = param[1];
                           }
                        }

                        if (lineStr != null)
                        {
                           try
                           {
                              int line = Integer.parseInt(lineStr);
                              int col = 0;

                              if (colStr != null)
                              {
                                 col = Integer.parseInt(colStr);
                              }

                              ((EditorNode)node).getEditor().goToLine(line, col);
                           }
                           catch (NumberFormatException e)
                           {
                              debug(e);
                           }
                        }
                     }
                  }
               }
            }
            catch (URISyntaxException e)
            {
               debug(e);
            }
         }
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null)
      {
         return;
      }

      if (command.equals("quit"))
      {
         quit();
      }
      else if (command.equals("copy"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            fp.copy();
         }
      }
      else if (command.equals("cut"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            fp.cut();
         }
      }
      else if (command.equals("paste"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            fp.paste();
         }
      }
      else if (command.equals("undo"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            fp.undo();
         }
      }
      else if (command.equals("redo"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            fp.redo();
         }
      }
      else if (command.equals("close"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            closeFileComponent(fp.getNode());
         }
      }
      else if (command.equals("gotoline"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            goToLineDialog.display(fp);
         }
      }
      else if (command.equals("find"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            findDialog.display(fp);
         }
      }
      else if (command.equals("findagain"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            fp.getNode().searchNext();
         }
      }
      else if (command.equals("findprev"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            fp.getNode().searchPrevious();
         }
      }
      else if (command.equals("replace"))
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            findDialog.display(fp, true);
         }
      }
      else if (command.equals("incfont"))
      {
         increaseFont();
      }
      else if (command.equals("decfont"))
      {
         decreaseFont();
      }
      else if (command.equals("setproperties"))
      {
         propertiesDialog.display();
      }
      else if (command.equals("details"))
      {
         projectDetailsDialog.display();
      }
      else if (command.equals("copymessages"))
      {
         JTextComponent comp = getSelectedMessageTab();

         if (comp != null)
         {
            if (comp.getSelectedText() == null)
            {
               comp.selectAll();
            }

            comp.copy();
         }
      }
      else if (command.equals("delmessages"))
      {
         try
         {
            JTextComponent comp = getSelectedMessageTab();

            if (comp == messageArea)
            {
               if (confirm(getMessage("confirm.delmessages"), getMessage("confirm.title"),
                  JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
               {
                  HTMLDocument doc = (HTMLDocument)messageArea.getDocument();
                  doc.setInnerHTML(doc.getElement("main"), "<p></p>");
               }
            }
            else if (comp != null)
            {
               comp.setText("");
            }
         }
         catch (Exception e)
         {
            error(e);
         }
      }
      else if (command.equals("import"))
      {
         try
         {
            importFiles();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("newfile"))
      {
         try
         {
            createNewFile();
         }
         catch (IOException | InvalidFileException e)
         {
            error(e);
         }
      }
      else if (command.equals("newfolder"))
      {
         try
         {
            createNewFolder();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("deletefile"))
      {
         try
         {
            deleteFile();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("removefromproject"))
      {
         try
         {
            removeFromProjectListing();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("movefile"))
      {
         try
         {
            moveFile();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("renamefile"))
      {
         try
         {
            renameFile();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("reload"))
      {
         try
         {
            FilePane fp = getSelectedFilePaneTab();

            if (fp != null)
            {
               fp.reload();
            }
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("export"))
      {
         error("Not yet implemented");
      }
      else if (command.equals("save"))
      {
         try
         {
            FilePane fp = getSelectedFilePaneTab();

            if (fp != null && fp instanceof FileEditor)
            {
               ((FileEditor)fp).save();
            }
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("save_all"))
      {
         try
         {
            saveAll();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("openpdf"))
      {
         try
         {
            openPdf();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("build"))
      {
         try
         {
            saveAll();

            currentWorker = new BuildWorker(this);
            progressPanel.startProgress(currentWorker);
            currentWorker.execute();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("runpass"))
      {
         try
         {
            if (!checkConfirmed())
            {
               error(getMessage("error.agree_required"));
               return;
            }

            if (!checkStudents())
            {
               error("error.missing_student_id");
               return;
            }

            saveAll();

            enableTools(false);

            messageLn(getMessage("message.runpass"));
            currentWorker  = new AssignmentProcessWorker(this);
            setCurrentProcess(currentWorker.getProcess());
            progressPanel.startProgress(currentWorker);
            currentWorker.execute();
         }
         catch (IOException e)
         {
            error(e);
         }
      }
      else if (command.equals("about"))
      {
         JOptionPane.showMessageDialog(this,
           passTools.getMessage("message.about",
            APP_NAME, APP_VERSION, APP_DATE, COPYRIGHT_OWNER, ABOUT_URL,
            PASSLIB_VERSION, PASSLIB_VERSION_DATE),
           passTools.getMessage("message.about.title"),
           JOptionPane.INFORMATION_MESSAGE);
      }
      else if (command.equals("licence"))
      {
         JOptionPane.showMessageDialog(this, licenceComp,
           licenceComp.getName(), JOptionPane.INFORMATION_MESSAGE);
      }
      else
      {
         error("Unknown action command: "+command);
      }
   }

   public void showSearchDialog(FilePane filePane, String searchText,
     SearchCriteria criteria)
   {
      findDialog.display(filePane, searchText, criteria);
   }

   public void showSearchDialog(FilePane filePane, String searchText,
     SearchCriteria criteria, boolean allowReplace)
   {
      findDialog.display(filePane, searchText, criteria, allowReplace);
   }

   public void enableFindNextPrev(boolean enable)
   {
      if (enable)
      {
         FilePane fp = getSelectedFilePaneTab();

         if (fp != null)
         {
            enable = !fp.getNode().getSearchTerm().isEmpty();
         }
         else
         {
            enable = false;
         }
      }

      findAgainItem.setEnabled(enable);
      findPreviousItem.setEnabled(enable);
   }

   private void importFiles() throws IOException
   {
      NavigationTreeNode tail = 
        (NavigationTreeNode)navigatorTree.getLastSelectedPathComponent();

      PathNode pathNode;

      Path basePath = project.getBase();

      File dir;

      if (tail == null || tail.getPath() == null)
      {
         dir = basePath.toFile();
         pathNode = (PathNode)navigatorModel.getRoot();
      }
      else
      {
         dir = tail.getPath().toFile();

         if (tail instanceof PathNode)
         {
            pathNode = (PathNode)tail;
         }
         else
         {
            pathNode = (PathNode)tail.getParent();
         }
      }

      if (!dir.isDirectory())
      {
         dir = dir.getParentFile();
      }

      importFileChooser.setCurrentDirectory(dir);

      if (importFileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
      {
         return;
      }

      File[] files = importFileChooser.getSelectedFiles();

      if (files.length == 0)
      {
         return;
      }

      javax.swing.filechooser.FileFilter filter = importFileChooser.getFileFilter();

      String lang = null;

      if (filter instanceof FileNameExtensionFilter)
      {
         lang = filter.getDescription();
      }

      AssignmentData assignment = project.getAssignment();

      for (File originalFile : files)
      {
         try
         {
            passTools.checkFileName(assignment, originalFile);
         }
         catch (InvalidFileException e)
         {
            error(e);
            continue;
         }

         File f = originalFile.getCanonicalFile();
         File parent = f.getParentFile();

         if (!parent.equals(dir))
         {
            NavigationTreeNode parentNode = findNode(parent);

            if (parentNode == null)
            {
               try
               {
                  Path relPath = basePath.relativize(parent.toPath());

                  if (relPath.startsWith(".."))
                  {
                     throw new IllegalArgumentException();
                  }

                  addFolder(relPath);
               }
               catch (IllegalArgumentException e)
               {
                  // file outside of project directory

                  f = new File(dir, originalFile.getName());

                  if (confirm(getMessage("confirm.copy_file", originalFile, f),
                              getMessage("confirm.copy_file.title"),
                              JOptionPane.YES_NO_OPTION)
                      != JOptionPane.YES_OPTION)
                  {
                     continue;
                  }

                  Files.copy(originalFile.toPath(), f.toPath());
               }
            }
         }

         if (f.isDirectory())
         {
            addFolder(f);
         }
         else
         {
            String fileLang = lang;
            ProjectFile pf;

            if (lang == null)
            {
               AllowedBinaryFilter binaryFilter = null;

               if (filter instanceof AllowedBinaryFilter)
               {
                  binaryFilter = (AllowedBinaryFilter)filter;
               }
               else
               {
                  binaryFilter = getAssignment().getAllowedBinaryFilter(f);
               }

               if (binaryFilter == null)
               {
                  int idx = f.getName().lastIndexOf(".");

                  if (idx > 0)
                  {
                     fileLang = getAssignment().getListingLanguage(
                       f.getName().substring(idx+1));
                  }
                  else
                  {
                     fileLang = getAssignment().getListingLanguage(f.getName());
                  }

                  pf = new ProjectFile(f, fileLang);
               }
               else
               {
                  pf = new ProjectFile(f, binaryFilter);
               }
            }
            else
            {
               pf = new ProjectFile(f, fileLang);
            }

            project.addFile(pf);

            addFile(pf, files.length == 1);
         }
      }

      project.save();
   }

   private void addFolder(File file) throws IOException
   {
      if (file.isAbsolute())
      {
         addFolder(getBasePath().relativize(file.toPath()));
      }
      else
      {
         addFolder(file.toPath());
      }
   }

   private PathNode addFolder(Path relPath) throws IOException
   {
      PathNode parentNode = (PathNode)navigatorTree.getModel().getRoot();
      Path parent = getBasePath();

      if (relPath == null)
      {
         return parentNode;
      }

      for (int i = 0, n = relPath.getNameCount(); i < n; i++)
      {
         Path p = parent.resolve(relPath.getName(i));

         PathNode node = null;

         File dir = p.toFile();

         if (Files.exists(p))
         {
            if (!Files.isDirectory(p))
            {
               project.removeFolder(dir);

               throw new NotDirectoryException(p.toString());
            }

            node = (PathNode)parentNode.getChild(dir);
         }
         else
         {
            Files.createDirectory(p);
         }

         if (node == null)
         {
            node = new PathNode(this, parentNode, dir);
            sortedNavigatorInsert(node, parentNode);
         }

         parentNode = node;
         parent = p;
      }

      return parentNode;
   }

   private FileEditor addFile(ProjectFile projectFile, boolean autoOpen)
   throws IOException
   {
      Path path = projectFile.getRelativePath(project);

      String filename = path.toString();

      String language = projectFile.getLanguage();

      if (language == null)
      {
         messageLn(getMessage("message.optional_file", filename));
      }
      else
      {
         messageLn(getMessage("message.optional_file_with_lang", filename, language));
      }

      PathNode parentNode = addFolder(path.getParent());

      FileEditor editor = new FileEditor(this, projectFile, filename, autoOpen);
      EditorNode editorNode = new EditorNode(editor);
         sortedNavigatorInsert(editorNode, parentNode);

      if (autoOpen)
      {
         addFileTab(editorNode, true);
      }

      return editor;
   }

   private void createNewFile() throws IOException,InvalidFileException
   {
      TreePath treePath = navigatorTree.getLeadSelectionPath();

      if (treePath == null) return;

      NavigationTreeNode tail = (NavigationTreeNode)treePath.getLastPathComponent();

      File dir = null;
      String dirname = null;
      Path basePath = project.getBase();

      if (tail == null || tail == navigatorModel.getRoot())
      {
         dir = basePath.toFile();
         dirname = getMessage("navigator.base");
         tail = (NavigationTreeNode)navigatorModel.getRoot();
      }
      else if (tail instanceof PathNode)
      {
         dir = ((PathNode)tail).getDirectory();
         dirname = "'"+dir.getName()+"'";
      }

      if (dir == null)
      {
         return;
      }

      String filename = JOptionPane.showInputDialog(this, 
        getMessage("navigator.add_file_prompt", dirname), 
        getMessage("navigator.new_file.title"), JOptionPane.QUESTION_MESSAGE);

      if (filename != null)
      {
         File file = new File(dir, filename);

         if (file.exists())
         {
            throw new FileAlreadyExistsException(
               getMessage("error.file_exists_in_dir", filename, dirname));
         }

         passTools.checkFileName(project.getAssignment(), file);

         if (file.createNewFile())
         {
            int idx = filename.lastIndexOf(".");

            String lang = project.getAssignment().getListingLanguage(
              idx >= 0 ? filename.substring(0, idx) : filename);

            String name = filename;

            for (int i = treePath.getPathCount()-1; i > 0; i--)
            {
               name = String.format("%s/%s", treePath.getPathComponent(i), name);
            }

            FileEditor editor = new FileEditor(this, file, name, lang);

            EditorNode editorNode = new EditorNode(editor);

            sortedNavigatorInsert(editorNode, tail);

            addFileTab(editorNode, true);

            project.addFile(editor.getProjectFile());
            project.save();
         }
         else
         {
            throw new IOException(
               getMessage("error.cant_create_file", filename));
         }
      }
   }

   private void createNewFolder() throws IOException
   {
      PathNode tail = (PathNode)navigatorTree.getLastSelectedPathComponent();

      File dir = null;
      String dirname = null;

      if (tail == null || tail == navigatorModel.getRoot())
      {
         dir = getBasePath().toFile();
         dirname = "base";
         tail = (PathNode)navigatorModel.getRoot();
      }
      else
      {
         dir = tail.getDirectory();
         dirname = "'"+dir.getName()+"'";
      }

      if (dir != null)
      {
         String filename = JOptionPane.showInputDialog(this, 
           getMessage("navigator.new_folder_prompt",
              dirname), 
          getMessage("navigator.new_folder.title"), JOptionPane.QUESTION_MESSAGE);

         if (filename != null)
         {
            File file = new File(dir, filename);

            if (file.exists())
            {
               if (!file.isDirectory() || findNode(file) != null)
               {
                  throw new FileAlreadyExistsException(
                     getMessage("error.file_exists_in_dir", 
                        filename, dirname));
               }

               project.addFolder(file);
            }
            else if (!file.mkdir())
            {
               throw new IOException(getMessage("error.cant_create_folder", filename));
            }

            PathNode childNode = new PathNode(this, tail, file);
            sortedNavigatorInsert(childNode, tail);

            project.addFolder(file);
            project.save();
         }
      }
   }

   private void removeFromProjectListing() throws IOException
   {
      NavigationTreeNode node =
         (NavigationTreeNode)navigatorTree.getLastSelectedPathComponent();

      if (node == null || node == navigatorModel.getRoot())
      {
         return;
      }

      if (node instanceof EditorNode)
      {
         FilePane editor = ((EditorNode)node).getEditor();

         if (!editor.allowDelete())
         {
            throw new IOException(getMessage("error.remove_forbidden"));
         }

         if (confirm(getMessage("confirm.removefromproject", node.toString()),
               getMessage("confirm.removefromproject.title"),
               JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
         {
            return;
         }

         if (editor.isModified() && 
              confirm(getMessage("confirm.discard", node.toString()),
                  getMessage("confirm.discard.title"),
                  JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
         {
            return;
         }

         fileTabPane.remove(((EditorNode)node).getScrollPane());
         navigatorModel.removeNodeFromParent(node);
         project.removeFile(editor.getProjectFile());
      }
      else if (node.getChildCount() > 0)
      {
         throw new DirectoryNotEmptyException(
          getMessage("error.dir_contains_project_files", node.toString()));
      }
      else
      {
         File dir = ((PathNode)node).getDirectory();

         navigatorModel.removeNodeFromParent(node);
      }

      project.save();
   }

   private void deleteFile() throws IOException
   {
      NavigationTreeNode node =
         (NavigationTreeNode)navigatorTree.getLastSelectedPathComponent();

      if (node == null || node == navigatorModel.getRoot())
      {
         return;
      }

      if (node instanceof EditorNode)
      {
         FilePane editor = ((EditorNode)node).getEditor();

         if (!editor.allowDelete())
         {
            throw new IOException(getMessage("error.del_forbidden"));
         }

         File file = editor.getFile();

         if (confirm(getMessage("confirm.delfile", node.toString()),
               getMessage("confirm.delfile.title"),
               JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
         {
            fileTabPane.remove(((EditorNode)node).getScrollPane());
            navigatorModel.removeNodeFromParent(node);
            Files.delete(file.toPath());
            project.removeFile(editor.getProjectFile());
         }
      }
      else if (node.getChildCount() > 0)
      {
         throw new DirectoryNotEmptyException(
          getMessage("error.dir_not_empty", node.toString()));
      }
      else
      {
         File dir = ((PathNode)node).getDirectory();

         navigatorModel.removeNodeFromParent(node);
         Files.delete(dir.toPath());
         project.removeFolder(dir);
      }

      project.save();
   }

   private void renameFile() throws IOException
   {
      NavigationTreeNode node =
         (NavigationTreeNode)navigatorTree.getLastSelectedPathComponent();

      if (node == null || node == navigatorModel.getRoot())
      {
         return;
      }

      String msg;
      String msgTitle;

      String oldName = node.toString();

      if (node instanceof PathNode)
      {
         msg = getMessage("navigator.rename_folder_prompt", oldName);
         msgTitle = getMessage("navigator.rename_folder.title");
      }
      else
      {
         msg = getMessage("navigator.rename_file_prompt", oldName);
         msgTitle = getMessage("navigator.rename_file.title");
      }

      String newName = JOptionPane.showInputDialog(this, 
        msg, msgTitle, JOptionPane.QUESTION_MESSAGE);

      if (newName == null || newName.equals(oldName))
      {
         return;
      }

      Path source = node.getPath();

      Path parent = source.getParent();
      Path target = parent.resolve(newName);

      Files.move(source, target);
      node.setPath(target);

      project.fileMove(source, target);
      project.save();

      if (node instanceof EditorNode)
      {
         editorNodeChanged((EditorNode)node);
      }

      navigatorTree.revalidate();
   }

   public void editorNodeChanged(EditorNode node)
   {
      if (!modified)
      {
         setModified(true);
      }
   }

   public void closeFileComponent(EditorNode node)
   {
      int idx = fileTabPane.indexOfComponent(((EditorNode)node).getScrollPane());

      if (idx < 0) return;

      if (node.isModified() 
          && confirm(getMessage("confirm.discard", node.toString()),
               getMessage("confirm.discard.title"), JOptionPane.YES_NO_OPTION)
             != JOptionPane.YES_OPTION)
      {
         return;
      }

      fileTabPane.remove(idx);
      node.setModified(false);
   }

   private void moveFile() throws IOException
   {
      NavigationTreeNode node =
         (NavigationTreeNode)navigatorTree.getLastSelectedPathComponent();

      if (node == null || node == navigatorModel.getRoot())
      {
         return;
      }

      PathNode result = fileDirectoryDialog.showMoveTo(node.toString());

      if (result == null)
      {
         return;
      }

      if (result.equals(node.getParent()))
      {
         error(getMessage("error.file_exists_in_dir", node, result));
         return;
      }

      Path parentTarget = result.getPath();

      if (parentTarget == null)
      {
         parentTarget = project.getBase();
      }

      Path src = node.getPath();

      Path target = Files.move(src, parentTarget.resolve(node.toString()));

      project.fileMove(src, target);
      project.save();

      navigatorModel.removeNodeFromParent(node);
      node.setPath(target);
      node.setParent(result);
      sortedNavigatorInsert(node, result);

      if (node instanceof EditorNode)
      {
         editorNodeChanged((EditorNode)node);
      }

      navigatorTree.setLeadSelectionPath(getTreePath(node));
   }

   @Override
   public void stateChanged(ChangeEvent e)
   {
      if (e.getSource() == fileTabPane)
      {
         enableEditTools(buildItem.isEnabled());

         NavigationTreeNode selectedNode =
            (NavigationTreeNode)navigatorTree.getLastSelectedPathComponent();

         FilePane fp = getSelectedFilePaneTab();

         if (fp == null) return;

         EditorNode node = fp.getNode();

         if (selectedNode != node)
         {
            navigatorTree.setSelectionPath(getTreePath(node));
         }

         if (findDialog != null && findDialog.isVisible())
         {
            findDialog.updateFilePane(node.getEditor());
         }
      }
   }

   public NavigationTreeNode findNode(File file)
   {
      PathNode start = (PathNode)navigatorModel.getRoot();

      if (file.toPath().equals(project.getBase()))
      {
         return start;
      }

      return findNode(file, start);
   }

   public NavigationTreeNode findNode(File file, PathNode start)
   {
      for (int i = 0, n = start.getChildCount(); i < n; i++)
      {
         NavigationTreeNode node = (NavigationTreeNode)start.getChildAt(i);

         if (node.isFile(file))
         {
            return node;
         }

         if (node instanceof PathNode)
         {
            NavigationTreeNode result = findNode(file, (PathNode)node);

            if (result != null)
            {
               return result;
            }
         }
      }

      return null;
   }

   public TreePath getTreePath(NavigationTreeNode node)
   {
      if (node == null) return null;

      NavigationTreeNode parent = (NavigationTreeNode)node.getParent();

      if (parent == null)
      {
         return new TreePath(node);
      }

      Vector<NavigationTreeNode> elements = new Vector<NavigationTreeNode>();
      elements.add(node);

      while (parent != null)
      {
         elements.add(0, parent);
         parent = (NavigationTreeNode)parent.getParent();
      }

      return new TreePath(elements.toArray());
   }

   public void openPdf() throws IOException
   {
      File pdfFile = project.getPdfFile();

      if (pdfFile == null) return;

      passGuiTools.openPdf(pdfFile);
   }

   public void saveAll() throws IOException
   {
      for (int i = 0, n = fileTabPane.getTabCount(); i < n; i++)
      {
         FilePane fp = getFilePaneTab(i);

         if (fp instanceof FileEditor)
         {
            ((FileEditor)fp).save();
         }
      }

      project.save();
      setModified(false);
   }

   public void setCurrentProcess(AssignmentProcess process)
   {
      if (currentProcess != null)
      {
         currentProcess.removeTemporaryFiles();
      }

      currentProcess = process;

      if (process != null)
      {
         warningBuffer = new StringBuilder();
      }
   }

   protected void updateProcessMessageAreas()
   {
      if (currentProcess != null)
      {
         updateTextArea(currentProcess.getCompilerMessagesFile(), compilerLogPane);
         updateTextArea(currentProcess.getStderrFile(), stdErrPane);
         updateTextArea(currentProcess.getStdoutFile(), stdOutPane);
      }

      if (warningBuffer != null)
      {
         messageLn(warningBuffer.toString(), "strong", false);
         warningBuffer = null;
      }
   }

   protected JTextComponent getSelectedMessageTab()
   {
      Component comp = logPane.getSelectedComponent();

      if (comp == null)
      {
         return null;
      }

      if (comp instanceof JScrollPane)
      {
         comp = ((JScrollPane)comp).getViewport().getView();
      }

      if (comp instanceof JTextComponent)
      {
         return (JTextComponent)comp;
      }

      return null;
   }

   protected void updateTextArea(File file, JTextComponent comp)
   {
      comp.setText("");

      if (file == null || !file.exists()) return;

      BufferedReader reader = null;

      try
      {
         reader = passTools.newBufferedReader(file);
         comp.read(reader, null);
      }
      catch (IOException e)
      {
         error(e);
      }
      finally
      {
         if (reader != null)
         {
            try
            {
               reader.close();
            }
            catch (IOException e)
            {
               error(e);
            }
         }
      }
   }

   public void setIndeterminateProgress(boolean state)
   {
      progressPanel.setIndeterminate(state);
   }

   public void finishedBuild()
   {
      updateProcessMessageAreas();

      progressPanel.endProgress();

      currentWorker = null;

      enableTools(true);
   }

   public void finished(boolean successful, File tmpPdfFile)
   {
      updateProcessMessageAreas();

      enableTools(true);

      if (successful)
      {
         messageLn(getMessage("message.process_finished"));
      }
      else
      {
         messageLn(getMessage("message.process_failed"));
      }

      try
      {
         currentProcess.copyResultFiles(project.getBase(), 
           StandardCopyOption.REPLACE_EXISTING);
   
         addResultFiles(project.getAssignment().getResultFiles(), true);
      }
      catch (Exception e)
      {
         error(e);
      }

      if (tmpPdfFile != null && tmpPdfFile.exists())
      {
         File dest = project.getPdfFile();

         if (dest == null)
         {
            File file = new File(project.getBase().toFile(),
               getDefaultBaseName()+".pdf");

            pdfFileChooser.setSelectedFile(file);

            do
            {
               if (pdfFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
               {
                  return;
               }

               dest = pdfFileChooser.getSelectedFile();

               if (dest.exists()
                && confirm(getMessage("confirm.overwrite", file),
                           getMessage("confirm.overwrite.title"),
                            JOptionPane.YES_NO_OPTION)
                      != JOptionPane.YES_OPTION)
               {
                  dest = null;
               }
            }
            while (dest == null);
         }

         try
         {
            Path target = Files.copy(tmpPdfFile.toPath(), dest.toPath(),
               StandardCopyOption.REPLACE_EXISTING);

            project.setPdfFile(target.toFile());
            project.save();

            enableTools(true);
            completed = true;

            openPdf();

            messageLn(getMessage("message.nb_pdf"), "strong");
         }
         catch (IOException e)
         {
            error(e);
         }
      }

      progressPanel.endProgress();
      currentWorker = null;
   }

   public void setModified(boolean modified)
   {
      this.modified = modified;

      if (modified)
      {
         completed = false;
      }

      updateTitle();
   }

   public void setModified(boolean modified, EditorNode node)
   {
/*
      int idx = fileTabPane.indexOfComponent(node.getScrollPane());

      if (idx >= 0)
      {
         fileTabPane.setTitleAt(idx, modified ? node.getName()+"*" : node.getName());
      }
*/

      if (modified)
      {
         completed = false;
         this.modified = true;
      }
      else if (this.modified)
      {// file associated with node has been saved but are there any others that
       // are still unsaved?

         int idx = fileTabPane.indexOfComponent(node.getScrollPane());

         for (int i = 0, n = fileTabPane.getTabCount(); i < n; i++)
         {
            if (i == idx) continue;

            FilePane fp = getFilePaneTab(i);

            if (fp.isModified())
            {
               return;
            }
         }

         this.modified = false;
      }

      updateTitle();
   }

   public void enableTools(boolean enable)
   {
      if (openPdfItem != null)
      {
         openPdfItem.setEnabled(enable && project.pdfFileExists());
      }

      saveItem.setEnabled(enable);
      saveAllItem.setEnabled(enable);
      runPassItem.setEnabled(enable);
      buildItem.setEnabled(enable);

      enableEditTools(enable);
   }

   public void updateEditTools(boolean canEdit, boolean canUndo, boolean canRedo,
     boolean hasSelected)
   {
      undoItem.setEnabled(canUndo);
      redoItem.setEnabled(canRedo);
      copyItem.setEnabled(hasSelected);
      cutItem.setEnabled(canEdit && hasSelected);
   }

   public void enableEditTools(boolean enable)
   {
      FilePane filePane = getSelectedFilePaneTab();

      if (filePane == null)
      {
         goToLineItem.setEnabled(false);
         findItem.setEnabled(false);
         enableFindNextPrev(false);
         replaceItem.setEnabled(false);

         undoItem.setEnabled(false);
         redoItem.setEnabled(false);

         copyItem.setEnabled(false);
         cutItem.setEnabled(false);
         pasteItem.setEnabled(false);
      }
      else
      {
         enableFindNextPrev(true);

         boolean notBinary = !filePane.isBinary();

         findItem.setEnabled(notBinary);
         goToLineItem.setEnabled(notBinary);

         replaceItem.setEnabled(filePane.isEditable());

         boolean hasSelected = (filePane.getSelectedText() != null);

         copyItem.setEnabled(enable && hasSelected);

         if (enable && !filePane.isEditable())
         {
            enable = false;
         }

         cutItem.setEnabled(enable && hasSelected);
         pasteItem.setEnabled(enable);

         if (enable)
         {
            FileEditor editor = (FileEditor)filePane;

            undoItem.setEnabled(editor.canUndo());
            redoItem.setEnabled(editor.canRedo());
         }
         else
         {
            undoItem.setEnabled(enable);
            redoItem.setEnabled(enable);
         }
      }
   }

   @Override
   public void valueChanged(TreeSelectionEvent evt)
   {
      TreePath treePath = evt.getNewLeadSelectionPath();

      if (treePath == null)
      {
         return;
      }

      NavigationTreeNode tail = (NavigationTreeNode)treePath.getLastPathComponent();

      if (tail == null)
      {
         newFileItem.setEnabled(true);
         newFolderItem.setEnabled(true);
         deleteFileItem.setEnabled(false);
         deleteFileItem.setText("Delete Folder");
         reloadFileItem.setEnabled(false);
         removeFromProjectItem.setEnabled(false);
         navigatorRemoveFromProjectItem.setEnabled(false);

         navigatorNewFileItem.setEnabled(true);
         navigatorNewFolderItem.setEnabled(true);
         navigatorDeleteItem.setEnabled(false);
         navigatorReloadItem.setEnabled(false);
      }
      else if (tail instanceof EditorNode)
      {
         newFileItem.setEnabled(false);
         newFolderItem.setEnabled(false);

         navigatorNewFileItem.setEnabled(false);
         navigatorNewFolderItem.setEnabled(false);

         reloadFileItem.setEnabled(true);
         navigatorReloadItem.setEnabled(true);

         EditorNode node = (EditorNode)tail;
         FilePane filePane = node.getEditor();

         if (node.isEditable())
         {
            moveFileItem.setEnabled(true);
            navigatorMoveItem.setEnabled(true);
            renameFileItem.setEnabled(true);
            navigatorRenameItem.setEnabled(true);
         }
         else
         {
            moveFileItem.setEnabled(false);
            navigatorMoveItem.setEnabled(false);
            renameFileItem.setEnabled(false);
            navigatorRenameItem.setEnabled(false);
         }

         deleteFileItem.setText("Delete File");
         deleteFileItem.setEnabled(filePane.allowDelete());
         navigatorDeleteItem.setEnabled(deleteFileItem.isEnabled());
         removeFromProjectItem.setEnabled(deleteFileItem.isEnabled());
         navigatorRemoveFromProjectItem.setEnabled(deleteFileItem.isEnabled());

         try
         {
            fileTabPane.setSelectedComponent(node.getScrollPane());
         }
         catch (IllegalArgumentException e)
         {// file not open
         }
      }
      else
      {
         newFileItem.setEnabled(true);
         newFolderItem.setEnabled(true);
         deleteFileItem.setText("Delete Folder");

         navigatorNewFileItem.setEnabled(true);
         navigatorNewFolderItem.setEnabled(true);

         reloadFileItem.setEnabled(false);
         navigatorReloadItem.setEnabled(false);

         if (tail.getChildCount() == 0)
         {
            deleteFileItem.setEnabled(true);
            navigatorDeleteItem.setEnabled(true);
            removeFromProjectItem.setEnabled(true);
            navigatorRemoveFromProjectItem.setEnabled(true);
         }
         else
         {
            deleteFileItem.setEnabled(false);
            navigatorDeleteItem.setEnabled(false);
            removeFromProjectItem.setEnabled(false);
            navigatorRemoveFromProjectItem.setEnabled(false);
         }

         if (tail.isEditable())
         {
            moveFileItem.setEnabled(true);
            navigatorMoveItem.setEnabled(true);
            renameFileItem.setEnabled(true);
            navigatorRenameItem.setEnabled(true);
         }
         else
         {
            moveFileItem.setEnabled(false);
            navigatorMoveItem.setEnabled(false);
            renameFileItem.setEnabled(false);
            navigatorRenameItem.setEnabled(false);
         }
      }
   }

   @Override
   public void propertyChange(PropertyChangeEvent evt)
   {
      if ("progress".equals(evt.getPropertyName()))
      {
         progressPanel.setValue((Integer)evt.getNewValue());
      }
   }

   public TreeModel getNavigationModel()
   {
      return navigatorModel;
   }

   @Override
   public Date getSubmittedDate()
   {
      return null;
   }

   @Override
   public long getTimeOut()
   {
      return timeout;
   }

   @Override
   public void setTimeOut(long value)
   {
      timeout = value;
   }

   @Override
   public Vector<PassFile> getFiles()
   {
      Vector<RequiredFileEditor> requiredFiles = new Vector<RequiredFileEditor>();
      Vector<PassFile> additionalFiles = new Vector<PassFile>();

      PathNode root = (PathNode)navigatorModel.getRoot();

      addLeafNodes(requiredFiles, additionalFiles, root);

      if (requiredFiles.isEmpty())
      {
         return additionalFiles;
      }

      int total = requiredFiles.size() + additionalFiles.size();

      // required files must be in the correct order

      Vector<PassFile> files = new Vector<PassFile>(total);

      AssignmentData assignment = getAssignment();

      // ensure required files are in the correct order

      for (int i = 0; i < assignment.fileCount(); i++)
      {
         String filename = assignment.getFile(i);

         PassFile passFile = popMatchingFile(requiredFiles, filename);

         if (passFile != null)
         {
            files.add(passFile);
         }
      }

      // requiredFiles should now be empty, but if not add any
      // remaining

      files.addAll(requiredFiles);
      files.addAll(additionalFiles);

      return files;
   }

   private RequiredPassFile popMatchingFile(Vector<RequiredFileEditor> files,
      String filename)
   {
      for (int i = 0; i < files.size(); i++)
      {
         RequiredFileEditor editor = files.get(i);

         if (editor.getName().equals(filename))
         {
            files.remove(i);
            return (RequiredPassFile)editor;
         }
      }

      return null;
   }

   private void addLeafNodes(Vector<RequiredFileEditor> files, 
     Vector<PassFile> additionalFiles, TreeNode node)
   {
      if (node instanceof EditorNode)
      {
         FilePane pane = ((EditorNode)node).getEditor();

         if (pane instanceof RequiredFileEditor)
         {
            files.add((RequiredFileEditor)pane);
         }
         else if (pane instanceof PassFile)
         {
            additionalFiles.add((PassFile)pane);
         }
      }
      else
      {
         for (int i = 0, n = node.getChildCount(); i < n; i++)
         {
            addLeafNodes(files, additionalFiles, node.getChildAt(i));
         }
      }
   }

   @Override
   public Path getBasePath()
   {
      return project == null ? null : project.getBase();
   }

   public AssignmentData getAssignment()
   {
      return project.getAssignment();
   }

   public void setAssignment(AssignmentData assignment)
   {
      project.setAssignment(assignment);
   }

   public void updateInfoField()
   {
      infoField.setText(getMessage("info.course_assignment",
           project.getCourseCode(), project.getAssignmentTitle()));

      updateTitle();
   }

   public void updateTitle()
   {
      if (modified)
      {
         setTitle(getMessage("main.title_modified", 
           APP_NAME, project.getAssignmentTitle()));
      }
      else
      {
         setTitle(getMessage("main.title", APP_NAME, project.getAssignmentTitle()));
      }
   }

   @Override
   public String getEncoding()
   {
      return ENCODING_UTF8;
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

   public String getConfirmText(boolean markup)
   {
      if (markup)
      {
         String text = passTools.getMessage("message.i_we_confirm");

         int mnemonic = passTools.getMnemonic("message.i_we_confirm", 'I');

         String mnemStr = new String(Character.toChars(mnemonic));

         int idx = text.indexOf(mnemStr);

         if (idx == -1)
         {
            return String.format("<html>%s</html>", passTools.stringWrap(
             text, 80, "<br>"));
         }

         int mnemLength = mnemStr.length();
         mnemStr = String.format("<u>%s</u>", mnemStr);

         if (idx == 0)
         {
            return String.format("<html>%s</html>", passTools.stringWrap(
              mnemStr + text.substring(mnemLength), 80, "<br>"));
         }
         else
         {
            return String.format("<html>%s</html>", passTools.stringWrap(
              text.substring(0, idx) + mnemStr
              +text.substring(idx+mnemLength),
              80, "<br>"));
         }
      }
      else
      {
         return passTools.getConfirmText();
      }
   }

   @Override
   public boolean isGroupProject()
   {
      return project.getStudents().size() > 1;
   }

   @Override
   public Vector<Student> getProjectTeam()
   {
      return project.getStudents();
   }

   public Student getStudent()
   {
      return project.getStudent();
   }

   public void addStudent(Student student)
   {
      project.addStudent(student);
   }

   public boolean checkStudents()
   {
      if (getStudent() == null)
      {
         projectDetailsDialog.display();
      }

      return getStudent() != null;
   }

   public Project getProject()
   {
      return project;
   }

   public String getDefaultBaseName()
   {
      return passTools.getConfig().getDefaultBaseName(getAssignment(), getStudent());
   }

   @Override
   public String getApplicationName()
   {
      return APP_NAME;
   }

   @Override
   public String getApplicationVersion()
   {
      return APP_VERSION;
   }

   @Override
   public boolean isConfirmed()
   {
      return project != null && project.isConfirmed();
   }

   public boolean checkConfirmed()
   {
      if (!isConfirmed())
      {
         int result = confirm(getConfirmText(false), "Confirm",
           JOptionPane.YES_NO_OPTION);

         project.setConfirmed(result == JOptionPane.YES_OPTION);
      }

      return isConfirmed();
   }

   @Override
   public void transcriptMessage(String msg)
   {
      messageLn(msg);
   }

   public void error(JFrame parent, String msg)
   {
      dialogMessageArea.setText(msg);
      JOptionPane.showMessageDialog(parent, dialogMessageAreaSp, "Error",
        JOptionPane.ERROR_MESSAGE);

      if (warningBuffer == null)
      {
         messageLn(msg, "strong");
      }
      else
      {
         warningBuffer.append(msg);
         warningBuffer.append("<br>");
      }
   }

   public void error(JDialog parent, String msg)
   {
      dialogMessageArea.setText(msg);
      JOptionPane.showMessageDialog(parent, dialogMessageAreaSp, "Error",
        JOptionPane.ERROR_MESSAGE);
   }

   @Override
   public void error(String msg)
   {
      error(this, msg);
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

      if (isDebugMode())
      {
         e.printStackTrace();
      }
   }

   public void fatalError(Throwable e)
   {
      fatalError(null, e);
   }

   public void fatalError(String msg, Throwable e)
   {
      String errMess = String.format(
        "Fatal Error: %s", e.getMessage());

      if (msg != null)
      {
         errMess = String.format("%s%n%s", errMess, msg);
      }

      error(errMess);

      if (isDebugMode())
      {
         e.printStackTrace();
      }

      System.exit(1);
   }

   public void verbatim(CharSequence sequence)
   {
      HTMLDocument doc = (HTMLDocument)messageArea.getDocument();

      Element elem = doc.getElement("main");

      try
      {
         doc.insertBeforeEnd(elem, "<pre>"+replaceHTMLEntities(sequence)+"</pre>");

         messageArea.setCaretPosition(elem.getEndOffset()-1);
      }
      catch (BadLocationException | IOException e)
      {
         error(e);
      }
   }

   public void buildMessages(CharSequence sequence)
   {
      if (sequence.length() == 0) return;

      Pattern pattern = Pattern.compile("(^|\\R)([a-zA-Z0-9\\/_\\.\\-+]+):(\\d+)(?:\\:(\\d+))?:");

      Matcher m = pattern.matcher(replaceHTMLEntities(sequence));

      StringBuilder sb = new StringBuilder(sequence.length()+11);
      sb.append("<pre>");

      Path basePath = getBasePath();

      while (m.find())
      {
         String name = m.group(2);

         Path p = basePath.resolve(name);
         NavigationTreeNode node = null;

         if (Files.exists(p))
         {
            node = findNode(p.toFile());
         }

         if (node != null && node instanceof EditorNode)
         {
            int line=0;
            int col=0;

            try
            {
               line = Integer.parseInt(m.group(3));

               if (m.groupCount() == 4)
               {
                  col = Integer.parseInt(m.group(4));
               }
            }
            catch (NumberFormatException e)
            {// shouldn't happen (pattern match enforces format)
            }

            m.appendReplacement(sb, "$1");

            if (col > 0)
            {
               sb.append(String.format(
                 "<a href=\"%s?line=%d&amp;col=%d\">%s:%d:%d</a>:", 
                  p.toUri(), line, col, name, line, col));
            }
            else
            {
               sb.append(String.format("<a href=\"%s?line=%d\">%s:%d</a>:", 
                  p.toUri(), line, name, line));
            }
         }
         else
         {
            m.appendReplacement(sb, "$0");
         }
      }

      m.appendTail(sb);

      sb.append("</pre>");

      HTMLDocument doc = (HTMLDocument)messageArea.getDocument();

      Element elem = doc.getElement("main");

      try
      {
         doc.insertBeforeEnd(elem, sb.toString());

         messageArea.setCaretPosition(elem.getEndOffset()-1);
      }
      catch (BadLocationException | IOException e)
      {
         error(e);
      }
   }

   public void message(int cp)
   {
      if (cp <= Character.MAX_VALUE)
      {
         message((char)cp);
      }
      else
      {
         message(new String(Character.toChars(cp)));
      }
   }

   public void message(char c)
   {
      message(String.format("%c", c));
   }

   public void message(String msg)
   {
      message(msg, null);
   }

   public void message(String msg, String tag)
   {
      message(msg, tag, true);
   }

   public void message(String msg, boolean replaceEntities)
   {
      message(msg, null, replaceEntities);
   }

   public void message(String msg, String tag, boolean replaceEntities)
   {
      if (messageArea == null)
      {
         System.out.print(msg);
      }
      else
      {
         HTMLDocument doc = (HTMLDocument)messageArea.getDocument();

         Element elem = doc.getElement("main");
         Element lastElem = elem.getElement(elem.getElementCount()-1);

         String text = replaceEntities ? replaceHTMLEntities(msg).toString() : msg;

         if (tag != null)
         {
            text = String.format("<%s>%s</%s>", tag, text, tag);
         }

         try
         {
            doc.insertBeforeEnd(lastElem, text);

            messageArea.setCaretPosition(elem.getEndOffset()-1);
         }
         catch (BadLocationException | IOException e)
         {
            error(e);
         }
      }
   }

   public void messageLn(String msg)
   {
      messageLn(msg, null);
   }

   public void messageLn(String msg, boolean replaceEntities)
   {
      messageLn(msg, null, replaceEntities);
   }

   public void messageLn(String msg, String tag)
   {
      messageLn(msg, tag, true);
   }

   public void messageLn(String msg, String tag, boolean replaceEntities)
   {
      if (messageArea == null)
      {
         System.out.println(msg);
      }
      else
      {
         HTMLDocument doc = (HTMLDocument)messageArea.getDocument();
         Element elem = doc.getElement("main");

         Element lastElem = elem.getElement(elem.getElementCount()-1);

         String text = replaceEntities ? replaceHTMLEntities(msg).toString() : msg;

         if (tag != null)
         {
            text = String.format("<%s>%s</%s>", tag, text, tag);
         }

         try
         {
            if (replaceEntities || !text.endsWith("<br>"))
            {
               text = text+"<br>";
            }

            if (lastElem.getName().equals("p"))
            {
               doc.insertBeforeEnd(lastElem, text);
            }
            else
            {
               doc.insertBeforeEnd(elem, "<p>"+text);
            }

            messageArea.setCaretPosition(elem.getEndOffset()-1);
         }
         catch (BadLocationException | IOException e)
         {
            error(e);
         }
      }
   }

   public CharSequence replaceHTMLEntities(CharSequence sequence)
   {
      if (sequence.length() == 0) return sequence;

      StringBuilder result = new StringBuilder(sequence.length());

      for (int i = 0; i < sequence.length(); i++)
      {
         char c = sequence.charAt(i);

         if (c == '&')
         {
            result.append("&amp;");
         }
         else if (c == '<')
         {
            result.append("&lt;");
         }
         else if (c == '>')
         {
            result.append("&gt;");
         }
         else if (c == '\'')
         {
            result.append("&#x27;");
         }
         else if (c == '"')
         {
            result.append("&#x22;");
         }
         else
         {
            result.append(c);
         }
      }

      return result;
   }

   public int confirm(String msg, String title, int options)
   {
      return confirm(this, msg, title, options, JOptionPane.QUESTION_MESSAGE);
   }

   public int confirm(String msg, String title,
     int options, int msgType)
   {
      return confirm(this, msg, title, options, msgType);
   }

   public int confirm(JFrame parent, String msg, String title,
     int options, int msgType)
   {
      dialogMessageArea.setText(msg);

      return JOptionPane.showConfirmDialog(parent,
                 dialogMessageAreaSp, title, options, msgType);
   }

   @Override
   public boolean isDebugMode()
   {
      return messageType >= MESSAGE_DEBUG;
   }

   @Override
   public void debug(String msg)
   {
      if (messageType >= MESSAGE_DEBUG)
      {
         System.out.println(msg);
      }
   }

   public void debug(Throwable e)
   {
      if (messageType >= MESSAGE_DEBUG)
      {
         e.printStackTrace();
      }
   }

   @Override
   public void debugNoLn(String msg)
   {
      if (messageType >= MESSAGE_DEBUG)
      {
         System.out.print(msg);
      }
   }

   @Override
   public void warning(String msg)
   {
      if (warningBuffer == null)
      {
         messageLn(getMessage("message.warning", msg), "strong");
      }
      else
      {
         warningBuffer.append(getMessage("message.warning", msg));
         warningBuffer.append("<br>");
      }
   }

   @Override
   public void verboseCodePoint(int cp)
   {
      if (messageType >= MESSAGE_VERBOSE)
      {
         message(cp);
      }
   }

   @Override
   public void verbose(String msg)
   {
      if (messageType >= MESSAGE_VERBOSE)
      {
         messageLn(msg);
      }
   }

   public Course getCourse(String code) throws IOException,SAXException
   {
      if (courseData == null)
      {
         loadCourseData();
      }

      for (Course course : courseData)
      {
         if (course.getCode().equals(code))
         {
            return course;
         }
      }

      throw new InputResourceException(
         getMessage("error.unknown_course", code));
   }

   private void loadCourseData() throws IOException,SAXException
   {
      messageLn(getMessage("message.fetching_course"));

      URL resourceURL = getClass().getResource("/resources.xml");

      courseData = passTools.loadCourseData(resourceURL);

      if (!passTools.isAgreeRequired())
      {
         throw new InputResourceException(
           getMessage("error.agree_not_permitted", APP_NAME, resourceURL));
      }

      if (courseData.isEmpty())
      {
         throw new InputResourceException(getMessage(
           "error.no_courses", resourceURL));
      }
   }

   public Course requestCourse() throws IOException,SAXException
   {
      if (project.getCourse() != null)
      {
         return project.getCourse();
      }

      if (courseData == null)
      {
         loadCourseData();
      }

      int n = courseData.size();

      Course[] values = new Course[n];

      for (int i = 0; i < n; i++)
      {
         values[i] = courseData.get(i);
      }

      Object result = JOptionPane.showInputDialog(this,
        getMessage("course.select_prompt"),
        getMessage("course.select.title"),
         JOptionPane.QUESTION_MESSAGE, null,
         values, null);

      if (result == null)
      {
         return null;
      }

      project.setCourse((Course)result);

      return project.getCourse();
   }

   private void loadAssignments(Course course) throws SAXException,IOException
   {
      messageLn(getMessage("message.fetching_assignment"));

      assignments = passTools.loadAssignments(course);
   }

   public AssignmentData getAssignment(Course course, String assignmentLabel)
     throws SAXException,IOException
   {
      if (assignments == null)
      {
         loadAssignments(course);
      }

      for (AssignmentData assignment : assignments)
      {
         if (assignmentLabel.equals(assignment.getLabel()))
         {
            return assignment;
         }
      }

      throw new InputResourceException(getMessage(
        "error.unknown_assignment",
          project.getCourse(), assignmentLabel));
   }

   protected boolean readXML() throws SAXException,IOException
   {
      Course course = requestCourse();

      if (course == null) return false;

      if (assignments == null)
      {
         loadAssignments(course);
      }

      assignmentSelector = new AssignmentSelector(this, assignments, 
        course);

      assignmentSelector.setVisible(true);

      project.setConfirmed(assignmentSelector.isConfirmed());

      return project.getAssignment() != null;
   }

   private void createListingLanguages()
   {
      listingLanguages = new ListingLanguage[AssignmentData.LISTING_LANGUAGES.length];

      for (int i = 0; i < listingLanguages.length; i++)
      {
         listingLanguages[i] 
           = new ListingLanguage(this, AssignmentData.LISTING_LANGUAGES[i]);
      }
   }

   public ListingLanguage[] getListingLanguages()
   {
      return listingLanguages;
   }

   protected void version()
   {
      System.out.println(getMessage("message.about", 
       APP_NAME, APP_VERSION, APP_DATE, COPYRIGHT_OWNER, ABOUT_URL,
         PASSLIB_VERSION, PASSLIB_VERSION_DATE));
   }

   protected void help()
   {
      System.out.println(getMessage("syntax.summary", APP_NAME));

      System.out.println();
      System.out.println(getMessage("syntax.options"));
      System.out.println();

      System.out.println(getMessage("syntax.help", "--help", "-h"));
      System.out.println(getMessage("syntax.version", "--version", "-v"));
      System.out.println();

      System.out.println(getMessage("syntax.messages", "--messages",
        "silent", "verbose", "debug"));
      System.out.println(getMessage("syntax.silent", "--silent", "--messages silent"));
      System.out.println(getMessage("syntax.debug", "--debug", "--messages debug"));
   }

   protected void parseArgs(String[] args) throws InvalidSettingException
   {
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--help") || args[i].equals("-h"))
         {
            help();
            System.exit(0);
         }
         else if (args[i].equals("--version") || args[i].equals("-v"))
         {
            version();
            System.exit(0);
         }
         else if (args[i].equals("--debug"))
         {
            setMessageType(MESSAGE_DEBUG);
         }
         else if (args[i].startsWith("-"))
         {
            String[] split = args[i].split("=");
            String argName = split[0];
            String argValue = null;

            if (split.length == 1)
            {
               i++;

               if (i < args.length)
               {
                  argValue = args[i];
               }
            }
            else
            {
               argValue = split[1];
            }

            if (argName.equals("--messages"))
            {
               if (argValue == null)
               {
                  throw new InvalidSettingException(
                     getMessage("error.missing_value", argName));
               }

               try
               {
                  setMessageType(argName);
               }
               catch (IllegalArgumentException e)
               {
                  throw new InvalidSettingException(
                    getMessage("error.invalid_setting", argName, argValue), e);
               }
            }
            else
            {
               throw new InvalidSettingException(
                  getMessage("error.unknown_option", argName));
            }
         }
         else
         {
            throw new InvalidSettingException(
               getMessage("error.unknown_option", args[i]));
         }
      }
   }

   public void setMessageType(String value) throws IllegalArgumentException
   {
      try
      {
         setMessageType(Integer.parseInt(value));
      }
      catch (NumberFormatException e)
      {
         if (value.equals("silent"))
         {
            messageType = MESSAGE_SILENT;
         }
         else if (value.equals("verbose"))
         {
            messageType = MESSAGE_VERBOSE;
         }
         else if (value.equals("debug"))
         {
            messageType = MESSAGE_DEBUG;
         }
         else
         {
            throw new IllegalArgumentException(
              getMessage("error.invalid_message_type", value));
         }
      }
   }

   public void setMessageType(int value) throws IllegalArgumentException
   {
      switch (value)
      {
         case MESSAGE_SILENT:
         case MESSAGE_VERBOSE:
         case MESSAGE_DEBUG:
            messageType = value;
         break;
         default:
            throw new IllegalArgumentException(
              getMessage("error.invalid_message_type", value));
      }
   }

   private void initialise()
   {
      SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>()
      {
         @Override
         public Void doInBackground()
         {
             Cursor orgCursor = getCursor();
             setCursor(new Cursor(Cursor.WAIT_CURSOR));

             try
             {
                createAndShowGUI();
             }
             catch (Exception e)
             {
                System.err.println(e.getMessage());
                e.printStackTrace();
                System.exit(2);
             }
             finally
             {
                setCursor(orgCursor);
             }

             return null;
         }
      };

      worker.execute();
   }

   public static void createEditor(final String[] args)
   {
      SwingUtilities.invokeLater(new Runnable()
      {
          public void run()
          {
             try
             {
                PassEditor gui = new PassEditor();

                gui.showStartUpFrame();

                gui.parseArgs(args);

                gui.initialise();
             }
             catch (Exception e)
             {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Fatal Error",
                   JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
             }
          }
      });
   }

   public static void main(String[] args)
   {
      createEditor(args);
   }

   private PassTools passTools;
   private PassGuiTools passGuiTools;

   private Project project;
   private Vector<Course> courseData;
   private Vector<AssignmentData> assignments;

   private ListingLanguage[] listingLanguages;

   private long timeout=60L;

   private AssignmentProcess currentProcess=null;
   private AssignmentProcessWorker currentWorker = null;

   private Properties properties;

   private Font editorFont, manualFont;

   private NodeTransferHandler nodeTransferHandler;

   private JFrame startUpFrame;
   private JEditorPane messageArea;
   private JTextArea dialogMessageArea;
   private JScrollPane dialogMessageAreaSp, messageAreaSp;
   private JLabel infoField;
   private JTabbedPane fileTabPane;
   private JTree navigatorTree;
   private DefaultTreeModel navigatorModel;
   private ProgressPanel progressPanel;
   private AssignmentSelector assignmentSelector;
   private ProjectDetailsDialog projectDetailsDialog;
   private JTextArea compilerLogPane, stdErrPane, stdOutPane;
   private JTabbedPane logPane;

   private FilePaneProperties filePaneProperties;

   private JMenuItem saveItem, saveAllItem, openPdfItem, 
       runPassItem, buildItem, exportItem,
       goToLineItem, findItem, findAgainItem, findPreviousItem, replaceItem,
       copyItem, cutItem, pasteItem, undoItem, redoItem,
       newFolderItem, newFileItem, deleteFileItem, moveFileItem,
       renameFileItem, importFileItem, reloadFileItem, removeFromProjectItem;

   private JPopupMenu navigatorPopup, editorPopup;
   private JMenuItem editorCopyItem, editorCutItem, editorPasteItem,
    editorUndoItem, editorRedoItem,
    editorGoToLineItem, editorFindItem, editorFindAgainItem, 
    editorFindPreviousItem, editorReplaceItem,
    navigatorNewFolderItem, navigatorNewFileItem, navigatorDeleteItem,
    navigatorMoveItem, navigatorRenameItem, navigatorImportItem,
    navigatorReloadItem, navigatorRemoveFromProjectItem;

   private JFileChooser dirChooser, projectFileChooser, pdfFileChooser,
      importFileChooser;

   private SelectProjectDialog selectProjectDialog;
   private FileDirectoryDialog fileDirectoryDialog;
   private GoToLineDialog goToLineDialog;
   private FindDialog findDialog;
   private PropertiesDialog propertiesDialog;
   private JComponent licenceComp;

   private Vector<AbstractButton> toolbarButtons;
   private Vector<JSeparator> toolbarSeparators;

   private HelpBroker mainHelpBroker = null;
   private CSH.DisplayHelpFromSource csh = null;
   private JHelp jhelp = null;
   private JEditorPane helpEditorPane = null;

   private String helpLocaleId;

   private boolean completed = false, modified = false;

   private FileSearcher fileSearcher = null;

   public static final int FILE_SEARCH_MAX=100;

   private StringBuilder warningBuffer = null;

   public static final int MESSAGE_SILENT=0, MESSAGE_VERBOSE=1, MESSAGE_DEBUG=2;

   private int messageType = MESSAGE_SILENT;

   public static final String APP_NAME = "PASS Editor";
   public static final String APP_VERSION = "1.3.1";
   public static final String APP_DATE = "2022-11-16";

   private static final String COPYRIGHT_OWNER="Nicola L.C. Talbot";
   private static final String ABOUT_URL="https://www.dickimaw-books.com/software/pass/";

   private static final int START_COPYRIGHT_YEAR=2020;

   private static final String DEFAULT_PROP_NAME = ".progassignsys.prop";

   public static final String FILE_TABS_NAME = "filetabs";
}
