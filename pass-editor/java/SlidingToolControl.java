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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class SlidingToolControl extends JPanel
{
   protected AbstractButton block, unit, endPoint;

   protected SlidingToolBar slidingToolBar;

   protected int direction = UP;

   protected boolean compact;

   public static final int UP=0, DOWN=1;

   private PassEditor main;

   public SlidingToolControl(PassEditor main,
      SlidingToolBar slidingToolBar, int direction)
   {
      this(main, slidingToolBar, direction, false);
   }

   public SlidingToolControl(PassEditor main,
      SlidingToolBar slidingToolBar, int direction, boolean compact)
   {
      super();
      this.main = main;
      this.compact = compact;
      setLayout(new BoxLayout(this, 
        slidingToolBar.getOrientation() == SlidingToolBar.HORIZONTAL ? BoxLayout.LINE_AXIS : BoxLayout.PAGE_AXIS));

      setSlidingToolBar(slidingToolBar);
      setDirection(direction);

      if (compact)
      {
         if (direction == UP)
         {
            setEndControl(createEndControl());
            setBlockControl(createBlockControl());
            setUnitControl(createUnitControl());
         }
         else
         {
            setUnitControl(createUnitControl());
            setBlockControl(createBlockControl());
            setEndControl(createEndControl());
         }

         setBorder(null);
      }
      else
      {
         setUnitControl(createUnitControl());
         setBlockControl(createBlockControl());
         setEndControl(createEndControl());

         //setBorder(BorderFactory.createRaisedBevelBorder());

         int inset1 = 0;
         int inset2 = 0;

         if (direction == UP)
         {
            inset2 = 10;
         }
         else
         {
            inset1 = 10;
         }

         if (slidingToolBar.getOrientation() == SlidingToolBar.HORIZONTAL)
         {
            setBorder(BorderFactory.createEmptyBorder(0, inset1, 0, inset2));
         }
         else
         {
            setBorder(BorderFactory.createEmptyBorder(inset1, 0, inset2, 0));
         }
      }

   }

   public void removeSlidingToolBar()
   {
      if (this.slidingToolBar != null)
      {
         if (block != null)
         {
            block.removeActionListener(this.slidingToolBar);
         }

         if (unit != null)
         {
            unit.removeActionListener(this.slidingToolBar);
         }

         if (endPoint != null)
         {
            endPoint.removeActionListener(this.slidingToolBar);
         }
      }

      this.slidingToolBar = null;
   }

   public void setSlidingToolBar(SlidingToolBar slidingToolBar)
   {
      SlidingToolBar oldValue = this.slidingToolBar;

      if (this.slidingToolBar != null)
      {
         if (block != null)
         {
            block.removeActionListener(this.slidingToolBar);
         }

         if (unit != null)
         {
            unit.removeActionListener(this.slidingToolBar);
         }

         if (endPoint != null)
         {
            endPoint.removeActionListener(this.slidingToolBar);
         }
      }

      this.slidingToolBar = slidingToolBar;

      if (this.slidingToolBar != null)
      {
         if (block != null)
         {
            block.addActionListener(this.slidingToolBar);
         }

         if (unit != null)
         {
            unit.addActionListener(this.slidingToolBar);
         }

         if (endPoint != null)
         {
            endPoint.addActionListener(this.slidingToolBar);
         }
      }

      if ((oldValue != null && this.slidingToolBar != null
             && oldValue.getOrientation()
                   != this.slidingToolBar.getOrientation())
       || (oldValue == null && this.slidingToolBar != null))
      {
         setLayout(createLayout());
      }
   }

   public SlidingToolBar getSlidingToolBar()
   {
      return slidingToolBar;
   }

   public void setDirection(int direction)
   {
      switch (direction)
      {
         case UP:
         case DOWN:
            break;
         default:
            throw new IllegalArgumentException(
               "Direction must be one of: UP, DOWN");
      }

      this.direction = direction;

      if (unit != null) setUnitActionCommand();
      if (block != null) setBlockActionCommand();
      if (endPoint != null) setEndActionCommand();
   }

   public int getDirection()
   {
      return direction;
   }

   public void setUnitControl(AbstractButton button)
   {
      if (unit != null)
      {
         remove(unit);

         if (slidingToolBar != null)
         {
            unit.removeActionListener(slidingToolBar);
            unit.setActionCommand(null);
         }
      }

      unit = button;

      if (unit != null)
      {
         add(unit);

         if (slidingToolBar != null)
         {
            unit.addActionListener(slidingToolBar);
            setUnitActionCommand();
         }
      }
   }

   private void setUnitActionCommand()
   {
      unit.setActionCommand(direction == UP ?
         "scrollUnitUp" : "scrollUnitDown");

      if (unit instanceof SlidingToolControlButton)
      {
         ((SlidingToolControlButton)unit).updateToolTipText();
      }
   }

   private void setBlockActionCommand()
   {
      block.setActionCommand(direction == UP ?
         "scrollBlockUp" : "scrollBlockDown");

      if (block instanceof SlidingToolControlButton)
      {
         ((SlidingToolControlButton)block).updateToolTipText();
      }
   }

   private void setEndActionCommand()
   {
      endPoint.setActionCommand(direction == UP ?
         "scrollEndUp" : "scrollEndDown");

      if (endPoint instanceof SlidingToolControlButton)
      {
         ((SlidingToolControlButton)endPoint).updateToolTipText();
      }
   }

   public void setBlockControl(AbstractButton button)
   {
      if (block != null)
      {
         remove(block);

         if (slidingToolBar != null)
         {
            block.removeActionListener(slidingToolBar);
            block.setActionCommand(null);
         }
      }

      block = button;

      if (block != null)
      {
         add(block);

         if (slidingToolBar != null)
         {
            block.addActionListener(slidingToolBar);
            setBlockActionCommand();
         }
      }
   }

   public void setEndControl(AbstractButton button)
   {
      if (endPoint != null)
      {
         remove(endPoint);

         if (slidingToolBar != null)
         {
            endPoint.removeActionListener(slidingToolBar);
            endPoint.setActionCommand(null);
         }
      }

      endPoint = button;

      if (endPoint != null)
      {
         add(endPoint);

         if (slidingToolBar != null)
         {
            endPoint.addActionListener(slidingToolBar);
            setEndActionCommand();
         }
      }
   }

   public AbstractButton getUnitControl()
   {
      return unit;
   }

   public AbstractButton getBlockControl()
   {
      return block;
   }

   public AbstractButton getEndControl()
   {
      return endPoint;
   }

   protected LayoutManager createLayout()
   {
      if (compact)
      {
         if (slidingToolBar.getOrientation()==SlidingToolBar.VERTICAL)
         {
            GridLayout layout = new GridLayout(3, 1);
            layout.setVgap(0);
            return layout;
         }
         else
         {
            GridLayout layout = new GridLayout(1, 3);
            layout.setHgap(0);
            return layout;
         }
      }

      if (slidingToolBar.getOrientation()==SlidingToolBar.HORIZONTAL)
      {
         GridLayout layout = new GridLayout(3, 1);
         layout.setVgap(0);
         return layout;
      }
      else
      {
         GridLayout layout = new GridLayout(1, 3);
         layout.setHgap(0);
         return layout;
      }
   }

   protected AbstractButton createUnitControl()
   {
      return SlidingToolControlButton.createUnit(this);
   }

   protected AbstractButton createBlockControl()
   {
      return SlidingToolControlButton.createBlock(this);
   }

   protected AbstractButton createEndControl()
   {
      return SlidingToolControlButton.createEnd(this);
   }

   public PassEditor getPassEditor()
   {
      return main;
   }
}

