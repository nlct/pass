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

 * Action recorder page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('View Action Recorder');
$pass->require_admin();// ensure user is logged in as admin

process_params();

$pass->page_header();

if ($pass->isParam('action', 'confirmcleardata'))
{
   confirm_clear_data();
}
elseif ($pass->isParam('action', 'cleardata'))
{
   delete_data();
}
elseif ($pass->isParam('action', 'cancelled'))
{
   echo '<p>', $pass->href_self('View Records'), '.';
}
else
{
   list_messages();
}

$pass->page_footer();

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^((confirm)?cleardata|search|cancel)$/',
           'search');

   $pass->get_date_variable('before');

   if (!$pass->isParamSet('before'))
   {
      $now = new DateTime();
      $pass->setParam('before', 
        $now->sub(new DateInterval("P1M"))->format('Y-m-d'));
   }

   if ($pass->isParam('action', 'search'))
   {
      $pass->get_int_variable('user_id');
      $pass->get_htmlentities_variable('actionfilter');
      $pass->get_htmlentities_variable('commentsfilter');
      $pass->get_matched_variable('boolop', '/^(AND|OR)$/', 'OR');
      $pass->get_boolean_variable('searchnotaction');
      $pass->get_boolean_variable('searchnotcomment');

      $pass->get_date_variable('since');

      if (!$pass->isParamSet('since'))
      {
         $now = new DateTime();
         $pass->setParam('since', 
           $now->sub(new DateInterval("P1M"))->format('Y-m-d'));
      }
   }
   elseif ($pass->isParam('action', 'cancel'))
   {
      $_SESSION['confirmation_message'] = 'Cancelled';

      $pass->setParam('since', $pass->getParam('before'));
   }
   else
   {
      $pass->check_not_empty('before');

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'confirmcleardata');
      }
   }
}

function delete_data()
{
   global $pass;

   $datetime = $pass->getParam('before');

   $sql = sprintf("DELETE FROM action_recorder WHERE time_stamp < '%s'",
      $pass->db_escape_sql($datetime->format('Y-m-d')));

   $result = $pass->db_query($sql);

   if ($result)
   {
      $n = $pass->db_affected_rows();
      echo "<p>$n ", $n == 1 ? 'row' : 'rows', ' deleted.';
   }
   else
   {
      echo '<p>Query failed.';

      if ($pass->has_errors())
      {
         $pass->print_errors();
      }
   }
}

function confirm_clear_data()
{
   global $pass;

   if ($pass->has_errors())
   {
      $pass->print_errors();

      clear_data_form();
   }
   else
   {
      echo $pass->start_form();

      $before = $pass->getParam('before')->format('Y-m-d');
?>
<input type="hidden" name="before" value="<?php echo $before; ?>">
<p>Confirm delete action recorder data older than <?php

      echo $before, '?';

      echo '<p>', $pass->form_submit_button(['value'=>'cleardata'],
       'Confirm Delete'), ' ', $pass->form_cancel_button();

?>
</form>
<?php
   }
}

function clear_data_form()
{
   global $pass;

   echo $pass->start_form();
?>
<p>Delete data <label for="before">before:</label>

<?php

   echo $pass->form_input_date('before', ['id'=>'before']);
   $pass->element_error_if_set('before');

   echo '<p>', $pass->form_submit_button(['value'=>'confirmcleardata'],
    'Delete Old Data');
?>

</form>
<?php
}

