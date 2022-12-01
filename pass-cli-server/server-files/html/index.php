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

 * Index (home) page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass();

$pass->page_header();
?>
<p>Welcome to the server version of <?php echo $pass->getShortTitle(); ?>.
<?php

if ($pass->getUserRole())
{
?>
<p>Would you like to <?php echo $pass->get_upload_link('upload a project'); ?> or <?php echo $pass->get_upload_lists_link('view your uploaded projects'); ?>?
<?php
}
else
{
   $pass->printBlurb();
?>
 <p>Since <?php echo $pass->getShortTitle(); ?> is designed to compile and run
source code, it's necessary to isolate this server from the rest of the
university for security reasons. This means that you can't use your 
normal <?php echo PassConfig::UNIVERSITY_NAME; ?> single sign-on (SSO) credentials to use this site. Instead you need to create an account
specifically for this server version of <?php echo $pass->getShortTitle(); ?>.
If you have already created an account, you can <?php echo $pass->get_login_link('login'); ?>. If you haven't yet created an account, you need to <?php echo $pass->get_create_new_account_link('create a new account'); ?>.

<?php
}

$pass->page_footer();
?>
