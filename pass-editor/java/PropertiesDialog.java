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

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.event.*;

public class PropertiesDialog extends JDialog 
 implements ActionListener
{
   public PropertiesDialog(PassEditor gui)
   {
      super(gui, gui.getMessage("properties.title"));
      this.gui = gui;

      JTabbedPane tabbedPane = new JTabbedPane();
      getContentPane().add(tabbedPane, "Center");

      JPanel editorPanel = new JPanel(new BorderLayout());
      codeFontPanel = new FontPanel(this, true);
      codeFontPanel.setBorder(BorderFactory.createTitledBorder(
         gui.getMessage("properties.code_font")));
      editorPanel.add(codeFontPanel, "Center");

      locationPanel = new CursorLocationPanel(this);
      locationPanel.setBorder(BorderFactory.createTitledBorder(
         gui.getMessage("properties.cursor_location")));
      editorPanel.add(locationPanel, "North");

      addTab(tabbedPane, editorPanel, 
       gui.getMessage("properties.code_editor"), 
       gui.getMnemonic("properties.code_editor"));

      manualFontPanel = new FontPanel(this, false);

      addTab(tabbedPane, manualFontPanel, 
       gui.getMessage("properties.manual_font"), 
       gui.getMnemonic("properties.manual_font"));

      uiPanel = new UIPanel(this);

      addTab(tabbedPane, new JScrollPane(uiPanel), 
       gui.getMessage("properties.ui"), 
       gui.getMnemonic("properties.ui"));

      JComponent bottomPanel = new JPanel(new BorderLayout());
      getContentPane().add(bottomPanel, "South");

      bottomPanel.add(gui.createHelpButton("properties"), "East");

      JComponent buttonPanel = new JPanel();
      bottomPanel.add(buttonPanel, "Center");

      buttonPanel.add(createJButton("okay", null, true));

      buttonPanel.add(createJButton("cancel", 
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), false));

      pack();
      setLocationRelativeTo(gui);
   }

   protected JButton createJButton(String action, KeyStroke keyStroke, 
      boolean isDefault)
   {
      String label = "button."+action;

      return gui.createJButton(gui.getMessage(label), gui.getMnemonic(label),
        action, this, getRootPane(), keyStroke, isDefault);
   }

   private void addTab(JTabbedPane tabbedPane, JComponent component,
      String title, int mnemonic)
   {
      tabbedPane.addTab(title, component);

      if (mnemonic != -1)
      {
         int idx = tabbedPane.getTabCount()-1;
         tabbedPane.setMnemonicAt(idx, mnemonic);
      }
   }

   protected JRadioButton createJRadioButton(String label, int mnemonic, ButtonGroup grp,
     String action)
   {
      return createJRadioButton(label, mnemonic, grp, action, this);
   }

   protected JRadioButton createJRadioButton(String label, int mnemonic, ButtonGroup grp,
     String action, ActionListener listener)
   {
      JRadioButton button = new JRadioButton(label);

      grp.add(button);

      if (mnemonic != -1)
      {
         button.setMnemonic(mnemonic);
      }

      if (action != null)
      {
         button.setActionCommand(action);
         button.addActionListener(listener);
      }

      return button;
   }

   protected JCheckBox createCheckBox(String propName)
   {
      return createCheckBox(gui.getMessage(propName), 
         gui.getMnemonic(propName), null, false, null, this);
   }

   protected JCheckBox createCheckBox(String label, int mnemonic, String action)
   {
      return createCheckBox(label, mnemonic, action, false, null, this);
   }

   protected JCheckBox createCheckBox(String label, int mnemonic, String action,
     ActionListener listener)
   {
      return createCheckBox(label, mnemonic, action, false, null, listener);
   }

   protected JCheckBox createCheckBox(String label, int mnemonic, String action,
     boolean selected)
   {
      return createCheckBox(label, mnemonic, action, selected, null, this);
   }

   protected JCheckBox createCheckBox(String label, int mnemonic, String action,
     boolean selected, ActionListener listener)
   {
      return createCheckBox(label, mnemonic, action, selected, null, listener);
   }

   protected JCheckBox createCheckBox(String label, int mnemonic,
     String action, boolean selected, String iconName)
   {
      return createCheckBox(label, mnemonic, action, selected, iconName, this);
   }

   protected JCheckBox createCheckBox(String label, int mnemonic,
     String action, boolean selected, String iconName, ActionListener listener)
   {
      JCheckBox box = new JCheckBox(label);
      box.setSelected(selected);

      if (mnemonic != -1)
      {
         box.setMnemonic(mnemonic);
      }

      if (iconName != null)
      {
         box.setIcon(gui.getToolIcon(iconName));
      }

      if (listener != null)
      {
         if (action != null)
         {
            box.setActionCommand(action);
         }

         box.addActionListener(listener);
      }

      return box;
   }

   protected JTextArea createTextArea(String label, Object... params)
   {
      JTextArea textarea = new JTextArea(gui.getMessage(label, params));
      textarea.setLineWrap(true);
      textarea.setWrapStyleWord(true);
      textarea.setEditable(false);

      return textarea;
   }

   protected JTextArea createTextArea(int rows, int columns, 
       String label, Object... params)
   {
      JTextArea textarea = new JTextArea(gui.getMessage(label, params), 
        rows, columns);
      textarea.setLineWrap(true);
      textarea.setWrapStyleWord(true);
      textarea.setEditable(false);

      return textarea;
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null) return;

      if (command.equals("okay"))
      {
         gui.setEditorFont(codeFontPanel.getSampleFont());
         gui.setManualFont(manualFontPanel.getSampleFont());
         uiPanel.okay();
         locationPanel.okay();
         setVisible(false);
      }
      else if (command.equals("cancel"))
      {
         setVisible(false);
      }
   }

   public void display()
   {
      codeFontPanel.setSampleFont(gui.getEditorFont());
      manualFontPanel.setSampleFont(gui.getManualFont());
      uiPanel.reset();
      locationPanel.reset();
      pack();
      setVisible(true);
   }


   private UIPanel uiPanel;
   private CursorLocationPanel locationPanel;
   private FontPanel codeFontPanel, manualFontPanel;

   protected PassEditor gui;
}

