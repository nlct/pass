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

 * Forgotten password page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Forgotten Password?');

if ($pass->getUserRole())
{// already logged in
   $pass->redirect_header($pass->getChangePasswordRef());
}
else
{
   process_params();

   if ($pass->isParam('action', 'submit'))
   {
      $message = $pass->createPasswordReset($pass->getParam('username'));

      if ($message === false)
      {
         $pass->page_header();
         show_form();
         $pass->page_footer();
      }
      else
      {
         $_SESSION['confirmation_message'] = $message;
         $pass->redirect_header($pass->getResetPasswordRef());
      }
   }
   else
   {
      $pass->page_header();
      show_form();
      $pass->page_footer();
   }
}

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(showform|submit)$/', 'showform');

   $pass->get_username_variable('username');

   if ($pass->isParam('action', 'submit'))
   {
      $pass->check_not_empty('username');

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'showform');
      }
   }
}

function show_form()
{
   global $pass;

   if ($pass->has_errors())
   {
      $pass->print_errors();
   }

   echo $pass->start_form();
?>
<p>
<label for="username"><?php echo PassConfig::USERNAME_LABEL; ?> (e.g. <?php echo $pass->getExampleUserName(); ?>):</label>
<?php
   echo $pass->form_input_username('username', ['id'=>'username', 'required'=>true]);
   echo '<p>', $pass->form_submit_button(['value'=>'submit'], 'Send Reset Link');
   echo '</form>';
}
?>
