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

 * Upload directories page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Upload Directories');
$pass->require_admin();// ensure user is logged in as admin

$pass->page_header();

$pass->debug_message('Debugging mode ON.');

process_params();

if ($pass->has_errors())
{
   $pass->print_errors();
}

if ($pass->isParam('action', 'list'))
{
   list_uploads();
}
elseif ($pass->isParam('action', 'delete'))
{
   if (!$pass->isBoolParamOn('confirm'))
   {
      confirmDelete();
   }
   elseif ($pass->isParam('confirm', false))
   {
      echo '<div class="confirmation">Action cancelled. </div><p>',
     $pass->href_self('action=list', 'Return to list'), '</p>';
   }
   else
   {
      delete_selected();

      echo '<p>', $pass->href_self('action=list', 'Return to list'), '</p>';
   }
}
elseif ($pass->isParam('action', 'requeue'))
{
   if (!$pass->isBoolParamOn('confirm'))
   {
      confirmRequeue();
   }
   elseif ($pass->isParam('confirm', false))
   {
      echo '<div class="confirmation">Action cancelled. </div><p>',
        $pass->href_self('action=list', 'Return to list'), '</p>';
   }
   else
   {
      requeue_selected();

      echo '<p>', $pass->href_self('action=list', 'Return to list'), '</p>';
   }
}
else
{
   echo '<p>Invalid form action.</p>';
}

$pass->page_footer();

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(delete|requeue|list)$/', 'list');

   if ($pass->isParam('action', 'delete') || $pass->isParam('action', 'requeue'))
   {
      $pass->get_matched_variable('id', '/^\d{4}-\d{2}-\d{2}T\d{9}[+\-]\d{4}_[a-z0-9]{10}\|\d*\|[a-z]*$/');

      if (!$pass->isParamSet('id'))
      {
         $pass->add_error_message('No items selected.');
      }

      $pass->get_boolean_variable('delete_from_db', false);

      if (isset($_POST['confirm']))
      {
         $pass->get_boolean_variable('confirm');
      }
   }

   $pass->get_int_variable('page', 1);

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'list');
      return;
   }
}

