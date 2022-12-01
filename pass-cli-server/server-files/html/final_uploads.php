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

 * Final uploads page.
 */

require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Final Uploads');

$pass->require_login();// ensure user is logged in

if ($pass->isUserStaffOrAdmin())
{
   process_params();

   $pass->page_header();

   if ($pass->has_errors())
   {
      $pass->print_errors();
   }

   show_list();

   $pass->page_footer();
}
else
{
   $pass->page_header();
   echo "<p>You don't have permission to view this page.";
   $pass->page_footer();
}

function process_params()
{
   global $pass;

   $pass->get_matched_variable('action', '/^(showform|search)$/', 'showform');

   $pass->get_int_variable('page', 1);

   $pass->get_htmlentities_variable('usernames');

   $courses = $pass->load_courses();
   $pass->get_choice_variable('course', array_keys($courses), '---');

   if (!$pass->isParam('course', '---'))
   {
      $assignments = $pass->fetch_assignments($pass->getParam('course'));

      if ($assignments)
      {
         $pass->get_choice_variable('assignment', array_keys($assignments), '---');
      }
   }

   if (!$pass->isParamSet('assignment'))
   {
      $pass->setParam('assignment', '---');
   }
}

function show_list()
{
   global $pass;

   $filters = null;

   if (!$pass->isParam('course', '---'))
   {
      $filters = array();
      $filters['course'] = $pass->getParam('course');

      if (!$pass->isParam('assignment', '---'))
      {
         $filters['assignment'] = $pass->getParam('assignment');
      }
   }

   if ($pass->isParamSet('usernames'))
   {
      $blackboardIds = explode(PHP_EOL, $pass->getParam('usernames'));

      if (!isset($filters))
      {
         $filters = array();
      }

      $filters['usernames'] = $blackboardIds;

      $users = array();

      foreach ($blackboardIds as $id)
      {
         $id = trim($id);

	 if (!empty($id))
	 {
            $users[$id] = htmlentities($id);
	 }
      }
   }

   if ($pass->isParam('action', 'showform'))
   {
     $num_uploads = false;
?>
<p>You can use this form to search for the most recent upload for each assignment or 
for a specific assignment either for all students or for just the listed students.
All form elements are optional. In order to select an assignment, you must first 
select the course.
<?php
   }
   else
   {
      $data = $pass->getFinalUploads($filters);

      if ($data === false)
      {
?>
<p>Failed to fetch upload data.
<?php
         return;
      }
      else
      {
         $num_uploads = count($data);
      }
   }

   $courses = $pass->fetch_courses();

   echo $pass->start_form();

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
   <p>
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
   <p>
   <label for="usernames"><?php echo PassConfig::USERNAMES_LABEL; ?>: </label>
   (Use a newline to separate each <?php echo PassConfig::USERNAME_LABEL;?>.)<p>
<?php 
   echo $pass->form_input_textarea('usernames', ['id'=>'usernames']);
   echo '<p>';
   echo $pass->form_submit_button(['value'=>'search'], 'Search &#x1F50D;');
?>
</form>
<?php

   if ($num_uploads > 0)
   {

?>
<p>This is a list of the most recent upload for 
<?php
      if ($pass->isParam('assignment', '---'))
      {
         echo 'each assignment';

	 if (!$pass->isParam('course', '---'))
	 {
            echo ' in course ', htmlentities($pass->getParam('course'));
	 }
      }
      else
      {
         $assignment = $pass->fetch_assignment($pass->getParam('course'), $pass->getParam('assignment'));
	 echo 'the assignment &#x201C;', htmlentities($assignment['title']),
		 '&#x201D;';
      }

      $num_users = (empty($users) ? 0 : count($users));

      if ($num_users > 1)
      {
         echo ' for the given users';
      }
      elseif ($num_users === 1)
      {
         echo ' for user ', htmlentities(trim($pass->getParam('usernames')));
      }
      else
      {
         echo ' for all users';
      }

      echo '.';
   }

   if ($num_uploads !== false)
   {
      echo "<p>", $num_uploads, ' ', ($num_uploads === 1 ? 'upload' : 'uploads'), ' found.';
   }

   if ($num_uploads)
   {
      $pagination = $pass->getListPages($num_uploads);
      $pass->setParam('page', $pagination['page']);

      if ($pagination['num_pages'] > 1)
      {
         $pageList = $pass->page_list($pagination['num_pages'], 
	      'action=search&amp;course='.urlencode($pass->getParam('course'))
	      . '&amp;assignment='.urlencode($pass->getParam('assignment'))
	      . '&amp;usernames='.urlencode($pass->getParam('usernames')));
      }
      else
      {
         $pageList = '';
      }

      echo $pageList;

?>
<table class="upload_list">
<tr>
<?php
      if ($num_users != 1)
      {
?>
 <th class="username"><?php echo PassConfig::USERNAME_LABEL; ?></th>
<?php
      }

      if ($pass->isParam('course', '---'))
      {
?>
 <th class="course">Course</th>
<?php
      }

      if ($pass->isParam('assignment', '---'))
      {
?>
 <th class="assignment">Assignment</th>
<?php
      }

?>
 <th class="upload_time">Timestamp</th>
 <th class="num_uploads">Number of Uploads</th>
</tr>
<?php
      if (isset($users))
      {
         $i = 0;

         foreach ($data as $row)
	 {
            if ($i >= $pagination['start_idx'] && $i <= $pagination['end_idx'])
	    {
               $timestamp = date_create_from_format(Pass::TIME_STAMP_FORMAT, $row['timestamp']);

?>
<tr>
<?php
      if ($num_users != 1)
      {
?>
 <td class="username"><?php echo htmlentities($row['username']); ?></td>
<?php
      }

      if ($pass->isParam('course', '---'))
      {
         echo '<td class="course">', htmlentities($row['course']), '</td>';
      }

      if ($pass->isParam('assignment', '---'))
      {
         echo '<td class="assignment">', htmlentities($row['assignment']), '</td>';
      }

?>
 <td class="upload_time"><?php echo htmlentities(date_format($timestamp, 'Y-m-d H:i:s')); ?></td>
 <td class="num_uploads"><?php 
      echo $pass->get_upload_lists_link($row['num_submissions'], null, 
	      'course='.urlencode($row['course']) 
	      . '&amp;assignment='.urlencode($row['assignment'])
              . '&amp;username='.urlencode($row['username'])); 
?></td>
</tr>
<?php
	    }

            if (isset($users[$row['username']]))
	    {
               unset($users[$row['username']]);
	    }

	    $i++;
	 }
      }
      else
      {
         for ($i = $pagination['start_idx']; $i <= $pagination['end_idx']; $i++)
         {
            if (!isset($data[$i]))
            {// pagination start or end is off
               continue;
            }

	    $row = $data[$i];
            $timestamp = date_create_from_format(Pass::TIME_STAMP_FORMAT, $row['timestamp']);
?>
<tr>
<?php
      if ($num_users != 1)
      {
?>
 <td class="username"><?php echo htmlentities($row['username']); ?></td>
<?php
      }

      if ($pass->isParam('course', '---'))
      {
         echo '<td class="course">', htmlentities($row['course']), '</td>';
      }

      if ($pass->isParam('assignment', '---'))
      {
         echo '<td class="assignment">', htmlentities($row['assignment']), '</td>';
      }

?>
 <td class="upload_time"><?php echo htmlentities(date_format($timestamp, 'Y-m-d H:i:s')); ?></td>
 <td class="num_uploads"><?php 
      echo $pass->get_upload_lists_link($row['num_submissions'], null, 
	      'course='.urlencode($row['course']) 
	      . '&amp;assignment='.urlencode($row['assignment'])
              . '&amp;username='.urlencode($row['username'])); 
?></td>
</tr>
<?php
         }
      }
?>
</table>
<?php

      echo $pageList;

      if (!empty($users))
      {
         $n = count($users);

	 echo "<p>$n ", ($n == 1 ? 'user' : 'users'), ' not found: ',
            implode(',', $users), '.'; 
      }
   }
?>

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
}

?>
