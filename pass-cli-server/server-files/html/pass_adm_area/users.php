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

 * List of users page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Users');
$pass->require_admin();// ensure user is logged in as admin

process_params();

$pass->page_header();

$pass->debug_message('Debugging mode ON.');

if ($pass->has_errors())
{
   $pass->print_errors();
}

if ($pass->isParam('action', 'update'))
{
   update_user();
}
elseif ($pass->isParam('action', 'edit'))
{
   edit_form();
}
elseif ($pass->isParam('action', 'delete'))
{
   confirmDelete();
}
elseif ($pass->isParam('action', 'deleteconfirmed'))
{
   delete_user();
}
elseif ($pass->isParam('action', 'merge'))
{
   confirmMerge();
}
elseif ($pass->isParam('action', 'mergeconfirmed'))
{
   merge_user();
}
elseif ($pass->isParam('action', 'invalidateregnum'))
{
   invalidate_regnum();
}
else
{
   list_users();
}

$pass->page_footer();

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(search|list|edit|update|cancel|delete|deleteconfirmed|merge|mergeconfirmed|invalidateregnum)$/', 'list');

   $pass->setParam('boolop', ['AND'=>'and', 'OR'=>'or']);

   $pass->get_int_variable('id');

   if ($pass->isParam('action', 'edit'))
   {
      $pass->check_not_empty('id');

      if ($pass->isParamSet('id'))
      {
         $user = $pass->getUserInfoById($pass->getParam('id'));

         $pass->get_username_variable('username', $user['username']);
         $pass->get_regnum_variable('regnum', $user['regnum']);
         $pass->get_choice_variable('role', Pass::ROLE_OPTIONS, $user['role']);
         $pass->get_choice_variable('status', Pass::STATUS_OPTIONS, $user['status']);

	 $pass->get_username_variable('originalusername', $user['username']);
         $pass->get_regnum_variable('originalregnum', $user['regnum']);
	 $pass->get_choice_variable('originalrole', Pass::ROLE_OPTIONS,
		 $user['role']);
	 $pass->get_choice_variable('originalstatus', Pass::STATUS_OPTIONS,
		 $user['status']);
      }
      else
      {
         $pass->setParam('action', 'list');
      }
   }
   elseif ($pass->isParam('action', 'update'))
   {
      $pass->get_username_variable('username');
      $pass->get_regnum_variable('regnum');
      $pass->get_choice_variable('role', Pass::ROLE_OPTIONS);
      $pass->get_choice_variable('status', Pass::STATUS_OPTIONS);
      $pass->get_username_variable('originalusername');
      $pass->get_regnum_variable('originalregnum');
      $pass->get_choice_variable('originalrole', Pass::ROLE_OPTIONS);
      $pass->get_choice_variable('originalstatus', Pass::STATUS_OPTIONS);

      $pass->check_not_empty('id');
      $pass->check_not_empty('username');

      if ($pass->has_errors())
      {
         $pass->setParam('action', $pass->isParamSet('id') ? 'edit' : 'list');
      }
   }
   elseif ($pass->isParam('action', 'invalidateregnum'))
   {
      $pass->get_username_variable('username');
      $pass->get_regnum_variable('regnum');
      $pass->check_not_empty('username');
   }
   elseif ($pass->isParam('action', 'merge') || $pass->isParam('action', 'mergeconfirmed'))
   {
      $pass->get_int_variable('id2');

      $pass->get_username_variable('username');
      $pass->get_regnum_variable('regnum');
      $pass->get_choice_variable('role', Pass::ROLE_OPTIONS);
      $pass->get_choice_variable('status', Pass::STATUS_OPTIONS);
      $pass->get_username_variable('originalusername');
      $pass->get_regnum_variable('originalregnum');
      $pass->get_choice_variable('originalrole', Pass::ROLE_OPTIONS);
      $pass->get_choice_variable('originalstatus', Pass::STATUS_OPTIONS);

      if (!$pass->isParamSet('id'))
      {
         $pass->add_error_message("Can't merge, no primary ID set.");
         $pass->setParam('action', 'list');
      }
      elseif (!$pass->isParamSet('id2'))
      {
         $pass->add_error_message("Missing secondary ID.", 'id2', 'Missing');
         $pass->setParam('action', 'edit');
      }
      elseif ($pass->getParam('id') === $pass->getParam('id2'))
      {
         $pass->add_error_message("Secondary ID must be different from primary ID",
            'id2', 'Invalid');
         $pass->setParam('action', 'edit');
      }
      elseif ($pass->isParam('action', 'mergeconfirmed'))
      {
         $pass->check_not_empty('username');
         $pass->check_not_empty('role');
         $pass->check_not_empty('status');

         if ($pass->has_errors())
         {
            $pass->setParam('action', $pass->isParamSet('id2') ? 'merge' : 'edit');
         }
      }
   }
   elseif ($pass->isParam('action', 'delete') || $pass->isParam('action', 'deleteconfirmed'))
   {
      $pass->get_username_variable('username');
      $pass->check_not_empty('id');

      if ($pass->has_errors())
      {
         $pass->setParam('action', 'list');
      }
   }

   $pass->get_unfiltered_variable('searchusername');
   $pass->get_boolean_variable('searchnotusername');
   $pass->get_unfiltered_variable('searchregnum');
   $pass->get_boolean_variable('searchnotregnum');
   $pass->get_choice_variable('searchboolop', array_keys($pass->getParam('boolop')),
      'OR');

   $pass->get_int_variable('page', 1);

}

