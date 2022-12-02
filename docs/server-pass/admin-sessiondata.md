# Admin - Session Data

The Session Data page is available to admin users via the
**Admin** ⏵ **Session Data** link.

A session expires when a user logs out or logs in or when the
session cookie expires. The session cookie should be deleted by the
browser when the window is closed, but this can vary according to
the browser. The session cookie can also be manually deleted by the
user through the browser's privacy and data setting.  A session needs
both the cookie and the corresponding entry in the database. If
either are expired or missing a new session is created.

The course and assignment data is stored in session data so that it
doesn't have to be fetched from the XML files every time a page that
requires it is loaded (for example, while stepping through the
upload form). Unfortunately this means that if the XML files are
changed (for example, to add a new assignment) then this change
won't show up until your session data is updated.

Whilst it's possible for you to log out and log back in again, there
may be other users who are currently logged in who need to pick up
the update. If so (which you can tell from the
[Who's Online](admin-whosonline.md) page) then you can clear their
session data to force them to log back in again (but check first to
make sure that they're not currently uploading files for another
course).

The session garbage collector should remove expired sessions from
the database, but there's no guarantee that this will happen or when
it will happen, so the session data page also allows you to delete
old session data that hasn't been removed.

There are two options on this page:

  - clear all session data except your own;
  - clear all session data older than a certain number of hours that
    you need to specify (default 1 hour).

Select the option you want and click on the "Clear Session Data"
button at the bottom of the page. In both cases, your own session
data won't be deleted but the course and assignment information
stored in your session data will be cleared. This means that the
next page you visit that requires this information (such as the
Upload page) will reload the information from the XML files.

If there are other users uploading files, the second option
shouldn't interfere with their use of the site. The first option
will ensure that everyone else on the site has to log in again when
they move to a new page or if the page they are currently on reloads.

---

 - &#x23EE; Admin ⏵  [Who's Online](admin-whosonline.md)
 - &#x23ED; Admin ⏵  [Action Recorder](admin-recorder.md)
 - &#x23F6; [Server Pass Summary](README.md)