function list_uploads()
{
   global $pass;

   $list = scandir($pass->getUploadPath(), SCANDIR_SORT_DESCENDING);

   if ($list === false)
   {
      echo '<p>Unable to scan upload path</p>';
      return;
   }

   // Discount 'php_errors.log', '.' and '..'
   // If there are any other items in the upload path the pagination may be off
   $num_items = count($list)-3;

   if ($num_items === 0)
   {
?>
<p>No upload directories found. Use the
<?php echo $pass->get_upload_lists_link('view uploads link'); ?> 
to find out if there is any upload data in the database.</p>
<?php
   }

   $pagination = $pass->getListPages($num_items);
   $pass->setParam('page', $pagination['page']);

   $uploads = array();

   $timestamp_list = array();
   $token_list = array();

   for ($i = $pagination['start_idx']; $i <= $pagination['end_idx']; $i++)
   {
      $value = $list[$i];

      $dir = $pass->getUploadPath('uploadpath').'/'.$value;

      if (is_dir($dir) 
          && preg_match('/^([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{9}[+\-][0-9]{4})_(.+)$/',
		   $value, $matches))
      {
	 $submission_time = $matches[1];
	 $submission_token = $matches[2];

	 array_push($timestamp_list, "'" . $pass->db_escape_sql($submission_time) . "'");
	 array_push($token_list, "'" . $pass->db_escape_sql($submission_token) . "'");

	 $project = array('timestamp'=>$submission_time, 'token'=>$submission_token,
	      'dir'=>$dir, 'base'=>$value);

         $settings_file = "$dir/pass-settings-" . $project['token'] . ".txt";

	 if (!file_exists($settings_file))
	 {
	    $settings_file = "$dir/pass-settings.txt";
	 }

	 $fh = fopen($settings_file, "r");

	 if ($fh === false)
	 {
            $project['error'] = 'Unable to open settings file.';
	 }
	 else
	 {
             $students = array();
             $files = array();

	     while (!feof($fh))
	     {
                $line = fgets($fh);

		if (preg_match('/^([a-zA-Z0-9\-]+): *(.*)$/', $line, $matches))
		{
		   if ($matches[1] == 'Student')
		   {
		      $arr = explode("\t", $matches[2]);
		      array_push($students, $arr[0]);
		   }
		   elseif ($matches[1] == 'User-id')
		   {
		      array_push($students, $matches[2]);
		   }
		   elseif ($matches[1] == 'File')
		   {
		      array_push($files, $matches[2]);
		   }
		   elseif ($matches[1] == 'Submission-timestamp')
		   {
                      if ($matches[2] != $project['timestamp'])
                      {
                         if (isset($project['error']))
			 {
                            $project['error'] .= '<br>';
			 }
			 else
			 {
                            $project['error'] = '';
			 }

			 $project['error'] .= 'Submission-timestamp ('
				 . htmlentities($matches[2]) 
				 . ') doesn\'t match directory timestamp ('
				 . htmlentities($project['timestamp']) . ').';
                      }
		      else
		      {
			 $subdate = 
                            date_create_from_format('Y-m-d\THisvO', $matches[2]);

			 if ($subdate === false)
                         {
                            if (isset($project['error']))
			    {
                               $project['error'] .= '<br>';
			    }
			    else
			    {
                               $project['error'] = '';
			    }

			    $project['error'] .= 'Invalid date-time format \''
				    . htmlentities($matches[2]) . '\'';
			 }
			 else
                         {
                            $project['Submission-timestamp'] = $subdate;
			 }
		      }
		   }
		   else
		   {
                      $project[$matches[1]] = $matches[2];
		   }
		}
             }

	     fclose($fh);

	     $project['students'] = $students;
	     $project['files'] = $files;
         }

	 array_push($uploads, $project);
      }
   }

   $submission_data = array();

   if (!empty($timestamp_list))
   {
      $result = $pass->db_query("SELECT id, upload_time, token, status, exit_code FROM submissions WHERE upload_time IN (" . implode(',', $timestamp_list) . ") AND token IN (" . implode(',', $token_list) . ')');

      if ($result)
      {
         while ($row = mysqli_fetch_assoc($result))
	 {
            $submission_data[$row['upload_time'].'_'.$row['token']]
		    = array('id'=>$row['id'], 'status'=>$row['status'], 'exit_code'=>$row['exit_code']);
	 }
      }
      else
      {
         echo '<p>Failed to query submissions table.';
      }
   }

   $pageList = $pass->page_list($pagination['num_pages'], 'action=list');

   if ($num_items > 0)
   {
?>
<p>This is a list of all upload directories. Deleting an upload directory doesn't delete the corresponding result directory. Upload directories should only be deleted after PASS has completed the process and the original uploaded content is no longer required (or has been archived). You will be prompted for confirmation before deletion.</p>

<p>Result directories can't be deleted from this webscript as their permissions don't allow write/delete for the webserver user.

<p><strong>Important:</strong> only requeue uploads if some accident, tampering or outage has deleted the message queue. You can't requeue uploads that have already been processed.
<?php 
      echo "<p>$num_items ", ($num_items==1 ? 'upload' : 'uploads'), ' found.</p>';
      echo $pageList; 
      echo $pass->start_form();
?>
<table><tr><td><input title="Select all" type="checkbox" id="toggle_selected" onclick="toggleSelected()" /></td>
<th>Submission Time</th>
<th>Token</th>
<th>Course</th>
<th>Assignment</th>
<th>Student(s)</th>
<th>Encoding</th>
<th>Submission Data</th>
</tr>
<?php

      foreach ($uploads as $project)
      {
         $dir = $project['timestamp'].'_'.$project['token'];

         echo '<tr>';

	 $id_param = "$dir|";

	 if (isset($submission_data[$dir]['id']))
	 {
            $id_param .= $submission_data[$dir]['id'];
	 }

	 $id_param .= '|';

	 if (isset($submission_data[$dir]['status']))
	 {
            $id_param .= $submission_data[$dir]['status'];
	 }

	 echo '<td>', $pass->form_input_checkbox('id[]', 
		 ['value'=>$id_param, 'class'=>'toggle']), '</td>';

         echo '<td>';

         if (isset($project['Submission-timestamp']))
	 {
            echo date_format($project['Submission-timestamp'], 'jS M Y H:i:s');
	 }
	 else
	 {
	    echo 'Not found';
	 }

	 echo '</td>';
	 echo '<td>', $project['token'], '</td>';
	 echo '<td>';
	 echo isset($project['Course']) ? htmlentities($project['Course']) : 'Not found';
	 echo '</td>';
	 echo '<td>';
	 echo isset($project['Assignment']) ? htmlentities($project['Assignment']) : 'Not found';
	 echo '</td>';

	 echo '<td>';
	 echo isset($project['students']) ? htmlentities(implode(', ', $project['students'])) : 'Not found';
	 echo '</td>';

	 echo '<td>', isset($project['Project-encoding']) ? htmlentities($project['Project-encoding']) : 'Not found', '</td>';

         echo '<td>';

	 if (isset($submission_data[$dir]['status']))
	 {
            echo htmlentities($submission_data[$dir]['status']);

	    if (isset($submission_data[$dir]['exit_code']))
	    {
               echo " (", $submission_data[$dir]['exit_code'], ')';
	    }
	 }
	 else
	 {
            echo 'Not found';
	 }

         echo '</td>';

	 if (isset($project['error']))
	 {
            echo '<td>', $project['error'], '<td>';
	 }

?>
</tr>
<tr>
<td></td>
<td colspan="7"><button type="button" class="collapsible">Files</button>
<div class="content">
<?php

	 if (isset($project['files']))
	 {
	    $sep = '';

	    foreach ($project['files'] as $file)
	    {
               $fullfilepath = $project['dir']."/$file";

               echo $sep, '<span class="file">', htmlentities($file), '</span> ', 
                  human_filesize(filesize($fullfilepath));

	       if (!file_exists($fullfilepath))
	       {
                  echo ' File doesn\'t exist!';
	       }

	       $sep = '<br>';
            }
         }
	 else
	 {
            echo 'No files found!';
	 }

	 echo '</div></td></tr>';
      }

      echo '</table>';
      echo $pageList;
      echo '<p>', $pass->form_submit_button(['value'=>'delete'], 'Delete Selected');
      echo '<span class="spacer"> </span>';
      echo $pass->form_submit_button(['value'=>'requeue'], 'Requeue Selected');
      echo '</form>';
   }
?>
<script>
<?php
   $pass->writeCollapsibleJavaScript(false);
?>

var toggle = document.getElementById("toggle_selected");

function toggleSelected()
{
   if (toggle.checked)
   {
      toggle.title = 'Deselect all';
   }
   else
   {
      toggle.title = 'Select all';
   }

   var elems = document.getElementsByClassName("toggle");

   if (elems)
   {
      for (i = 0; i < elems.length; i++)
      {
         elems[i].checked = toggle.checked;
      }
   }
}
</script>
<?php
}

