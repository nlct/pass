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

 * Site configuration page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Configuration');
$pass->require_admin();// ensure user is logged in as admin

process_params();

if ($pass->isParam('action', 'update'))
{
   $success = $pass->updateConfigSettings();
}

$pass->page_header();

$pass->debug_message('Debugging mode ON.');

if ($pass->has_errors())
{
   $pass->print_errors();
}

if ($pass->isParam('action', 'update') && $success)
{
   echo '<div class="confirmation">Settings have been updated.</div>';
}
elseif ($pass->isParam('action', 'cancel'))
{
   echo '<div class="confirmation">Cancelled.</div>';
}

list_settings();

$pass->page_footer();

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(update|list|cancel)$/', 'list');

   $settings = $pass->getConfigSettings();

   foreach ($settings as $setting)
   {
      $pass->get_config_variable($setting);
   }

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'list');
   }
}

function list_settings()
{
   global $pass;

   echo $pass->start_form();

?>
<p>If you change any settings, you must click on the 'Update' button below for the changes to take effect.</p>
<table class="config">
<?php

   $settings = $pass->getConfigSettings();

   foreach ($settings as $setting)
   {
?>
<tr>
  <td><?php echo htmlentities($setting); ?></td>
  <td><?php echo $pass->form_input_config($setting); 
    $pass->element_error_if_set($setting);
    if ($pass->isParamSet("${setting}_striptagsdiff"))
    {
       echo '<pre>', $pass->getParam("${setting}_striptagsdiff"), '</pre>';
    }
   ?></td>
  <td class="description"><?php echo $pass->getConfigDescription($setting); ?></td>
</tr>
<?php
   }

?>
</table>
<?php echo $pass->form_submit_button(['value'=>'update'], 'Update');
   echo '<span class="spacer"> </span>';
   echo $pass->form_cancel_button();
   echo '</form>';
}

?>
