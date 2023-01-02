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

 * Change password page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Change Password');

$pass->require_login();// ensure user is logged in

process_params();

if ($pass->isParam('action', 'update') 
 && $pass->update_password($pass->getParam('newpassword')))
{
   $_SESSION['confirmation_message'] = 'Password changed.';
   $pass->logout_all();
   $pass->redirect_header($pass->getLoginRef());
}
elseif ($pass->isParam('action', 'cancel'))
{
   $_SESSION['confirmation_message'] = 'Cancelled.';
   $pass->redirect_header($pass->getAccountRef());
}
else
{
   $pass->page_header();

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'form');

      $pass->print_errors();
   }

   print_form();

   $pass->page_footer();
}

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(update|form|cancel)$/', 'form');

   if ($pass->isParam('action', 'update'))
   {
      $pass->get_password_variable('password');
      $pass->get_password_variable('newpassword', true);
      $pass->get_password_variable('confirmnewpassword', false, 'password confirmation');

      $pass->check_not_empty('password');
      $pass->check_not_empty('newpassword');
      $pass->check_not_empty('confirmnewpassword', 'password confirmation');

      if (!$pass->isErrorParamSet('newpassword')
          && ($pass->getParam('newpassword') !== $pass->getParam('confirmnewpassword')))
      {
         $pass->add_error_message('Password confirmation must match', 'confirmpassword', 'Mismatch');
      }

      $userDetails = $pass->verifyCredentials($pass->getUserID(), $pass->getParam('password'), false);

      if ($userDetails === false)
      {
         $pass->setParam('action', 'form');
      }
   }

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'form');
   }
}

function print_form()
{
   global $pass;

   echo $pass->start_form();
?>
   <p>Minimum password length: <?php echo Pass::MIN_PASSWORD_LENGTH; ?>.
   <table>
   <tr>
    <th><label for="password">Current password:</label>
    </th>
    <td><?php echo $pass->form_input_password('password', 
       ['id'=>'password', 'required'=>true]); 
       $pass->element_error_if_set('password');
    ?></td>
   </tr>
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
   <tr><td colspan="2"><?php echo $pass->form_submit_button(['value'=>'update'], 'Change');?>
   <span class="spacer"> </span>
   <?php echo $pass->form_submit_button(['value'=>'cancel', 'formnovalidate'=>true], 'Cancel'); ?></td></tr>
   </table>
   </form>
<?php
}

?>
