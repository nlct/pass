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

 * Upload form.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

const CHECKMARK="\u{2705}";
const CROSSMARK="\u{274C}";
const OPENFOLDER="\u{1F4C2}";

$pass = new Pass('Upload');
$pass->require_login();// ensure user is logged in

$role = $pass->getUserRole();
$regnum = $pass->getUserRegNum();

if ($role === 'student' && empty($regnum))
{
   /*
    * Students must set their registration number first.
    * Staff/admin will have a dummy value set, since they don't have 
    * a registration number.
    */
   $pass->redirect_header($pass->getSetRegNumRef(), true);
}
else
{
   process_params();

   if ($pass->isParam('action', 'runpass'))
   {
      if (run_pass())
      {
         if ($pass->isBoolParamOn('advanced_upload'))
	 {
            $pass->page_header();

	    echo '<!-- Upload success ', $pass->getParam('submission_id'), ' -->';

            echo $pass->start_form(['action'=>$pass->getUploadListsRef(), 'method'=>'get']);
?>
<p>Upload successful. Submission ID <?php echo $pass->getParam('submission_id'), '. ';
 echo $pass->form_input_hidden('submission_id');
 echo $pass->form_submit_button(['value'=>'list'], 'Continue  &#x23F5;');
?>
</form>
<?php
            $pass->page_footer();
	 }
	 else
	 {
            $_SESSION['confirmation_message'] = 'Upload successful.';
            $pass->redirect_header($pass->getUploadListsRef());
	 }
      }
   }
   else
   {
      $pass->page_header();
   
      $pass->debug_message('Debugging mode ON.');
   
      if ($pass->has_errors())
      {
         $pass->print_errors();

         // record any problems to help detect bugs and provide
         // assistance

         $pass->record_action('upload', sprintf('%s errors: %s',  
             $pass->getParam('action'),
             $pass->get_error_list()));
      }
   
      if ($pass->isParam('action', 'listcourses'))
      {
         list_courses();
      }
      elseif ($pass->isParam('action', 'groupmembers'))
      {
         group_members();
      }
      elseif ($pass->isParam('action', 'listassignments'))
      {
         list_assignments();
      }
      elseif ($pass->isParam('action', 'assignmentfiles'))
      {
         assignment_files();
      }
      else
      {
         echo '<p>Invalid form action.</p>';
         $pass->debug_message('Action: \''.htmlentities($params['action']).'\'');
      }
   
      $pass->page_footer();
   }
}

function process_params()
{
   global $pass;
   $pass->get_matched_variable('action', '/^[a-z]+$/', 'listcourses');

   if ($pass->has_errors())
   {
      $pass->setParam('action', 'listcourses');
      return;
   }

   $filetypes = Pass::FILE_TYPES;

   $pass->get_htmlentities_variable('course', '---');

   if (!$pass->isParam('action', 'listcourses') && $pass->isParam('course', '---'))
   {
      $pass->add_error_message('Please select the course', 'course', 'Required');
      $pass->setParam('action', 'listcourses');
   }

   $pass->get_htmlentities_variable('courseurl');

   $pass->get_htmlentities_variable('assignment', '---');

   if (!($pass->isParam('action', 'listcourses') 
           || $pass->isParam('action', 'listassignments'))
         && $pass->isParam('assignment', '---'))
   {
      $pass->add_error_message('Please select the assignment', 
        'assignment', 'Required');
      $pass->setParam('action', 'listassignments');
   }

   $assignmentdata = null;

   if ($pass->isParamSet('courseurl') && $pass->isParamSet('assignment')
       && !$pass->isParam('assignment', '---'))
   {
      $assignmentdata = fetch_assignment();
      $pass->setParam('assignmentdata', $assignmentdata);

      if (isset($assignmentdata['allowedbinaries']))
      {
         array_push($filetypes, 'BINARY');
      }
   }

   $pass->setParam('filetypes', $filetypes);

   $pass->get_int_variable('groupnumber', 1);

   $pass->get_boolean_variable('agree', false);

   $pass->get_matched_variable('encoding', '/^(utf8|ascii|latin1)$/', 'utf8');

   $pass->get_int_variable('num_extra_files', 4);

   $pass->get_htmlentities_variable('blackboardid');

   if ($pass->isParamSet('groupnumber'))
   {
      if (!$pass->isParamSet('blackboardid'))
      {
         if ($pass->isParam('groupnumber', 1))
         {
            $pass->setParam('blackboardid', $pass->getUserName());
         }
         else
         {
            $pass->setParam('blackboardid', array($pass->getUserName()));
         }
      }
   }

   $userinfo = false;

   if ($pass->isParamSet('blackboardid'))
   {
      $userinfo = $pass->getUserInfo($pass->getParam('blackboardid'), true);

      if (empty($userinfo))
      {
         $pass->add_error_message("No registration number available or invalid username supplied.");
      }
      elseif (is_array($userinfo))
      {
         $missing = '';
         $unknown = '';
         $unknown_tag = 'Unknown username';

         if (is_array($pass->getParam('blackboardid')))
         {
            foreach ($pass->getParam('blackboardid') as $user)
            {
               if (!isset($userinfo[$user]))
               {
                  if (empty($unknown))
                  {
                     $unknown = htmlentities($user);
                  }
                  else
                  {
                     $unknown .= ', ' .htmlentities($user);
                     $unknown_tag = 'Unknown usernames';
                  }
               }
               elseif (empty($userinfo[$user]['regnum']))
               {
                  if (empty($missing))
                  {
                     $missing = htmlentities($user);
                  }
                  else
                  {
                     $missing .= ', ' .htmlentities($user);
                  }
               }
            }
         }
         elseif (empty($userinfo['regnum']))
         {
            $missing = htmlentities($pass->getParam('blackboardid'));
         }

         if (!empty($unknown))
         {
            $pass->add_error_message("$unknown_tag: $unknown. Please check the spelling and ensure that all group members have created an account.");
            $pass->setParam('action', 'groupmembers');
         }

         if (!empty($missing))
         {
            $pass->add_error_message("No registration number available for: $missing.");
            $pass->setParam('action', 'groupmembers');
         }
      }

      $pass->setParam('user_info', $userinfo);
   }

   if ($pass->isParam('action', 'runpass'))
   {
      $pass->get_boolean_variable('advanced_upload', false);

      if (!$pass->isParamSet('course'))
      {
         $pass->add_error_message('Missing Course', 'course', 'Required');
         $pass->setParam('action', 'listcourses');
      }
      elseif (!$pass->isParamSet('assignment'))
      {
         $pass->add_error_message('Missing Assignment', 'assignment', 'Required');
         $pass->setParam('action', 'listassignment');
      }
      else
      {
         if (!$pass->isBoolParamOn('agree'))
         {
            $pass->add_error_message('Agreement required', 'agree', 'Required');
            $pass->setParam('action', 'assignmentfiles');
         }

         $numid = 1;

         if (!$pass->isParamSet('blackboardid'))
         {
            $pass->add_error_message('Blackboard ID missing', 'blackboardid', 'Required');
            $pass->setParam('action', 'assignmentfiles');
         }
         elseif ($pass->isParamArray('blackboardid'))
         {
            $numid = count($pass->getParam('blackboardid'));
         }

         if (!$pass->isParam('groupnumber', $numid))
         {
            $pass->add_error_message(sprintf(
             "Missing one or more Blackboard IDs (group number: %s, number supplied: %s)", $pass->getParam('groupnumber'), $numid), 
              'blackboardid', 'Required');
            $pass->setParam('action', 'assignmentfiles');
         }

         $pass->get_boolean_variable('sub_paths');

         if (has_sub_paths())
         {
            $pass->get_subpath_variable('subpath_file');
            $pass->get_subpath_variable('subpath_reportfile');
            $pass->get_subpath_variable('subpath_additionalfile');
         }

	 $pass->get_choice_variable('additionalfilelang', 
            $pass->getParam('filetypes'));

         if (isset($assignmentdata) && isset($_FILES))
         {
            $pass->setParam('assignmentdata', $assignmentdata);

            check_files('file');
            check_files('additionalfile');
            check_files('reportfile');

	    if (empty($_FILES))
	    {
               $pass->add_error_message('No files uploaded!');
	    }

            if ($pass->has_errors())
            {
               $pass->setParam('action', 'assignmentfiles');
            }
         }
      }
   }
}