class UIPanel extends JPanel
{
   protected UIPanel(PropertiesDialog dialog)
   {
      super();
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      gui = dialog.gui;
      init(dialog);
   }

   private void init(PropertiesDialog dialog)
   {
      JComponent comp = new JPanel();
      add(comp);

      add(dialog.createTextArea(3, 60, "properties.lookandfeel.info"));

      comp = new JPanel();
      add(comp);

      JLabel label = gui.createJLabel("properties.lookandfeel");
      comp.add(label);

      LookAndFeel lookandfeel = UIManager.getLookAndFeel();
      String current = lookandfeel.getClass().getName();

      lookAndFeelInfo = UIManager.getInstalledLookAndFeels();
      String[] names = new String[lookAndFeelInfo.length];

      int selectedIdx = -1;

      for (int i = 0; i < lookAndFeelInfo.length; i++)
      {
         names[i] = lookAndFeelInfo[i].getName();

         if (selectedIdx == -1 && lookAndFeelInfo[i].getClassName().equals(current))
         {
            selectedIdx = i;
            currentLookAndFeelInfo = lookAndFeelInfo[i];
         }
      }

      lookAndFeelBox = new JComboBox<String>(names);
      label.setLabelFor(lookAndFeelBox);
      comp.add(lookAndFeelBox);

      if (selectedIdx != -1)
      {
         lookAndFeelBox.setSelectedIndex(selectedIdx);
      }

      comp = new JPanel();
      add(comp);

      JLabel fontSizeLabel = gui.createJLabel("properties.widgetfontsize");
      comp.add(fontSizeLabel);

      widgetFontSample = new JLabel("Aa");

      widgetNumberSpinnerModel = new SpinnerNumberModel(12, 1, 900, 1);
      widgetNumberSpinner = new JSpinner(widgetNumberSpinnerModel);
      fontSizeLabel.setLabelFor(widgetNumberSpinner);
      widgetNumberSpinner.addChangeListener(new ChangeListener()
      {
         public void stateChanged(ChangeEvent evt)
         {
            Font f = widgetFontSample.getFont();
            int size = f.getSize();
            int newSize = widgetNumberSpinnerModel.getNumber().intValue();

            if (newSize != size)
            {
               widgetFontSample.setFont(f.deriveFont((float)newSize));
            }
         }
      });

      comp.add(widgetNumberSpinner);

      Object value = UIManager.get("defaultFont");

      comp.add(widgetFontSample);

      comp.add(Box.createHorizontalStrut(10));
      comp.add(gui.createJLabel("properties.restart_required"));

      comp = new JPanel();
      add(comp);

      comp.add(gui.createJLabel("properties.toolbarbuttons.info"));

      ButtonGroup grp = new ButtonGroup();

      smallIconsButton = dialog.createJRadioButton(
        gui.getMessage("properties.toolbarbuttons.small"),
        gui.getMnemonic("properties.toolbarbuttons.small"),
        grp, null
      );
      comp.add(smallIconsButton);
      comp.add(new JLabel(gui.getToolIcon("general/Preferences", null, true)));

      comp.add(Box.createHorizontalStrut(10));

      largeIconsButton = dialog.createJRadioButton(
        gui.getMessage("properties.toolbarbuttons.large"),
        gui.getMnemonic("properties.toolbarbuttons.large"),
        grp, null
      );
      comp.add(largeIconsButton);
      comp.add(new JLabel(gui.getToolIcon("general/Preferences", null, false)));

      if (gui.getBooleanProperty("toolbariconslarge", true))
      {
         largeIconsButton.setSelected(true);
      }
      else
      {
         smallIconsButton.setSelected(true);
      }

      comp.add(Box.createHorizontalStrut(20));
      comp.add(gui.createJLabel("properties.restart_required"));

      Vector<AbstractButton> toolBarButtons = gui.getToolBarButtons();

      int n = toolBarButtons.size();

      comp = new JPanel(new GridLayout((int)Math.ceil(n/3.0), 3));
      comp.setBorder(BorderFactory.createTitledBorder(gui.getMessage(
         "properties.toolbarbuttons.visible")));
      add(comp);

      toolBarButtonPanels = new Vector<ToolBarButtonPanel>(n);

      for (AbstractButton button : toolBarButtons)
      {
         ToolBarButtonPanel p = new ToolBarButtonPanel(button);
         toolBarButtonPanels.add(p);
         comp.add(p);
      }

      comp = new JPanel();
      comp.setBorder(BorderFactory.createTitledBorder(
         gui.getMessage("properties.toolbarseps")));
      add(comp);

      Vector<JSeparator> separators = gui.getToolBarSeparators();

      toolBarSeparatorPanels = new Vector<ToolBarSeparatorPanel>(separators.size());

      for (JSeparator sep : separators)
      {
         ToolBarSeparatorPanel p = new ToolBarSeparatorPanel(dialog, sep);
         toolBarSeparatorPanels.add(p);
         comp.add(p);
      }
   }

