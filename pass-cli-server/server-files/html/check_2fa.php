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

 * MFA page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Verify Credentials');

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
      else
      {
         $pass->redirect_header($pass->getAccountRef());
      }
   }
   elseif ($pass->isParam('action', 'cancel'))
   {
      $pass->logout();
      $pass->redirect_header($pass->getLoginRef());
   }
   else
   {
      $pass->page_header();

      if ($pass->isUserRole(false))
      {
?>
<p>Verification failed. Please try <?php echo $pass->get_login_link('logging in'); ?> again.
<?php
      }
      elseif ($pass->has_errors())
      {
         $pass->print_errors();
      }
      elseif ($pass->isParam('action', 'verifyrecoveryform'))
      {
         verify_recovery_form();
      }
      else
      {
         verify_totp_form();
      }

      $pass->page_footer();
   }
}

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(verify(totp|recovery)(form)?|cancel)$/',
       'verifytotpform');

   $pass->get_from_variable('from');

   if ($pass->isParam('action', 'verifytotp'))
   {
      $pass->get_matched_variable('code', '/^[0-9]{6}$/');
      $pass->check_not_empty('code');

      $pass->get_boolean_variable('trust', false);

      if (!$pass->has_errors())
      {
         if (!$pass->verifyTOTP($pass->getParam('code'), $pass->getParam('trust')))
         {
            $pass->setParam('action', 'verifytotpform');
         }
      }

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'verifytotpform');
      }
   }
   elseif ($pass->isParam('action', 'verifyrecovery'))
   {
      $pass->get_matched_variable('code1', '/^[a-f0-9]{6}$/');
      $pass->check_not_empty('code1');

      $pass->get_matched_variable('code2', '/^[a-f0-9]{6}$/');
      $pass->check_not_empty('code2');


      if (!$pass->has_errors())
      {
         if (!$pass->verifyRecoveryCode(
                $pass->getParam('code1'), $pass->getParam('code2')))
         {
            $pass->setParam('action', 'verifyrecoveryform');
         }
      }

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'verifyrecoveryform');
      }
   }
}

function verify_recovery_form()
{
   global $pass;

   $pass->start_form();

   echo $pass->form_input_hidden('from');
?>
<p><label for="code1">Verification code:</label>
<?php

   echo $pass->form_input_textfield('code1',
     ['id'=>'code1', 'pattern'=>'[a-f0-9]{6}', 'required'=>true,
      'size'=>6, 'autocomplete'=>'off']);

?><label for="code2">-</label><?php

   echo $pass->form_input_textfield('code2',
     ['id'=>'code2', 'pattern'=>'[a-f0-9]{6}', 'required'=>true,
      'size'=>6, 'autocomplete'=>'off']);

   echo '<p>', $pass->form_submit_button(['value'=>'verifyrecovery'], 'Verify');

   echo '<span class="spacer"></span>', $pass->form_cancel_button();

   echo '</form><p>';

   echo $pass->href_self('action=verifytotpform', 'Use TOTP authentication instead'), '.';
}

function verify_totp_form()
{
   global $pass;

   $pass->start_form();

   echo $pass->form_input_hidden('from');
?>
<p><label for="code">Verification code:</label>
<?php

   echo $pass->form_input_textfield('code',
     ['id'=>'code', 'pattern'=>'[0-9]{6}', 'required'=>true,
      'size'=>6, 'autocomplete'=>'off']);

   echo '<p>', $pass->form_input_boolean_checkbox('trust', false, ['id'=>'trust']);
?>
<label for="trust">Trust this device for 30 days.</label> This setting requires a persistant cookie that will expire in 30 days. If set, you will only be prompted for your password when you login on this device for the next 30 days.
<?php

   echo '<p>', $pass->form_submit_button(['value'=>'verifytotp'], 'Verify');

   echo '<span class="spacer"></span>', $pass->form_cancel_button();
   echo '</form><p>';

   echo $pass->href_self('action=verifyrecoveryform', 'Use recovery code instead'), '.';
}

?>