function run_pass_test()
{
   global $pass;
   $user = $pass->getUserName();

   $timestamp = date(Pass::TIME_STAMP_FORMAT);
   $user_assign = "${user}_".$pass->getParam('assignment');
   $resultsfile = "${user_assign}.pdf";

   $token = create_token();
   $dir = "${timestamp}_${token}";
   $uploaddir = $pass->getUploadPath() . "/$dir";

   $content = [ 
        "Submission-timestamp: $timestamp",
        "Course: ".$pass->getParam('course'), 
        "Assignment: ".$pass->getParam('assignment'), 
        "Agree: " . ($pass->isBoolParamOn('agree') ? 'true' : 'false'),
        "Project-encoding: " . $pass->getParam('encoding'),
        "Pdf-result: $resultsfile",
        "Timeout: " . $pass->getConfigValue('timeout')
   ];
   
   if (has_sub_paths())
   {
      array_push($content, 'Base-path: /usr/src/app/files');
   }
   
   $userids = $pass->getParam('blackboardid');
   $userinfo = $pass->getParam('user_info');
   
   if (is_array($userids))
   {
      foreach ($userids as $username)
      {
         array_push($content, sprintf("Student: %s\t%s", $username, $userinfo[$username]['regnum']));
      }
   }
   else
   {
      array_push($content, sprintf("Student: %s\t%s", $userids, $userinfo['regnum']));
   }

   $total = 0;
   
   if (isset($_FILES))
   {
      $total = process_uploaded_file('file', $content, $uploaddir);
      $total += process_uploaded_file('additionalfile', $content, $uploaddir);
      $total += process_uploaded_file('reportfile', $content, $uploaddir);
   }

   if ($total === 0)
   {
      $pass->add_error_message('No files uploaded.');
   }
   
   $pass->page_header();

   if ($pass->has_errors())
   {
      $pass->print_errors();
   }

   echo "<p>Total files: $total\n";

   echo '<p>Settings: <pre>', implode(PHP_EOL, $content), '</pre>';

   echo "<p>Sub-paths: <pre>", var_export($pass->getParam('subpath_additionalfile'), true), '</pre>';

   echo '<p>Files:<pre>';
   print_r($_FILES);
   echo '</pre>';

   echo "<a href=\"", $_SERVER['PHP_SELF'], "\">Restart</a>.";

   $pass->page_footer();

   return false;
}

function run_pass()
{
   global $pass;
   $user = $pass->getUserName();

   $timestamp = date(Pass::TIME_STAMP_FORMAT);
   $user_assign = "${user}_".$pass->getParam('assignment');

   $token = create_token();
   $dir = "${timestamp}_${token}";
   $uploaddir = $pass->getUploadPath() . "/$dir";

   while (file_exists($uploaddir))
   {
      $pass->record_action('upload', 
         "Upload directory '$uploaddir' exists", true); 

      $token = create_token();
      $dir = "${timestamp}_${token}";
      $uploaddir = $pass->getUploadPath() . "/$dir";
   }

   if (!mkdir($uploaddir, $pass->getUploadPathPermissions()))
   {
      $pass->add_error_message("Unable to create upload directory");
      error_log("Unable to create directory '$uploaddir'");

      $pass->record_action('upload', "Failed to create directory '$uploaddir'", true);
   }
   else
   {
      $settingsfile = "$uploaddir/pass-settings-${token}.txt";
      $resultsfile = "${user_assign}.pdf";
   
      $content = [ 
        "Submission-timestamp: $timestamp",
        "Course: ".$pass->getParam('course'), 
        "Assignment: ".$pass->getParam('assignment'), 
        "Agree: " . ($pass->isBoolParamOn('agree') ? 'true' : 'false'),
        "Project-encoding: " . $pass->getParam('encoding'),
        "Pdf-result: $resultsfile",
        "Timeout: " . $pass->getConfigValue('timeout')
      ];

      if (has_sub_paths())
      {
         array_push($content, 'Base-path: /usr/src/app/files');
      }
   
      $userids = $pass->getParam('blackboardid');
      $userinfo = $pass->getParam('user_info');
   
      if (is_array($userids))
      {
         foreach ($userids as $username)
         {
            array_push($content, sprintf("Student: %s\t%s", $username, $userinfo[$username]['regnum']));
         }
      }
      else
      {
         array_push($content, sprintf("Student: %s\t%s", $userids, $userinfo['regnum']));
      }

      $total = 0;
   
      if (isset($_FILES))
      {
         $total = process_uploaded_file('file', $content, $uploaddir);
         $total += process_uploaded_file('additionalfile', $content, $uploaddir);
         $total += process_uploaded_file('reportfile', $content, $uploaddir);

         if ($pass->has_errors())
         {
            $pass->record_action('upload',
              "File upload failed " . $pass->get_error_list());
         }
      }

      if ($total === 0)
      {
         $pass->add_error_message('No files uploaded.');
         $pass->record_action('upload', "No files");
      }
      else
      {
         if (file_put_contents($settingsfile, implode(PHP_EOL, $content)) === FALSE) 
         {
            $pass->add_error_message("Failed to write settings file.");
            error_log("Failed to write '$settingsfile'");
            $pass->record_action('upload', "Failed to write settings file '$settingsfile'", true);
         }
         else
         {
            if (!chmod($settingsfile, $pass->getUploadFilePermissions()))
            {
               $pass->add_error_message('Failed to change file permissions for settings file.');
               error_log("Failed to change file permissions for $settingsfile to "
                       . $pass->getUploadFilePermissions());

               $pass->record_action('upload', "Failed to chmod settings file '$settingsfile'", true);
            }
   
            $submission_id = $pass->publishUpload($userinfo, $pass->getParam('course'), $pass->getParam('assignment'), $timestamp, $token);

	    $pass->setParam('submission_id', $submission_id);

            $pass->record_action('upload', 
               "Successful (submission ID: $submission_id, token: '$token', upload time: $timestamp)", true);
         }
      }
   }

   if ($pass->has_errors())
   {
      $pass->page_header();
      $pass->print_errors();
      echo "Please contact an administrator and quote ID '$token' and upload time $timestamp";

      if (isset($submission_id) && $submission_id !== false)
      {
         echo " and the submission ID: $submission_id. You can check the ",
           $pass->get_upload_lists_link('Uploads page', null, "submission_id=$submission_id"),
           " to find the upload status.";

         $pass->record_action('upload',
           "Failed (submission ID: $submission_id, token: '$token', upload time: $timestamp)", true);
      }
      else
      {
         echo '.';

         $pass->record_action('upload', "Failed (token: '$token', upload time: $timestamp)", true);
      }

      $pass->page_footer();

      return false;
   }
   else
   {
      return true;
   }
}

function process_uploaded_file($field, &$content, $uploaddir)
{
   global $pass;
   if (!isset($_FILES[$field])) return;

   $num_processed = 0;

   if (is_array($_FILES[$field]['name']))
   {
      $total = count($_FILES[$field]['name']);
   
      for ($i = 0; $i < $total; $i++)
      {
         if (!empty($_FILES[$field]['name'][$i]))
	 {
            process_file($_FILES[$field]['tmp_name'][$i],
		    $_FILES[$field]['name'][$i],
		    has_sub_paths() ? $pass->getParamElement("subpath_$field", $i) : null,
                    $content, $uploaddir,
                    $pass->isParamElementSet("${field}lang", $i) ? 
                       $pass->getParamElement("${field}lang", $i) : null
            );

            $num_processed++;
	 }
      }
   }
   else if (isset($_FILES[$field]['name']))
   {
      if (!empty($_FILES[$field]['name']))
      {
         process_file($_FILES[$field]['tmp_name'],
            $_FILES[$field]['name'], 
            has_sub_paths() ? ($pass->isParamArray("subpath_$field") ?
               $pass->getParamElement("subpath_$field", 0) : $pass->getParam("subpath_$field")) : null,
            $content, $uploaddir,
            $pass->isParamElementSet("${field}lang", 0) ? 
            $pass->getParamElement("${field}lang", 0) : null);

         $num_processed++;
      }
   }

   return $num_processed;
}

