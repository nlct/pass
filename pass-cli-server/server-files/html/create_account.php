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

 * Create an account.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Create Account');

if ($pass->getUserRole())
{// already logged in
   $pass->redirect_header();
}
else
{
   process_params();

   if ($pass->isParam('action', 'createaccount'))
   {
      $message = $pass->create_user($pass->getParam('username'),
                    $pass->getParam('newpassword'));

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
            $pass->unsetParam('newpassword');
            $pass->unsetParam('confirmnewpassword');
            $pass->print_errors();
         }

         if (!$pass->getUserRole())
	 {
            print_form($pass);
	 }

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

   $pass->get_matched_variable('action', '/^(createaccount|form)$/', 'form');

   if ($pass->isParam('action', 'createaccount'))
   {
      $pass->get_username_variable('username');

      $pass->check_not_empty('username');

      $pass->get_password_variable('newpassword', true, 'password');

      $pass->get_password_variable('confirmnewpassword', false, 'password confirmation');

      $pass->check_not_empty('newpassword', 'password');
      $pass->check_not_empty('confirmnewpassword', 'password confirmation');

      if (!$pass->isErrorParamSet('newpassword')
          && ($pass->getParam('newpassword') !== $pass->getParam('confirmnewpassword'))
      {
         $pass->add_error_message('Password confirmation must match', 'confirmnewpassword', 'Mismatch');
      }

      $pass->get_boolean_variable('agree');

      if (!$pass->isBoolParamOn('agree'))
      {
         $pass->add_error_message('Agreement to terms and conditions required', 'agree',
	    'Required');
      }

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
<p>The PDF created by <?php echo $pass->getShortTitle(); ?> doesn't display your name anywhere
as assignment submissions are anonymous. However, 
your <?php echo PassConfig::USERNAME_LABEL; ?> and 
<?php echo PassConfig::REGNUM_LABEL; ?> are required <strong>in order to log your
marks</strong>.  It's therefore essential that you ensure you have your
<?php echo PassConfig::USERNAME_LABEL; ?> and <?php echo PassConfig::REGNUM_LABEL; ?> correctly entered when you setup your
<?php echo $pass->getShortTitle(); ?> account.  
Your <?php echo PassConfig::USERNAME_LABEL; ?> is your 
<?php echo PassConfig::USERNAME_OTHER_LABEL; ?>, which is typically in
the form <code><?php echo $pass->getExampleUserName(); ?></code>.

<p>Be sure to use a password that you haven't used with any other account. If you don't already use one, consider using a password manager. Passwords must have at least 
<?php echo Pass::MIN_PASSWORD_LENGTH; ?> characters and should not be common or easy to guess (such as &#x201C;password&#x201D; or &#x201C;12345678&#x201D;).

<table class="create_account">
 <tr>
  <th><label for="username"><?php echo PassConfig::USERNAME_LABEL; ?>:</label><br>
  <span class="note">(Don't include <code>@<?php echo PassConfig::EMAIL_DOMAIN; ?></code> domain.)</span>
  </th>
  <td><?php
   echo $pass->form_input_username('username',
	   [ 'id' => 'username', 'required'=>'required' ]);
   $pass->element_error_if_set('username');
  ?>
  </td>
 </tr>
 <tr id="emailaccountrow" style="visibility: hidden;">
  <th><?php echo PassConfig::UNIVERSITY_NAME; ?> email account:</th>
  <th id="emailaccount"></th>
 </tr>
 <tr>
  <th><label for="password">Password:</label><br>
  <span class="note">Minimum length: <?php echo Pass::MIN_PASSWORD_LENGTH; ?></span></th>
  <td><?php
   echo $pass->form_input_password('newpassword',
	   [ 'id' => 'password', 'required'=>'required' ]);
   $pass->element_error_if_set('newpassword');
   ?>
  </td>
 </tr>
 <tr>
  <th><label for="confirmnewpassword">Confirm Password:</label></th>
  <td><?php 
   echo $pass->form_input_password('confirmnewpassword',
	   [ 'id' => 'confirmnewpassword', 'required'=>'required' ]);
   $pass->element_error_if_set('confirmnewpassword'); 
  ?>
  </td>     
 </tr>
 <tr>
 <td colspan="2"><?php
   echo $pass->form_input_boolean_checkbox('agree', false, 
    [ 'id'=> 'agree' ]);
  ?>
  <label for="agree">I agree to the <?php echo $pass->get_terms_link('terms and conditions', ['target'=>'_blank']); ?></label>.
  </td>
 </tr>
 <tr>
  <td colspan="2">
   <button type="submit" id="create" name="action" value="createaccount">Create Account</button>
  </td>
 </tr>
</table>
</form>
<script>
const agreeelem = document.getElementById("agree");

 window.addEventListener('load', function()
 {
    var elem = document.getElementById("create");

    if (agreeelem.checked)
    {
       elem.disabled = false;
    }
    else
    {
       elem.disabled = true;
    }
 });

 agreeelem.addEventListener("change", (event) =>
 {
    var elem = document.getElementById("create");

    if (agreeelem.checked)
    {
       elem.disabled = false;
    }
    else
    {
       elem.disabled = true;
    }
 });

var usernameElem = document.getElementById("username");

usernameElem.addEventListener("change", (event) =>
 {
    var elem = document.getElementById("emailaccountrow");
    elem.style.visibility='visible';

    elem = document.getElementById("emailaccount");
    elem.innerHTML = usernameElem.value + '@<?php echo strtoupper(PassConfig::EMAIL_DOMAIN); ?>';
 });
</script>
<?php
}

?>