class SlidingToolControlButton extends JButton
{
   protected SlidingToolControl control;

   protected static ImageIcon vScrollBlockDown, vScrollBlockUp,
      vScrollEndDown, vScrollEndUp, vScrollUnitDown, vScrollUnitUp,
      hScrollBlockLeft, hScrollBlockRight, hScrollEndLeft,
      hScrollEndRight, hScrollUnitLeft, hScrollUnitRight;

   protected static ImageIcon vScrollBlockDownPressed, vScrollBlockUpPressed,
      vScrollEndDownPressed, vScrollEndUpPressed, vScrollUnitDownPressed, vScrollUnitUpPressed,
      hScrollBlockLeftPressed, hScrollBlockRightPressed, hScrollEndLeftPressed,
      hScrollEndRightPressed, hScrollUnitLeftPressed, hScrollUnitRightPressed;

   protected static String vScrollBlockDownTT, vScrollBlockUpTT,
      vScrollEndDownTT, vScrollEndUpTT, vScrollUnitDownTT, vScrollUnitUpTT,
      hScrollBlockLeftTT, hScrollBlockRightTT, hScrollEndLeftTT,
      hScrollEndRightTT, hScrollUnitLeftTT, hScrollUnitRightTT;

   private static boolean imagesLoaded = false;

   private int type;

   public static final int UNIT=0, BLOCK=1, END=2;

   private SlidingToolControlButton()
   {
      this(null, UNIT);
   }

   public SlidingToolControlButton(SlidingToolControl control, int type)
   {
      super();
      this.control = control;
      setType(type);

      PassEditor main = control.getPassEditor();

      if (!imagesLoaded)
      {
         loadImages(main);
      }

      setIconTextGap(0);
      setBorderPainted(false);
      setContentAreaFilled(false);
      setMargin(new Insets(0, 0, 0, 0));
      setBorder(null);
      updateToolTipText();
   }