   public void reset()
   {
      lookAndFeelBox.setSelectedItem(currentLookAndFeelInfo.getName());
      widgetNumberSpinnerModel.setValue(gui.getWidgetFontSize());

      for (ToolBarButtonPanel p : toolBarButtonPanels)
      {
         p.reset();
      }

      for (ToolBarSeparatorPanel p : toolBarSeparatorPanels)
      {
         p.reset();
      }
   }

   public void okay()
   {
      gui.setWidgetFontSizes(widgetNumberSpinnerModel.getNumber().floatValue(),
            false);

      for (ToolBarButtonPanel p : toolBarButtonPanels)
      {
         p.okay();
      }

      for (ToolBarSeparatorPanel p : toolBarSeparatorPanels)
      {
         p.okay();
      }

      int idx = lookAndFeelBox.getSelectedIndex();
      UIManager.LookAndFeelInfo info = lookAndFeelInfo[idx];
      LookAndFeel lookandfeel = UIManager.getLookAndFeel();
      String current = lookandfeel.getClass().getName();

      if (!current.equals(info.getClassName()))
      {
         try
         {
            gui.setUI(info.getClassName());
            currentLookAndFeelInfo = info;
         }
         catch (Exception e)
         {// shouldn't happen
            gui.error(e);
            e.printStackTrace();
            return;
         }
      }

      gui.setBooleanProperty("toolbariconslarge", largeIconsButton.isSelected());
   }

