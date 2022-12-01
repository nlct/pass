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

 * Account verification page. 
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Verify Account');

if ($pass->getUserRole())
{// already logged in
   $pass->redirect_header();
}
else
{
   process_params();

   if ($pass->isParam('action', 'submit'))
   {
      if ($pass->verify_account($pass->getParam('token')))
      {
         $_SESSION['confirmation_message'] = 'Account verified. You can now log in.';

         $pass->redirect_login_header(false);
      }
      else
      {
         $pass->page_header();

         if ($pass->has_errors())
         {
            $pass->print_errors();
         }
         else
         {
?>
<p>Unable to verify account.
<?php
         }

         show_form();

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

      show_form();

      $pass->page_footer();
   }
}

function process_params()
{
   global $pass;

   $pass->get_token_variable('token');

   $pass->get_matched_variable('action', '/^(submit|showform)$/', 
      $pass->isParamSet('token') ? 'submit' : 'showform');

   if ($pass->isParam('action', 'submit'))
   {
      $pass->check_not_empty('token');
   }

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'showform');
   }
}

function show_form()
{
   global $pass;

   echo $pass->start_form();
?>
<p>To verify your account, please enter the token supplied in your
confirmation email. If you have already verified your account,
use the <?php echo $pass->get_login_link('login page'); ?> to login.
<p>
<label>Token:
<?php

   echo $pass->form_input_token('token', ['required'=>true]);
   $pass->element_error_if_set('token');

 ?>
</label>

<?php echo $pass->form_submit_button(['value'=>'submit']); ?>

<p>
</form>
<?php

   echo $pass->get_resend_verify_link('Need a new token?');
}

?>
