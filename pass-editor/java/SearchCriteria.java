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

public class SearchCriteria
{
   public SearchCriteria()
   {
      setDirection(Direction.FORWARDS);
      setFrom(From.CURSOR);
      setMatch(Match.EXACT);
      setCase(Case.SENSITIVE);
   }

   public void setCase(Case matchCase)
   {
      this.matchCase = matchCase;
   }

   public void setDirection(Direction direction)
   {
      this.direction = direction;
   }

   public void setFrom(From from)
   {
      this.from = from;
   }

   public void setMatch(Match match)
   {
      this.match = match;
   }

   public Direction getDirection()
   {
      return direction;
   }

   public From getFrom()
   {
      return from;
   }

   public Match getMatch()
   {
      return match;
   }

   public Case getCase()
   {
      return matchCase;
   }

   public void set(SearchCriteria other)
   {
      direction = other.direction;
      from = other.from;
      match = other.match;
      matchCase = other.matchCase;
   }

   @Override
   public boolean equals(Object other)
   {
      if (other == null || !(other instanceof SearchCriteria))
      {
         return false;
      }

      if (this == other) return true;

      SearchCriteria criteria = (SearchCriteria)other;

      return direction == criteria.direction
          && from == criteria.from
          && match == criteria.match
          && matchCase == criteria.matchCase;
   }

   @Override
   public String toString()
   {
      return String.format("%s[direction=%s,from=%s,match=%s,case=%s]", 
       getClass().getSimpleName(), 
       direction, from, match, matchCase);
   }

   private Direction direction;
   private From from;
   private Match match;
   private Case matchCase;

   public enum Direction { FORWARDS, BACKWARDS; }
   public enum From { CURSOR, START; }
   public enum Match { EXACT, REGEX; }
   public enum Case { SENSITIVE, INSENSITIVE; }
}
