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

 * Admin home page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Admin');
$pass->require_admin();// ensure user is logged in as admin

$pass->page_header();

$pass->debug_message('Debugging mode ON.');
?>
<p>If you want to view all the submission data available in the database, use the 
<?php echo $pass->get_upload_lists_link('View Uploads'); ?> link.
If you want to check the upload directories or requeue uploads, use the Admin -&gt; 
<?php echo $pass->get_admin_upload_dirs_link('Upload Directories'); ?> menu link.
If you want to check for error messages from the backend see the
<?php echo $pass->get_admin_viewlogs_link(); ?> page. Common error
messages are listed in the <?php echo $pass->get_faq_link(); ?>.

<p>If the resource or assignment XML files have been modified, users will need
to logout and log back in again to pick up the changes. You can force everyone
to login again by clearing the session data with the 
Admin -&gt; <?php echo $pass->get_admin_sessions_link(); ?> menu link.

<p>If you need to make any disruptive changes you can check if there's anyone 
currently logged in with the Admin -&gt; <?php echo $pass->get_admin_whosonline_link(); ?> 
menu link. You can add a site-wide message banner and switch the site to maintenance mode in the 
<?php echo $pass->get_admin_config_link('configuration page'); ?>.
In maintenance mode, the login page can be used by all users who aren't already
logged in (although they will have to remember the address or have it
bookmarked) but all other pages will redirect to the maintenance page unless
the user is an administrator.

<p>If a new version of PASS needs to be installed or if other
maintenance work is required on the backend, see the Admin -&gt;
<?php echo $pass->get_admin_maintenance_link(); ?> page.

<?php
$pass->page_footer();

?>
