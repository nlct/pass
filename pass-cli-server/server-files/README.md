# Server Pass Files

Requirements:

  - Web server
  - MySQL
  - RabbitMQ
  - Docker
  - TeX Live

Replace 'example.com' with your domain.

Ensure that firewalls and other appropriate measures are set up,
such as access via the University's VPN only. The server should be
isolated from the rest of the University. Therefore there's no
support for single sign in.

This assumes that student email addresses can be formed by their University
username (Blackboard ID) appended with the University's domain name.
No alternative email addresses are permitted.

See [documentation](https://github.com/nlct/pass/tree/main/docs/server-pass) for further
details.