function update_user()
{
   global $pass;
   
   if ($pass->updateUser())
   {
      echo '<div class="confirmation">User details updated.</div>';
      list_users();
   }
   elseif ($pass->has_errors())
   {
      $pass->print_errors();
      edit_form();
   }
   else
   {
      echo "<div class=\"error\">I'm sorry, something went wrong.</div>";
      edit_form();
   }
}

function delete_user()
{
   global $pass;

   if ($pass->deleteUser($pass->getParam('id')))
   {
      if ($pass->isParamSet('username'))
      {
         echo '<div class="confirmation">User ', htmlentities($pass->getParam('username')), ' deleted.</div>';
      }
      else
      {
         echo '<div class="confirmation">User ', $pass->getParam('id'), ' deleted.</div>';
      }

   }
   elseif ($pass->has_errors())
   {
      $pass->print_errors();
      edit_form();
   }
   else
   {
      echo "<div class=\"error\">I'm sorry, something went wrong.</div>";
      edit_form();
   }
}

function confirmDelete()
{
   global $pass;

   echo $pass->start_form();

   echo '<p>Are you sure you want to delete user ';

   if ($pass->isParamSet('username'))
   {
      echo htmlentities($pass->getParam('username')), ' (ID ', $pass->getParam('id'), ')';
   }
   else
   {
      echo ' identified by ID ', $pass->getParam('id');
   }

   echo '? ';

   echo $pass->form_input_hidden('id');
   echo $pass->form_input_hidden('username');

   echo $pass->form_submit_button(['value'=>'deleteconfirmed'], 'Delete');
   echo '<span class="spacer"> </span>';
   echo $pass->form_cancel_button();

   echo '</form>';
}

