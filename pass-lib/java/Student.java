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

/**
 * Class representing a student. Assignment submissions are usually
 * anonymous but the student registration number needs to be
 * included, because university regulations require this, but the
 * user name is also needed, because the marking system requires it.
 * The user name (the ID used by Blackboard) doesn't include
 * personally identifiable information in it.
 *
 * Some of the Pass applications use a regular expression
 * AssignmentProcessConfig.USER_NAME_REGEX and
 * AssignmentProcessConfig.REG_NUM_REGEX to check the user name and
 * registration number supplied by the student. New students
 * sometimes get confused and enter the wrong information (such as
 * their name or email or some random invented name).
 */

public class Student
{
   public Student(String blackboardId, String regNumber)
   {
      this.blackboardId = blackboardId;
      this.regNumber = regNumber;
   }

   public String getBlackboardId()
   {
      return blackboardId;
   }

   public String getRegNumber()
   {
      return regNumber;
   }

   private String blackboardId, regNumber;
}
