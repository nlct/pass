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
package com.dickimawbooks.passchecker;

/**
 * This interface was added to allow for a GUI alternative. Retained
 * in case it's implemented in future.
 */
public interface MessageSystem
{
   public void debugMessage(String message, Throwable throwable);
   public void warningMessage(String message, Throwable throwable);
   public void errorMessage(String message, Throwable throwable);
}
