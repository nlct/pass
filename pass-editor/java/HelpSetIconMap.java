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

import java.util.HashMap;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;
import java.util.Set;
import java.net.URL;
import java.net.MalformedURLException;

import javax.help.Map;
import javax.help.HelpSet;

public class HelpSetIconMap implements Map
{
   public HelpSetIconMap(HelpSet hs)
   {
      super();
      this.helpset = hs;

      idToUrl = new HashMap<String,URL>();
      urlToId = new HashMap<URL,String>();

      initialise();
   }

   private void initialise()
   {
      addMap("homeIcon", 
         getClass().getResource("/toolbarButtonGraphics/navigation/Home24.gif"));
      addMap("backIcon", 
         getClass().getResource("/toolbarButtonGraphics/navigation/Back24.gif"));
      addMap("forwardIcon", 
         getClass().getResource("/toolbarButtonGraphics/navigation/Forward24.gif"));
/*
      addMap("popupIcon", 
         getClass().getResource("/toolbarButtonGraphics/media/Play16.gif"));
*/
   }

   public void addMap(String id, URL url)
   {
      idToUrl.put(id, url);
      urlToId.put(url, id);
   }

   @Override
   public Enumeration<Map.ID> getAllIDs()
   { 
      Vector<Map.ID> result = new Vector<Map.ID>();

      for (Iterator<String> it = idToUrl.keySet().iterator(); it.hasNext(); )
      {
         String id = it.next();

         result.add(Map.ID.create(id, helpset));
      }

      return result.elements();
   }

   @Override
   public Map.ID getClosestID(URL url)
   {
      return getIDFromURL(url);
   }

   @Override
   public boolean isID(URL url)
   {
      return urlToId.containsKey(url);
   }

   @Override
   public Map.ID getIDFromURL(URL url)
   {
      String id = urlToId.get(url);

      if (id != null)
      {
         return Map.ID.create(id, helpset);
      }

      return null;
   }

   // Returns enumeration of Map.Key (Strings/HelpSet)
   @Override
   public Enumeration<Map.ID> getIDs(URL url)
   {
      Vector<Map.ID> result = new Vector<Map.ID>();

      for (Iterator<String> it = idToUrl.keySet().iterator(); it.hasNext(); )
      {
         String id = it.next();

         URL value = idToUrl.get(id);

         if (value.equals(url))
         {
            result.add(Map.ID.create(id, helpset));
         }
      }

      return result.elements();
   }

   @Override
   public URL getURLFromID(Map.ID id) throws MalformedURLException
   {
      return idToUrl.get(id.getIDString());
   }

   @Override
   public boolean isValidID(String id, HelpSet hs)
   {
      return (hs == helpset && idToUrl.containsKey(id));
   }

   private HelpSet helpset;
   private HashMap<String,URL> idToUrl;
   private HashMap<URL,String> urlToId;
}
