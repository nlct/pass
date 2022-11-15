# Admin - Users

The Users page is available to admin users via the 
**Admin** ⏵ **Users** link. This page will list all users.
You can search for a specific user or a sub-set of users in the
search form using regular expressions on their username or
registration number, or you can search for a
specific user according to their numeric user ID (UID), which
corresponds to the primary identifier in the database.

The list of results is tabulated.

| Column | Description |
| --- | --- |
| ID | The UID |
| User | The username |
| Reg Num | The registration number |
| Role | May be one of: `admin`, `staff` or `student` |
| Status | May be one of: `active`, `pending` (account created but not verified) or `blocked` |
| Created | The timestamp when the user created their account |
| Action | Links to edit or delete the account or view the list of the user's uploads |

## Edit Account

You can change the user name or registration number for a particular
user. Click "Update" to save the changes. Alternatively, click
"Cancel" to return to the list of users.

There are no email addresses stored in the database. All email
addresses are formed from the username with the university's domain name.
This is the username that's included in the PDF created by PASS and
must match their university username used to log their marks.

For security reasons, there's no provision to provide an alternative
email address. Only registered students or staff may create an
account. If a student has another email account that they prefer to
use, then they will need to setup a redirect in their university
email account.

Unfortunately, students don't always read instructions and they
invent a username when they create a Server PASS account, which
means they don't receive the email with the account verification
token. The email will instead bounce to the "envelope from" address.
If a bounced email is received then the admin user can correct the
user name in the "Edit Account" page.

Sometimes a student may then try to create another account when they
fail to receive the verification token. If this happens, you can
either delete the extra account or use the "Merge with User ID"
form. Enter the UID of the duplicate account and click the "Merge"
button.
