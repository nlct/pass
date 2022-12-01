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

 * Reset password page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Password Reset');

process_params();

if ($pass->isParam('action', 'submit')
 && $pass->passwordReset($pass->getParam('token'), $pass->getParam('newpassword')))
{
   $_SESSION['confirmation_message']='Password reset successful. Please login with your new password.';

   $pass->redirect_login_header(false);
}
else
{
   $pass->page_header();
   show_form();
   $pass->page_footer();
}

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(submit|showform)$/', 'showform');

   $pass->get_token_variable('token');

   if ($pass->isParam('action', 'submit'))
   {
      $pass->check_not_empty('token');

      $pass->get_password_variable('newpassword', true);
      $pass->get_password_variable('confirmnewpassword', false, 'password confirmation');

      $pass->check_not_empty('newpassword');
      $pass->check_not_empty('confirmnewpassword', 'password confirmation');

      if (!$pass->isErrorParamSet('newpassword')
          && ($pass->getParam('newpassword') !== $pass->getParam('confirmnewpassword'))
      {
         $pass->add_error_message('Password confirmation must match',
           'confirmnewpassword', 'Mismatch');
      }
   }

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'showform');
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

   if ($pass->isParamSet('token') && !$pass->isErrorParamSet('token'))
   {
      echo $pass->form_input_hidden('token');
   }
   else
   {
?>
<p>Enter the token specified in the email you received following
your request for a password reset. Tokens expire after <?php 

      echo $pass->getConfigValue('reset_link_timeout');

?> minutes. (Need a <?php echo $pass->get_forgotten_password_link('new token'); ?>?)
<p><label for="token">Token:</label>
<?php
      echo $pass->form_input_token('token', ['id'=>'token', 'required'=>true]);
      $pass->element_error_if_set('token');

   }

?>
<p>Please set a new password. Minimum password length: <?php echo Pass::MIN_PASSWORD_LENGTH; ?>.
<table>
   <tr>
    <th><label for="newpassword">New password:</label>
    </th>
    <td><?php echo $pass->form_input_password('newpassword',
       ['id'=>'newpassword', 'required'=>true]);
       $pass->element_error_if_set('newpassword');
    ?></td>
   </tr>
   <tr>
    <th><label for="confirmnewpassword">Confirm new password:</label>
    </th>
    <td><?php echo $pass->form_input_password('confirmnewpassword',
       ['id'=>'confirmnewpassword', 'required'=>true]);
       $pass->element_error_if_set('confirmnewpassword');
    ?></td>
   </tr>
</table>
<?php
   echo $pass->form_submit_button(['value'=>'submit']);

   echo '</form>';
}
?>
