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

 * View upload lists page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Uploads');

$pass->check_site_mode();
$pass->require_login();// ensure user is logged in

process_params();

if ($pass->isParam('action', 'export') && !$pass->has_errors())
{
   if (!export())
   {
      $pass->page_header();

      if ($pass->has_errors())
      {
        $pass->print_errors();
      }

      show_list();

      $pass->page_footer();
   }
}
else
{
   if ($pass->isBoolParamOn('has_pending'))
   {
      header(sprintf("Refresh: %s; URL=%s", $pass->getConfigValue('upload_refresh'), $_SERVER['REQUEST_URI']));
   }

   $pass->page_header();

   if ($pass->has_errors())
   {
     $pass->print_errors();
   }

   show_list();

   $pass->page_footer();
}

function process_params()
{
   global $pass;

   $filters = array();

   if ($pass->isUserStaffOrAdmin())
   {
      $pass->get_username_variable('username');

      if ($pass->isParamSet('username'))
      {
         $filters['username'] = $pass->getParam('username');
      }
      else
      {
         $filters['user_id'] = true;// any user
      }

      $pass->get_matched_variable('action', '/^(search|export|list)$/', 'search');
   }
   else
   {
      $pass->get_matched_variable('action', '/^(search|list)$/', 'search');
   }

   $pass->setParam('timeselectorvalues', ['before'=>'Before', 'after'=>'After']);
   $pass->get_choice_variable('timeselector', 
	   array_keys($pass->getParam('timeselectorvalues')), 'after');
   $pass->get_date_variable('date');

   if ($pass->isParamSet('date'))
   {
      if ($pass->isParam('timeselector', 'before'))
      {
         date_time_set($pass->getParam('date'), 0, 0);
      }
      else
      {
         date_time_set($pass->getParam('date'), 23, 59, 59, 999);
      }

      $filters[$pass->getParam('timeselector') . 'date' ] =
            $pass->getParam('date')->format('Y-m-d\THisrv');
   }

   $courses = $pass->load_courses();
   $pass->get_choice_variable('course', array_keys($courses), '---');

   if (!$pass->isParam('course', '---'))
   {
      $filters['course'] = $pass->getParam('course');

      $assignments = $pass->fetch_assignments($pass->getParam('course'));

      if ($assignments)
      {
         $pass->get_choice_variable('assignment', array_keys($assignments), '---');

         if (!$pass->isParam('assignment', '---'))
         {
            $filters['assignment'] = $pass->getParam('assignment');
         }
      }
   }

   $pass->get_int_variable('submission_id');

   if ($pass->isParamSet('submission_id'))
   {
      $filters['submission_ids'] = $pass->getParam('submission_id');
   }

   $pass->setParam('exitcodeoptions', ['any'=>'Any', 'zero'=>'Zero', 'nonzero'=>'Non-Zero']);
   $pass->get_choice_variable('exitcode',
	   array_keys($pass->getParam('exitcodeoptions')), 'any');

   if ($pass->isParam('exitcode', 'zero'))
   {
      $filters['exit_code'] = 0;
   }
   elseif ($pass->isParam('exitcode', 'nonzero'))
   {
      $filters['not_exit_code'] = 0;
   }

   // pagination simplistic as all data is fetched, but we're not dealing with huge lists.
   $pass->get_int_variable('page', 1);

   $data = $pass->getUploads($filters, false);
   $pass->setParam('data', $data);

   if (!empty($data) && $pass->isParam('page', 1))
   {// Submissions listed in reverse order so first item is the last submission.
    // Queuing is FIFO so if the first element in the array has been processed,
    // then all have been processed.
      $value = reset($data);
      $pass->setParam('has_pending', $value['status'] !== 'processed' ? true : false);

      $pass->setParam('queued', $pass->getQueuedSubmissions());
   }
}