function confirmMerge()
{
   global $pass;

   $id = $pass->getParam('id');
   $id2 = $pass->getParam('id2');

   $data = $pass->getUserInfoByID([$id, $id2]);

   if ($data === false)
   {
      echo "<p>No data found for user IDs $id, $id2";
      return;
   }
   elseif (!isset($data[$id]))
   {
      echo "<p>No data found for user ID $id";
      return;
   }
   elseif (!isset($data[$id2]))
   {
      echo "<p>No data found for user ID $id2";
      return;
   }

   $pass->setParam('originalusername', $data[$id]['username']);
   $pass->setParam('originalrole', $data[$id]['role']);
   $pass->setParam('originalstatus', $data[$id]['status']);

   if (isset($data[$id]['regnum']))
   {
      $pass->setParam('originalregnum', $data[$id]['regnum']);
   }

   if (!$pass->isParamSet('username'))
   {
      $pass->setParam('username', $data[$id]['username']);
   }

   if (!$pass->isParamSet('role'))
   {
      $pass->setParam('role', $data[$id]['role']);
   }

   if (!$pass->isParamSet('status'))
   {
      $pass->setParam('status', $data[$id]['status']);
   }

   if (!$pass->isParamSet('regnum'))
   {
      if (isset($data[$id]['regnum']))
      {
         $pass->setParam('regnum', $data[$id]['regnum']);
      }
      elseif (isset($data[$id2]['regnum']))
      {
         $pass->setParam('regnum', $data[$id2]['regnum']);
      }
   }

   echo $pass->start_form();
?>
<p>Are you sure you want to merge users?

<table>
<tr>
 <th>ID</th>
 <td><?php echo $id, $pass->form_input_hidden('id'); ?></td>
 <td><?php echo $id2, $pass->form_input_hidden('id2'); ?></td>
</tr>
<tr>
 <th>Username</th>
 <td><?php echo $data[$id]['username'];?></td>
 <td><?php echo $data[$id2]['username'];?></td>
</tr>
<tr>
 <th>Reg. num.</th>
 <td><?php echo isset($data[$id]['regnum']) ? $data[$id]['regnum'] : 'NULL';?></td>
 <td><?php echo isset($data[$id2]['regnum']) ? $data[$id2]['regnum'] : 'NULL';?></td>
</tr>
<tr>
 <th>Role</th>
 <td><?php echo $data[$id]['role'];?></td>
 <td><?php echo $data[$id2]['role'];?></td>
</tr>
<tr>
 <th>Status</th>
 <td><?php echo $data[$id]['status'];?></td>
 <td><?php echo $data[$id2]['status'];?></td>
</tr>
</table>
<p>Combined user data:
<table>
 <tr>
  <th>Username:</th>
  <td><?php echo $pass->form_input_username('username');
     $pass->element_error_if_set('username'); ?></td>
 </tr>
 <tr>
  <th>Reg. num.:</th>
  <td><?php echo $pass->form_input_regnum('regnum');
     $pass->element_error_if_set('regnum'); ?></td>
 </tr>
 <tr>
  <th>Role:</th>
  <td><?php echo $pass->form_input_role('role');
     $pass->element_error_if_set('role'); ?></td>
 </tr>
 <tr>
  <th>Status:</th>
  <td><?php echo $pass->form_input_status('status');
     $pass->element_error_if_set('status'); ?></td>
 </tr>
</table>
<?php

   echo $pass->form_submit_button(['value'=>'mergeconfirmed'], 'Confirm Merge');
   echo '<span class="spacer"> </span>';
   echo $pass->form_cancel_button();

   echo '</form>';
}

function merge_user()
{
   global $pass;

   $info = $pass->mergeUsers();

   if ($info !== false)
   {
      echo '<div class="confirmation"><p>Merger successful.</div>';

      echo '<p>', implode("<br>", $info);

?>
<table>
 <tr>
  <th>PASS User ID:</th>
  <td><?php echo $pass->getParam('id');?></td>
 </tr>
 <tr>
  <th>Username:</th>
  <td><?php echo $pass->getParam('username');?></td>
 </tr>
 <tr>
  <th>Reg. num.:</th>
  <td><?php echo $pass->getParam('regnum'); ?></td>
 </tr>
 <tr>
  <th>Role:</th>
  <td><?php echo $pass->getParam('role'); ?></td>
 </tr>
 <tr>
  <th>Status:</th>
  <td><?php echo $pass->getParam('status'); ?></td>
 </tr>
</table>
<?php
   }
   else
   {
      echo '<div class="error"><p>Merger failed.</div>';

      if ($pass->has_errors())
      {
         $pass->print_errors();
      }
   }

   echo '<p>', $pass->href_self('', 'Return to List'),
    '<span class="spacer"> </span>',
     $pass->href_self('action=edit&amp;id='.$pass->getParam('id'),
        'Edit<span class="symbol">&#x1F4DD;</span>'); 
}

function invalidate_regnum()
{
   global $pass;

   if ($pass->invalidateRegNum($pass->getParam('id'),
         $pass->getParam('username'), $pass->getParam('regnum')))
   {
?>
<div class="confirmation">Registration number cleared and user notified.</div>
<?php
   }
   else
   {
?>
<div class="error">Registration number invalidation failed.</div>
<?php

      if ($pass->has_errors())
      {
         $pass->print_errors();
      }
   }

   list_users();
}

