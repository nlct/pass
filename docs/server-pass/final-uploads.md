# Final Uploads (Staff)

The Final Uploads page is only available to staff and admin users.
Unlike the [Uploads](list-uploads.md) page, this lists the total
number of uploads for each assignment, rather than listing each
upload individually, and shows the timestamp for the last upload.

For example, if Bob and Carol have tried uploading their Shop
assignment three times (because there was a problem on their first
two attempts) then the Final Uploads page will show the timestamp of
their final attempt.

The page starts with a search form. Enter the search criteria and
click on the "Search" button. If you want to search for multiple
usernames, enter them in the text area with a newline between each
username. (This was designed to make it easy to copy and paste a
list of usernames.)

> Course: <kbd>Any⏷</kbd>  
> Assignment: <kbd>Any⏷</kbd>  
> Blackboard IDs: (Use a newline to separate each Blackboard ID.)  
> _multiline textfield_  
> <kbd>Search &#x1F50D;</kbd>

(Admin note: Text such as "Blackboard IDs" and "Blackboard ID" can be
changed to something more applicable by making
the [appropriate modification to `PassConfig.php`](setupfrontend.md).)

The result list will contain the following columns:

 - Blackboard ID (only included if multiple students or no students specified);
 - Course (only included if course set to "Any");
 - Assignment (only included if assignment set to "Any");
 - Timestamp (the time of the most recent upload for the given assignment);
 - Number of Uploads (for the given assignment).

The final column shows the total number of uploads for the given
project, incorporating a link to the [Uploads](list-uploads.md) page
for the given username and assignment.

For group projects, only one username will be shown per row but
there will be repeated rows for each group member included in the
search criteria. (If the number of uploads differs, then it's likely
that one or more members were omitted in some of the uploads.) Use the
link to the Uploads page to find out further information.

For example, Alice searches for Bob and Carol by their usernames for
the Shop assignment.

> Course: <kbd>CMP-101: Introduction to Java⏷</kbd>  
> Assignment: <kbd>Shop⏷</kbd>  
> Blackboard IDs: (Use a newline to separate each Blackboard ID.)  
> `vqs23ygl`  
> `jwh22ird`  
> <kbd>Search &#x1F50D;</kbd>

Since she has specified a particular assignment, the course and
assignment columns are omitted in the results.

> | Blackboard ID | Timestamp | Number of Uploads |
> | --- | --- | --- |
> | vqs23ygl | 2022-11-29 13:02:50 | 3 |
> | jwh22ird | 2022-11-29 13:02:50 | 2 |

Bob accidentally forgot add Carol on his first upload and only
included her on the other two attempts, so Bob has a total of 3
uploads for the Shop assignment but Carol has 2. If Carol's
timestamp is earlier than Bob's then Bob forgot to include her on
the final attempt.

---

 - &#x23EE; [List Uploads](list-uploads.md)
 - &#x23ED; [Configuration (Admin)](admin-config.md)
 - &#x23F6; [Server Pass Summary](README.md)
