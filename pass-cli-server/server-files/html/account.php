<?php
/*
 * Server Pass
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

 * Show account details.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Account');

$pass->require_login();// ensure user is logged in

process_params();

$pass->page_header();

if ($pass->has_errors())
{
   $pass->print_errors();
}

if ($pass->isParam('action', 'verify2fa'))
{
   verify2fa();
}
elseif ($pass->isParam('action', 'enable2fa'))
{
   enable2fa();
}
elseif ($pass->isParam('action', 'view2fa'))
{
   mfa_form();
}
elseif ($pass->isParam('action', 'querydisable2fa'))
{
   query_disable2fa();
}
elseif ($pass->isParam('action', 'disable2fa'))
{
   disable2fa();
}
elseif ($pass->isParam('action', 'delete2fa'))
{
   delete2fa();
}
elseif ($pass->isParam('action', 'showtrusted'))
{
   show_trusted_devices();
}
elseif ($pass->isParam('action', 'removetrustedselection'))
{
   remove_trusted_selection();
}
elseif ($pass->isParam('action', 'recoverycodes'))
{
   recovery_codes();
}
elseif ($pass->isParam('action', 'replacerecoverycodes'))
{
   new_recovery_codes(true);
}
elseif ($pass->isParam('action', 'newrecoverycodes'))
{
   new_recovery_codes(false);
}
else
{
   summary();
}

$pass->page_footer();

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', 
     '/^(summary|verify2fa|enable2fa|view2fa|(query)?disable2fa|delete2fa|cancel|showtrusted|removetrustedselection|(new|replace)?recoverycodes)$/',
     'summary');

   if ($pass->isParam('action', 'verify2fa'))
   {
      $pass->get_matched_variable('code', '/^\d{6}$/');

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'enable2fa');
      }
   }

}

function verify2fa()
{
   global $pass;
   if ($pass->enable2FA($pass->getParam('code')))
   {
?>
<div class="confirmation">2FA enabled.</div>
<?php

      summary();
   }
   elseif ($pass->has_errors())
   {
      $pass->print_errors();

      mfa_form();
   }
   else
   {
?>
<p>Something went wrong.
<?php
      $pass->log_error("Failed to verify 2FA");
   }
}

function enable2fa()
{
   global $pass;
   if ($pass->has2FAKey() && $pass->is2FAVerified())
   {
      if ($pass->enable2FA())
      {
?>
<div class="confirmation">2FA re-enabled.</div>
<?php
      }
      elseif ($pass->has_errors())
      {
         $pass->print_errors();
      }
      else
      {
?>
<p>Something went wrong.
<?php
         $pass->log_error("Failed to enable 2FA");
      }

      summary();
   }
   else
   {
      if ($pass->has_errors())
      {
         $pass->print_errors();
      }

      mfa_form();
   }
}

function mfa_form()
{
   global $pass;
   $pass->start_form();
?>
<p>To enable 2FA you need a time-based one time password (TOTP) authenticator app,
such as Google Authenticator, installed on your mobile device.

<p>Scan the QR code below or manually enter the key in your authenticator app:
<?php

   $pass->show2FAchart();

?>
<p><label for="code">Enter the 6-digit code provided by your authenticator app:</label>
<?php

   echo $pass->form_input_textfield('code',
     ['id'=>'code', 'pattern'=>"[0-9]{6}", 'required'=>true,
      'autocomplete'=>'off']);

   echo $pass->form_submit_button(
     ['value'=>'verify2fa', 'class'=>'visually-hidden'],
     'Verify');

?>
<div class="clearfix">
<p class="alignleft">
<?php

   echo $pass->form_cancel_button();

?>
</p>
<p class="alignright">
<?php

   echo $pass->form_submit_button(['value'=>'verify2fa'],
     'Verify');

?>
</p>
</div>
</form>
<?php
}

function query_disable2fa()
{
   global $pass;
   $pass->start_form();

?>
<p>Would you like to just temporarily disable 2FA or do you want to delete
your 2FA key? If you want to delete your 2FA key you will also need to delete
the corresponding entry in your authenticator app.

<div class="clearfix">
<p class="alignleft">
<?php

   echo $pass->form_cancel_button();
?>
</p>
<p class="alignright">
<?php

   echo $pass->form_submit_button(['value'=>'disable2fa'], 'Disable Only');

?>
<span class="spacer"> </span>
<?php

   echo $pass->form_submit_button(['value'=>'delete2fa'], 'Delete 2FA Key');

?>
</p>
</div>
</form>
<?php
}

function disable2fa()
{
   global $pass;
   if ($pass->disable2FA())
   {
?>
<div class="confirmation">2FA disabled. The 2FA key has been retained so you can easily re-enable the 2FA setting.</div>
<?php
   }
   elseif ($pass->has_errors())
   {
      $pass->print_errors();
   }

   summary();
}

function delete2fa()
{
   global $pass;
   if ($pass->disable2FA(true))
   {
?>
<div class="confirmation">2FA code deleted. Remember to delete the entry from your authenticator app.</div>
<?php
   }
   elseif ($pass->has_errors())
   {
      $pass->print_errors();
   }

   summary();
}

function trusted_device_blurb($totp='time-based one time password (TOTP)')
{
?>
<p>If you enable 2FA then you can select the “trust this device for 30 days” checkbox
when you authenticate with your <?php echo $totp; ?>. If you select
this option, a unique token is created and stored in a persistent cookie (called
<code><?php echo Pass::COOKIE_NAME_TRUST; ?>)</code> that expires after 30 days.

<p>The same token (split in two parts) is also stored in a database with a unique
numeric identifier associated with your account. In addition, your IP, browser
and platform (if they can be determined) are stored as encrypted data in the
same table row. This information is saved to allow you to double-check the list
of trusted devices. (The information is not used for verification
or for any other purpose.)

<p>The cookie only contains the token. It does not contain any personally identifable
information. Both the cookie and the matching row in the database must be present
and active (not expired) for the TOTP step to be skipped.
<?php
}

function show_trusted_devices()
{
   global $pass;
?>
<h2>Trusted Devices</h2>
<?php
      trusted_device_blurb();
?>

<p>If you have any active trusted devices they will be listed below. You can
remove a trusted device by either deleting the associated cookie or by
removing the corresponding entry in the table below.

<p>The IP address corresponds to the IP address at the time the trust setting
was activated. Many IP addresses are dynamically allocated and can change over
time. The platform and browser information is determined by parsing the user
agent string. This doesn’t have a standard syntax (and can sometimes be empty)
so the platform and browser details may be incorrect or unavailable.

<?php

   $all_rows = $pass->get_trusted_devices();

   if ($all_rows === false)
   {
?>
<p>Unable to obtain list of trusted devices.
<?php
      return;
   }

   $num_rows = count($all_rows);

   if ($num_rows === 0)
   {
?>
<p>No trusted devices.
<?php

      $pass->start_form();

      echo back_to_summary_button();
      echo '</form>';

      return;
   }
   elseif ($num_rows === 1)
   {
?>
<p>1 trusted device.
<?php
   }
   else
   {
?>
<p><?php echo $num_rows; ?> trusted devices.
<?php
   }

   $pass->start_form();
?>
<table class="topheader">
<tr><th></th><th>Browser</th><th>Platform</th><th>IP</th><th>Expires</th></tr>
<?php

   foreach ($all_rows as $row)
   {
      try
      {
         $info = json_decode(decryptDeviceData($row['device']), true);
      }
      catch (Exception $e)
      {
         $info = array();
      }

?>
<tr>
  <td><input type="checkbox" name="row[]" value="<?php echo $row['id']; ?>"/></td>
  <td><?php

   echo isset($info['browser']) ? htmlentities($info['browser']) : 'unknown';

?></td>
  <td><?php

   echo isset($info['platform']) ? htmlentities($info['platform']) : 'unknown';

?></td>
  <td><?php

   echo isset($info['ip']) ? htmlentities($info['ip']) : 'unknown';

?></td>
  <td><?php echo htmlentities($row['expires']); ?></td>
</tr>
<?php
   }

?>
</table>
<?php

   echo $pass->form_submit_button(
     ['value'=>'removetrustedselection', 'class'=>'visually-hidden'],
     'Remove Selected');

?>
<div class="clearfix">
<p class="alignleft">
<?php

   echo back_to_summary_button();
?>
</p>
<p class="alignright">
<span class="spacer"> </span>
<?php

   echo $pass->form_submit_button(['value'=>'removetrustedselection'],
     'Remove Selected');

?>
</p>
</div>
</form>
<?php
}

function remove_trusted_selection()
{
   global $pass;
   $total = 0;

   if ($pass->isParamSet('row'))
   {
      $total = $pass->remove_trusted_devices($pass->getParam('row'));

      if ($pass->has_errors())
      {
         $pass->print_errors();
      }
   }

   echo '<div class="confirmation">';

   if ($total === 1)
   {
?>
1 device removed.
<?php
   }
   elseif ($total > 1)
   {
?>
<?php echo $total;?> devices removed.
<?php
   }
   else
   {
?>
No devices removed.
<?php
   }

   echo '</div>';

   show_trusted_devices();
}

function recovery_codes()
{
   global $pass;
   $pass->start_form();

   $codes = $pass->get_recovery_codes();

   if (empty($codes))
   {
?>
<p>You don't have any recovery codes set.
<?php
      $action_value = 'newrecoverycodes';
      $action_text = 'Create Recovery Codes';

   }
   else
   {
      $n = count($codes);

      $action_value = 'replacerecoverycodes';
      $action_text = 'Create New Recovery Codes';
?>
<p>You have <?php echo $n, ' recovery ', $n === 1 ? 'code' : 'codes'; ?> available.
Each code can only be used once.
<pre>
<?php

      foreach ($codes as $code)
      {
         echo htmlentities($code), PHP_EOL;
      }

?>
</pre>
<p>If you create a new set of recovery codes, the existing codes will be deleted.
<?php
   }

   echo $pass->form_submit_button(
        ['value'=>$action_value, 'class'=>'visually-hidden'],
        $action_text);

?>
<div class="clearfix">
<p class="alignleft">
<?php

   echo back_to_summary_button();
?>
</p>
<p class="alignright">
<?php

   echo $pass->form_submit_button(['value'=>$action_value],
        $action_text);

?>
</p>
</div>
</form>
<?php
}

function new_recovery_codes($delete_old)
{
   global $pass;
   if ($delete_old)
   {
      if (!$pass->delete_recovery_codes())
      {
?>
<p>Unable to delete old recovery codes.
<?php
      }
   }

   $codes = $pass->create_recovery_codes();

   $n = count($codes);
?>
<p>You have <?php echo $n, ' recovery ', $n === 1 ? 'code' : 'codes'; ?> available.
Store them in a safe place. Each code can only be used once.
<pre>
<?php

   foreach ($codes as $code)
   {
      echo htmlentities($code), PHP_EOL;
   }

?>
</pre>
<?php

   $pass->start_form();

   echo back_to_summary_button();

   echo '</form>';
}

function back_to_summary_button($text='&#x23F4; Back to Summary')
{
   global $pass;

   echo $pass->form_prev_button(['value'=>'summary'], $text);
}

function back_to_summary($text='&#x23F4; Back to Summary')
{
   global $pass;

   echo '<p>', $pass->href_self('', $text);
}

function summary()
{
   global $pass;

   $role = $pass->getUserRole();
?>
<section id="general" class="anchor">
<h2>General</h2>
<table class="account">
  <tr>
    <th>User name:</th>
    <td class="username"><?php echo htmlentities($pass->getUserName()); ?></td>
  </tr>
  <tr>
    <th>Account type:</th>
    <td class="role"><?php echo htmlentities($role); ?></td>
  </tr>
<?php
      $regnum = $pass->getUserRegNum();
?>
  <tr>
    <th>Registration Number:</th>
    <td class="regnum"><?php

      if (empty($regnum))
      {
         echo '<span class="notset">Not set</span> ', $pass->get_set_regnum_link();
      }
      elseif ($pass->is_valid_regnum($regnum))
      {
         echo htmlentities($regnum), ' (',
           $pass->get_set_regnum_link('Incorrect?'), ')';
      }
      else
      {
         echo '<span class="error">', htmlentities($regnum), '</span>',
          $pass->get_set_regnum_link('Invalid');
      }
?></td>
  </tr>
</table>
<?php

   if ($role === 'student')
   {
      echo '<p>Please ensure that your registration number is correct before uploading a project.';
   }
?>
</section>

<section id="security" class="anchor">
<h2>Security</h2>

<?php
   echo '<p>', $pass->get_change_password_link();

   echo '<div>';

   $mfa_enabled = $pass->is2FAEnabled();

   $totp = 'time-based one time password (TOTP)';

   if (!$mfa_enabled)
   {
?>
<p>Secure your account by setting up two-factor authentication (2FA).
To enable 2FA you need a <?php echo $totp; ?> authenticator app,
such as Google Authenticator, installed on your mobile device.
<?php

      $totp = 'TOTP';
   }

?>
  <div class="tableheader">Two-factor authentication (2FA):</div>
<?php

   if ($mfa_enabled)
   {
?>
  <div class="tablecontent">on</div>
  <div class="tableaction">
<?php

      echo $pass->href_self('action=querydisable2fa', 'Disable 2FA');
      echo ' | ';
      echo $pass->href_self('action=view2fa', 'Re-verify Existing 2FA Key'); 
?>
  </div>
  <br>(If you need a new key, <?php echo $pass->href_self('action=delete2fa', 'delete the existing key'); ?> and re-enable 2FA.)<p>
<?php
   }
   else
   {
?>
  <div class="tablecontent">off</div>
  <div class="tableaction"><?php

      echo $pass->href_self('action=enable2fa', 'Enable 2FA');
?></div>
<?php
   }
?>
</div>
<?php
   if ($mfa_enabled)
   {
?>
<div>
  <div class="tableheader">Recovery codes:</div>
  <div class="tablecontent"></div>
  <div class="tableaction"><?php

       echo $pass->href_self('action=recoverycodes', 'View/Set Recovery Codes');

?></div>
</div>
<?php
   }
?>
</div>
<?php

   trusted_device_blurb($totp);

   echo '<p>', $pass->href_self('action=showtrusted', 'Show Trusted Devices');
?>

</section>
<?php
}

?>