function confirmDelete()
{
   global $pass;

   $num_items = count($pass->getParam('id'));

   if ($num_items == 0)
   {
      echo '<p>No items selected.';
      return;
   }

   echo $pass->start_form();
   echo '<p>', $pass->form_input_boolean_checkbox('delete_from_db', ['id'=>'delete_from_db']);
?>
<label for="delete_from_db">Also delete associated submission data from database.</label>
<p>Are you sure you want to delete <?php echo $num_items, ' ', ($num_items===1 ? 'directory' : 'directories'), '?';?>
<?php echo $pass->form_input_hidden('id[]'); ?>
<input type="hidden" name="action" value="delete">
<p>
<button type="submit" name="confirm" value="true" >Yes</button>
<button type="submit" name="confirm" value="false" >No</button>
</form>
<?php
}

function delete_selected()
{
   global $pass;

   $submission_ids = array();

   $num_deleted = 0;

   foreach ($pass->getParam('id') as $id_param)
   {
      if (preg_match('/^(\d{4}-\d{2}-\d{2}T\d{9}[+\-]\d{4}_[a-z0-9]{10})\|(\d*)\|([a-z]*)$/', $id_param, $matches))
      {
         $dir = $matches[1];

	 if (!empty($matches[2]))
	 {
	    array_push($submission_ids, $matches[2]);
	 }

         if ($pass->delUploadDir($dir))
         {
            echo '<div>Directory tree ', htmlentities($dir),  ' has been deleted.</div>';
            $num_deleted++;
         }
	 else
         {
            echo '<div>Failed to delete directory tree ', htmlentities($dir), '</div>';
         }
      }
   }

   echo '<p>', $num_deleted, ' directory ', ($num_deleted === 1? 'tree' : 'trees'), ' deleted.';

   if ($pass->isBoolParamOn('delete_from_db') && !empty($submission_ids))
   {
      $id_list = implode(',', $submission_ids);

      if ($pass->db_multi_query("DELETE FROM submissions WHERE id IN ($id_list);DELETE FROM projectgroup WHERE submission_id IN ($id_list);"))
      {
         echo '<p>Deleted submission data from database.';
      }
      else
      {
         echo '<p>Failed to delete submission data from database.';
      }
   }

   if ($pass->has_errors())
   {
      $pass->print_errors();
   }
}