function show_list()
{
   global $pass;

?>
<div>Remember that 
<span class="important">you must upload your final PDF to <?php echo PassConfig::SUBMISSION_SITE; ?></span>
before the assignment deadline to ensure that your work is marked.
<?php

   echo $pass->get_faq_link(
     $pass->getShortTitle() . ' does not submit your work for you.', 'whynosubmit');

?>
</div>
<p>
<?php

   $data = $pass->getParam('data');

   $courses = $pass->fetch_courses();

   echo $pass->start_form();

   echo '<div class="search">';

   $staff = $pass->isUserStaffOrAdmin();

   if ($staff)
   {
?>
<label for="username"><?php echo PassConfig::USERNAME_LABEL; ?>:</label>
<?php
      echo $pass->form_input_username('username', ['id'=>'username']), '<br>';

   }

?>
 <label for="course">Course:</label>
 <select id="course" name="course">
 <option value="---">Any</option>
<?php
   foreach ($courses as $resource)
   {
      echo '<option value="', htmlentities($resource['name']), '"';

      if ($pass->isParam('course', $resource['name']))
      {
         echo ' selected';
      }

      echo '>', htmlentities($resource['name']. ': ' . $resource['title']), "</option>";
   }

?>
 </select>
 <br>

 <label for="assignment">Assignment:</label>
 <select id="assignment" name="assignment">
 <option value="---">Any</option>
<?php

   if (!$pass->isParam('course', '---'))
   {
      $assignments = $pass->fetch_assignments($pass->getParam('course'));

      foreach ($assignments as $label=>$assignment)
      {
         echo '<option value="', htmlentities($label), '"';

	 if ($pass->isParam('assignment', $label))
	 {
            echo ' selected';
	 }

	 echo '>', htmlentities($assignment['title']),  '</option>';
      }
   }

?>
   </select>
 <br>
 <label for="submission_id">Submission ID:</label>
<?php
   echo $pass->form_input_number('submission_id', ['id'=>'course', 'min'=>1, 'size'=>5]), ' ';
?>
<br>
<label for="exitcode">Exit code: <label>
<?php
   echo $pass->form_input_select('exitcode', $pass->getParam('exitcodeoptions'),
     'any', '', true, ['id'=>'exitcode']);

?>
<br>
<label for="timeselector">Uploaded: </label>
<?php
   echo $pass->form_input_select('timeselector', 
    $pass->getParam('timeselectorvalues'), '', '', true);
   echo $pass->form_input_date('date');

   echo '<br>', $pass->form_submit_button(['value'=>'search'], 'Search &#x1F50D;');

   if ($staff)
   {
      echo '<span class="spacer"> </span>',
         $pass->form_submit_button(['value'=>'export'], 'Export &#x1F4E5;');
   }
?>
</div>
</form>

<script>
const courses = {};
<?php
   foreach ($courses as $resource)
   {
      $htmlcourselabel = htmlentities($resource['name']);
?>
courses["<?php echo $htmlcourselabel; ?>"] = {};
<?php

      $assignments = $pass->fetch_assignments($resource['name']);

      foreach ($assignments as $assignment)
      {
?>
courses["<?php echo $htmlcourselabel; ?>"]["<?php echo htmlentities($assignment['name']); ?>"] = "<?php echo htmlentities($assignment['title']); ?>";
<?php
      }
   }
?>

const courseElem = document.getElementById("course");

courseElem.addEventListener("change", (event) =>
 {
    var assignmentElem = document.getElementById("assignment");

    assignmentElem.options.length=1;

    if (courseElem.value === '---')
    {
       return;
    }

    var course = courses[courseElem.value];

    for (var key in course)
    {
       var option = document.createElement('option');
       option.text = course[key];
       option.value = key;
       assignmentElem.appendChild(option);
    }
 });

</script>
<?php

   if ($data === false)
   {
      echo '<p>No results found.</p>';
      return;
   }

   $uploadpath = $pass->getUploadPath();

   $numrows = count($data);

   echo '<p>', $numrows, ' ', $numrows == 1 ? "upload" : "uploads", " found.";

   if ($numrows === 0)
   {
      return;
   }

   $pagination = $pass->getListPages($numrows);
   $pass->setParam('page', $pagination['page']);

   if ($staff)
   {
?>
<p>The status will be one of 'Uploaded' (files have been uploaded),
'Queued (<em>N</em>/<em>M</em>)' (waiting to be processed, <em>N</em>/<em>M</em>
indicates the position in the queue), 'Processing' (currently being processed)
or 'Processed (<em>n</em>)' where <em>n</em> is the exit code (processing
finished). A negative exit code means that something went wrong when setting up the docker
container. If the status is stuck on 'Uploaded' then it's likely that the
connection to the message queue has failed. If the status is stuck on 'Queued' with no
change in <em>N</em>/<em>M</em> then the backend process that listens to the queue may
have been terminated. <?php
   if ($pass->isUserRole('admin'))
   {
	   ?> (Use the admin area to view the <?php echo $pass->get_admin_viewlogs_link('backend logs'); ?> or <?php echo $pass->get_admin_upload_dirs_link('requeue processes'); ?>.) <?php
   }
   else
   {
?> (Admin users can view the backend logs and requeue processes.) <?php
   }
?>

<div>
<button type="button" class="collapsible">Exit Codes</button>
<div class="content">
 The exit codes returned
by PASS CLI are as follows:
<table>
<tr><td>0</td><td>Success.</td></tr>
<tr><td>1</td><td>Syntax error (such as invalid command line arguments).</td></tr>
<tr><td>2</td><td>No course data.</td></tr>
<tr><td>3</td><td>I/O error.</td></tr>
<tr><td>4</td><td>XML parser error.</td></tr>
<tr><td>5</td><td>Unsupported setting.</td></tr>
<tr><td>6</td><td>Invalid file.</td></tr>
<tr><td>100</td><td>Other.</td></tr>
</table>
<p>Most of these exit codes are unlikely to occur with a production-ready
system as the command line arguments are automated. I/O and XML errors may
occur if there's a problem fetching a remote resource.

<p>The invalid file code indicates that a forbidden file was supplied, such as
an executable or a resource file that PASS should fetch or a result file that
the assignment application should generate. The upload script should deter
these files from being uploaded, but a user can force a file upload (for example,
by disabling JavaScript or using a file extension that isn't on the forbidden list).

<p>If something goes wrong whilst PASS is running the compiler or the
application then the associated error code is written to the PDF and PASS's
transcript. If something goes wrong during the LaTeX step then PASS's
transcript and the LaTeX transcript will include error messages. If the PDF is
created then the process is deemed successful, even if something went wrong
with the compilation or testing stages.
</div>
</div>
<?php
   }
   else
   {
?>
<p>The status will be one of 'uploaded' (files have been uploaded),
'queued (<em>N</em>/<em>M</em>)' (waiting to be processed, <em>N</em>/<em>M</em>
indicates the position in the queue), 'processing' (currently being processed)
or 'processed' (completed).

<p>If the status seems to be stuck on 'uploaded' then the queuing
system may have failed. If the status seems to be stuck on 'queued'
(with no change to <em>N</em>/<em>M</em>) then the backend may have
gone offline. In either case, please contact an administrator.
<?php
   }

   if ($pagination['num_pages'] > 1)
   {
      $query = 'course='.urlencode($pass->getParam('course'))
	   . '&amp;exitcode='.urlencode($pass->getParam('exitcode'))
	   . '&amp;timeselector='.urlencode($pass->getParam('timeselector'));

      if ($pass->isParamSet('assignment'))
      {
         $query .= '&amp;assignment='.urlencode($pass->getParam('assignment'));
      }

      if ($pass->isParamSet('username'))
      {
         $query .= '&amp;username=' . urlencode($pass->getParam('username'));
      }

      if ($pass->isParamSet('submission_id'))
      {
         $query .= '&amp;submission_id=' . urlencode($pass->getParam('submission_id'));
      }

      if ($pass->isParamSet('date'))
      {
         $query .= '&amp;date=' . date_format($pass->getParam('date'), 'Y-m-d');
      }

      $pageList = $pass->page_list($pagination['num_pages'], $query);
   }
   else
   {
      $pageList = '';
   }

   if ($pass->isParamSet('queued'))
   {
      $queue_size = count($pass->getParam('queued'));
   }
   else
   {
      $queue_size = 0;
   }

   echo $pageList;
?>
<table class="upload_list">
 <tr>
  <th class="submission_id">ID</th>
  <th class="upload_time">Uploaded</th>
  <th class="course">Course</th>
  <th class="assignment">Assignment</th>
  <th class="projectmembers">Project Members</th>
  <th class="status">Status</th>
  <th class="downloads">Downloads</th>
 </tr>
<?php
   for ($i = $pagination['start_idx']; $i <= $pagination['end_idx']; $i++)
   {
      if (!isset($data[$i]))
      {// pagination start or end is off
         continue;
      }

      $row = $data[$i];
      $submission_id = $row['submission_id'];

      $timestamp = date_create_from_format(Pass::TIME_STAMP_FORMAT, $row['upload_time']);
      $assignment = $pass->fetch_assignment($row['course'], $row['assignment']);

      if ($assignment === false)
      {
	 $assignmenttitle = $row['assignment'];
      }
      else
      {
         $assignmenttitle = $assignment['title'];
      }

      $dir = $row['upload_time'] . "_" . $row['token'];
      $upload_dir = "$uploadpath/$dir"; 
      $uploaded_files = scan_uploads($upload_dir);
      $completed_dir = $pass->getDockerPath() . "/completed/$dir";
?>
<tr>
 <td class="submission_id"><?php echo $submission_id; ?></td>
 <td class="upload_time"><?php echo htmlentities(date_format($timestamp, 'Y-m-d H:i:s')); ?></td>
 <td class="course"><?php echo htmlentities($row['course']); ?></td>
 <td class="assignment"><?php echo htmlentities($assignmenttitle); ?></td>
<?php 
   $group_project = (count($row['users']) > 1 ? true : false);
?>
 <td class="projectmembers<?php if (!$group_project) echo ' solo'; ?>"><?php 

   $sep = '';
   foreach ($row['users'] as $user_id => $user)
   {
      echo $sep, htmlentities($user['username']);

      if ($row['uploaded_by'] === $user_id)
      {
         if ($group_project)
         {
            echo ' (uploader)';
         }

	 $uploader = $user['username'];
      }

      $sep = '<br>';
   }

   if (!isset($uploader))
   {
      echo '<br>Uploader not project member!';
   }

 ?></td>
 <td class="status"><?php 
      echo htmlentities($row['status']);

      if ($row['status'] === 'queued' && $queue_size > 0
         && $pass->isParamElementSet('queued', $submission_id))
      {
         echo ' (', $pass->getParamElement('queued', $submission_id)['index'], "/$queue_size)";
      }
      elseif ($staff && $row['status'] == 'processed' && isset($row['exit_code']))
      {
         echo " (", $row['exit_code'], ")";
      }
   ?></td><td class="downloads">
<?php
      if ($row['status'] == 'processed')
      {
         $pdf = "$completed_dir/" . $uploader . '_' . $row['assignment'] . ".pdf";

         if (file_exists($pdf))
         {
            if ($pass->isUserRole('student'))
	    {
               echo $pass->get_download_link($submission_id, 'pdf'), ' ';
	    }
	    else
	    {
               echo $pass->get_download_link($submission_id, 'pdf', $row['uploaded_by']), ' ';
	    }
         }
         else
         {
            echo "no PDF ";
         }

         $log = "$completed_dir/pass.log";

         if (file_exists($log))
         {
            if ($pass->isUserRole('student'))
	    {
               echo $pass->get_download_link($submission_id, 'log'), ' ';
	    }
	    else
	    {
               echo $pass->get_download_link($submission_id, 'log', $row['uploaded_by']), ' ';
	    }
         }
         else
         {
            echo 'no log ';

	    if (!isset($row['exit_code']) || $row['exit_code'] < 0)
	    {
               echo '<br>[PASS consumer failed]';
	    }
	    elseif ($row['exit_code'] > 0)
	    {
               echo '<br>[PASS CLI failed with exit code ', $row['exit_code'], ']';
	    }
         }

      }
      else
      {
         echo 'pending';
      }
?>
</td>
</tr>
<tr><td colspan="8"><button type="button" class="collapsible">Files</button><div class="content">
<?php
      $sep = '';
      if (isset($assignment['mainfile']))
      {
         listfile($upload_dir, $assignment['mainfile'], $uploaded_files, 'main project file', $sep);
      }

      if (isset($assignment['files']))
      {
         foreach ($assignment['files'] as $file)
	 {
            listfile($upload_dir, $file, $uploaded_files, 'project file', $sep);
	 }
      }

      if (isset($assignment['reports']))
      {
         foreach ($assignment['reports'] as $file)
	 {
            listfile($upload_dir, $file, $uploaded_files, 'report', $sep,
               ['pdf', 'doc', 'docx']);
	 }
      }

      if (!empty($uploaded_files))
      {
         foreach ($uploaded_files as $file)
	 {
	    if (!($file === '.' || $file === '..' 
		 || $file === 'pass-settings-'  . $row['token']. '.txt'))
	    {
               listfile($upload_dir, $file, $uploaded_files, 'additional file', $sep);
	    }
	 }
      }

      echo '</div></td></tr>';

      if ($staff)
      {
?>
<tr><td colspan="8"><button type="button" class="collapsible">Pass Settings</button><div class="content">
<?php
         $settings_file = "$upload_dir/pass-settings-" . $row['token'] . ".txt";

	 if (file_exists($settings_file))
	 {
            echo '<pre class="filecontents">', htmlentities(file_get_contents($settings_file)), '</pre>';
	 }
	 else
	 {
            echo 'File Not Found.';
	 }
?></div></td></tr><?php
      }
   }
   echo '</table>';
   echo $pageList;

   echo $pass->writeCollapsibleJavaScript();
}

function scan_uploads($path)
{
   $uploaded_files = scandir($path);

   foreach ($uploaded_files as $file)
   {
      $fp = "$path/$file";

      if ($file != '..' && $file !== '.' && is_dir($fp))
      {
         recursive_scan($uploaded_files, $fp, $file);
      }
   }

   return $uploaded_files;
}

function recursive_scan(&$uploaded_files, $dir, $relpath)
{
   $files = scandir($dir);

   foreach ($files as $f)
   {
      if ($f === '.' || $f === '..') continue;

      $fp = "$dir/$f";

      if (is_dir($fp))
      {
         recursive_scan($uploaded_files, $fp, "$relpath/$f");
      }
      else
      {
         array_push($uploaded_files, "$relpath/$f");
      }
   }
}

function listfile($uploaddir, $file, &$uploaded_files, $info, &$sep, $extensions=null)
{
   $fullpath = "$uploaddir/$file";

   if (is_dir($fullpath))
   {
      $key = array_search($file, $uploaded_files);

      if ($key !== false)
      {
         unset($uploaded_files[$key]);
      }

      return;
   }

   if (file_exists($fullpath))
   {
      echo $sep, htmlentities($file), ' ';

      echo human_filesize(filesize($fullpath));

      $key = array_search($file, $uploaded_files);

      if ($key !== false)
      {
         unset($uploaded_files[$key]);
      }
   }
   elseif (isset($extensions))
   {
      $found = false;

      foreach ($extensions as $ext)
      {
         $fn = "$file.$ext";
         $fp = "$uploaddir/$fn";

         if (file_exists($fp))
         {
            echo $sep, htmlentities($fn), ' ';

            echo human_filesize(filesize($fp));

            $key = array_search($fn, $uploaded_files);

            if ($key !== false)
            {
               unset($uploaded_files[$key]);
            }

	    $found = true;
	    break;
         }
      }

      if (!$found)
      {
         echo $sep, htmlentities($file), ' not found';
      }
   }
   else
   {
      echo $sep, htmlentities($file), ' not found';
   }

   echo " ($info)";

   $sep = '<br>';
}

function export()
{
   global $pass;

   $data = $pass->getParam('data');

   if ($data === false)
   {
      $pass->add_error_message('No results found.');
      return false;
   }

   header('Content-Description: File Transfer');
   header('Content-Type: text/tab-separated-values');
   header('Content-Disposition: attachment; filename="uploads.tsv"');
   header('Expires: 0');

   $columns = array('submission_id'=>'Submission ID',
     'upload_time'=>'Upload Time',
     'course'=>'Course',
     'assignment'=>'Assignment',
     'exit_code'=>'Exit Code');

   echo implode("\t", $columns), "\tUploaded By\tProject Group\tPDF MD5", PHP_EOL;

   $completed_dir = $pass->getDockerPath() . "/completed";

   foreach ($data as $row)
   {
      foreach ($columns as $key=>$value)
      {
         if (isset($row[$key]))
	 {
            echo $row[$key];
	 }

	 echo "\t";
      }

      $users = $row['users'];

      $username = $users[$row['uploaded_by']]['username'];
      echo $username;

      $sep = "\t";

      foreach ($users as $user)
      {
         echo $sep;
	 echo $user['username'];
	 $sep = ',';
      }

      echo "\t";

      $file = "$completed_dir/".$row['upload_time'].'_'.$row['token']
	      . "/${username}_" . $row['assignment'] . '.pdf';

      if (file_exists($file))
      {
         echo md5_file($file);
      }

      echo PHP_EOL;
   }

   return true;
}

?>
