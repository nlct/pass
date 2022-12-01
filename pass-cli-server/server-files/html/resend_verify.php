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

 * Send a new account verification link.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Resend Verification Token');

if ($pass->getUserRole())
{// already logged in
   $pass->redirect_header();
}
else
{
   process_params();

   if ($pass->isParam('action', 'submit'))
   {
      $message = $pass->resend_verification_email($pass->getParam('username'));

      if ($message !== false)
      {
         $_SESSION['confirmation_message'] = $message;
         $pass->redirect_header($pass->getVerifyAccountRef());
      }
      else
      {
         $pass->page_header();

         if ($pass->has_errors())
         {
            $pass->setParam('action', 'form');
            $pass->print_errors();
         }

         print_form($pass);

         $pass->page_footer();
      }
   }
   else
   {
      $pass->page_header();

      if ($pass->has_errors())
      {
         $pass->print_errors();
      }

      print_form();

      $pass->page_footer();
   }
}

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(submit|form)$/', 'form');

   if ($pass->isParam('action', 'submit'))
   {
      $pass->get_username_variable('username');

      $pass->check_not_empty('username');

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'form');
      }
   }
}

function print_form()
{
   global $pass;
   echo $pass->start_form();
?>
<p>Resend account verification for
<label for="username"><?php echo PassConfig::USERNAME_LABEL; ?>:</label>
<?php
   echo $pass->form_input_username('username',
	   [ 'id' => 'username', 'required'=>'required' ]);
   $pass->element_error_if_set('username');
?>
<br>
<span class="note"> (Don't include <code>@<?php echo PassConfig::EMAIL_DOMAIN; ?></code> domain.)</span>
<p>
<?php echo $pass->form_submit_button(['value'=>'submit']); ?>
</form>
<?php
}

?>