   public static SlidingToolControlButton createBlock(SlidingToolControl c)
   {
      return new SlidingToolControlButton(c, BLOCK);
   }

   public static SlidingToolControlButton createUnit(SlidingToolControl c)
   {
      return new SlidingToolControlButton(c, UNIT);
   }

   public static SlidingToolControlButton createEnd(SlidingToolControl c)
   {
      return new SlidingToolControlButton(c, END);
   }

   private static void loadImages(PassEditor main)
   {
      vScrollBlockDown = main.getIcon("icons",
        "vScrollBlockDown.png");
      vScrollBlockUp = main.getIcon("icons",
        "vScrollBlockUp.png");
      vScrollEndDown = main.getIcon("icons",
        "vScrollEndDown.png");
      vScrollEndUp = main.getIcon("icons",
        "vScrollEndUp.png");
      vScrollUnitDown = main.getIcon("icons",
        "vScrollUnitDown.png");
      vScrollUnitUp = main.getIcon("icons",
        "vScrollUnitUp.png");
      hScrollBlockLeft = main.getIcon("icons",
        "hScrollBlockLeft.png");
      hScrollBlockRight = main.getIcon("icons",
        "hScrollBlockRight.png");
      hScrollEndLeft = main.getIcon("icons",
        "hScrollEndLeft.png");
      hScrollEndRight = main.getIcon("icons",
        "hScrollEndRight.png");
      hScrollUnitLeft = main.getIcon("icons",
        "hScrollUnitLeft.png");
      hScrollUnitRight = main.getIcon("icons",
        "hScrollUnitRight.png");

      vScrollBlockDownPressed = main.getIcon("icons",
        "vScrollBlockDownPressed.png");
      vScrollBlockUpPressed = main.getIcon("icons",
        "vScrollBlockUpPressed.png");
      vScrollEndDownPressed = main.getIcon("icons",
        "vScrollEndDownPressed.png");
      vScrollEndUpPressed = main.getIcon("icons",
        "vScrollEndUpPressed.png");
      vScrollUnitDownPressed = main.getIcon("icons",
        "vScrollUnitDownPressed.png");
      vScrollUnitUpPressed = main.getIcon("icons",
        "vScrollUnitUpPressed.png");
      hScrollBlockLeftPressed = main.getIcon("icons",
        "hScrollBlockLeftPressed.png");
      hScrollBlockRightPressed = main.getIcon("icons",
        "hScrollBlockRightPressed.png");
      hScrollEndLeftPressed = main.getIcon("icons",
        "hScrollEndLeftPressed.png");
      hScrollEndRightPressed = main.getIcon("icons",
        "hScrollEndRightPressed.png");
      hScrollUnitLeftPressed = main.getIcon("icons",
        "hScrollUnitLeftPressed.png");
      hScrollUnitRightPressed = main.getIcon("icons",
        "hScrollUnitRightPressed.png");

      vScrollBlockDownTT = main.getMessage("tooltip.vScrollBlockDown");
      vScrollBlockUpTT = main.getMessage("tooltip.vScrollBlockUp");
      vScrollEndDownTT = main.getMessage("tooltip.vScrollEndDown");
      vScrollEndUpTT = main.getMessage("tooltip.vScrollEndUp");
      vScrollUnitDownTT = main.getMessage("tooltip.vScrollUnitDown");
      vScrollUnitUpTT = main.getMessage("tooltip.vScrollUnitUp");
      hScrollBlockLeftTT = main.getMessage("tooltip.hScrollBlockLeft");
      hScrollBlockRightTT = main.getMessage("tooltip.hScrollBlockRight");
      hScrollEndLeftTT = main.getMessage("tooltip.hScrollEndLeft");
      hScrollEndRightTT = main.getMessage("tooltip.hScrollEndRight");
      hScrollUnitLeftTT = main.getMessage("tooltip.hScrollUnitLeft");
      hScrollUnitRightTT = main.getMessage("tooltip.hScrollUnitRight");

      imagesLoaded = true;
   }

   public void setType(int type)
   {
      switch (type)
      {
         case UNIT:
         case BLOCK:
         case END:
            break;
         default:
            throw new IllegalArgumentException(
               "Control type must be one of: UNIT, BLOCK, END");
      }

      this.type = type;
   }

   public int getType()
   {
      return type;
   }

