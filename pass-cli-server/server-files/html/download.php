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

 * Download log/pdf file created by PASS.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Download');

$pass->require_login();// ensure user is logged in

process_params();

if ($pass->has_errors())
{
   $pass->page_header();

   if ($pass->has_errors())
   {
      $pass->print_errors();
   }

   $pass->page_footer();
}
else
{
   $pass->check_site_mode();// check not in maintenance mode

   header('Content-Description: File Transfer');
   header('Content-Type: ' . ($pass->isParam('type', 'pdf') ? 'application/pdf' : 'text/plain'));
   header('Content-Disposition: attachment; filename="' . basename($pass->getParam('filename') . '"'));
   header('Expires: 0');
   header('Cache-Control: must-revalidate');
   header('Pragma: public');
   header('Content-Length: ' . filesize($pass->getParam('filename')));
   readfile($pass->getParam('filename'));
   exit;
}

function process_params()
{
   global $pass;

   $pass->get_int_variable('id');// submission id
   $pass->get_int_variable('uid');// user id
   $pass->get_matched_variable('type', '/^(log|pdf)$/');

   $pass->check_not_empty('id');
   $pass->check_not_empty('type');

   if ($pass->has_errors())
   {
      return;
   }

   $filter = array('submission_ids'=>$pass->getParam('id'));

   if ($pass->isParamSet('uid'))
   {
      $filter['user_id'] = $pass->getParam('uid');
   }

   $data = $pass->getUploads($filter);

   if (empty($data) || !isset($data[$pass->getParam('id')]))
   {
      $pass->add_error_message("Can't find upload data for submission ID " . $pass->getParam('id') . " associated with user ID " . ($pass->isParamSet('uid') ? $pass->getParam('uid') : $pass->getUserID()));
      return;
   }

   $data_row = $data[$pass->getParam('id')];

   $role = $pass->getUserRole();

   if ($role === 'student')
   {
      if (!isset($data_row['users'][$pass->getUserID()]))
      {
         $pass->add_error_message("You aren't listed as an author of submission " 
	      . $pass->getParam('id'));
	 return;
      }
   }

   $uploaded_by = $data_row['uploaded_by'];

   $username = $data_row['users'][$uploaded_by]['username'];
   $assignment = $data_row['assignment'];
   $timestamp = $data_row['upload_time'];
   $token = $data_row['token'];

   $completed_dir = $pass->getCompletedPath() . "/${timestamp}_${token}"; 

   if ($pass->isParam('type', 'log'))
   {
      $pass->setParam('filename', "$completed_dir/pass.log");
   }
   else
   {
      $pass->setParam('filename', "$completed_dir/${username}_${assignment}.pdf");
   }

   if (!file_exists($pass->getParam('filename')))
   {
      if ($role === 'admin')
      {
         $pass->add_error_message("File doesn't exist: ".htmlentities($pass->getParam('filename')));
      }
      else
      {
         $pass->add_error_message("File doesn't exist");
      }
   }
}

?>
