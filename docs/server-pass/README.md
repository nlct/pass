# Server PASS Documentation

🚧 This is the documentation for Server PASS.

In addition to the [general assumptions](../README.md), also assume:

  - a server called `cmp-server-01` has been setup specifically for
    Server Pass;
  - the server has had appropriate firewalls set up so that the
    server can only connect to URLs that are sub-paths of
    `http://cmp.example.com/pass/` (where the Pass XML files and
    associated assignment files are);
  - the server has http server software installed with PHP;
  - the server has MySQL installed;
  - the server has Docker installed;
  - the server has RabbitMQ installed;
  - Alice has an account on the server with her username `ans` and
    home directory `/home/ans`.

## Installation

First [compile](../compile.md) the Server Pass command line
application (`pass-cli-server`).

## Website Documentation

### General Web Pages

 - [List Uploads](list-uploads.md)

### Admin Pages

Users with "admin" status can access the Admin pages.

 - [Configuration](admin-config.md)
 - [Users](admin-users.md)
 - [Upload Directories](list-uploads.md)
