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

 * Fallback upload page that doesn't require JavaScript. This page is
 * an alternative to the main upload page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Upload');

$pass->require_login();// ensure user is logged in

$role = $pass->getUserRole();
$regnum = $pass->getUserRegNum();

if ($role == 'student' && empty($regnum))
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
         $_SESSION['confirmation_message'] = 'Upload successful.';
         $pass->redirect_header($pass->getUploadListsRef());
      }
   }
   else
   {
      $pass->page_header();
   
      $pass->debug_message('Debugging mode ON.');
   
      if ($pass->has_errors())
      {
         $pass->print_errors();

         $pass->record_action('upload_fallback', sprintf('%s errors: %s',  
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
      elseif ($pass->isParam('action', 'structure'))
      {
         encoding_and_subdirs();
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

   if ($pass->isParamSet('course') && !$pass->isParamSet('courseurl')
       && !$pass->isParam('course', '---'))
   {
      fetch_courseurl();
   }

   $pass->get_htmlentities_variable('assignment', '---');

   $assignmentdata = null;

   if ($pass->isParamSet('courseurl') 
        && $pass->isParamSet('assignment')
        && !$pass->isParam('assignment', '---')
      )
   {
      $assignmentdata = fetch_assignment();
      $pass->setParam('assignmentdata', $assignmentdata);

      if (isset($assignmentdata['allowedbinaries']))
      {
         array_push($filetypes, 'BINARY');
      }
   }

   if (!($pass->isParam('action', 'listcourses') 
	   || $pass->isParam('action', 'listassignments'))
         && $pass->isParam('assignment', '---'))
   {
      $pass->add_error_message('Please select the assignment', 
        'assignment', 'Required');
      $pass->setParam('action', 'listassignments');
   }

   $pass->get_int_variable('groupnumber', 1);

   $pass->get_boolean_variable('agree', false);

   $pass->get_matched_variable('encoding', '/^(utf8|ascii|latin1)$/', 'utf8');

   $pass->get_boolean_variable('sub_paths');

   $pass->get_int_variable('num_extra_files', 0);

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
            $pass->add_error_message(
              PassConfig::USERNAME_LABEL . ' missing', 'blackboardid', 'Required');
            $pass->setParam('action', 'assignmentfiles');
         }
         elseif ($pass->isParamArray('blackboardid'))
         {
            $numid = count($pass->getParam('blackboardid'));
         }

         if (!$pass->isParam('groupnumber', $numid))
         {
	    $pass->add_error_message(sprintf(
	     "Missing one or more %s (group number: %s, number supplied: %s)", 
               PassConfig::USERNAMES_LABEL,
               $pass->getParam('groupnumber'), $numid), 
              'blackboardid', 'Required');
            $pass->setParam('action', 'assignmentfiles');
         }

	 if (has_sub_paths())
	 {
	    $pass->get_subpath_variable('subpath_file');
	    $pass->get_subpath_variable('subpath_reportfile');
	    $pass->get_subpath_variable('subpath_additionalfile');
	 }

	 if (isset($assignmentdata))
         {
            if (empty($_FILES))
	    {
               $pass->add_error_message('At least one file must be uploaded');
	    }
	    else
	    {
               $file_list = array();

	       check_files('file', $file_list);
	       check_files('additionalfile', $file_list);
	       check_files('reportfile', $file_list);
	    }

	    if ($pass->has_errors())
            {
               $pass->setParam('action', 'assignmentfiles');
            }
	 }

	 $pass->get_choice_variable('additionalfilelang', $filetypes);
      }
   }

   $pass->setParam('filetypes', $filetypes);
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
      $pass->record_action('upload_fallback', 
         "Upload directory '$uploaddir' exists", true); 

      $token = create_token();
      $dir = "${timestamp}_${token}";
      $uploaddir = $pass->getUploadPath() . "/$dir";
   }

   if (!mkdir($uploaddir, $pass->getUploadPathPermissions()))
   {
      $pass->add_error_message("Unable to create upload directory");
      error_log("Unable to create directory '$uploaddir'");
      $pass->record_action('upload_fallback',
        "Unable to create upload directory '$uploaddir'", true);
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
   
      process_uploaded_file('file', $content, $uploaddir);
      process_uploaded_file('additionalfile', $content, $uploaddir,
        $pass->getParam('additionalfilelang'));
      process_uploaded_file('reportfile', $content, $uploaddir);

      if ($pass->has_errors())
      {
         $pass->record_action('upload_fallback',
           "File upload failed " . $pass->get_error_list());
      }
   
      if (file_put_contents($settingsfile, implode(PHP_EOL, $content)) === FALSE) 
      {
         $pass->add_error_message("Failed to write settings file.");
         error_log("Failed to write '$settingsfile'");
         $pass->record_action('upload_fallback',
           "Failed to write settings file '$settingsfile'", true);
      }
      else
      {
         if (!chmod($settingsfile, $pass->getUploadFilePermissions()))
         {
            $pass->add_error_message('Failed to change file permissions for settings file.');
            error_log("Failed to change file permissions for $settingsfile to "
   	         . $pass->getUploadFilePermissions());

            $pass->record_action('upload_fallback', "Failed to chmod settings file '$settingsfile'", true);
         }
   
         $submission_id = $pass->publishUpload($userinfo, $pass->getParam('course'), $pass->getParam('assignment'), $timestamp, $token);

         $pass->record_action('upload_fallback', 
               "Successful (submission ID: $submission_id, token: '$token', upload time: $timestamp)", true);
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

         $pass->record_action('upload_fallback',
            "Failed (submission ID: $submission_id, token: '$token', upload time: $timestamp)", true);
      }
      else
      {
         echo '.';

         $pass->record_action('upload_fallback',
            "Failed (token: '$token', upload time: $timestamp)", true);
      }

      $pass->page_footer();

      return false;
   }
   else
   {
      return true;
   }
}

