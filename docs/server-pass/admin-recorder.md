# Admin - Action Recorder

The Action Recorder page is available to admin users via the
**Admin** ⏵ **Action Recorder** link.

This page is intended for debugging purposes. Significant actions
and the success or failure is recorded to make it easier to diagnose
faults. For example, if a user attempts to log in but supplies
either the wrong username or the wrong password, the error response
doesn't tell them what they got wrong (which is a security measure).
However, the usernames aren't easy to remember and it's easy for new
students to get it wrong. If a user complains that the site won't
accept their password, the action recorder will identify whether or
not they actually got their username wrong rather than the password.
Similarly, if a student complains they're not receiving the
verification email, it again may be down to a mistyped username.

You can use the search form to search for messages but bear in mind
that verification errors won't have the UID field set. You can do a
regular expression search on the action name or the comments.

At the bottom of the page is a form to delete old records. Set the
date to delete all records before that date and click on the
"Delete Old Data" button. You will prompted for confirmation.
Click "Confirm Delete" to confirm deletion or "Cancel" to cancel the
delete request.

---

 - &#x23EE; Admin ⏵  [Session Data](admin-sessiondata.md)
 - &#x23ED; Admin ⏵  [Backend Maintenance](admin-maintenance.md)
 - &#x23F6; [Server Pass Summary](README.md)
