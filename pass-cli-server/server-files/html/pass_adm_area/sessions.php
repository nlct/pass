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

 * Session data page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass("Session Data");
$pass->require_admin();// ensure user is logged in as admin

$pass->page_header();

process_params();

if ($pass->has_errors())
{
   $pass->print_errors();
}

if ($pass->isParam('action', 'cleardata'))
{
   clear_data();
}

show_form();

$pass->page_footer();

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(cleardata|showform)$/',
           'showform');

   $pass->get_int_variable('hours', 1);

   $pass->get_matched_variable('selection', '/^(all|since)$/', 'all');
}

function clear_data()
{
   global $pass;

   $whos_online_sql = null;

   $pass->clearCourseData();

   if ($pass->isParam('selection', 'since'))
   {
      $sql = sprintf("DELETE FROM sessions WHERE date_touched < UNIX_TIMESTAMP(DATE_SUB(now(), INTERVAL %d HOUR))", $pass->getParam('hours'));

      if ($pass->getParam('hours') >= 1)
      {
         // entries are automatically deleted when the last click time was at least
         // an hour ago

         $whos_online_sql = sprintf("DELETE FROM whos_online WHERE last_time_clicked < DATE_SUB(NOW(), INTERVAL %d HOUR)", $pass->getParam('hours'));
      }
   }
   else
   {
      $sql = sprintf("DELETE FROM sessions WHERE session_id <> '%s'",
         $pass->db_escape_sql(session_id()));

      $whos_online_sql = sprintf("DELETE FROM whos_online WHERE user_id <> %d", $pass->getUserID());
   }

   $result = $pass->db_query($sql);

   if ($result)
   {
      $n = $pass->db_affected_rows();
      echo "<p>$n ", $n == 1 ? 'row' : 'rows', ' deleted.';

      if (isset($whos_online_sql))
      {
         $result = $pass->db_query($whos_online_sql);
      }
   }
   else
   {
      echo '<p>SQL statement failed.';
   }
}

function show_form()
{
   global $pass;

?>
<p>Course and assignment data is stored in the session data so that it doesn't
have to be fetched from the XML files every time a page that requires it is loaded
(for example, while stepping through the upload form).
Unfortunately this means that if the XML files are changed the updates won't be
picked up until users logout and log back in again.

<p>The session garbage collector should remove expired sessions from the database
but there's no guarantee.

<p>The form below allows you to delete all session data (except your own), which
will force all users to log in again. Alternatively, you can delete all old session 
data if the garbage collector hasn't done it.
<?php

   $sql = "SELECT COUNT(id) AS total, MIN(date_touched) AS earliest FROM sessions";

   $result = $pass->db_query($sql);

   if ($result)
   {
      $row = mysqli_fetch_assoc($result);

      if ($row)
      {
         $total = (int)$row['total'];
?>
<p>The database currently has information about 
<?php 
         echo "$total ", $total === 1 ? 'session' : 'sessions';
?> dating back as far as <?php
	 echo date(DATE_RFC7231, $row['earliest']), '.';

	 $pass->start_form();
?>
<p>
Both options below will clear all your course and assignment session data, 
but won't log you out.
<p>
<label><input type="radio" name="selection" value="all" 
<?php if ($pass->isParam('selection', 'all')) echo ' checked ';?>
/>
Clear all session data except mine.</label>
<p>
<label><input type="radio" name="selection" value="since" 
<?php if ($pass->isParam('selection', 'since')) echo ' checked ';?>
/>Clear all session data</label>
older than <?php echo $pass->form_input_number('hours', ['min'=>0]) ?> hour(s).
<p>
<?php
        echo $pass->form_submit_button(['value'=>'cleardata'], 'Clear Session Data');
?>
</form>
<?php
      }
      else
      {
         echo '<p>Failed to count session data.';
      }
   }
   else
   {
      echo "<p>No session information was found in the database.";
   }
}
?>