function list_messages()
{
   global $pass;

   if ($pass->has_errors())
   {
      $pass->print_errors();
   }

   echo $pass->start_form();
?>
<p><label for="since">Since:</label>

<?php

   echo $pass->form_input_date('since', ['id'=>'since']);
   $pass->element_error_if_set('since');

?>
<p><label for="uid">UID: </label>
<?php

   echo $pass->form_input_number('user_id', ['id'=>'uid']);
   $pass->element_error_if_set('user_id');

?>
<p>And
<p><label for="action">action: </label>
<span class="nobr">
<label>
<?php
   echo $pass->form_input_boolean_checkbox('searchnotaction', false,
           ['id'=>'searchnotaction', 'onchange'=>'toggleNotActionCheckBox()']);
   ?>
   <span id="notaction">Not</span>
</label>
<?php

   echo $pass->form_input_textfield('actionfilter',
      ['id'=>'action', 'placeholder'=>'regex']);
   $pass->element_error_if_set('actionfilter');

   echo ' ', $pass->form_input_select('boolop', ['AND', 'OR'], '' , '', false);
   $pass->element_error_if_set('boolop');
?> <label for="comments">comments: </label>
<span class="nobr">
<label>
<?php
   echo $pass->form_input_boolean_checkbox('searchnotcomment', false,
           ['id'=>'searchnotcomment', 'onchange'=>'toggleNotCommentCheckBox()']);
   ?>
   <span id="notcomment">Not</span>
</label>
<?php

   echo $pass->form_input_textfield('commentsfilter',
      ['id'=>'comments', 'placeholder'=>'regex']);
   $pass->element_error_if_set('commentsfilter');

   echo '<p>', $pass->form_submit_button();
?>

</form>
<?php

   $since = $pass->getParam('since');

   if ($since instanceof DateTime)
   {
      $since = $since->format('Y-m-d 00:00:00');
   }
   else
   {
      $since .= ' 00:00:00';
   }

   $user_id = $pass->getParam('user_id');
   $action = $pass->getParam('actionfilter');
   $comments = $pass->getParam('commentsfilter');

   $bind_param_types = 's';
   $bind_param_var = array($since);

   $where = 'time_stamp > ?';

   if (isset($user_id))
   {
      $uid_regexp = "UID: $user_id\\b";
      $where .= " AND (user_id=? OR comments REGEXP ?)";
      $bind_param_types .= 'is';
      array_push($bind_param_var, $user_id, $uid_regexp);
   }

   if (isset($action) && $action !== '')
   {
      if (isset($comments) && $comments !== '')
      {
         $where .= ' AND (action';

         if ($pass->isBoolParamOn('searchnotaction'))
         {
            $where .= ' NOT',
         }

         $where .= '  REGEXP ? ' . $pass->getParam('boolop') . ' comments';

         if ($pass->isBoolParamOn('searchnotaction'))
         {
            $where .= ' NOT';
         }

         $where .= ' REGEXP ? )';

         $bind_param_types .= 'ss';
         array_push($bind_param_var, $action, $comments);
      }
      else
      {
         $where .= ' AND action';

         if ($pass->isBoolParamOn('searchnotaction'))
         {
            $where .= ' NOT';
         }

         $where .= ' REGEXP ?';

         $bind_param_types .= 's';
         array_push($bind_param_var, $action);
      }
   }
   elseif (isset($comments) && $comments !== '')
   {
      $where .= ' AND comments';

      if ($pass->isBoolParamOn('searchnotcomment'))
      {
         $where .= ' NOT';
      }

      $where .= ' REGEXP ?';

      $bind_param_types .= 's';
      array_push($bind_param_var, $comments);
   }

   $stmt = $pass->db_prepare("SELECT id, user_id, time_stamp, action, comments FROM action_recorder WHERE $where ORDER BY id DESC");

   if (!$stmt)
   {
      echo '<p>Failed to prepare statement.';
      return;
   }

   $stmt->bind_param($bind_param_types, ...$bind_param_var);

   if ($stmt->execute())
   {
      $action_id = null;
      $time_stamp = null;

      $stmt->bind_result($action_id, $user_id, $time_stamp, $action, $comments);

      $stmt->store_result();
      $num_rows = $stmt->num_rows();

?>
<p>Found <?php echo $num_rows, ' ', $num_rows === 1 ?  'message' : 'messages'; ?>.
<p>SQL failures indicate either a problem with the database
connection or a bug in the code. Check the
<span class="file">php_errors.log</span> file for further details.
<p>
The User ID column indicates the user was authenticated when the
action was performed. If the UID is instead shown in the comments
column, it means that the user was found in the database but either
wasn't logged in or the session data hadn't been set (for example,
pending 2FA check).
<?php

      if ($num_rows > 0)
      {
?>
<table class="action_recorder">
<tr><th>Date</th><th>User ID</th><th>Action</th><th>Comments</th></tr>
<?php

         while ($stmt->fetch())
         {
?>
<tr>
<td><?php echo $time_stamp; ?></td>
<td><?php 

            if (isset($user_id))
            {
               $user_id = (int)$user_id;

               echo $pass->get_admin_users_link($user_id, null, "id=$user_id"); 

               if ($user_id === $pass->getUserID())
               {
                  echo ' (me)';
               }
            }
            else
            {
               echo '&#x2014;';
            }

?></td>
<td><?php

            // record_action uses htmlentities
            if (isset($action))
            {
               echo $action;
            }
            else
            {
               echo '&#x2014;';
            }

?></td>
<td><?php

            if (isset($comments))
            {
               echo preg_replace('/UID: (\d+)/', 
                 sprintf('UID: <a href="%s?id=$1">$1</a>', $pass->getAdminUsersRef()),
                           $comments);
            }
            else
            {
               echo '&#x2014;';
            }

?></td>
</tr>
<?php
         }

?>
</table>
<?php
      }
   }
   else
   {
?>
<p>Failed to execute statement.
<?php
   }

   $stmt->close();

   echo '<h2>Clear Old Data</h2>';

   clear_data_form();
?>
<script>
var notactionLabel = document.getElementById("notaction");
var notcommentLabel = document.getElementById("notcomment");

var notactionCheckBox = document.getElementById('searchnotaction');
var notcommentCheckBox = document.getElementById('searchnotcomment');

window.addEventListener('load', function()
 {
    if (!notactionCheckBox.checked)
    {
       notactionLabel.classList.add('faded');
    }

    if (!notcommentCheckBox.checked)
    {
       notcommentLabel.classList.add('faded');
    }

 });
 function toggleNotActionCheckBox()
 {
    if (notactionCheckBox.checked)
    {
       notactionLabel.classList.remove('faded');
    }
    else
    {
       notactionLabel.classList.add('faded');
    }
 }
 function toggleNotCommentCheckBox()
 {
    if (notcommentCheckBox.checked)
    {
       notcommentLabel.classList.remove('faded');
    }
    else
    {
       notcommentLabel.classList.add('faded');
    }
 }
</script>
<?php
}

?>