function process_uploaded_file($field, &$content, $uploaddir, $lang=null)
{
   global $pass;
   if (!isset($_FILES[$field])) return;

   $defLang = null;

   if (is_string($lang) && in_array($lang, $pass->getParam('filetypes')))
   {
      $defLang = $lang;
   }

   if (is_array($_FILES[$field]['name']))
   {
      $total = count($_FILES[$field]['name']);
   
      for ($i = 0; $i < $total; $i++)
      {
         $filelang = $defLang;

	 if (!empty($lang) && is_array($lang) && isset($lang[$i]))
	 {
            $filelang = $lang[$i];
	 }

         process_file($_FILES[$field]['tmp_name'][$i],
		 $_FILES[$field]['name'][$i], 
		 has_sub_paths() ? 
		    $pass->getParamElement("subpath_$field", $i) : null,
		 $content, $uploaddir, $filelang);
      }
   }
   else if (isset($_FILES[$field]['name']))
   {
      process_file($_FILES[$field]['tmp_name'],
         $_FILES[$field]['name'], 
	 has_sub_paths() ? ($pass->isParamArray("subpath_$field") ?
	    $pass->getParamElement("subpath_$field", 0) : $pass->getParam("subpath_$field")) : null,
         $content, $uploaddir, $defLang);
   }
}