function process_file($tmppath, $name, $subpath, &$content, $uploaddir, $lang)
{
   global $pass;
   $dir = $uploaddir;
   $relpath = $name;

   if ($tmppath)
   {
      if (!empty($subpath) && $subpath !== '.')
      {
         $relpath = "$subpath/$name";
         $dir = "$uploaddir/$subpath";
   
	 if (!file_exists($dir)
              && !mkdir($dir, $pass->getUploadPathPermissions(), true))
         {
            $pass->debug_message("Unable to create upload directory " 
             . htmlentities($dir));
            error_log("Unable to create directory '$dir'");
            $dir = $uploaddir;
            $relpath = $name;
         }
         elseif (!is_dir($dir))
         {
            echo "<p>'", htmlentities($subpath), "' is not a directory.";
            $dir = $uploaddir;
	    $relpath = $name;
         }
      }
      elseif (!file_exists($dir)
              && !mkdir($dir, $pass->getUploadPathPermissions(), true))
      {
         $pass->debug_message("Unable to create upload directory " 
          . htmlentities($dir));
         error_log("Unable to create directory '$dir'");
      }
   
      $file = "$dir/$name";
   
      if (file_exists($file))
      {
         echo '<p>Duplicate file \'', htmlentities($relpath), "'";
      }
      elseif (move_uploaded_file($tmppath, $file))
      {
         if (!chmod($file, $pass->getUploadFilePermissions()))
         {
            $pass->debug_message('Failed to change file permissions for '
            . htmlentities($file) . ' to ' . $pass->getUploadFilePermissions());
            error_log("Failed to change file permissions for $file to "
                    . $pass->getUploadFilePermissions());
         }

         $pass->debug_message("Moved uploaded file to " . htmlentities($file));
   
	 if (isset($lang))
	 {
            array_push($content, "File: $relpath\t$lang");
	 }
	 else
	 {
            array_push($content, "File: $relpath");
	 }
      }
      else
      {
         echo "<p>I'm sorry, something went wrong trying to move uploaded file '",
                 htmlentities($relpath), "'";
   
         $pass->debug_message("Failed to move uploaded file "
           . htmlentities($tmppath) . " to " . htmlentities($file));
   
         error_log("Failed to move uploaded file $tmppath to $file");
      }
   }
}

function check_files($field)
{
   global $pass;
   if (isset($_FILES[$field]))
   {
      if (is_array($_FILES[$field]['name']))
      {
         $total = count($_FILES[$field]['name']);

         for ($i = 0; $i < $total; $i++)
         {
            check_filename($_FILES[$field]['name'][$i]);
         }
      }
      else
      {
         check_filename($_FILES[$field]['name']);
      }
   }
}

function check_filename($filename)
{
   global $pass;

   if (empty($filename))
   {
      return;
   }

   $assignmentdata = $pass->getParam('assignmentdata');

   if (isset($assignmentdata['resourcefiles']))
   {
      foreach ($assignmentdata['resourcefiles'] as $resourcefile)
      {
         if ($resourcefile === $filename)
         {
            $pass->add_error_message( 
            "File name '".htmlentities($filename) . "' clashes with supplied file");
            return;
         }
      }
   }

   if (isset($assignmentdata['resultfiles']))
   {
      foreach ($assignmentdata['resultfiles'] as $resultfile)
      {
         $name = basename($resultfile);

         if ($name === $filename)
         {
            $pass->add_error_message( 
            "File name '".htmlentities($filename) . "' clashes with result file");
            return;
         }
      }
   }

   if ($filename === 'a.out')
   {
      $pass->add_error_message("Binary file 'a.out' forbidden.");
      return;
   }

   if (!preg_match('/^[a-zA-Z0-9_\-+]+(\.[a-zA-Z0-9_\-+]+)?$/', $filename))
   {
      $pass->add_error_message("File name '" . htmlentities($filename) . "' contains invalid characters");
   }

   $ext = pathinfo($filename, PATHINFO_EXTENSION);

   if ($ext !== false)
   {
      $ext = strtolower($ext);

      if (isset($assignmentdata['allowedbinaries']))
      {
         foreach ($assignmentdata['allowedbinaries']['ext_array'] as $allowed_ext)
         {
            if ($ext === strtolower($allowed_ext))
            {
               return;
            }
         }
      }

      if (preg_match('/^(zip|exe|tar|gz|tgz|jar|a|ar|iso|bz2|lz|lz4|xz|7z|s7z|cab|class|o|png|jpg|jpeg|gif)$/i', $ext))
      {
         $pass->add_error_message(
            "Binary file '" . htmlentities($filename). "' forbidden.");
      }
   }
}