   public String getControlLabel()
   {
      if (control == null || control.getSlidingToolBar() == null)
      {
         return null;
      }

      String direction;

      String prefix;

      if (control.getSlidingToolBar().getOrientation()
            == SlidingToolBar.HORIZONTAL)
      {
         direction = (control.getDirection() == SlidingToolControl.UP?
            "Left" : "Right");
         prefix = "h";
      }
      else
      {
         direction = (control.getDirection() == SlidingToolControl.UP?
            "Up" : "Down");
         prefix = "v";
      }

      String typeString="";

      switch (type)
      {
         case UNIT:
            typeString = "Unit";
            break;
         case BLOCK:
            typeString = "Block";
            break;
         case END:
            typeString = "End";
            break;
      }

      return prefix+"Scroll"+typeString+direction;
   }

   public void updateToolTipText()
   {
      String label = getControlLabel();

      String text = null;

      if (label == null) return;

      if (label.equals("vScrollBlockDown"))
      {
         text = vScrollBlockDownTT;
      }
      else if (label.equals("vScrollBlockUp"))
      {
         text = vScrollBlockUpTT;
      }
      else if (label.equals("vScrollUnitDown"))
      {
         text = vScrollUnitDownTT;
      }
      else if (label.equals("vScrollUnitUp"))
      {
         text = vScrollUnitUpTT;
      }
      else if (label.equals("vScrollEndUp"))
      {
         text = vScrollEndUpTT;
      }
      else if (label.equals("vScrollEndDown"))
      {
         text = vScrollEndDownTT;
      }
      else if (label.equals("hScrollBlockLeft"))
      {
         text = hScrollBlockLeftTT;
      }
      else if (label.equals("hScrollBlockRight"))
      {
         text = hScrollBlockRightTT;
      }
      else if (label.equals("hScrollUnitLeft"))
      {
         text = hScrollUnitLeftTT;
      }
      else if (label.equals("hScrollUnitRight"))
      {
         text = hScrollUnitRightTT;
      }
      else if (label.equals("hScrollEndLeft"))
      {
         text = hScrollEndLeftTT;
      }
      else if (label.equals("hScrollEndRight"))
      {
         text = hScrollEndRightTT;
      }

      setToolTipText(text);
   }

   public Icon getIcon()
   {
      String label = getControlLabel();

      if (label == null) return null;

      if (label.equals("vScrollBlockDown"))
      {
         return vScrollBlockDown;
      }

      if (label.equals("vScrollBlockUp"))
      {
         return vScrollBlockUp;
      }

      if (label.equals("vScrollUnitDown"))
      {
         return vScrollUnitDown;
      }

      if (label.equals("vScrollUnitUp"))
      {
         return vScrollUnitUp;
      }

      if (label.equals("vScrollEndUp"))
      {
         return vScrollEndUp;
      }

      if (label.equals("vScrollEndDown"))
      {
         return vScrollEndDown;
      }

      if (label.equals("hScrollBlockLeft"))
      {
         return hScrollBlockLeft;
      }

      if (label.equals("hScrollBlockRight"))
      {
         return hScrollBlockRight;
      }

      if (label.equals("hScrollUnitLeft"))
      {
         return hScrollUnitLeft;
      }

      if (label.equals("hScrollUnitRight"))
      {
         return hScrollUnitRight;
      }

      if (label.equals("hScrollEndLeft"))
      {
         return hScrollEndLeft;
      }

      if (label.equals("hScrollEndRight"))
      {
         return hScrollEndRight;
      }

      return null;
   }

   public Icon getPressedIcon()
   {
      String label = getControlLabel();

      if (label == null) return null;

      if (label.equals("vScrollBlockDown"))
      {
         return vScrollBlockDownPressed;
      }

      if (label.equals("vScrollBlockUp"))
      {
         return vScrollBlockUpPressed;
      }

      if (label.equals("vScrollUnitDown"))
      {
         return vScrollUnitDownPressed;
      }

      if (label.equals("vScrollUnitUp"))
      {
         return vScrollUnitUpPressed;
      }

      if (label.equals("vScrollEndUp"))
      {
         return vScrollEndUpPressed;
      }

      if (label.equals("vScrollEndDown"))
      {
         return vScrollEndDownPressed;
      }

      if (label.equals("hScrollBlockLeft"))
      {
         return hScrollBlockLeftPressed;
      }

      if (label.equals("hScrollBlockRight"))
      {
         return hScrollBlockRightPressed;
      }

      if (label.equals("hScrollUnitLeft"))
      {
         return hScrollUnitLeftPressed;
      }

      if (label.equals("hScrollUnitRight"))
      {
         return hScrollUnitRightPressed;
      }

      if (label.equals("hScrollEndLeft"))
      {
         return hScrollEndLeftPressed;
      }

      if (label.equals("hScrollEndRight"))
      {
         return hScrollEndRightPressed;
      }

      return null;
   }
}
