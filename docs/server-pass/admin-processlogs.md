# Admin: Process Logs

The Process Logs page is available to admin users via the
**Admin** ⏵ **Process Logs** link.

If something goes wrong, you may be able to find error messages in
the process logs. The other places to look are the Action Recorder
or SSH to the server and look at the PHP error log file.

## Configuration

The configuration section tells you the maximum file uploads.
If a student tries to upload more than this number, the upload will
fail. For other configuration settings, see the PHP Info page.
If these server settings need changing, you may have to ask your IT
support for help if you don't have permission to change them. You
can't change them from the frontend.

## nohup.out

The backend runs in the background using `nohup` (no hang up).
All output that would normally go to the terminal goes to the file
`nohup.out` instead.

Error messages involving `PhpAmqpLib` relate to RabbitMQ (the
message queue) and may mean that the connection to RabbitMQ has
failed. SSH to the server and check the RabbitMQ status.

If `nohup.out` is empty, that's usually a good sign as it likely
means there have been no errors to report, but it's possible for the
backend to terminate without printing any error messages, so if jobs
are stuck in the queue and RabbitMQ still seems to be online, SSH to
the server and check the backend is still running.

## passdocker.log

The backend script writes a line to the `passdocker.log` file every
time it starts and finishes a job. For example:
<pre>
2022-11-29 15:02 INFO: Starting submission ID _ID_
2022-11-29 15:02 INFO: Finished submission ID _ID_. Exit code 0
</pre>

This means that this file can get quite large (normally at least
twice as many lines as jobs), so the Process Logs page parses the
file to detect any failed jobs. 

This section prints the number of lines in the `passdocker.log` file,
a summary of failed jobs, if any detected, and the last 100 hundred
lines of the file (or all the file if smaller).

The summary failed jobs is in a  grey collapsible area that can be toggled
(with the title "1 Failed Job" or "_N_ Failed Jobs").

For example:
<pre>
Submission _ID_ failed with exit code _N_
2022-11-29 15:39 [Submission _ID_] Process failed in container _name_
2022-11-29 15:39:52 #_ID_ CMP-101/HelloWorldJava/abc01xyz: Main language: Java
2022-11-29 15:39:52 #_ID_ CMP-101/HelloWorldJava/abc01xyz: Main file: HelloWorld.java
2022-11-29 15:39 [Submission _ID_] Failed to cp abc01xyz\_CMP-101-HelloWorldJava.pdf from container
</pre>

The lines that start with
<pre>
_timestamp_ #_ID_ _course_/_assignment_/_username_:
</pre>
are written by `pass-cli-server`. The lines that start with
<pre>
_timestamp_ [Submission _ID_]
</pre>
are written by `passconsumer.php`.
The first line for each summary block has a hyperlink on the
submission ID, which will take you to the [Upload Lists](list-uploads.md) page
with that submission ID set in the search parameters.

---

 - &#x23EE; Admin ⏵  [Users](admin-uploaddirs.md)
 - &#x23ED; Admin ⏵  [Process Logs](admin-whosonline.md)
 - &#x23F6; [Server Pass Summary](README.md)