function assignment_files()
{
   global $pass;

   if (!$pass->isParamSet('courseurl'))
   {
      if (!fetch_courseurl())
      {
         echo "<p>Unknown course.</p>";

         list_courses();
         return;
      }
   }

   if ($pass->isParamSet('assignmentdata'))
   {
      $assignment = $pass->getParam('assignmentdata');
   }
   else
   {
      $assignment = fetch_assignment();
   }

   if ($assignment === false)
   {
      echo "<p>Unknown assignment.</p>";

      list_assignments();
      return;
   }

   echo '<h2>', htmlentities($pass->getParam('course')), ': ', 
     htmlentities($assignment['title']), '</h2>';

   $groupnum = (int)$pass->getParam('groupnumber');
   $issolo = ($groupnum === 1 ? true : false);

   $pass->start_form(array('enctype'=>"multipart/form-data", 'id'=>'uploader'));

?>

   <h2>Project Files</h2>
   <p>File names may only contain alphanumerics (a&#x2013;z, A&#x2013;Z, 0&#x2013;9), underscores
   <code>_</code> or hyphens <code>-</code> or plus <code>+</code> (and a dot for the extension).
   <strong>File names are case-sensitive.</strong>
The only permitted binary files are PDF or Word documents<?php

   if (isset($assignment['allowedbinaries']))
   {
      if ($assignment['allowedbinaries']['num_exts'] > 1)
      {
         echo ' or files with the following extensions: ';
      }
      else
      {
         echo ' or files with the extension ';
      }

      echo $assignment['allowedbinaries']['exts'];

   }
?>. Make sure you set the correct file type.

   <p><label for="encoding">File encoding:</label> 
   <select name="encoding" id="encoding" >
   <option value="utf8"<?php if ($pass->isParam('encoding', 'utf8')) {echo ' selected';}
     ?> >UTF-8</option>
   <option value="ascii"<?php if ($pass->isParam('encoding', 'ascii')) {echo ' selected';}
     ?> >ASCII</option>
   <option value="latin1"<?php if ($pass->isParam('encoding', 'latin1')) {echo ' selected';}
     ?> >Latin 1</option>
   </select>
<?php
   echo ' ', $pass->get_faq_link(
    'What\'s the difference between ASCII, Latin 1 and UTF-8?', 'encoding',
     ['target'=>'_blank']);

?>
<p>Maximum number of files: <?php echo ini_get('max_file_uploads'); ?> (including any required files).

<?php
   if (!$pass->isParamSet('sub_paths') && isset($assignment['mainfile']))
   {
      $info = pathinfo($assignment['mainfile'], PATHINFO_DIRNAME);

      if ($info !== '.')
      {
         $pass->setBoolParam('sub_paths', true);
      }
   }

   if (!$pass->isParamSet('sub_paths') && isset($assignment['files']))
   {
      foreach ($assignment['files'] as $file)
      {
         $info = pathinfo($file, PATHINFO_DIRNAME);

         if ($info !== '.')
         {
            $pass->setBoolParam('sub_paths', true);
            break;
         }
      }
   }

   if (!$pass->isParamSet('sub_paths') && isset($assignment['reports']))
   {
      foreach ($assignment['reports'] as $val)
      {
         $info = pathinfo($val, PATHINFO_DIRNAME);

         if ($info !== '.')
         {
            $pass->setBoolParam('sub_paths', true);
            break;
         }
      }
   }

   echo '<p>', $pass->form_input_boolean_checkbox('sub_paths',
           isset($assignment['relpath']) ? $assignment['relpath'] : false, 
           array("id"=>"sub_paths"));

?>
   <label for="sub_paths">Project has sub-directories.</label>
   <div id="sub_paths_note" <?php 
     echo 'style="display: ', has_sub_paths() ? 'block' : 'none', '"'; 
?>>The sub-path relative to the project's base
   directory should be provided in the field near the browse buttons below. Omit
   the final slash. The sub-path must not start with a slash. Each path element
   must consist of only alphanumerics (a-z, A-Z, 0-9) or an underscore (<code>_</code>) or
   a hyphen (<code>-</code>). Use a single dot (<code>.</code>) to indicate
   the project's base directory.</div>

   <div id="dropbox" class="box_dragndrop">
   <p>Either use the upload <?php echo OPENFOLDER; ?> file selectors 
   below or drag and drop files here in this drop area. Alternatively, you can use
   this multi-file selector: 
   <label for="multifiles"><span class="btn"><?php echo OPENFOLDER;?></span></label>
   <input type="file" multiple id="multifiles" name="multifiles" class="visually-hidden">
  <p>
  Make sure that you check that any required files have been correctly identified 
   below, where appropriate, and check the language selectors for additional files.</p>
   <p style="text-align: center; font-size: xx-large;">&#x1F5D0;</p>
   <div id="drop_status"></div>
   </div>
   <div class="box_uploading">Uploading&#x2026;</div>
   <div class="box_error">Upload error <span>(at least one file must be uploaded)</span>.</div>

   <h3>Required Files</h3>
<?php

   if (!$pass->isParamSet('subpath_file'))
   {
      $pass->setParam('subpath_file', array());
   }

   $id = 0;

   if (isset($assignment['mainfile']))
   {
      file_row($assignment['mainfile'], 'file[]', "file$id", true);

      $id++;
   }

   if (isset($assignment['files']))
   {
      foreach ($assignment['files'] as $file)
      {
         file_row($file, 'file[]', "file$id", true);

         $id++;
      }
   }

   $num_required_files = $id;

   $num_reports = 0;

   if (isset($assignment['reports']))
   {
      if ($num_required_files === 0)
      {
?>
<p>This assignment doesn't require specific file names for source code. If you
have any source code files, you need to add them to the 'Additional Files' section
below (or drag and drop them in the drop area above).
<?php
      }

      $num_reports = count($assignment['reports']);
?>
<p>This assignment requires <?php echo $num_reports, ' report ', $num_reports == 1 ? 'file' : 'files'?>, which may be either a PDF or Word document. PDF works better 
with PASS as the contents can be included whereas a Word document can only be added
as an attachment. The basename must be as indicated below. The extension should be
<span class="file">.pdf</span>, <span class="file">.doc</span> or 
<span class="file">.docx</span>, according to the file format.
<?php
      if (!$pass->isParamSet('subpath_report'))
      {
         $pass->setParam('subpath_report', array());
      }

      $id = 0;

      foreach ($assignment['reports'] as $val)
      {
         file_row($val, 'reportfile[]', "report$id");

         $id++;
      }
   }

   if (isset($assignment['resourcefiles']))
   {
      echo '<h3>Supplied Project Files</h3>';

      $total = count($assignment['resourcefiles']);

      echo "<p>This project has $total supplied ", ($total == 1 ? 'file' : 'files'),
              " which will automatically be fetched by PASS. ",
              ($total == 1 ? 'This file' : 'These files'), ' should not be uploaded.';

      foreach ($assignment['resourcefiles'] as $filename)
      {
         echo '<p><span class="file">', htmlentities($filename), '</span>';
      }
   }

   if (isset($assignment['resultfiles']))
   {
      echo '<h3>Project Result Files</h3>';

      $total = count($assignment['resultfiles']);

      echo "<p>This project has $total result ", ($total == 1 ? 'file' : 'files'),
              " which should be created by your application. ",
              ($total == 1 ? 'This file' : 'These files'), ' should not be uploaded.';

      foreach ($assignment['resultfiles'] as $resultfile)
      {
         echo '<p><span class="file">', htmlentities($resultfile), '</span>';
      }
   }

   if ($num_required_files > 0)
   {
?>
<h3>Additional Files (Optional)</h3>
<p>If you have gone beyond the basic project requirements and have additional source
code files, you can add them in this section using the browse buttons below (or drag and drop 
the files into the drop area above).
<?php
   }
   elseif ($num_reports > 0)
   {
?>
<h3>Additional Files (Optional)</h3>
<p>This assignment doesn't require source code with specific file names. If you have
to provide source code in addition to your <?php echo $num_reports===1 ? 'report' : 'reports' ?>, 
you can add them in this section using the browse buttons below (or drag and drop 
the files into the drop area above).
<?php
   }
   else
   {
?>
<h3>Assignment Files</h3>
<p>This assignment doesn't require source code with specific file names. 
You can supply your source code files in this section using the browse buttons below 
(or drag and drop the files into the drop area above).
<?php
   }
?>
<p>Make sure that the file type selector is set correctly. If the language
isn't listed, select 'Plain Text'.
<?php

   if (!$pass->isParamSet('subpath_additionalfile'))
   {
      $pass->setParam('subpath_additionalfile', array());
   }

   echo '<div id="supplementalfiles">';

   for ($id = 0; $id < $pass->getParam('num_extra_files') ; $id++)
   {
      additional_file_row($id);
   }
?>

   </div>
   <button id="add_extra" type="button" onclick="addExtraFields()">Add </button>
   <input id="num_extra" type="number" min="1" value="1" />
   more file field(s).
<?php

   echo $pass->form_input_hidden('num_extra_files', ['id'=>'num_extra_files']);
   echo $pass->form_input_hidden('groupnumber');
   echo $pass->form_input_hidden('assignment');
   echo $pass->form_input_hidden('course');
   echo $pass->form_input_hidden('courseurl');
   echo $pass->form_input_hidden('blackboardid');

?>
   <p>
   <input type="checkbox" id="agree" name="agree">
   <label for="agree"><?php echo $issolo ? 'I' : 'We'; ?> agree that by submitting a PDF generated by <?php echo $pass->getShortTitle(), ' ', $issolo ? 'I am' : 'we are'; ?> confirming that <?php echo $issolo ? 'I' : 'we'; ?> have checked the PDF and that it correctly represents <?php echo $issolo ? 'my' : 'our'; ?> submission.</label>
   <div class="box_error">Upload error <span>(at least one file must be uploaded)</span>.</div>
   <div class="box_uploading">Uploading&#x2026;</div>
   <p>
<?php
    echo $pass->form_prev_button(
      [
        'value'=>$issolo ? 'listassignments' : 'groupmembers',
	'onclick'=>'this.form.submitted=this.value;', 
	'id'=>'prev'
      ]
    );
?>
   <span class="spacer"></span> 
<?php
    echo $pass->form_next_button(
      [
        'value'=>'runpass', 'id'=>'next', 'onclick'=>'this.form.submitted=this.value;'
      ]
    );
?>
   </p>
   <p>If you experience a problem with uploading files, try the <?php

   echo $pass->get_upload_fallback_link('simpler fallback upload script', null,
    sprintf("course=%s&amp;assignment=%s&amp;groupnumber=%d&amp;action=groupmembers&amp;num_extra_files=0", 
      urlencode($pass->getParam('course')), 
      urlencode($pass->getParam('assignment')), 
      $pass->getParam('groupnumber') 
     )
   );

?>.

   </form>
<script>
var agreeelem = document.getElementById("agree");

window.addEventListener('load', enableNext);

agreeelem.addEventListener("change", enableNext);

 function enableNext()
 {
    var elem = document.getElementById("next");

    if (agreeelem.checked)
    {
       elem.disabled = false;
    }
    else
    {
       elem.disabled = true;
    }
 }

 function validateForm()
 {
    if (uploaderForm.classList.contains("is-uploading"))
    {
       return false;
    }

    var fileStatus = uploaderForm.getElementsByClassName("filestatus");

    if (uploaderForm.submitted == "listassignments" || uploaderForm.submitted == "groupmembers"  || !fileStatus)
    {
       return true;
    }

    var missing = false;

    for (var i = 0; i < fileStatus.length; i++)
    {
       if (fileStatus[i].innerHTML !== '<?php echo CHECKMARK; ?>')
       {
          missing = true;
       }
    }


    if (missing)
    {
       if (confirm('One or more project files are missing. Do you still want to continue?'))
       {
          return true;
       }
       else
       {
          return false;
       }
    }


    uploaderForm.classList.add('is-uploading');
    uploaderForm.classList.remove('is-error');

    return true;
 }

 function advancedUploadSubmit(evt)
 {
    if (evt.submitter.id
         && (evt.submitter.id === 'prev' || evt.submitter.id === 'fallback'))
    {
       return;
    }

    evt.preventDefault();

    var form_data = new FormData(uploaderForm);
    var total_files = 0;
<?php
  if ($num_required_files > 0)
  {
?>
    form_data.delete('file[]');
    form_data.delete('subpath_file[]');

    for (var i = 0; i < uploadedFiles.length; i++)
    {
       if (uploadedFiles[i])
       {
          form_data.append('file[]', uploadedFiles[i]);
	  total_files++;

	  var subPathElem = uploadedFilesSubPaths[i];
	  form_data.append('subpath_file[]', subPathElem.value);
       }
    }
<?php 
  }

  if ($num_reports > 0)
  {
 ?>

    form_data.delete('reportfile[]');
    form_data.delete('subpath_reportfile[]');

    for (var i = 0; i < uploadedReports.length; i++)
    {
       if (uploadedReports[i])
       {
          form_data.append('reportfile[]', uploadedReports[i]);
	  total_files++;

	  var subPathElem = uploadedReportsSubPaths[i];
	  form_data.append('subpath_reportfile[]', subPathElem.value);
       }
    }
<?php 
  }
?>

    form_data.delete('additionalfile[]');
    form_data.delete('additionalfilelang[]');
    form_data.delete('subpath_additionalfile[]');

    for (var i = 0; i < uploadedAdditionalFiles.length; i++)
    {
       if (uploadedAdditionalFiles[i])
       {
          form_data.append('additionalfile[]', uploadedAdditionalFiles[i]);
	  total_files++;

	  var langElem = additionalFilesLang[i];
          var opts = langElem.options;
	  form_data.append('additionalfilelang[]', opts[langElem.selectedIndex].value);

	  var subPathElem = additionalSubPaths[i];
	  form_data.append('subpath_additionalfile[]', subPathElem.value);
       }
    }

    if (total_files == 0)
    {
       uploaderForm.classList.add('is-error');
       alert("At least one file must be uploaded.");
       return;
    }

    form_data.set('action', 'runpass');

    if (!validateForm())
    {
       return;
    }

    form_data.set('advanced_upload', 'on');

    document.getElementById("next").disabled = true;
    document.getElementById("prev").disabled = true;

    var request = new XMLHttpRequest();
    request.open("POST", "<?php echo $_SERVER['PHP_SELF']; ?>");
    request.onload = function(oEvent)
    {
       document.open();
       document.write(request.responseText);
       document.close();
       window.scrollTo(0,0);

       const found = request.responseText.match(/Upload success (\d+)/);

       if (found)
       {
	  setTimeout(function() {window.location.replace("<?php 
            echo $pass->getWebsite($pass->getUploadListsRef()), '?submission_id='; ?>"+found[1])}, 2000);
       }
    };

    request.send(form_data);

 }

 function getFileLanguage(filename)
 {
    const ext = filename.substring(filename.lastIndexOf('.')+1, filename.length) || filename;

    if (ext === 'java')
    {
       return 'Java';
    }
    else if (ext === 'cpp' || ext === 'cp' || ext === 'cc' || ext === 'C'
       || ext === 'CPP' || ext === 'c++' || ext === 'cxx' || ext === 'hh'
       || ext == 'hpp' || ext === 'H')
    {
       return 'C++';
    }
    else if (ext === 'h')
    {
       return 'C++' === getMainLanguage() ? 'C++' : 'C';
    }
    else if (ext === 'c')
    {
       return 'C';
    }
    else if (ext === 'perl' || ext === 'pl')
    {
       return 'Perl';
    }
    else if (ext === 'php')
    {
       return 'PHP';
    }
    else if (ext === 'sh')
    {
       return 'bash';
    }
    else if (ext === 'bat' || ext === 'com')
    {
       return 'command.com';
    }
    else if (ext === 'html' || ext === 'xhtml' || ext === 'htm' || ext === 'shtml')
    {
       return 'HTML';
    }
    else if (ext === 'py')
    {
       return 'Python';
    }
    else if (ext === 'm')
    {
       return 'Matlab';
    }
    else if (ext === 'Makefile' || ext === "makefile" || ext === 'mk')
    {
       return 'make';
    }
    else if (ext === 'pdf')
    {
       return 'PDF';
    }
    else if (ext === 'doc' || ext === 'docx')
    {
       return 'DOC';
    }
    else if (ext === 'txt' || ext === 'csv')
    {
       return 'Plain Text';
    }
<?php
  foreach (Pass::FILE_TYPES as $filetype)
  { ?>
    else if (ext === '<?php echo htmlentities(strtolower($filetype)); ?>')
    {
       return '<?php echo htmlentities($filetype); ?>';
    }
<?php
  } 

  if ($pass->isParamElementSet('assignmentdata', 'allowedbinaries'))
  {
     $accept = $pass->getParamElement('assignmentdata', 'allowedbinaries');

     foreach ($accept['ext_array'] as $filetype)
     {
?>
    else if (ext === '<?php echo htmlentities(strtolower($filetype)); ?>')
    {
       return 'BINARY';
    }
<?php
     }
  }
?>

    return getMainLanguage();
 }

 function getMainLanguage()
 {
     return "<?php echo $pass->isParamElementSet('assignmentdata', 'language') ? $pass->getParamElement('assignmentdata', 'language') : ''; ?>";
 }

 function isFileNameValid(fileName)
 {
    const lcfilename = fileName.toLowerCase();

<?php
 
   if (isset($assignment['resourcefiles']))
   {
      foreach ($assignment['resourcefiles'] as $resourcefile)
      {
?>
    if (fileName === '<?php echo htmlentities($resourcefile); ?>')
    {
       return false;
    }

<?php
      }
   }

   if (isset($assignment['resultfiles']))
   {
      foreach ($assignment['resultfiles'] as $resultfile)
      {
         $name = basename($resultfile);

?>
    if (fileName === '<?php echo htmlentities($name); ?>')
    {
       return false;
    }
<?php
      }
   }

   if ($pass->isParamElementSet('assignmentdata', 'allowedbinaries'))
   {
      $accept = $pass->getParamElement('assignmentdata', 'allowedbinaries');

      foreach ($accept['ext_array'] as $filetype)
      {
?>
    if (lcfilename.endsWith('<?php echo "." . htmlentities(strtolower($filetype)); ?>'))
    {
       return true;
    }
<?php
      }
   }
?>
    if (lcfilename.match(/(a\.out|\.zip|\.exe|\.tar|\.gz|\.tgz|\.jar|\.a|\.ar|\.iso|\.bz2|\.lz4?|\.xz|\.s?7z|\.cab|\.class|\.o|\.png|\.jpg|\.jpeg|\.gif)$/) || !fileName.match(/^[a-zA-Z0-9\-_+]+(\.[a-zA-Z0-9\-_+]+)?$/))
    {
       return false;
    }
    else
    {
       return true;
    }
 }

 function checkAdditionalFile(event)
 {
    document.getElementById("drop_status").innerHTML = '';

    var id = event.currentTarget.id;
    var fileName = event.currentTarget.value;
    var statusElem = document.getElementById("filestatus-"+id);
    var file = event.currentTarget.files[0];

    const found = id.match(/[0-9]+/);
    const idnum = found[0];

    if (fileName)
    {
       var n = fileName.lastIndexOf('\\');

        if (n > 0)
        {
           fileName = fileName.substr(n+1);
        }

        if (isFileNameValid(fileName))
        {
           uploaderForm.classList.remove('is-error');

           statusElem.innerHTML = '';
	   statusElem.appendChild(document.createTextNode(fileName+' <?php echo CHECKMARK; ?>'));

           uploadedAdditionalFiles[idnum] = file;
	   additionalSubPaths[idnum] = document.getElementById("subpath-"+id);

           var langElem = document.getElementById("lang-"+id);

	   additionalFilesLang[idnum] = langElem;

	   const lang = getFileLanguage(fileName);

	   if (lang)
	   {
              var opts = langElem.options;

              for (var i = 0; i < opts.length; i++)
              {
                 if (opts[i].value === lang)
                 {
                    langElem.selectedIndex = i;
                    break;
	         }
	      }
	   }
        }
        else
        {
           event.currentTarget.value = '';
           statusElem.innerHTML = '';
	   statusElem.appendChild(document.createTextNode('<?php 
            echo CROSSMARK; ?> invalid filename "'+fileName +'".'));
        }
    }
 }

const subPathsElem = document.getElementById("sub_paths");

if (subPathsElem)
{
  subPathsElem.addEventListener("click", function()
   {
      var elem = document.getElementById("sub_paths_note");
  
      if (elem)
      {
         if (this.checked)
         {
            elem.style.display="block";
         }
         else
         {
            elem.style.display="none";
         }
      }

      var subPathElems = document.getElementsByClassName("sub_path");

      for (var i = 0; i < subPathElems.length; i++)
      {
         if (this.checked)
         {
            subPathElems[i].style.display="inline";
         }
         else
         {
            subPathElems[i].style.display="none";
         }
      }
   }
   );
}

 function addExtraFields()
 {
    var subPathsButton = document.getElementById("sub_paths");
    var enableSubPaths = subPathsButton.checked;
    var div = document.getElementById("supplementalfiles");
    var numExtra = document.getElementById("num_extra");
    var elems = document.getElementsByClassName("additionalfile");
    var numExtraFilesField = document.getElementById("num_extra_files");

    var startIdx = elems.length;
    var n = startIdx+parseInt(numExtra.value);

    numExtraFilesField.value = n;

    for (var i = startIdx; i < n; i++)
    {
       addExtraField(div, i, enableSubPaths);
    }
 }

 function addExtraField(div, i, enableSubPaths)
 {
    var parNode = document.createElement("P");
    div.appendChild(parNode);

    var labelNode = document.createElement("label");
    parNode.appendChild(labelNode);

    labelNode.setAttribute("for", "additionalfile"+i);

    var textNode = document.createTextNode("Supplementary File "+(i+1)+" ");

    labelNode.append(textNode);

    var folderNode = document.createElement("span");
    folderNode.classList.add("btn");
    folderNode.innerHTML = '<?php echo OPENFOLDER; ?>';
    labelNode.append(folderNode);

    parNode.appendChild(document.createTextNode(" "));

    var spanNode = document.createElement("span");
    parNode.appendChild(spanNode);

    spanNode.classList.add("sub_path");

    if (enableSubPaths)
    {
       spanNode.style.display = "inline";
    }
    else
    {
       spanNode.style.display = "none";
    }

    var subPathInputNode = document.createElement("input");
    spanNode.appendChild(subPathInputNode);

    subPathInputNode.id = "subpath-additionalfile"+i;
    subPathInputNode.value = '.';
    subPathInputNode.setAttribute("type", "text");
    subPathInputNode.setAttribute("name", "subpath_additionalfile[]");
    subPathInputNode.setAttribute("pattern", "<?php echo Pass::SUB_PATH_PATTERN; ?>");

    spanNode.appendChild(document.createTextNode("/"));

    var statusNode = document.createElement("span");
    parNode.appendChild(statusNode);
    statusNode.id = "filestatus-additionalfile"+i;
    statusNode.classList.add("additionalfilestatus");
    statusNode.appendChild(document.createTextNode("No file selected."));

    var spanNode = document.createElement("span");
    var inputNode = document.createElement("input");
    inputNode.id = "additionalfile"+i;
    inputNode.setAttribute("name", "additionalfile[]");
    inputNode.setAttribute("type", "file");
    inputNode.classList.add("additionalfile");
    inputNode.classList.add("visually-hidden");
    inputNode.addEventListener('change', checkAdditionalFile);
    parNode.appendChild(inputNode);

    parNode.appendChild(document.createTextNode(' '));

    var langNode = document.createElement("select");
    langNode.id = "lang-additionalfile"+i;
    langNode.setAttribute("name", "additionalfilelang[]");
    parNode.appendChild(langNode);

<?php
   foreach ($pass->getParam('filetypes') as $filetype)
   {
?>
      var opt = document.createElement('option');
      opt.value = '<?php echo htmlentities($filetype); ?>';
      opt.innerHTML = opt.value;
      langNode.appendChild(opt);
<?php
   }
?>
 }

 var isAdvancedUpload = function()
 {
    var div = document.createElement('div');
    return (('draggable' in div) || ('ondragstart' in div && 'ondrop' in div)) && 'FormData' in window && 'FileReader' in window;
 }();

 var uploaderForm = document.getElementById("uploader");

 if (isAdvancedUpload)
 {
    uploaderForm.classList.add('has-advanced-upload');
    uploaderForm.addEventListener('submit', function submitUpload(evt) { advancedUploadSubmit(evt); }, false);

    uploadedFiles = [ <?php
      for ($i = 0; $i < $num_required_files; $i++) 
      {
         if ($i > 0) echo ', ';
         echo 'null';
      }?> ];
    uploadedFilesSubPaths = [ <?php
      for ($i = 0; $i < $num_required_files; $i++) 
      {
         if ($i > 0) echo ', ';
         echo 'null';
      }?> ];

    uploadedReports = [ <?php
      for ($i = 0; $i < $num_reports; $i++) 
      {
         if ($i > 0) echo ', ';
         echo 'null';
      }?>  ];
    uploadedReportsSubPaths = [ <?php
      for ($i = 0; $i < $num_reports; $i++) 
      {
         if ($i > 0) echo ', ';
         echo 'null';
      }?>  ];

    const numExtraFields = <?php echo $pass->getParam('num_extra_files'); ?>;
    uploadedAdditionalFiles = Array(numExtraFields).fill(null);
    additionalFilesLang = Array(numExtraFields).fill(null);
    additionalSubPaths = Array(numExtraFields).fill(null);

    // Don't allow drop on rest of the page

    document.addEventListener("dragenter", defaultDragHandler, false);
    document.addEventListener("dragover", defaultDragHandler, false);
    document.addEventListener("dragstart", defaultDragHandler, false);
    document.addEventListener("drag", defaultDragHandler, false);
    document.addEventListener("dragend", defaultDragHandler, false);
    document.addEventListener("dragleave", defaultDragHandler, false);
    document.addEventListener("drop", defaultDragHandler, false);

    let dropbox;

    dropbox = document.getElementById("dropbox");
    dropbox.addEventListener("dragenter", addDragOver, false);
    dropbox.addEventListener("dragover", addDragOver, false);

    dropbox.addEventListener("dragend", removeDragOver, false);
    dropbox.addEventListener("dragleave", removeDragOver, false);

    dropbox.addEventListener("drop", dropHandler, false);

    var multifilesSelector = document.getElementById("multifiles");

    multifilesSelector.addEventListener('change',
       (event) =>  
       {
          var numFiles = 0;
	  var total = multifilesSelector.files.length;

          for (var i = 0; i < total; i++)
	  {
             if (checkAndAppendFile(multifilesSelector.files[i]))
             {
                numFiles++;
             }
	  }

          var elem = document.getElementById("drop_status");

          if (numFiles === 1)
          {
             elem.innerHTML = "<?php echo CHECKMARK; ?> 1 file added.";
          }
          else if (numFiles > 1)
          {
             elem.innerHTML = "<?php echo CHECKMARK; ?> "+numFiles+" files added.";
          }
          else
          {
             elem.innerHTML = "No files added.";
          }

          if (numFiles != total)
          {
             var numOmitted = total-numFiles;

	     elem.innerHTML += " <?php echo CROSSMARK; ?> "+numOmitted+" ";

             if (numOmitted == 1)
             {
                elem.innerHTML += "file";
             }
             else
             {
		elem.innerHTML += "files";
             }

	     elem.innerHTML += " omitted (" + forbiddenReason() + ").";
          }
       }, false);
 }
 else
 {
    uploaderForm.addEventListener('submit', validateForm, false);
 }

 function forbiddenReason()
 {
    return "forbidden type or filename contains forbidden characters<?php
      if (isset($assignment['resourcefiles']))
      {
         echo ' or filename conflicts with supplied project file'; 
      }

      if (isset($assignment['resultfiles']))
      {
         echo ' or filename conflicts with a file that your application needs to create'; 
      }
    ?>";
 }

 function dropHandler(event)
 {
    event.stopPropagation();
    event.preventDefault();
    uploaderForm.classList.remove('is-dragover');

    var numFiles = 0;
    var total;

    if (event.dataTransfer.items)
    {
       total = event.dataTransfer.items.length;

       for (var i = 0; i < event.dataTransfer.items.length; i++)
       {
          if (event.dataTransfer.items[i].kind === 'file')
          {
             if (checkAndAppendFile(event.dataTransfer.items[i].getAsFile()))
             {
                numFiles++;
             }
          }
       }
    }
    else
    {
       total = event.dataTransfer.files.length;

       for (var i = 0; i < event.dataTransfer.files.length; i++)
       {
          if (checkAndAppendFile(event.dataTransfer.files[i]));
          {
             numFiles++;
          }
       }
    }

    var elem = document.getElementById("drop_status");

    if (numFiles === 1)
    {
       elem.innerHTML = "<?php echo CHECKMARK; ?> 1 file added.";
    }
    else if (numFiles > 1)
    {
       elem.innerHTML = "<?php echo CHECKMARK; ?> "+numFiles+" files added.";
    }
    else
    {
       elem.innerHTML = "No files added.";
    }

    if (numFiles != total)
    {
       var numOmitted = total-numFiles;

       elem.innerHTML += " <?php echo CROSSMARK; ?> "+numOmitted+" ";

       if (numOmitted == 1)
       {
          elem.innerHTML += "file";
       }
       else
       {
          elem.innerHTML += "files";
       }

       elem.innerHTML += " omitted (" + forbiddenReason() + ").";
    }
 }

 const requiredFiles = [<?php 
   $sep = '';
   if (isset($assignment['mainfile']))
   {
      echo '"', htmlentities(basename($assignment['mainfile'])), '"';
      $sep = ', ';
   }

   if (isset($assignment['files']))
   {
      foreach ($assignment['files'] as $file)
      {
         echo $sep, '"', htmlentities(basename($file)), '"';
         $sep = ', ';
      }
   }
?>];

const reportFiles = [<?php 
   $sep = '';

   if (isset($assignment['reports']))
   {
      foreach ($assignment['reports'] as $val)
      {
         echo $sep, '"', htmlentities($val), '"';
         $sep = ', ';
      }
   }
?>];

 function checkAndAppendFile(file)
 {
    var fileName = file.name;

    for (var i = 0; i < requiredFiles.length; i++)
    {
       if (fileName === requiredFiles[i])
       {
          var statusElem = document.getElementById("filestatus-file"+i);

          uploadedFiles[i] = file;
	  uploadedFilesSubPaths[i] = document.getElementById('subpath-file'+i);
          statusElem.innerHTML = '<?php echo CHECKMARK; ?>';

          uploaderForm.classList.remove('is-error');

          return true;
       }
    }

    for (var i = 0; i < reportFiles.length; i++)
    {
       if (fileName === reportFiles[i] || fileName === reportFiles[i]+'.pdf'
          || fileName === reportFiles[i]+'.doc'
          || fileName === reportFiles[i]+'.docx')
       {
          var statusElem = document.getElementById("filestatus-report"+i);

          uploadedReports[i] = file;
	  uploadedReportsSubPaths[i] = document.getElementById('subpath-reportfile'+i);
          statusElem.innerHTML = '<?php echo CHECKMARK; ?>';

          uploaderForm.classList.remove('is-error');

          return true;
       }
    }

    if (isFileNameValid(fileName))
    {
       const lang = getFileLanguage(fileName);
       elemList = document.getElementsByClassName("additionalfile");

       uploaderForm.classList.remove('is-error');

       var index = uploadedAdditionalFiles.findIndex(elem => elem && elem.name === fileName);

       if (index > -1)
       {
          uploadedAdditionalFiles[index] = file;
	  return true;
       }

       for (var i = 0; i < elemList.length; i++)
       {
          if (!elemList[i].value)
	  {
             var id = elemList[i].id;

             var statusElem = document.getElementById("filestatus-"+id);

	     if (statusElem.innerHTML === 'No file selected.')
	     {
                statusElem.innerHTML = '';

                uploadedAdditionalFiles[i] = file;
                statusElem.innerHTML = fileName + ' <?php echo CHECKMARK; ?>';

                additionalSubPaths[i] = document.getElementById("subpath-"+id);

                var langElem = document.getElementById("lang-"+id);
		additionalFilesLang[i] = langElem;

		if (lang)
		{
                   var opts = langElem.options;

                   for (var i = 0; i < opts.length; i++)
                   {
                      if (opts[i].value === lang)
                      {
                         langElem.selectedIndex = i;
                         break;
                      }
                   }
		}

	        return true;
	     }
	  }
       }

       var subPathsButton = document.getElementById("sub_paths");
       var enableSubPaths = subPathsButton.checked;
       var div = document.getElementById("supplementalfiles");

       var id = "filestatus-additionalfile"+elemList.length;
       var langId = "lang-additionalfile"+elemList.length;
       var subPathId = "subpath-additionalfile"+elemList.length;
 
       addExtraField(div, elemList.length, enableSubPaths);

       var statusElem = document.getElementById(id);

       statusElem.innerHTML = '';
       uploadedAdditionalFiles.push(file);
       statusElem.innerHTML = '<?php echo CHECKMARK; ?> '+fileName;

       var langElem = document.getElementById(langId);
       additionalFilesLang.push(langElem);
       additionalSubPaths.push(document.getElementById(subPathId));

       if (lang)
       {
          var opts = langElem.options;

          for (var i = 0; i < opts.length; i++)
          {
             if (opts[i].value === lang)
             {
                langElem.selectedIndex = i;
                break;
             }
          }
       }

       return true;
    }

    return false;
 }

 function defaultDragHandler(event)
 {
    event.stopPropagation();
    event.preventDefault();
 }

 function addDragOver(event)
 {
    event.stopPropagation();
    event.preventDefault();
    uploaderForm.classList.add('is-dragover');
 }

 function removeDragOver(event)
 {
    event.stopPropagation();
    event.preventDefault();
    uploaderForm.classList.remove('is-dragover');
 }
</script>
<?php
}

function sub_path_field($name, $idnum)
{
   global $pass;

   $id = $name;

   if (preg_match('/^(.*)\[\]$/', $name, $matches))
   {
      $id = $matches[1];
   }

   $id = "subpath-$id$idnum";

   echo '<span class="sub_path" style="display: ',
       has_sub_paths() ? 'inline' : 'none',
      ';">', $pass->form_input_textfield("subpath_$name", 
      'pattern="' . Pass::SUB_PATH_PATTERN . '" id="' . $id . '"', $idnum), '/</span>';
}

function has_sub_paths()
{
   global $pass;
   return $pass->isBoolParamOn('sub_paths');
}

function file_row($file, $name, $id, $projectfile=false)
{
   global $pass;
   $info = pathinfo($file);

   echo '<p>';

   $subpath_field = str_replace('[]', '', "subpath_$name");

   $idnum = 0;
   $type = 'file';

   if (preg_match('/([a-z]+)(\d+)$/', $id, $matches))
   {
      $type = $matches[1];
      $idnum = $matches[2];

      if (!$pass->isParamElementSet($subpath_field, $idnum))
      {
         if ($info['dirname'])
         {
            $pass->setParamElement($subpath_field, $idnum, $info['dirname']);
         }
         else
         {
            $pass->setParamElement($subpath_field, $idnum, '.');
         }
      }
   }

   sub_path_field($name, $idnum);

   echo '<label for="', $id, '"><span class="file">', htmlentities($info['basename']),
	   '</span> <span class="btn">', OPENFOLDER, 
	   '</span></label> <span class="filestatus" id="filestatus-', $id,
           '">No file selected.</span>';

   echo '<input type="file" name="', $name, '" id="', $id, '" class="visually-hidden';

   if ($projectfile)
   {
      echo ' projectfile';
   }

   echo '" ';

   if ($name === 'reportfile[]')
   {
      echo 'accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"';
   }
   elseif ($info['extension'])
   {
      if ($info['extension'] === 'pdf')
      {
         echo 'accept=".pdf,application/pdf"';
      }
      elseif ($info['extension'] === 'doc')
      {
         echo 'accept=".doc,application/msword"';
      }
      elseif ($info['extension'] === 'docx')
      {
         echo 'accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"';
      }
      else
      {
         echo 'accept="text/*,.', htmlentities($info['extension']), '"';
      }
   }

   echo '> ';

?>
<script>
document.getElementById('<?php echo $id; ?>').addEventListener('change',
   (event) => 
   {
      document.getElementById("drop_status").innerHTML = '';

      var statusElem = document.getElementById('<?php echo "filestatus-$id"?>');

      var fileName = event.currentTarget.value;

      var file = event.currentTarget.files[0];

      const expectedFileName = '<?php echo htmlentities(basename($file)); ?>';

      if (fileName)
      {
         var n = fileName.lastIndexOf('\\');

         if (n > 0)
         {
            fileName = fileName.substr(n+1);
         }

<?php 

   if ($type === 'file')
   {
?>
         if (fileName === expectedFileName)
         {
            statusElem.innerHTML = '<?php echo CHECKMARK; ?>';
            uploadedFiles[<?php echo $idnum; ?>] = file;
	    uploadedFilesSubPaths[<?php echo $idnum; ?>] = document.getElementById('subpath-file<?php echo $idnum; ?>');
         }
<?php
   }
   else
   {
?>
         if (fileName === expectedFileName || fileName === expectedFileName+".pdf"
          || fileName === expectedFileName+".doc"
          || fileName === expectedFileName+".docx")
         {
            statusElem.innerHTML = '<?php echo CHECKMARK; ?>';
            uploadedReports[<?php echo $idnum; ?>] = file;
	    uploadedReportsSubPaths[<?php echo $idnum; ?>] = document.getElementById('subpath-reportfile<?php echo $idnum; ?>');
         }
<?php
   }
?>
         else
         {
            event.currentTarget.value = '';
            statusElem.innerHTML = '';
	    statusElem.appendChild(document.createTextNode('<?php 
               echo CROSSMARK; 
             ?> invalid filename "'+fileName +'". Please select the file "' + expectedFileName + '" (case-sensitive).'));
         }
      }
   });
</script>
<?php
}

function additional_file_row($id)
{
   global $pass;

   if (!$pass->isParamElementSet('subpath_additionalfile', $id))
   {
      $pass->setParamElement('subpath_additionalfile', $id,  '.');
   }

?>
<p><label for="additionalfile<?php echo $id; ?>">Supplementary File <?php echo $id+1, ' <span class="btn">', OPENFOLDER; ?></span></label> 
<?php sub_path_field('additionalfile[]', $id); ?>
<span id="filestatus-additionalfile<?php echo $id; ?>" class="additionalfilestatus">No file selected.</span>
<input onchange="checkAdditionalFile(event)" type="file" name="additionalfile[]" class="additionalfile visually-hidden" id="additionalfile<?php echo $id; ?>" >
<?php
   echo $pass->form_input_select('additionalfilelang[]', $pass->getParam('filetypes'),
    $pass->isParamElementSet('assignmentdata', 'language') ? $pass->getParamElement('assignmentdata', 'language') : 'Plain Text', '', false, ['id'=>"lang-additionalfile$id"]);
}

function group_members()
{
   global $pass;
   $groupnum = (int)$pass->getParam('groupnumber');
   $issolo = ($groupnum === 1 ? true : false);
   $regnum = $pass->getUserRegNum();

   if ($issolo)
   {
?>
<h2>Solo Project</h2>
<p>If this should be a group project, go back to the previous page and change the group number, otherwise continue.
<?php
   }
   else
   {
?>
<h2>Project Group Members</h2>
<p>Please ensure that all members of the group have created an account and have set their registration number before proceeding.
<?php
   }

   echo $pass->start_form();

   echo $pass->form_next_button(['value'=>'assignmentfiles', 
	   'class'=>'visually-hidden', 'tabindex'=>-1]);

   echo '<table><th></th><th>Blackboard ID</th>';

   for ($i = 0; $i < $groupnum; $i++)
   {
      echo student_row($i);
   }
?>
   </table>
<?php

   echo $pass->form_input_hidden('assignment');
   echo $pass->form_input_hidden('course');
   echo $pass->form_input_hidden('courseurl');
   echo $pass->form_input_hidden('groupnumber');
?>
 <p>
<?php echo $pass->form_prev_button(['value'=>'listassignments']); ?>
   <span class="spacer"></span>
<?php echo $pass->form_next_button(['value'=>'assignmentfiles']); ?>
</form>
<?php
}

function student_row($index)
{
   global $pass;
   $username = $pass->getUserName();

   if ($pass->isParamArray("blackboardid"))
   {
      if ($pass->isParamElement('blackboardid', $index, $username))
      {
         $isuser = true;
      }
      else
      {
         $isuser = false;
      }
   }
   else
   {
      $isuser = true;
   }

?>
   <tr>
<?php
   if ($isuser)
   {
      echo "<th>Student ";

      if ($pass->getParam('groupnumber') > 1)
      {
         echo $index+1;
      }

      echo "</th><td>", htmlentities($username), "</td>";
      echo '<input type="hidden" name="blackboardid[]" value="',
             htmlentities($username), '">';
   }
   else
   {
?>
<th><label for="blackboardid<?php echo $index; ?>">Student
<?php

      if ($pass->getParam('groupnumber') > 1)
      {
         echo $index+1;
      }

?>
   </label></th>
   <td>
<?php
      $name = 'blackboardid';
      $key = null;

      if ($pass->getParam('groupnumber') > 1)
      {
         $name .= '[]';
         $key = $index;
      }

      echo $pass->form_input_username($name, ['id'=>"blackboardid$index",
      'required'=>true], $key);

      echo '</td>';
   }

   echo '</tr>';
}

function fetch_assignment()
{
   global $pass;
   return $pass->fetch_assignment($pass->getParam('course'), $pass->getParam('assignment'), $pass->getParam('courseurl'));
}

function fetch_courseurl()
{
   global $pass;

   $href = $pass->fetch_courseurl($pass->getParam('course'));

   if ($href !== false)
   {
      $pass->setParam('courseurl', $href);
      return true;
   }

   return false;
}

function list_assignments()
{
   global $pass;

   if (!$pass->isParamSet('courseurl'))
   {
      if (!fetch_courseurl())
      {
         echo "<p>Unknown course.</p>";

         list_courses();
         return;
      }
   }

   $assignments = $pass->load_assignments($pass->getParam('course'), $pass->getParam('courseurl'));

   if (empty($assignments))
   {
?>
<p>No assignment data available. Please contact your lecturer.
There may be a connection issue or an invalid reference to the course's XML file
or missing/invalid information in the assignment XML file.
<?php
      return;
   }

   $pass->start_form();

   echo $pass->form_next_button(['value'=>'groupmembers', 
	   'class'=>'next visually-hidden', 'tabindex'=>-1]);

?>
<label for="assignment">'<?php echo htmlentities($pass->getParam('course')); 
?>' Assignment:</label>
<select name="assignment" id="assignment">
<option value="---">--Please Select--</option>
<?php

   foreach ($assignments as $assignment)
   {
      echo '<option value="', htmlentities($assignment['name']), '"';

      if ($pass->isParam('assignment', $assignment['name']))
      {
         echo ' selected';
      }

      echo ' >';

      echo htmlentities($assignment['title']);

      echo '</option>';
   }

   echo '</select>';

   $pass->element_error_if_set('assignment');

?>
  <span id="due"></span>
  <p>
   <label for="groupnumber">Number of Students in Project (1 for solo projects): </label>
<?php

   echo $pass->form_input_number('groupnumber', 
           ['id'=>'groupnumber', 'min'=>1]);
   echo $pass->form_input_hidden('course');
   echo $pass->form_input_hidden('courseurl');

   echo '<p>';

   echo $pass->form_prev_button(['value'=>'listcourses']);

   echo ' <span class="spacer"></span> ';

   echo $pass->form_next_button(['class'=>'next', 'value'=>'groupmembers']);
?>
</p>
</form>
<script>
const groupNumberElement = document.getElementById('groupnumber');
groupNumberElement.addEventListener("change", (event) =>
 {
    var nextElements = document.getElementsByClassName("next");

    if (nextElements)
    {
       for (var i = 0; i < nextElements.length; i++)
       {
          if (groupNumberElement.value == 1)
          {
             nextElements[i].value='assignmentfiles';
          }
          else
          {
             nextElements[i].value='groupmembers';
          }
       }
    }
 }
);

const assignmentElement = document.getElementById('assignment');
var dueElement = document.getElementById('due');

const dueDates = {"---":''
<?php
   foreach ($assignments as $assignment)
   {
      echo ', "', htmlentities($assignment['name']), '":"Due: ', htmlentities($assignment['due']), '"';
   }
?>
};

assignmentElement.addEventListener('change', (event) =>
 {
    dueElement.innerHTML = dueDates[assignmentElement.value];
 });

window.addEventListener('load', function()
 {
    var nextElements = document.getElementsByClassName("next");

    if (nextElements)
    {
       for (var i = 0; i < nextElements.length; i++)
       {
          if (groupNumberElement.value == 1)
          {
             nextElements[i].value='assignmentfiles';
          }
          else
          {
             nextElements[i].value='groupmembers';
          }
       }
    }
 }
);
</script>
<?php
}

function list_courses()
{
   global $pass;

   $pass->printBlurb();

   if ($pass->isDebugCourseEnabled())
   {
      if ($pass->isDebugMode(-1, true))
      {
         echo "<p>Courses with debug=\"true\" setting only available for staff/admin users.";
      }
      else
      {
         $pass->debug_message("Dummy course only available in debug mode. The 'Hello World Java' assignment just needs a file called 'HelloWorld.java'");
      }
   }

   $pass->start_form();

   $courses = $pass->fetch_courses();

?>
   <label for="course">Course:</label> 
   <select id="course" name="course">
   <option value="---">--Please Select--</option>
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

   echo '</select>';

   $pass->element_error_if_set('course');

   echo $pass->form_input_hidden('courseurl', ['id'=>'courseurl']), ' ';

?>
<noscript>
<?php
   echo $pass->form_next_button(['formaction'=>$pass->getUploadFallbackRef(),
     'value'=>'listassignments']);
?>
</noscript>
<?php
   echo $pass->form_next_button(['id'=>'next', 'value'=>'listassignments', 'style'=>'display: none;']);

   echo '</form>';

?>
<script>
   var nextelem = document.getElementById("next");
   nextelem.style.display = "inline";

const courseelem = document.getElementById("course");

courseelem.addEventListener("change", (event) =>
 {
    var courseurlelem = document.getElementById("courseurl");
    courseurlelem.value = getCourseUrl(event.target.value);
 });

function getCourseUrl(label)
{
<?php 
   foreach ($courses as $resource)
   {
      echo ' if (label === "', htmlentities($resource['name']), 
       '") { return "', htmlentities($resource['href']), '"; }', "\n";

      echo ' else';
   }

   echo ' { return ""; }', "\n";
?>
}
</script>
<?php
}

?>