   private SpinnerNumberModel widgetNumberSpinnerModel;
   private JSpinner widgetNumberSpinner;
   private Vector<ToolBarButtonPanel> toolBarButtonPanels;
   private Vector<ToolBarSeparatorPanel> toolBarSeparatorPanels;
   private JRadioButton smallIconsButton, largeIconsButton;

   private JComboBox<String> lookAndFeelBox;
   private JLabel widgetFontSample;

   private UIManager.LookAndFeelInfo currentLookAndFeelInfo;
   private UIManager.LookAndFeelInfo[] lookAndFeelInfo;

   private PassEditor gui;
}

class ToolBarSeparatorPanel extends JPanel
{
   protected ToolBarSeparatorPanel(PropertiesDialog dialog, JSeparator separator)
   {
      super();
      this.separator = separator;

      checkbox = dialog.createCheckBox("toolbarsep."+separator.getName());
      add(checkbox);
   }

   public void reset()
   {
      checkbox.setSelected(separator.isVisible());
   }

   public void okay()
   {
      separator.setVisible(checkbox.isSelected());
   }

   private JSeparator separator;
   private JCheckBox checkbox;
}

class CursorLocationPanel extends JPanel implements ActionListener
{
   protected CursorLocationPanel(PropertiesDialog dialog)
   {
      super();
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
      gui = dialog.gui;
      init(dialog);
   }

   private void init(PropertiesDialog dialog)
   {
      JComponent comp = new JPanel();
      add(comp);

      comp.add(new JLabel(gui.getMessage("properties.display")));

      locationSample = new JLabel();
      comp.add(locationSample);

      comp = new JPanel();
      add(comp);

      filePaneProperties = new FilePaneProperties();
      comp.add(new JLabel(gui.getMessage("properties.show")));

      showLineBox = dialog.createCheckBox(
        gui.getMessage("properties.line_number"),
        gui.getMnemonic("properties.line_number"),
        "locationupdate", this);

      comp.add(showLineBox);

      showColumnBox = dialog.createCheckBox(
        gui.getMessage("properties.column_number"),
        gui.getMnemonic("properties.column_number"),
        "locationupdate", this);

      comp.add(showColumnBox);

      showPositionBox = dialog.createCheckBox(
        gui.getMessage("properties.position_number"),
        gui.getMnemonic("properties.position_number"),
        "locationupdate", this);

      comp.add(showPositionBox);

      comp = new JPanel();
      add(comp);

      comp.add(new JLabel(gui.getMessage("properties.format")));

      ButtonGroup grp = new ButtonGroup();

      locationLongButton = dialog.createJRadioButton(
        gui.getMessage("properties.format_long"),
        gui.getMnemonic("properties.format_long"), grp, "locationupdate", this);

      comp.add(locationLongButton);

      locationShortButton = dialog.createJRadioButton(
        gui.getMessage("properties.format_short"),
        gui.getMnemonic("properties.format_short"), grp, "locationupdate", this);

      comp.add(locationShortButton);

      locationNumericButton = dialog.createJRadioButton(
        gui.getMessage("properties.format_numeric"),
        gui.getMnemonic("properties.format_numeric"), grp, "locationupdate", this);

      comp.add(locationNumericButton);

   }

