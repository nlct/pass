# Uploads

When a student uploads project files for an assignment, a unique
random [**token**](README.md#tokens) is created.

The token is saved in the "submissions" table in the database, along
with the student's UID, the upload time, the course label, and the
assignment label. The status is initially set to "uploaded". This row in the
table has a unique numeric identifier, which is the **submission ID**
or job ID.

A new sub-directory in the upload directory is created and
the uploaded files are saved in this new sub-directory, 
the **submission upload directory**, along with a
file containing the project information in a format that can be
input using `pass-cli-server`'s `--from-file` argument
(the Pass CLI settings file).

The job is queued and, if successful, the row in the "submissions"
table corresponding to the submission ID has the status changed to
"queued". If the status is stuck on "uploaded" then it's likely that
the messaging system (RabbitMQ) has stopped and will need to be
restarted.

When the backend picks up the job from the message queue, it starts
up an instance of `pass-cli-server` in a Docker container and saves
the resulting files, if successful, in a **processed directory**
(or "completed directory"). An email is then sent to the student to
inform them that their upload has been processed and they can log in
to download the PDF (which is fetched from the processed directory).

Collapsible blocks require JavaScript to toggle their state. If you
don't have JavaScript these collapsible blocks will always be open.

## View Uploads (Staff) or My Uploads (Students)

Admin and staff users can view the lists of uploads from the "View
Uploads" link in the navigation bar. This information is fetched
from the submissions table in the database. Students can also view
this page (it will be called "My Uploads" in the navigation bar
instead), but they will only see their own uploads listed or uploads
for group projects where they are listed as a member of the group.

The Uploads page has a form at the top, which can be used to
filter the results. Only admin and staff users have the username
field and export button available. All users have the course,
assignment, submission ID, exit code and uploaded fields available.

> Course: <kbd>Any⏷</kbd>  
> Assignment: <kbd>Any⏷</kbd>
> Submission ID: ` `🡙  
> Exit code: <kbd>Any⏷</kbd>  
> Uploaded: <kbd>After⏷</kbd> `dd/mm/yyyy`  
> <kbd>Search &#x1F50D;</kbd>

Admin and staff have a collapsible box titled "Exit Codes" which can
be clicked on to expand with information about the possible exit
codes.

The results are listed in a table, where each row is followed by the
"Files" section, which is a collapsed block spanning the table, that
lists the uploaded files, followed by the file size and the type of
file (main project file, project file, or additional file).

Admin and staff users have another collapsed block after the Files
block, headed "Pass Settings", that lists the settings contained the 
`pass-cli-server` input file that can be used to double-check the
supplied information in case an error has occurred. Things to check
in particular are the timeout and encoding settings, and also if a
language has been specified for any files, check that it makes sense
for the file type. (For example, has a binary file been identified
as "Plain Text"?) If the timeout value doesn't match the timeout in
the configuration page, make sure that the timeout hasn't been fixed
in any of the `resources.xml` files.

The columns of the upload list are described below.

| Column | Description |
| --- | --- |
| ID | The submission (job) ID |
| Uploaded | The timestamp when the project files were uploaded via the Server Pass [upload page](upload-project.md). |
| Course | The course title |
| Assignment | The assignment title |
| Project Members | The student username for a solo project or the list of usernames for a group project. |
| Status | The job status. If queued, the place in the queue will be shown. If processed, admin and staff can also view the exit code in parentheses. This should be 0 if the process exited successfully. |
| Downloads | If the job has been processed, this will have links to the PDF (if successfully created) and the log file. |

## Export (Staff)

Staff and admin users can click on the "Export" button to export the
search results as a tab-separated (TSV) file with the following columns:

 - Submission ID (job ID);
 - Upload Time (for example, 2022-11-29T130250000+0000);
 - Course (label/code, for example, CMP-101);
 - Assignment (label);
 - Exit Code;
 - Uploaded By (uploader's username, for example `vqs23ygl` for Bob);
 - Project Group (same as uploader for solo projects or comma-separated
   list of usernames for group projects, for example `vqs23ygl,jwh22ird`
   for Bob and Carol's group project);
 - PDF MD5 checksum (if blank then the PDF wasn't created, the exit
   code should be non-zero).

---

 - &#x23EE; [Upload Project](upload-project.md)
 - &#x23ED; [Final Uploads (Staff)](final-uploads.md)
 - &#x23F6; [Server Pass Summary](README.md)