function confirmRequeue()
{
   global $pass;

   $num_items = count($pass->getParam('id'));

   if ($num_items == 0)
   {
      echo '<p>No items selected.';
      return;
   }

   echo $pass->start_form();
?>
<p>Are you sure you want to requeue <?php echo $num_items, ' ', ($num_items===1 ? 'upload' : 'uploads'), '?';?>
<?php echo $pass->form_input_hidden('id[]'); ?>
<input type="hidden" name="action" value="requeue">
<p>
<button type="submit" name="confirm" value="true" >Yes</button>
<button type="submit" name="confirm" value="false" >No</button>
</form>
<?php
}

function requeue_selected()
{
   global $pass;

   // Since id starts with the timestamp this should put them in chronological order
   $id_list = $pass->getParam('id');
   sort($id_list);

   $status_updates = array();

   foreach ($id_list as $id_param)
   {
      if (preg_match('/^(\d{4}-\d{2}-\d{2}T\d{9}[+\-]\d{4})_([a-z0-9]{10})\|(\d*)\|(uploaded|queued|processing|processed)?$/', $id_param, $matches))
      {
         $timestamp = $matches[1];
         $token = $matches[2];
         $id = (int)$matches[3];
	 $status = $matches[4];
	 $dir = "${timestamp}_$token";
	 $fullpath = $pass->getUploadPath() . "/$dir";

	 if (!file_exists($fullpath))
         {
            echo "<p>Directory '$fullpath' doesn't exist. Can't requeue.";
	    continue;
         }

	 $result_path = $pass->getCompletedPath()."/$dir";

	 if (file_exists($result_path))
	 {
            echo "<p>Result directory '$result_path' already exists. Can't requeue.";
            continue;
	 }

	 if ($status === 'processing')
	 {
            echo "<p>Upload $id is currently being processed (timestamp=$timestamp, token=$token). Can't requeue.";
	    continue;
	 }

         $settings_file = "$fullpath/pass-settings-$token.txt";

	 $data = array('submission_id'=>$id, 'time'=>$timestamp, 'token'=>$token);

	 $fh = fopen($settings_file, "r");

	 if ($fh === false)
	 {
            echo "<p>Unable to open settings file '$settings_file'. Can't requeue";
	 }

	 $usernames = array();

         while (!feof($fh))
         {
            $line = fgets($fh);

	    if (preg_match('/^([a-zA-Z0-9\-]+): *(.*)$/', $line, $matches))
	    {
               if ($matches[1] == 'Student')
               {
                  $arr = explode("\t", $matches[2]);

                  if (!isset($data['user']))
		  {
                     $data['user'] = $arr[0];
		  }

		  array_push($usernames, $arr[0]);
               }
               elseif ($matches[1] == 'User-id')
               {
                  $usernames = explode(',', $matches[2]);
                  $data['user'] = $usernames[0];
               }
               elseif ($matches[1] == 'Assignment')
               {
                  $data['assignment'] = $matches[2];
               }
               elseif ($matches[1] == 'Course')
               {
                  $data['course'] = $matches[2];
               }
	    }
	 }

	 fclose($fh);

	 if (!isset($data['user']))
	 {
            $pass->add_error_message("Couldn't find uploader information for timestamp='$timestamp', token='$token'");
	 }

	 if (!isset($data['assignment']))
	 {
            $pass->add_error_message("Couldn't find assignment information for timestamp='$timestamp', token='$token'");
	 }

	 if ($pass->has_errors())
	 {
            $pass->print_errors();
            $pass->clear_errors();
	 }
	 elseif (isset($id))
	 {
            echo "<p>Requeuing submission ID $id.";

            $pass->requeueUpload($data);

	    if ($status !== 'queued')
	    {
               $status_updates[$id] = 'queued';
	    }
	 }
	 else // no submission id -> not in database
	 {
            echo "<p>No submission ID for timestamp='$timestamp', token='$token'. Publishing.";

            // Get user IDs

            $userinfo = $pass->getUserInfo($usernames, true);

	    $pass->publishUpload($userinfo,
               $data['course'], $data['assignment'], $data['time'], $data['token'],
	       $userinfo[$usernames[0]]['id']);
	 }
      }
      else
      {
         echo '<p>No match for ', htmlentities($id_param);
      }
   }

   if (!empty($status_updates))
   {
      $stmt = $pass->db_prepare("UPDATE submissions SET status=? WHERE id=?");

      if ($stmt === false)
      {
         echo '<p>Failed to prepare update statement.';
	 $error_log("Prepared statement failed: " . $pass->passdb->error);
	 return;
      }

      $stmt->bind_param("si", $submission_status, $submission_id);

      foreach ($status_updates as $submission_id => $submission_status)
      {
         if ($stmt->execute() === false)
	 {
            echo "<p>Failed to execute update statement for submission ID $submission_id ($submission_status)";
	 }
      }

      $stmt->close();
   }
}
?>
