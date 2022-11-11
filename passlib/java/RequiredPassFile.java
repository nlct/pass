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
 * Interface for a required file. This is a file listed in the
 * assignment XML file with the <code>file</code> or <code>mainfile</code> elements.
 */
public interface RequiredPassFile extends PassFile
{
   /**
    * Gets the name as specified in the XML file.
    * The student must provide a file with the same name.
    * @return the required file name
    */ 
   public String getRequiredName();
}
