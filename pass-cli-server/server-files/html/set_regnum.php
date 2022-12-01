<?php
/*
 * Server PASS
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

 * Set registration number page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Set Registration Number');

$pass->require_login();// ensure user is logged in

process_params();

if (!$pass->has_errors() && $pass->isParam('action', 'save_reg'))
{
   save_regnum();
}
else
{
   $pass->page_header();

   if ($pass->has_errors())
   {
     $pass->print_errors();
   }

   show_form();

   $pass->page_footer();
}

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(showform|save_reg)$/', 'showform');

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'showform');
      return;
   }

   $pass->get_from_variable();

   $pass->get_regnum_variable('regnum');

   if ($pass->isParam('action', 'showform'))
   {
      if (!$pass->isParamSet('regnum'))
      {
         $pass->setParam('regnum', $pass->getUserRegNum());
      }
   }
   elseif ($pass->isParam('action', 'save_reg'))
   {
      if ($pass->isUserRole('student'))
      {
         $pass->check_not_empty('regnum');
      }

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'showform');
      }
   }
}

function show_form()
{
   global $pass;

?>
<p>The registration number is normally a nine-digit number.
It’s required on assignment submissions and will be added to the PDF
by PASS.
<?php

   echo $pass->start_form();

   echo $pass->form_input_hidden('from');

   echo '<p><label for="regnum">Registration number:</label> ';

   $attrs = array('placeholder'=>'e.g. ' . $pass->getRegNumExample());

   if ($pass->isUserRole('student'))
   {
      $attrs['required'] = true;
   }

   echo $pass->form_input_regnum('regnum', $attrs);
   $pass->element_error_if_set('regnum');

   echo $pass->form_submit_button(['value'=>'save_reg']);

   echo '</form>';
}

function save_regnum()
{
   global $pass;

   if ($pass->setUserRegNum($pass->getParam('regnum')))
   {
      $_SESSION['confirmation_message']='Student number set.';

      if ($pass->isParamSet('from'))
      {
         $pass->redirect_header($pass->getParam('from'));
      }
      else
      {
         $pass->redirect_header($pass->getAccountRef());
      }
   }
   else
   {
      $pass->page_header();

      if ($pass->has_errors())
      {
        $pass->print_errors();
      }

      show_form();

      $pass->page_footer();
   }
}
?>
