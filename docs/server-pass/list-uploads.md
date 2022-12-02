# Uploads

When a student uploads project files, a unique
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

## View Uploads

Admin and staff users can view the lists of uploads from the "View
Uploads" link. This information is fetched from the submissions
table in the database. Students can also view this page (but it will
be called "My Uploads" instead), but they will only see their own
uploads listed or uploads for group projects where they are listed
as a member of the group.

The "View Uploads" page has a form at the top, which can be used to
filter the results. Only admin and staff users have the username
field and export button available. All users have the course,
assignment, submission ID, exit code and uploaded fields available.

The results are listed in a table, where each row is followed by the
"Files" section, which is a collapsed block spanning the table, that
lists the uploaded files, followed by the file size and the type of
file (main project file, project file, or additional file).

Admin and staff users have another collapsed block after the Files
block, headed Pass Settings, that lists the settings contained the 
`pass-cli-server` input file.

The columns of the upload list are described below.

| Column | Description |
| --- | --- |
| ID | The submission (job) ID |
| Uploaded | The timestamp when the project files were uploaded via the Server Pass upload page. |
| Course | The course title |
| Assignment | The assignment title |
| Project Members | The student username for a solo project or the list of usernames for a group project.
| Status | The job status. If queued, the place in the queue will be shown. If processed, admin and staff can also view the exit code in parentheses. This should be 0 if the process exited successfully. |
| Downloads | If the job has been processed, this will have links to the PDF (if successfully created) and the log file. |

---


 - &#x23F6; [Server Pass Summary](README.md)
