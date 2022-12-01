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

 * Who's online page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass("Who's Online");
$pass->require_admin();// ensure user is logged in

$pass->page_header();

show_list();

$pass->page_footer();

function show_list()
{
   global $pass;

   $sql = "SELECT * FROM whos_online ORDER BY time_last_clicked DESC";

   $result = $pass->db_query($sql);

   if ($result)
   {
?>
<p>Users are automatically removed from the database table if
they have been inactive for an hour or more. The table doesn't include visitors
who aren't logged in.
<table>
<tr><th>User ID</th><th>Logged in Since</th><th>Last Active</th><th>Page</th></tr>
<?php
      while ($row = mysqli_fetch_assoc($result))
      {
?>
<tr>
<td><?php
  $id = (int)$row['user_id'];

  echo $pass->get_admin_users_link($id, null, "id=$id"); 

  if ($id === $pass->getUserID()) 
  {
     echo ' (me)';
  }
?></td>
 <td><?php echo $row['start_time']; ?></td>
 <td><?php echo $row['time_last_clicked']; ?></td>
 <td class="file"><?php echo htmlentities($row['last_page_url']); ?></td>
</tr>
<?php
      }
?>
</table>
<?php
   }
   else
   {
      echo '<p>No data found.';
   }
}
?>
