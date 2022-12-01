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

 * Process logs page.
 */
require_once $_SERVER['DOCUMENT_ROOT'].'/../inc/Pass.php';

$pass = new Pass('Logs');
$pass->require_admin();// ensure user is logged in as admin

$pass->page_header();
?>
<h2>Configuration</h2>
<p>Maximum file uploads: 
<?php

   echo ini_get('max_file_uploads');

   ?>. For other configuration settings, see <?php echo $pass->get_admin_phpinfo_link(); ?>.

   <h2>nohup.out</h2>
   <p>The <span class="file">nohup.out</span> file contains any messages written to 
   STDOUT/STDERR by the <span class="file">passconsumer.php</span> script that 
   pops jobs from the queue and starts up the Docker container.

   <p>Common error messages are listed in the <?php echo $pass->get_faq_link(); ?>.
   Errors involving PhpAmqpLib relate to RabbitMQ (the queuing system)
   and most likely mean that the connection to RabbitMQ has failed.
   The error "MySQL server has gone away" means that the connection
   to the database backend has failed. This is usually as a result of too many 
   connections (such as a high volume of users). The maximum number
   of connections is given by the MySQL setting <code>max_connections</code>.
<?php

   $log = $pass->getDockerPath().'/nohup.out';

   if (file_exists($log))
   {
      $sz = filesize($log);

      if ($sz === false)
      {
?>
<p>Unable to read the size of file <span class="file"><?php echo htmlentities($log); ?></span>.
<?php
      }
      elseif ($sz === 0)
      {
?>
<p>File empty. This most likely means that nothing has gone wrong, but it's possible for the process to terminate without printing any error messages.
<?php
      }
      else
      {
         echo '<p>File size: ', human_filesize($sz), '. ';

         $fh = fopen($log, "r");

         if ($fh)
         {
            echo '<pre>';

            while (($line = fgets($fh)) !== false)
            {
               if (preg_match('/^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}) #(\d+) (.*)$/',
                    $line, $matches))
               {
                  echo $matches[1], ' ';

                  echo $pass->get_upload_lists_link('#'.$matches[2], null,
                    'submission_id='.$matches[2]);

                  echo ' ', $matches[3];
               }
               else
               {
                  echo htmlentities($line);
               }

               echo PHP_EOL;
            }

            echo '</pre>';

            fclose($fh);
         }
         else
         {
?>
<p>Unable to open file <span class="file"><?php echo htmlentities($log); ?></span>.
<?php
         }
      }
   }
   else
   {
?>
<p>File <span class="file"><?php echo htmlentities($log); ?></span>  doesn't exist or isn't readable. Check that passconsumer is running under nohup. It may be that the file doesn't have read permissions set for the web server.
<?php
   }

?>
   <h2>passdocker.log</h2>
   <p>The <span class="file">passconsumer.php</span> script writes a line to the
   <span class="file">passdocker.log</span> file every time it starts and finishes a job.
<?php

   $num_failed = 0;
   $log = $pass->getDockerPath().'/passdocker.log';

   if (filesize($log) === 0)
   {
      echo '<p>File empty. This most likely means that it was cleared following a reset of the backend and there have been no new jobs.';
   }
   elseif (file_exists($log))
   {
      $fh = fopen($log, "r");

      if ($fh === false)
      {
?>
<p>Unable open file for read access.
<?php
      }
      else
      {
         $lines = 0;
	 $truncated = false;
         $content = array();
	 $nonzero_submissions = array();
	 $current_submission = null;

	 while (($line = fgets($fh)) !== false)
	 {
            $lines++;

            if (count($content) >= 100)
	    {
               array_shift($content);
	       $truncated = true;
	    }

	    array_push($content, $line);

	    if (preg_match('/INFO: Starting submission ID (\d+)/', $line, $matches))
	    {
               if (isset($current_submission))
               {
                  echo "<p>l.$lines Start of submission ", $matches[1],
	            ' found before end of submission ', $current_submission['id'];
               }

               $current_submission = array('id' => $matches[1]);
	    }
	    elseif (preg_match('/INFO: Finished submission ID (\d+). Exit code: (\d+)/', $line, $matches))
	    {
               if (!isset($current_submission))
	       {
                  echo "<p>l.$lines End of submission ", $matches[1], ' found but no start.';
	       }
	       elseif ($matches[1] !== $current_submission['id'])
	       {
                  echo "<p>l.$lines End of submission ", $matches[1], ' doesn\'t match start of submission ', $current_submission['id'];
		  $current_submission = null;
	       }
	       else
	       {
                  $current_submission['exit_code'] = $matches[2];

		  if ($matches[2] != 0)
		  {
                     array_push($nonzero_submissions, $current_submission);
		  }

		  $current_submission = null;
	       }
	    }
	    elseif (isset($current_submission))
	    {
               if (isset($current_submission['content']))
	       {
                  $current_submission['content'] .= $line;
	       }
	       else
	       {
                  $current_submission['content'] = $line;
	       }
	    }
	    elseif ($line)
	    {
               echo "<p>l.$lines $line";
	    }
	 }

         fclose($fh);

         if (empty($content))
         {// shouldn't happen as already checked
?>
<p>File empty. This could mean that the passconsumer process has been restarted and hasn't yet found any messages on the queue.
<?php
         }
         else
         {
            echo "<p>File $lines ", $lines === 1 ? 'line' : 'lines', " long.";

	    if (isset($current_submission))
	    {
               echo '<p>Final submission ', $current_submission['id'], " hasn't finished.";
	    }

	    $num_failed = count($nonzero_submissions);

	    if ($num_failed > 0)
	    {
?>
<div>
 <button type="button" class=collapsible><?php echo $num_failed; ?> Failed <?php echo $num_failed === 1 ? 'Job' : 'Jobs'; ?></button>
 <div class="content">
 <pre>
<?php 
               foreach ($nonzero_submissions as $submission)
               {
                  echo 'Submission ', 
                     $pass->get_upload_lists_link($submission['id'], null, 
		       'submission_id='.$submission['id']),
		     ' failed with exit code ', $submission['exit_code'], PHP_EOL;

		  if (isset($submission['content']))
		  {
                     echo htmlentities($submission['content']);
		  }

		  echo PHP_EOL;
	       }
?>
 </pre>
 </div>
</div>
<?php
	    }

            if ($truncated)
            {
               echo "<p>Final ", count($content), ' lines:';
            }

            echo '<pre>', htmlentities(implode('', $content)), '</pre>';
         }
      }
   }
   else
   {
?>
<p>File <span class="file"><?php echo htmlentities($log); ?></span> doesn't exist or isn't readable. This could mean that the passconsumer process hasn't been started or the file doesn't have read permissions set for the web server.
<?php
   }

   if ($num_failed > 0)
   {
      $pass->writeCollapsibleJavaScript();
   }

$pass->page_footer();

?>