   public void reset()
   {
      filePane = gui.getSelectedFilePaneTab();

      filePaneProperties.set(gui.getFilePaneProperties());

      showLineBox.setSelected(filePaneProperties.isShowLines());
      showColumnBox.setSelected(filePaneProperties.isShowColumns());
      showPositionBox.setSelected(filePaneProperties.isShowPosition());

      switch (filePaneProperties.getLocationFormat())
      {
         case LONG:
            locationLongButton.setSelected(true);
         break;
         case SHORT:
            locationShortButton.setSelected(true);
         break;
         case NUMERIC:
            locationNumericButton.setSelected(true);
         break;
      }

      locationSample.setText(filePaneProperties.formatLocation(filePane));

   }

   public void okay()
   {
      gui.setFilePaneProperties(filePaneProperties);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null) return;

      if (command.equals("locationupdate"))
      {
         updateLocationFormat();
      }
   }

   private void updateLocationFormat()
   {
      filePaneProperties.setShowLines(showLineBox.isSelected());
      filePaneProperties.setShowColumns(showColumnBox.isSelected());
      filePaneProperties.setShowPosition(showPositionBox.isSelected());

      if (locationLongButton.isSelected())
      {
         filePaneProperties.setLocationFormat(FilePaneProperties.LocationFormat.LONG);
      }
      else if (locationShortButton.isSelected())
      {
         filePaneProperties.setLocationFormat(FilePaneProperties.LocationFormat.SHORT);
      }
      else if (locationNumericButton.isSelected())
      {
         filePaneProperties.setLocationFormat(FilePaneProperties.LocationFormat.NUMERIC);
      }

      locationSample.setText(filePaneProperties.formatLocation(filePane));
   }

   private JLabel locationSample;
   private JCheckBox showLineBox, showColumnBox, showPositionBox;
   private JRadioButton locationLongButton, locationShortButton, locationNumericButton;
   private FilePaneProperties filePaneProperties;

   private FilePane filePane;
   private PassEditor gui;
}

class ToolBarButtonPanel extends JPanel
{
   public ToolBarButtonPanel(AbstractButton button)
   {
      super(new FlowLayout(FlowLayout.LEADING));
      this.button = button;
      setAlignmentX(0.0f);
      String text = button.getText();

      if (text == null || text.isEmpty())
      {
         text = button.getToolTipText();
      }

      checkbox = new JCheckBox();
      checkbox.setSelected(button.isVisible());
      add(checkbox);
      add(new JLabel(text, button.getIcon(), SwingConstants.LEFT));
   }

   public void okay()
   {
      button.setVisible(checkbox.isSelected());
   }

   public void reset()
   {
      checkbox.setSelected(button.isVisible());
   }

   public String toString()
   {
      return String.format("%s[checkbox=%s,button=%s]", 
         getClass().getSimpleName(), checkbox, button);
   }

   private AbstractButton button;
   private JCheckBox checkbox;
}

