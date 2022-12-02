# Admin - Upload Directories

The Upload Directories page is available to admin users via the
**Admin** ⏵ **Upload Directories** link.

Note this is different from the [View Uploads](list-uploads.md)
page, which obtains the upload data from the database rather than
scanning the upload directory.

The Upload Directories page lists all the directories found in the
[upload directory (`PassConfig::UPLOAD_PATH`)](setupfrontend.md).
The submission timestamp and the job token are obtained from a
pattern match on the directory basename. The Pass CLI settings file
is read for the job information.

The submission job ID, status and exit code can then be found in the
database via a match on the upload timestamp and the job token.

The results are listed in a table with a checkbox at the start.
The columns are:

| Column | Description |
| --- | --- |
| Submission Time | The time the job was uploaded |
| Token | The job token |
| Course | The course code |
| Assignment | The assignment label |
| Student(s) | The student usernames listed in the job |
| Encoding | The file encoding of the source code (as indicated by the uploader) |
| Submission Data | The job status and (if processed) the exit code |

The grey files area below each row is a collapsible area that can be toggled to show
the file list and the size of each file.

If the submission data column has "Not found" then this means an
upload directory has been found, but there's no corresponding
submission data in the database. This either means that the user has
been removed from the database or their username has changed since
the upload.

The job status will be one of:

 - 'uploaded': files have been uploaded but job details haven't been
   added to the message queue;
 - 'queued': job details are in the message queue;
 - 'processing': the backend is currently processing the job;
 - 'processed': the backend has completed the job.

If the message queue failed while there were jobs queued or waiting
to be queued, select the jobs that need to be requeued, and click
the "Requeue Selected" button at the bottom of the page.

Only requeue jobs if some accident, tampering or outage has deleted
the message queue. You can't requeue uploads that have already been
processed.

To delete upload directories, select the relevant rows and click on
the "Delete Selected" button at the bottom of the page. You will
then be prompted for confirmation.

The web frontend doesn't have permission to delete completed
directories.

---

 - &#x23EE; Admin ⏵  [Users](admin-users.md)
 - &#x23ED; Admin ⏵  [Process Logs](admin-processlogs.md)
 - &#x23F6; [Server Pass Summary](README.md)
