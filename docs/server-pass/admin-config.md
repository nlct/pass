# Admin - Configuration

The Configuration page is available to admin users via the 
**Admin** ▶ **Configuration link**. The following settings are available:

## Debug

The debug setting is a numeric value, that indicates whether or not
to show debugging messages and whether or not to allow courses that
have been identified with the `debug="true"` attribute.

| Setting | Description |
| --- | --- |
| -1  | No debugging messages, but makes debug courses available for staff and admin users. |
| 0 | Off |
| 1 | Enable debug courses and display debugging messages. |
| 2 | Display extra debugging information for admin users. |

You may need to log out and log back in again for this setting to
take effect.

## Timeout

The timeout value (in seconds) written to the PASS settings when a
project is uploaded. Processes started by PASS will timeout after
this value.

## Envelope from

The "envelope from" setting used when sending email messages. This
needs to be a valid email address.

## Mail from

The "from" setting used when sending email messages. This may be a 
"no-reply" address.

## Banner message

A message to display at the start of every page. For example, this
may be a message about planned maintenance work.

## Max items per page

The maximum number of items to display on a page when listing search
results.

## Upload refresh

Number of seconds to refresh the upload list if there is an
unprocessed entry.

## Mode

The website mode. In maintenance mode, only admin users 
(and the special allowed test user, if enabled) can access
the site. The login page will always be available, but you will need
to bookmark or remember the URL.

| Mode | Description |
| --- | --- |
| 0 | Normal |
| 1 | Maintenance |

If you switch mode, you may first want to check if anyone is online
and force them to logout by clearing the session data.

## Reset link timeout

Number of minutes a password reset link remains valid.

## Verify link timeout

Number of minutes an account verification token remains valid.

## Allow test user

| Mode | Description |
| --- | --- |
| 0 | Don't allow |
| 1 | Allow |

If set, this mode permits the test user account to access the site
in maintenance mode.

## Test user

The user name for the test account. This is a fake student account
that staff or admin users can use to check how the site operates for
a student account.

## Test user redirect

Any emails that the site tries to send to the test user name will
be sent to this user instead.

## Help reference

In the event of an error, the user is told to get help from this
person.