function edit_form()
{
   global $pass;

   $pass->start_form();
   echo $pass->form_input_hidden('id');
   echo $pass->form_input_hidden('originalusername');
   echo $pass->form_input_hidden('originalregnum');
   echo $pass->form_input_hidden('originalrole');
   echo $pass->form_input_hidden('originalstatus');
?>
<table class="account">
<tr><th>Pass User ID:</th><td><?php echo $pass->getParam('id'); ?></td>
<td>Merge with User ID: 
<?php

   echo $pass->form_input_number('id2');
   $pass->element_error_if_set('id2');
   echo $pass->form_submit_button(['value'=>'merge'], 'Merge');

?></td>
</tr>
<tr>
<th><label for="username">Blackboard ID:</label>
</th>
<td class="username">
  <?php echo $pass->form_input_username('username', ['id'=>'username']);?>
</td>
</tr>
<tr>
<th><label for="regnum">Registration Number:</label></th>
<td class="regnum">
  <?php echo $pass->form_input_regnum('regnum', ['id'=>'regnum']);?>
</td>
<td>
<?php

   if ($pass->isParamSet('regnum'))
   {
      echo 'Invalid? ';
      echo $pass->form_submit_button(['value'=>'invalidateregnum'],
        "Clear and notify user");
   }

?>
</td>
</tr>
<tr>
<th><label for="role">Role:</label></th>
<td class="role">
  <?php 
   if ($pass->isParam('id', $pass->getUserID()))
   {
      if ($pass->isParamSet('role'))
      {
         echo htmlentities($pass->getParam('role'));
      }
      else
      {
         echo htmlentities($pass->getParam('originalrole'));
      }
   } 
   else
   {
      echo $pass->form_input_role('role'); 
   }
?>
</td>
</tr>
<tr>
<th><label for="status">Status:</label></th>
<td class="status">
  <?php 
   if ($pass->isParam('id', $pass->getUserID()))
   {
      if ($pass->isParamSet('status'))
      {
         echo htmlentities($pass->getParam('status'));
      }
      else
      {
         echo htmlentities($pass->getParam('originalstatus'));
      }
   } 
   else
   {
      echo $pass->form_input_status('status'); 
   }
?>
</td>
</tr>
</table>
<?php echo $pass->form_submit_button(['value'=>'update'], 'Update'); 
   echo '<span class="spacer"> </span>';
   echo $pass->form_submit_button(['value'=>'cancel'], 'Cancel');
?>
</form>
<?php
}