function process_file($tmppath, $name, $subpath, &$content, $uploaddir, $lang=null)
{
   global $pass;
   $dir = $uploaddir;
   $relpath = $name;
   
   if ($tmppath)
   {
      if (!empty($subpath) && $subpath !== '.')
      {
         $dir = "$uploaddir/$subpath";
   
         if (file_exists($dir) && is_dir($dir))
         {
            $relpath = $subpath . '/' . $name;
         }
         else if (!mkdir($dir, $pass->getUploadPathPermissions(), true))
         {
            $pass->debug_message("Unable to create upload directory " 
             . htmlentities($dir));
            error_log("Unable to create directory '$dir'");
            $dir = $uploaddir;
         }
         else
         {
            $relpath = $subpath . '/' . $name;
         }
      }
   
      $file = "$dir/" . $name;
   
      if (move_uploaded_file($tmppath, $file))
      {
         if (!chmod($file, $pass->getUploadFilePermissions()))
         {
	    $pass->debug_message('Failed to change file permissions for '
	    . htmlentities($file) . ' to ' . $pass->getUploadFilePermissions());
	    error_log("Failed to change file permissions for $file to "
		    . $pass->getUploadFilePermissions());
         }

         $pass->debug_message("Moved uploaded file to " . htmlentities($file));

	 if (empty($lang))
	 {
            array_push($content, "File: $relpath");
	 }
         else
	 {
            array_push($content, "File: $relpath\t$lang");
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

function check_files($field, &$file_list)
{
   global $pass;

   if (isset($_FILES[$field]))
   {
      if (is_array($_FILES[$field]['name']))
      {
         $total = count($_FILES[$field]['name']);

         for ($i = 0; $i < $total; $i++)
         {
            $relpath = $_FILES[$field]['name'][$i];

	    if ($pass->isParamElementSet("subpath_$field", $i))
	    {
               $relpath = $pass->getParamElement("subpath_$field", $i) . "/$relpath";
	    }

	    if (in_array($relpath, $file_list))
	    {
               $pass->add_error_message("Multiple upload of file "
        	     . htmlentities($relpath));
	    }
            else
	    {
               check_filename($_FILES[$field]['name'][$i]);
	       array_push($file_list, $relpath);
	    }
         }
      }
      else
      {
         $relpath = $_FILES[$field]['name'];

         if (is_array($pass->getParam("subpath_$field")))
         {
            if ($pass->isParamElementSet("subpath_$field", 0))
	    {
               $relpath = $pass->getParamElement("subpath_$field", 0) . "/$relpath";
	    }
         }
         elseif ($pass->isParamSet("subpath_$field"))
         {
            $relpath = $pass->getParam("subpath_$field") . "/$relpath";
         }

         if (in_array($relpath, $file_list))
         {
            $pass->add_error_message("Multiple upload of file "
        	     . htmlentities($relpath));
         }
         else
         {
            check_filename($_FILES[$field]['name']);
	      array_push($file_list, $relpath);
         }
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

   if (!preg_match('/^[a-zA-Z0-9\-_+]+(\.[a-zA-Z0-9+\-_]+)?$/', $filename))
   {
      $pass->add_error_message("Invalid filename '" . htmlentities($filename) . "'. File names must only contain alphanumerics, plus, underscore or hyphen with a single extension.");
      return;
   }

   if ($filename === 'a.out')
   {
      $pass->add_error_message("Binary file 'a.out' forbidden.");
      return;
   }

   $ext = pathinfo($filename, PATHINFO_EXTENSION);

   if ($ext !== false 
     && preg_match('/^(zip|exe|tar|gz|tgz|jar|a|ar|iso|bz2|lz|lz4|xz|7z|s7z|cab|class|o)$/i', $ext))
   {
      $pass->add_error_message(
         "Binary file '" . htmlentities($filename). "' forbidden.");
   }
}

function encoding_and_subdirs()
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

   echo $pass->start_form();

?>
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

   echo ' ', $pass->get_faq_link(
    'What\'s the difference between ASCII, Latin 1 and UTF-8?', 'encoding',
     ['target'=>'_blank']);

   echo '<p>', $pass->form_input_boolean_checkbox('sub_paths', 
          isset($assignment['relpath']) ? $assignment['relpath'] : false, 
	   array("id"=>"sub_paths"));
?>
   <label for="sub_paths">Project has sub-directories.</label>
<?php

   $required_files = array();

   if (isset($assignment['mainfile']))
   {
      array_push($required_files, $assignment['mainfile']);
   }

   if (isset($assignment['files']))
   {
      foreach ($assignment['files'] as $file)
      {
         array_push($required_files, $file);
      }
   }

   if (isset($assignment['reports']))
   {
      foreach ($assignment['reports'] as $file)
      {
         array_push($required_files, $file . " (PDF or Word)");
      }
   }

   $num_required = count($required_files);

   if ($num_required === 1)
   {
?>
<p>For this assignment, you are required to provide a file called
<span class="file"><?php echo htmlentities($required_files[0]); ?></span>.
<p>You can upload additional files (source code or accompanying PDF/Word reports)
if you want. 
<?php
   }
   elseif ($num_required > 1)
   {
?>
<p>For this assignment, you are required to provide files called:
<ul>
<?php
      foreach ($required_files as $file)
      {
         echo '<li class="file">', htmlentities($file), '</li>';
      }
?>
</ul>
<p>You can upload additional files (source code or accompanying PDF/Word reports)
if you want.
<?php
   }
   else
   {
?>
<p>For this assignment, there are no required file names. In addition to source code,
you may also upload accompanying PDF/Word reports.
<?php
   }

   if (isset($assignment['allowedbinaries']))
   {
?>
This assignment allows you to upload binary files with  
<?php

      if ($assignment['allowedbinaries']['num_exts'] > 1)
      {
         echo ' the following extensions: ';
      }
      else
      {
         echo ' the extension ';
      }

      echo $assignment['allowedbinaries']['exts'], '. ';
   }
   else
   {
?>
No binary files are permitted (aside from PDF/Word).
<?php
   }

   if ($num_required > 0)
   {
?>
How many additional files do you want to upload?
<?php
   }
   else
   {
?>
How many files do you want to upload?
<?php
   }

   $max_files = ini_get('max_file_uploads');

   echo $pass->form_input_number("num_extra_files", ['min'=>0,
      'max'=>($max_files-$num_required)]);
?>

   <p>Maximum number of files: <?php echo $max_files; 

   if ($required_files > 0)
   {
      echo " (including the $num_required required ", $num_required===1 ? 'file' : 'files', ")";
   }

?>.

<?php
   echo $pass->form_input_hidden('groupnumber');
   echo $pass->form_input_hidden('assignment');
   echo $pass->form_input_hidden('course');
   echo $pass->form_input_hidden('courseurl');
   echo $pass->form_input_hidden('blackboardid');

   echo '<p>', $pass->form_prev_button(
     ['value'=>($issolo ? 'listassignments' : 'groupmembers')]);
?>
   <span class="spacer"></span> 
<?php
   echo $pass->form_next_button(['value'=>"assignmentfiles"]);
   echo '</form>';
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

   $pass->start_form(array('onsubmit'=>'return validateForm(this);',
      'enctype'=>"multipart/form-data"));

?>

   <h2>Project Files</h2>

   <p>File names should only contain alphanumerics (a-z, A-Z, 0-9), plus, hyphens or underscores (and a dot for the extension).
<strong>Filenames are case-sensitive.</strong>
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

<?php
   echo $pass->form_input_hidden('encoding');
   echo $pass->form_input_hidden('sub_paths');

?>

   <div id="sub_paths_note" <?php 
     echo 'style="display: ', has_sub_paths() ? 'block' : 'none', '"'; 
?>>The sub-path relative to the project's base
   directory should be provided in the field before the browse button(s) below. Omit
   the final slash. The sub-path must not start with a slash. Each path element
   must consist of only alphanumerics (a-z, A-Z, 0-9) or an underscore (<code>_</code>) or
   a hyphen (<code>-</code>). Use a single dot (<code>.</code>) to indicate
   the project's base directory.</div>

<?php 
  
   $max_files = ini_get('max_file_uploads'); 

?>

   <p>Maximum number of files: <?php echo $max_files; ?> (including any required files).

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

   if (isset($assignment['reports']))
   {
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

   if ($pass->getParam('num_extra_files') > 0)
   {
      echo '<h3>Additional Files (Optional)</h3>';

      if (!$pass->isParamSet('subpath_additionalfile'))
      {
         $pass->setParam('subpath_additionalfile', array());
      }

      echo '<div id="supplementalfiles">';

      for ($id = 0; $id < $pass->getParam('num_extra_files') ; $id++)
      {
         if (!$pass->isParamElementSet('subpath_additionalfile', $id))
         {
            $pass->setParamElement('subpath_additionalfile', $id,  '.');
         }

?>
<p><label for="additionalfile<?php echo $id; ?>">Supplementary File <?php echo $id+1; ?>:</label> 
<?php sub_path_field('additionalfile[]', $id); ?>
<input type="file" name="additionalfile[]" class="additionalfile" id="additionalfile<?php echo $id; ?>" >
<?php
         echo $pass->form_input_select('additionalfilelang[]', $pass->getParam('filetypes'),
           'Plain Text');
      }
?>

   </div>
   <div id="add_extra_area" style="display: none;">
   <button id="add_extra" type="button" onclick="addExtraFields()">Add </button>
   <input id="num_extra" type="number" min="1" value="1" />
   more file field(s).
   </div>
<?php
   }

   echo $pass->form_input_hidden('groupnumber');
   echo $pass->form_input_hidden('assignment');
   echo $pass->form_input_hidden('course');
   echo $pass->form_input_hidden('courseurl');
   echo $pass->form_input_hidden('blackboardid');
   echo $pass->form_input_hidden('num_extra_files');

?>
   <p>
   <input type="checkbox" id="agree" name="agree">
   <label for="agree"><?php echo $issolo ? 'I' : 'We'; ?> agree that by submitting a PDF generated by <?php echo $pass->getShortTitle(), ' ', $issolo ? 'I am' : 'we are'; ?> confirming that <?php echo $issolo ? 'I' : 'we'; ?> have checked the PDF and that it correctly represents <?php echo $issolo ? 'my' : 'our'; ?> submission.</label>
   <p>
<?php
    echo $pass->form_prev_button(
      [
        'value'=>'structure', 'onclick'=>'this.form.submitted=this.value;'
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
   </form>
<script>
const agreeelem = document.getElementById("agree");

 window.addEventListener('load', function()
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

    elem = document.getElementById("add_extra_area");

    if (elem)
    {
       elem.style.display="block";
    }
 });


agreeelem.addEventListener("change", (event) =>
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
 });

 function validateForm(oForm)
 {
    var fileInputs = oForm.getElementsByClassName("projectfile");
    var missing = false;

    if (oForm.submitted == "listassignments"
     || oForm.submitted == "structure"
     || oForm.submitted == "groupmembers"  || !fileInputs)
    {
       return true;
    }

    for (var i = 0; i < fileInputs.length; i++)
    {
       var fileName = fileInputs[i].value

       if (!fileName)
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

    return true;
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

    var startIdx = elems.length;
    var n = startIdx+parseInt(numExtra.value);

    for (var i = startIdx; i < n; i++)
    {
       var parNode = document.createElement("P");
       div.appendChild(parNode);

       var labelNode = document.createElement("label");
       parNode.appendChild(labelNode);

       labelNode.setAttribute("for", "additionalfile"+i);

       var textNode = document.createTextNode("Supplementary File "+(i+1)+":");

       labelNode.append(textNode);

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

       subPathInputNode.value = '.';
       subPathInputNode.setAttribute("type", "text");
       subPathInputNode.setAttribute("name", "subpath_additionalfile[]");
       subPathInputNode.setAttribute("pattern", "<?php echo Pass::SUB_PATH_PATTERN; ?>");

       spanNode.appendChild(document.createTextNode("/"));

       var spanNode = document.createElement("span");
       var inputNode = document.createElement("input");
       inputNode.id = "additionalfile"+i;
       inputNode.setAttribute("name", "additionalfile[]");
       inputNode.setAttribute("type", "file");
       inputNode.classList.add("additionalfile");
       parNode.appendChild(inputNode);
    }
 }
</script>
<?php
}

function sub_path_field($name, $idnum)
{
   global $pass;
   echo '<span class="sub_path" style="display: ',
       has_sub_paths() ? 'inline' : 'none',
      ';">', $pass->form_input_textfield("subpath_$name", 
      'pattern="' . Pass::SUB_PATH_PATTERN . '"', $idnum), '/</span>';
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

   echo '<p><label for="', $id, '"><span class="file">', htmlentities($file),
	   '</span></label> ';

   $subpath_field = str_replace('[]', '', "subpath_$name");

   if (preg_match('/(\d+)$/', $id, $matches))
   {
      $idnum = $matches[0];

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

   echo '<input type="file" name="', $name, '" id="', $id, '" ';

   if ($projectfile)
   {
      echo 'class="projectfile" ';
   }

   if ($info['extension'])
   {
      echo "accept=\"text/*,.", htmlentities($info['extension']), "\"";
   }

   echo '>';
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

   echo '<table><th></th><th>', PassConfig::USERNAME_LABEL, '</th>';

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
<?php echo $pass->form_next_button(['value'=>'structure']); ?>
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

   $pass->start_form();
?>
<label for="assignment">'<?php echo htmlentities($pass->getParam('course')); 
?>' Assignment:</label>
<select name="assignment">
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

   echo $pass->form_next_button(['id'=>'next', 'value'=>'groupmembers']);
?>
</p>
</form>
<script>
const groupNumberElement = document.getElementById('groupnumber');
groupNumberElement.addEventListener("change", (event) =>
 {
    var nextElement = document.getElementById("next");

    if (groupNumberElement.value == 1)
    {
       nextElement.value='structure';
    }
    else
    {
       nextElement.value='groupmembers';
    }
 }
);

window.addEventListener('load', function()
 {
    var nextElement = document.getElementById("next");

    if (groupNumberElement.value == 1)
    {
       nextElement.value='structure';
    }
    else
    {
       nextElement.value='groupmembers';
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

   echo $pass->form_input_hidden('courseurl', ['id'=>'courseurl']);

   echo ' ', $pass->form_next_button(['value'=>'listassignments']);

   echo '</form>';

?>
<script>
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
