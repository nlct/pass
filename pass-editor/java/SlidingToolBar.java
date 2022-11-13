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
import javax.swing.border.*;
import javax.swing.event.*;

public class SlidingToolBar extends JPanel 
   implements ActionListener,ChangeListener
{
   private Border viewportBorder;

   protected int unitIncrement = 10;

   protected int policy = AS_NEEDED;

   protected int orientation = HORIZONTAL;

   protected JViewport viewport;

   protected SlidingToolControl upComponent;

   protected SlidingToolControl downComponent;

   private transient Insets margin;

   protected PassEditor main;

   public static final int AS_NEEDED=0;
   public static final int ALWAYS=1;
   public static final int NEVER=2;
   public static final int HORIZONTAL=0;
   public static final int VERTICAL=1;

   public SlidingToolBar(PassEditor main, 
      JComponent view, int policy, int orientation)
   {
      super();
      this.main = main;
      setLayout(new BorderLayout());

      setOrientation(orientation);
      setPolicy(policy);
      setViewport(createViewport());
      setUpComponent(createUpComponent());
      setDownComponent(createDownComponent());

      if (view != null)
      {
         setViewportView(view);
         view.setLayout(createDefaultViewLayout());
      }

      revalidate();
   }

   public SlidingToolBar(PassEditor main)
   {
      this(main, new JPanel(), AS_NEEDED, HORIZONTAL);
   }

   public SlidingToolBar(PassEditor main, JComponent view)
   {
      this(main, view, AS_NEEDED, HORIZONTAL);
   }

   public SlidingToolBar(PassEditor main, JComponent view, int orientation)
   {
      this(main, view, AS_NEEDED, orientation);
   }

   public JSeparator addSeparator()
   {
      JSeparator sep = new JToolBar.Separator();
      getViewportView().add(sep);
      return sep;
   }

   public JSeparator addSeparator(Dimension size)
   {
      JSeparator sep = new JToolBar.Separator(size);
      getViewportView().add(sep);
      return sep;
   }

   public void addWidget(AbstractButton widget)
   {
      getViewportView().add(widget);
      widget.setMargin(new Insets(0,0,0,0));

      Dimension pref = widget.getPreferredSize();

      if (orientation == HORIZONTAL)
      {
         unitIncrement = (int)Math.max(pref.width, unitIncrement);
      }
      else
      {
         unitIncrement = (int)Math.max(pref.height, unitIncrement);
      }
   }

   @Override
   public void updateUI()
   {
      super.updateUI();

      unitIncrement = 10;
      Component[] components = getComponents();

      for (Component c : components)
      {
         if (c instanceof AbstractButton)
         {
            Dimension pref = c.getPreferredSize();

            if (orientation == HORIZONTAL)
            {
               unitIncrement = (int)Math.max(pref.width, unitIncrement);
            }
            else
            {
               unitIncrement = (int)Math.max(pref.height, unitIncrement);
            }
         }
      }
   }

   public void setOrientation(int orientation)
   {
      switch (orientation)
      {
         case HORIZONTAL:
         case VERTICAL:
            break;
         default:
            throw new IllegalArgumentException(
               "orientation must be one of: VERTICAL, HORIZONTAL");
      }

      this.orientation = orientation;

      if (upComponent != null)
      {
         remove(upComponent);
         add(upComponent, orientation == HORIZONTAL ? "East" : "North");
      }
   }

   public int getOrientation()
   {
      return orientation;
   }

   public void setPolicy(int policy)
   {
      switch (policy)
      {
         case AS_NEEDED:
         case ALWAYS:
         case NEVER:
            break;
         default:
            throw new IllegalArgumentException(
               "policy must be one of: AS_NEEDED, ALWAYS, NEVER");
      }

      this.policy = policy;
      revalidate();
   }

   public void setViewport(JViewport viewport)
   {
      if (this.viewport != null)
      {
         remove(this.viewport);
         this.viewport.removeChangeListener(this);
      }

      this.viewport = viewport;

      if (viewport != null)
      {
         add(viewport, "Center");
         viewport.addChangeListener(this);
      }
   }

   public JViewport getViewport()
   {
      return viewport;
   }

   public void setViewportView(JComponent view)
   {
      viewport.setView(view);
   }

   public JComponent getViewportView()
   {
      if (viewport == null) return null;

      return (JComponent)viewport.getView();
   }

   public void setUpComponent(SlidingToolControl comp)
   {
      if (this.upComponent != null)
      {
         remove(this.upComponent);
         this.upComponent.removeSlidingToolBar();
      }

      this.upComponent = comp;

      if (comp != null)
      {
         add(comp, orientation==HORIZONTAL? "West" : "North");
         comp.setSlidingToolBar(this);
      }

      checkControls();
   }

   public SlidingToolControl getUpComponent()
   {
      return upComponent;
   }

   public void setDownComponent(SlidingToolControl comp)
   {
      if (this.downComponent != null)
      {
         remove(this.downComponent);
         this.downComponent.removeSlidingToolBar();
      }

      this.downComponent = comp;

      if (comp != null)
      {
         add(comp, orientation==HORIZONTAL? "East" : "South");
         comp.setSlidingToolBar(this);
      }

      checkControls();
   }

   public SlidingToolControl getDownComponent()
   {
      return downComponent;
   }

   protected JViewport createViewport()
   {
      return new JViewport();
   }

   protected SlidingToolControl createUpComponent()
   {
      return new SlidingToolControl(main, this, SlidingToolControl.UP);
   }

   protected SlidingToolControl createDownComponent()
   {
      return new SlidingToolControl(main, this, SlidingToolControl.DOWN);
   }

   protected void checkControls()
   {
      if (policy == ALWAYS || policy == NEVER || 
          getViewportView() == null)
      {
         if (upComponent != null)
         {
             upComponent.setVisible(policy == ALWAYS);
         }

         if (downComponent != null)
         {
             downComponent.setVisible(policy == ALWAYS);
         }

         return;
      }

      Dimension viewExtent = viewport.getExtentSize();

      Point pos = viewport.getViewPosition();

      Dimension prefSize = getViewportView().getPreferredSize();

      Dimension upSize = (upComponent == null ? 
         new Dimension(0,0) : upComponent.getPreferredSize());

      Dimension downSize = (downComponent == null ? 
         new Dimension(0,0) : downComponent.getPreferredSize());

      boolean showUpControl, showDownControl;

      if (orientation == HORIZONTAL)
      {
         showUpControl = (viewExtent.width < prefSize.width);
         showDownControl = showUpControl;

         if (pos.x == 0)
         {
            showUpControl = false;
         }

         if (pos.x+viewExtent.width >= prefSize.width)
         {
            showDownControl = false;
         }
      }
      else
      {
         showUpControl = (viewExtent.height < prefSize.height);
         showDownControl = showUpControl;

         if (pos.y == 0)
         {
            showUpControl = false;
         }

         if (pos.y+viewExtent.height >= prefSize.height)
         {
            showDownControl = false;
         }
      }

      if (upComponent != null)
      {
         upComponent.setVisible(showUpControl);
      }

      if (downComponent != null)
      {
         downComponent.setVisible(showDownControl);
      }
   }

   public int getUnitIncrement()
   {
      return unitIncrement;
   }

   public void setUnitIncrement(int increment)
   {
      if (increment <= 0)
      {
         throw new IllegalArgumentException(
            "Unit increment must be > 0");
      }

      unitIncrement = increment;
   }

   public void actionPerformed(ActionEvent evt)
   {
      Object source = evt.getSource();
      String action = evt.getActionCommand();

      if (action == null || viewport == null)
      {
         return;
      }

      Rectangle viewRect = viewport.getViewRect();
      Dimension prefSize = getViewportView().getPreferredSize();

      int increment = 0;

      if (action.equals("scrollUnitUp"))
      {
         increment = -unitIncrement;
      }
      else if (action.equals("scrollUnitDown"))
      {
         increment = unitIncrement;
      }
      else if (action.equals("scrollBlockUp"))
      {
         increment = -(orientation == HORIZONTAL ?
            viewRect.width : viewRect.height);
      }
      else if (action.equals("scrollBlockDown"))
      {
         increment = (orientation == HORIZONTAL ?
            viewRect.width : viewRect.height);
      }
      else if (action.equals("scrollEndUp"))
      {
         increment = -(orientation == HORIZONTAL ?
            prefSize.width : prefSize.height);
      }
      else if (action.equals("scrollEndDown"))
      {
         increment = (orientation == HORIZONTAL ?
            prefSize.width : prefSize.height);
      }

      if (orientation == HORIZONTAL)
      {
         viewRect.x += increment;

         if (viewRect.x + viewRect.width > prefSize.width)
         {
            viewRect.x = (prefSize.width-viewRect.width);
         }

         if (viewRect.x < 0)
         {
            viewRect.x = 0;
         }
      }
      else
      {
         viewRect.y += increment;

         if (viewRect.y + viewRect.height > prefSize.height)
         {
            viewRect.y = (prefSize.height-viewRect.height);
         }

         if (viewRect.y < 0)
         {
            viewRect.y = 0;
         }
      }

      viewport.setViewPosition(new Point(viewRect.x, viewRect.y));
      checkControls();
   }

   public void stateChanged(ChangeEvent evt)
   {
      if (evt.getSource() == getViewport())
      {
         checkControls();
      }
   }

   public Insets getMargin()
   {
      return margin;
   }

   public void setMargin(Insets margin)
   {
      if ((this.margin != null && margin == null)
         || (this.margin == null && margin != null)
         || (margin != null && this.margin != null
         && (margin.left != this.margin.left
         || margin.right != this.margin.right || margin.top != this.margin.top
         || margin.bottom != this.margin.bottom)))
      {
         Insets oldMargin = this.margin;
         this.margin = margin;
         firePropertyChange("margin", oldMargin, this.margin);
         revalidate();
         repaint();
      }
   }

   protected LayoutManager createDefaultViewLayout()
   {
      return new DefaultViewLayout();
   }

   // based on JToolBar.DefaultToolBarLayout
   private class DefaultViewLayout implements LayoutManager
   {
      public void addLayoutComponent(String name, Component comp)
      {
      }

      public void layoutContainer(Container c)
      {
         Insets insets = getInsets();
         Insets margin = getMargin();
         int middle;

         if (margin != null)
         {
           insets.left += margin.left;
           insets.top += margin.top;
           insets.bottom += margin.bottom;
           insets.right += margin.right;
         }

         Component[] components = c.getComponents();
         Dimension tdims = c.getSize();
         int start = 0;
         Dimension pref;

         if (getOrientation() == HORIZONTAL)
         {
            start += insets.left;

            for (int i = 0; i < components.length; i++)
            {
               if (components[i] != null && components[i].isVisible())
               {
                  pref = components[i].getPreferredSize();

                  if (pref != null)
                  {
                     middle = (tdims.height - pref.height) / 2;
                     components[i].setBounds(start, middle, pref.width,
                                           pref.height);
                     start += pref.width;
                  }
               }
            }
         }
         else
         {
            start += insets.top;

            for (int i = 0; i < components.length; i++)
            {
               if (components[i] != null && components[i].isVisible())
               {
                  pref = components[i].getPreferredSize();

                  if (pref != null)
                  {
                     middle = (tdims.width - pref.width) / 2;
                     components[i].setBounds(middle, start, pref.width,
                                             pref.height);
                     start += pref.height;
                  }
               }
            }
         }
      }

      public Dimension minimumLayoutSize(Container parent)
      {
         return preferredLayoutSize(parent);
      }

      public Dimension preferredLayoutSize(Container parent)
      {
         int orientation = getOrientation();

         JComponent view = getViewportView();

         Component[] components = view.getComponents();

         int limit = 0;
         int total = 0;
         Dimension dims;
   
         int w = 0;
         int h = 0;

         if (getOrientation() == HORIZONTAL)
         {
            for (int i = 0; i < components.length; i++)
            {
               dims = components[i].getPreferredSize();

               if (dims != null)
               {
                  if (dims.height > limit) limit = dims.height;

                  total += dims.width;
               }
            }

            w = total;
            h = limit;
         }
         else
         {
            for (int i = 0; i < components.length; i++)
            {
               dims = components[i].getPreferredSize();

               if (dims != null)
               {
                  if (dims.width > limit) limit = dims.width;

                  total += dims.height;
               }
            }

            w = limit;
            h = total;
         }

         Insets insets = getInsets();

         w += insets.left + insets.right;
         h += insets.top + insets.bottom;
   
         Insets margin = getMargin();

         if (margin != null)
         {
            w += margin.left + margin.right;
            h += margin.top + margin.bottom;
         }
   
         return new Dimension(w, h);
      }

      public void removeLayoutComponent(Component comp)
      {
      }
   }
}