function list_users()
{
   global $pass;

   if ($pass->isParam('action', 'cancel'))
   {
      echo '<div class="confirmation">Cancelled</div>';
   }

   echo $pass->start_form();
?>
<p>
<label for="searchusername">Blackboard ID:</label>
<span class="nobr">
<label>
<?php 
   echo $pass->form_input_boolean_checkbox('searchnotusername', false, 
	   ['id'=>'searchnotusername', 'onchange'=>'toggleNotUserNameCheckBox()']);
   ?>
   <span id="notusername">Not</span>
</label>
<?php
   echo $pass->form_input_textfield('searchusername', 
   ['id'=>'searchusername', 'placeholder'=>'regular expression']); 
   echo '</span> <br>';

   echo $pass->form_input_select('searchboolop', $pass->getParam('boolop'),
     'OR', '', true), ' <br>';
?> 
<label for="searchregnum">Registration Number:</label>
<span class="nobr">
<label>
<?php 
   echo $pass->form_input_boolean_checkbox('searchnotregnum', false,
     ['id'=>'searchnotregnum', 'onchange'=>'toggleNotRegNumCheckBox()']);
   ?>
   <span id="notregnum">Not</span>
</label>
<?php
   echo $pass->form_input_textfield('searchregnum', 
    ['id'=>'searchregnum', 'placeholder'=>'regular expression']);
   echo '</span> ';
?>
<p>(Use <code>^$</code> to search for registration numbers that haven't been set.)
<p>
<?php

   echo ' <label>UID: ', $pass->form_input_number('id', ['min'=>0,'size'=>4]), '</label>';
   echo $pass->form_submit_button(null, 'Search &#x1F50D;');
?>
</form>
<?php

   // Pagination simplistic here. All data is fetched, even if it's not on the current page. However, we don't have millions of users so it's not too much of a problem at the moment.
   
   $filter = array();
   $query = 'action=list';

   if ($pass->isParamSet('searchusername'))
   {
      $query .= '&amp;searchusername=' . urlencode($pass->getParam('searchusername'));

      if ($pass->isBoolParamOn('searchnotusername'))
      {
         $filter['usernamenotregex'] = $pass->getParam('searchusername');
         $query .= '&amp;searchnotusername=on';
      }
      else
      {
         $filter['usernameregex'] = $pass->getParam('searchusername');
      }
   }

   if ($pass->isParamSet('searchregnum'))
   {
      $query .= '&amp;searchregnum=' .  urlencode($pass->getParam('searchregnum'));

      if ($pass->isBoolParamOn('searchnotregnum'))
      {
         $filter['regnumnotregex'] = $pass->getParam('searchregnum');
         $query .= '&amp;searchnotregnum=on';
      }
      else
      {
         $filter['regnumregex'] = $pass->getParam('searchregnum');
      }
   }

   if ($pass->isParamSet('id'))
   {
      $filter['user_id'] = $pass->getParam('id');
      $query .= '&amp;id=' . $pass->getParam('id');
   }

   if (!empty($filter))
   {
      $query .= '&amp;searchboolop=' . $pass->getParam('searchboolop');
      echo '<p>', $pass->href_self('', '<span class="symbol">&#x23F4;</span> Back');
   }

   $users = $pass->getUserInfoByFilter($filter, $pass->getParam('searchboolop'), false);

   $total = count($users);

   $pagination = $pass->getListPages($total);
   $pass->setParam('page', $pagination['page']);
   $pageList = $pass->page_list($pagination['num_pages'], $query);

   echo sprintf('<p>%d %s found.', $total, ($total === 1 ? 'user' : 'users'));

   echo $pageList;

?>
<table class="userlist">
<tr>
<th>ID</th>
<th>User</th>
<th>Reg Num</th>
<th>Role</th>
<th>Status</th>
<th>Created</th>
<th class="action">Action</th>
</tr>
<?php
   for ($i = $pagination['start_idx']; $i <= $pagination['end_idx']; $i++)
   {
      if (!isset($users[$i]))
      {
         continue;
      }

      $user = $users[$i];
?>
<tr>
<td><?php echo $user['id']; ?></td>
<td class="username"><?php echo htmlentities($user['username']); 
   if ($pass->getUserID() === $user['id'])
   {
      echo ' (me)';
   }
?></td>
<td class="regnum"><?php echo empty($user['regnum']) ? '<span class="notset">Not set</span>' : htmlentities($user['regnum']); ?></td>
<td class="role"><?php echo htmlentities($user['role']); ?></td>
<td class="status"><?php echo htmlentities($user['status']); ?></td>
<td class="date"><?php echo htmlentities($user['account_creation']);?></td>
<td class="action"><?php 
   echo $pass->href_self('action=edit&amp;id='.$user['id'], 
   '<span class="nobr">Edit<span class="symbol">&#x1F4DD;</span></span>'); 
?>
<span class="spacer"> </span>
<?php 

   echo $pass->get_upload_lists_link(
    'View <span class="nobr">Uploads<span class="symbol">&#x1F50D;</span></span>',
    null, 'username='.urlencode($user['username']));

   if ($pass->getUserID() !== $user['id'])
   {
?>
<span class="spacer"> </span>
<?php 
      echo $pass->href_self('action=delete&amp;id='.$user['id'].'&amp;username='.urlencode($user['username']), 
        '<span class="nobr">Delete<span class="symbol">&#x1F5D1;</span></span>'); 
   }
?>
</td>
</tr>
<?php
   }

?>
</table>
<?php
   echo $pageList;

?>
<script>
var notusernameLabel = document.getElementById("notusername");
var notregnumLabel = document.getElementById("notregnum");

var notusernameCheckBox = document.getElementById('searchnotusername');
var notregnumCheckBox = document.getElementById('searchnotregnum');

window.addEventListener('load', function()
 {
    if (!notusernameCheckBox.checked)
    {
       notusernameLabel.classList.add('faded');
    }

    if (!notregnumCheckBox.checked)
    {
       notregnumLabel.classList.add('faded');
    }

 });

 function toggleNotUserNameCheckBox()
 {
    if (notusernameCheckBox.checked)
    {
       notusernameLabel.classList.remove('faded');
    }
    else
    {
       notusernameLabel.classList.add('faded');
    }
 }

 function toggleNotRegNumCheckBox()
 {
    if (notregnumCheckBox.checked)
    {
       notregnumLabel.classList.remove('faded');
    }
    else
    {
       notregnumLabel.classList.add('faded');
    }
 }
</script>
<?php
}

?>
