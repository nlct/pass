# Setting Up the Frontend

These are instructions to copy over all the relevant files and setup
the MySQL database. This only needs to be done once (barring minor
updates to the frontend website pages).

First archive the [`pass-cli-server/server-files`](https://github.com/nlct/pass/tree/main/pass-cli-server/server-files) directory:

```bash
cd pass-cli-server
tar zcvf server-files.tgz --dereference server-files/
```

FTP the archive to the server. (Change the port number, username and
server as applicable). For Alice, this is:

```bash
cd ../dist
sftp -P 22 ans@cmp-server-01.example.com
```
(enter password)

```
put server-files.tgz
exit
```

SSH to the server.

```bash
ssh -P 22 ans@cmp-server-01.example.com
```
(enter password) and unpack the archive:

```bash
tar zxvf server-files.tgz
```

The `backend` subdirectory contains the files which will be copied
over in the [backend setup](setupbackend.md).

## MySQL Database

The `passdb` database will need to be created by a MySQL admin user
(so Alice may need to ask IT support for this). A user then needs to
be created with the appropriate permissions to access this database.

The `pass.sql` file has the code to create all the tables for this
database. This will need to have `support`, `example.com` and
`support@example.com` replaced as applicable. This can also be
done via the web frontend admin configuration page later, but it
must be done before anyone uses a page that sends an email (such as
create an account). If your university usernames or registration
numbers are longer than 12 characters, you will have to edit the
`users` table to make the `username` and `regnum` fields bigger.

Outside of MySQL, the tables can be created from the command line:
```bash
mysql < pass.sql
```

If you are already running MySQL, you can load the file from the
prompt:
```sql
mysql> source pass.sql
```

---

 - Prev: [README](README.md)
 - Next: Installation ⏵ [Building the PASS Docker Image](buildingimage.md)