class FontPanel extends JPanel
    implements ActionListener,ChangeListener,ItemListener
{
   protected FontPanel(PropertiesDialog dialog, boolean monoselector)
   {
      super(new BorderLayout());

      init(dialog, monoselector);
   }

   private void init(PropertiesDialog dialog, boolean monoselector)
   {
      PassEditor gui = dialog.gui;

      String sampleLine1, sampleLine2;

      String monoTest1=null;
      String monoTest2=null;

      if (monoselector)
      {
         sampleLine1 = gui.getMessage("properties.code_sample_1");
         sampleLine2 = gui.getMessage("properties.code_sample_2");

         int testLen = (int)Math.min(sampleLine1.length(), sampleLine2.length());
         monoTest1 = sampleLine1.substring(0, testLen);
         monoTest2 = sampleLine2.substring(0, testLen);

         sampleArea = new JTextArea(String.format("%s\n%s", sampleLine1, sampleLine2));
         ((JTextArea)sampleArea).setLineWrap(true);
         ((JTextArea)sampleArea).setWrapStyleWord(true);
      }
      else
      {
         sampleLine1 = gui.getMessage("properties.notcode_sample_1");
         sampleLine2 = gui.getMessage("properties.notcode_sample_2");

         sampleArea = new JEditorPane("text/html", String.format(
          "<html><head><style>kbd {padding-left: 2px; padding-right: 2px; color: purple; font-weight: bold; background-color: #eeeeee;} .menu {font-family: sans-serif; font-weight: bold; } .symbol{ font-family: Symbola, D050000L, FreeSerif, sans-serif; }</style></head><body>%s<br>%s</body></html>",
          sampleLine1, sampleLine2));

         sampleArea.putClientProperty(
            JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
      }

      sampleArea.setMargin(new Insets(10, 10, 10, 10));
      sampleArea.setEditable(false);
      add(new JScrollPane(sampleArea), "Center");
   
      JComponent mainPanel = new JPanel();
      add(mainPanel, "North");
   
      mainPanel.add(Box.createVerticalGlue());
   
      JComponent subPanel = Box.createHorizontalBox();
      mainPanel.add(subPanel);

      GraphicsEnvironment env =
         GraphicsEnvironment.getLocalGraphicsEnvironment();
      Font fonts[] = env.getAllFonts();
   
      Vector<String> monoFonts = null;

      if (monoselector)
      {
         monoFonts = new Vector<String>();
      }

      Vector<String> allFonts = new Vector<String>();
   
      FontRenderContext frc = new FontRenderContext(null, 
        RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT, 
        RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);

      for (Font f : fonts)
      {
         if (allFonts.contains(f.getFamily()))
         {
            continue;
         }
   
         allFonts.add(f.getFamily());

         if (monoselector && f.canDisplay('a'))
         {
            f = f.deriveFont(10.0f);

            Rectangle2D bounds1 = f.getStringBounds(monoTest1, frc);
            Rectangle2D bounds2 = f.getStringBounds(monoTest2, frc);

            int w = (int)bounds1.getWidth();
            int h = (int)bounds2.getWidth();
            
            if (w == h && w > 0)
            {
               monoFonts.add(f.getFamily());
            }
         }
      }
   
      JComponent allFontsComp;

      if (monoselector)
      {
         monoOnlyBox = dialog.createCheckBox(
           gui.getMessage("properties.monospaced"),
           gui.getMnemonic("properties.monospaced"), "monospaced", true, this);

         monoOnlyBox.addActionListener(this);
         subPanel.add(monoOnlyBox);
         subPanel.add(Box.createHorizontalStrut(10));

         fontFamilyLayout = new CardLayout();
         fontFamilyPanel = new JPanel(fontFamilyLayout);
         subPanel.add(fontFamilyPanel);
   
         Box monoFontsComp = Box.createHorizontalBox();
         fontFamilyPanel.add(monoFontsComp, "monofonts");
   
         JLabel fontFamilyLabel = gui.createJLabel("properties.font");
         monoFontsComp.add(fontFamilyLabel);
   
         monoFontFamilyBox = new JComboBox<String>(monoFonts);
         monoFontFamilyBox.addItemListener(this);
         fontFamilyLabel.setLabelFor(monoFontFamilyBox);
         monoFontsComp.add(monoFontFamilyBox);

         allFontsComp = Box.createHorizontalBox();
         fontFamilyPanel.add(allFontsComp, "allfonts");
      }
      else
      {
         allFontsComp = subPanel;
      }
   
      JLabel fontFamilyLabel = gui.createJLabel("properties.font");
      allFontsComp.add(fontFamilyLabel);
   
      fontFamilyBox = new JComboBox<String>(allFonts);
      fontFamilyBox.setSelectedItem("FreeSans");
      fontFamilyBox.addItemListener(this);
      fontFamilyLabel.setLabelFor(fontFamilyBox);
      allFontsComp.add(fontFamilyBox);

      JLabel fontSizeLabel = gui.createJLabel("properties.fontsize");
      subPanel.add(fontSizeLabel);

      fontSizeModel = new SpinnerNumberModel(Integer.valueOf(10),
         Integer.valueOf(1), Integer.valueOf(900), Integer.valueOf(1));

      fontSizeSpinner = new JSpinner(fontSizeModel);
      fontSizeSpinner.addChangeListener(this);
      subPanel.add(fontSizeSpinner);
      fontSizeLabel.setLabelFor(fontSizeSpinner);

      subPanel.add(Box.createHorizontalGlue());

      subPanel = Box.createHorizontalBox();
      mainPanel.add(subPanel);
   
      boldBox = dialog.createCheckBox(gui.getMessage("properties.bold"), 
         gui.getMnemonic("properties.bold"), "fontupdate", this);

      subPanel.add(boldBox);

      subPanel.add(Box.createHorizontalStrut(10));
   
      italicBox = dialog.createCheckBox(gui.getMessage("properties.italic"), 
        gui.getMnemonic("properties.italic"), "fontupdate", this);

      subPanel.add(italicBox);
   
      if (monoselector)
      {
         add(dialog.createTextArea("properties.code_font.info"), "South"); 
      }

      //mainPanel.add(Box.createVerticalGlue());
   }

   public Font getSampleFont()
   {
      return sampleArea.getFont();
   }

   private void updateSample()
   {
      sampleArea.setFont(getSelectedFont());
   }

   private Font getSelectedFont()
   {
      int style = 0;

      if (boldBox.isSelected())
      {
         style = style | Font.BOLD;
      }

      if (italicBox.isSelected())
      {
         style = style | Font.ITALIC;
      }

      String family;

      if (monoOnlyBox != null && monoOnlyBox.isSelected())
      {
         family = (String)monoFontFamilyBox.getSelectedItem();
      }
      else
      {
         family = (String)fontFamilyBox.getSelectedItem();
      }

      return new Font(family, style, fontSizeModel.getNumber().intValue());
   }

   public void setSampleFont(Font f)
   {
      String family = f.getFamily();

      if (monoOnlyBox != null && monoOnlyBox.isSelected())
      {
         monoFontFamilyBox.setSelectedItem(family);

         if (!monoFontFamilyBox.getSelectedItem().equals(family))
         {
            // not monospaced

            monoOnlyBox.setSelected(false);
            fontFamilyBox.setSelectedItem(family);
         }

      }
      else
      {
         fontFamilyBox.setSelectedItem(family);
      }

      fontSizeModel.setValue(Integer.valueOf(f.getSize()));

      boldBox.setSelected(f.isBold());
      italicBox.setSelected(f.isItalic());
   }

   @Override
   public void stateChanged(ChangeEvent evt)
   {
      Object src = evt.getSource();

      if (src == fontSizeSpinner)
      {
         updateSample();
      }
   }

   @Override
   public void itemStateChanged(ItemEvent evt)
   {
      if (evt.getSource() instanceof JComboBox)
      {
         updateSample();
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String command = evt.getActionCommand();

      if (command == null) return;

      if (command.equals("fontupdate"))
      {
         updateSample();
      }
      else if (command.equals("monospaced") && fontFamilyLayout != null)
      {
         if (monoOnlyBox.isSelected())
         {
            fontFamilyLayout.show(fontFamilyPanel, "monofonts");
         }
         else
         {
            fontFamilyLayout.show(fontFamilyPanel, "allfonts");
         }

         updateSample();
      }
   }

   private JComboBox<String> fontFamilyBox, monoFontFamilyBox;
   private JSpinner fontSizeSpinner;
   private SpinnerNumberModel fontSizeModel;
   private JCheckBox boldBox, italicBox, monoOnlyBox;
   private JTextComponent sampleArea;
   private CardLayout fontFamilyLayout;
   private JComponent fontFamilyPanel;
}
