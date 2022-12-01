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

 * Login page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Login');

if ($pass->isUserAuthenticated())
{// already logged in
   $pass->redirect_header();
}
else
{
   process_params();

   if ($pass->isUserAuthenticated())
   {
      if ($pass->isParamSet('from'))
      {
         $pass->redirect_header($pass->getParam('from'));
      }
      elseif ($pass->isUserRole('student') && !$pass->is_valid_regnum($pass->getUserRegNum()))
      {
         $pass->redirect_header($pass->getAccountRef());
      }
      else
      {
         $pass->redirect_header();
      }
   }
   elseif ($pass->isUserVerificationRequired())
   {
      if ($pass->isParamSet('from'))
      {
         $pass->redirect_header($pass->getCheckMultiFactorRef()
           . '?from=' . urlencode($pass->getParam('from')));
      }
      else
      {
         $pass->redirect_header($pass->getCheckMultiFactorRef());
      }
   }
   elseif ($pass->isUserStatus('blocked'))
   {
      $pass->page_header();

      echo "<p>This account has been blocked. Please contact ",
       $pass->getConfigValue('help_reference'), ' for assistance.';
   
      $pass->page_footer();
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

   $pass->get_matched_variable('action', '/^(login|loginform)$/',
       'loginform');

   $pass->get_from_variable();

   if ($pass->isParam('action', 'login'))
   {
      $pass->get_username_variable('username');
      $pass->get_password_variable('password');

      $pass->check_not_empty('username');
      $pass->check_not_empty('password');

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'loginform');
      }
      else
      {
         $userDetails = $pass->verifyCredentials($pass->getParam('username'), $pass->getParam('password'));

         if ($userDetails === false || $pass->has_errors())
         {
            $pass->setParam('action', 'loginform');
         }
         elseif (!$pass->login())
         {
            $pass->add_error_message('Login failed');
            $pass->setParam('action', 'loginform');
         }
      }
   }
   elseif ($pass->isParam('action', 'loginform'))
   {
      $pass->get_username_variable('username');
   }
   else
   {
      $pass->setParam('action', 'loginform');
   }
}

function print_form()
{
   global $pass;

   echo $pass->start_form();
   echo $pass->form_input_hidden('from');
?>
   <p>If you don't already have an account, you can 
   <?php echo $pass->get_create_new_account_link('register an account'); ?>.
   If you have created an account but need to verify it, please use
   the <?php echo $pass->get_verify_account_link('verification page'); ?>.
   <table class="login">
    <tr>
     <th>
       <label for="username"><?php echo PassConfig::USERNAME_LABEL; ?>:</label><br>
       <span class="note">(Don't include <code>@<?php echo PassConfig::EMAIL_DOMAIN; ?></code> domain.)</span>
     </th>
     <td><?php
       echo $pass->form_input_username('username', 
        ['id'=>'username', 'required'=>'required']); 
       $pass->element_error_if_set('username');
       ?>
     </td>
    </tr>
     <th><label for="password">Password:</label></th>
     <td><?php 
       echo $pass->form_input_password('password', 
         ['id'=>'password', 'required'=>'required']);
       $pass->element_error_if_set('password');
      ?>
     </td>
    </tr>
    <tr><td colspan="2"><button type="submit" name="action" value="login">Login</button></td></tr>
   </table>
   </form>
<?php
   echo $pass->get_forgotten_password_link();

?> (<a href="<?php echo PassConfig::RESET_PASSWORD_HREF; ?>">Already have a password reset token?</a>)<?php
}

?>
