# Building the PASS Docker Image

First [compile](../compile.md) the Server Pass command line
application (`pass-cli-server`) and create the `pass-cli-server.tgz`
archive.

```bash
cd pass-cli-server
make ../dist/pass-cli-server.tgz
```

FTP the archive to the server. (Change the port number, username and
server as applicable).

```bash
cd ../dist
sftp -P 22 ans@cmp-server-01.example.com
```
(enter password)

```
put pass-cli-server.tgz
exit
```

SSH to the server.

```bash
ssh -P 22 ans@cmp-server-01.example.com
```
(enter password) and unpack the archive:

```bash
tar zxvf pass-cli-server.tgz
cd pass-cli-server
cp lib/resources.xml /var/www/html
docker build --network=host --tag pass:latest .
```

This copies the local `resources.xml` to the web root to ensure that
the frontend website references the same courses as the backend. The
`docker` command builds the Docker image with the `pass-cli-server`
application installed in it. This needs to be done whenever a new
version of `pass-cli-server` is available (although you don't need
to copy the `resources.xml` file to `/var/www/html` if it hasn't
changed).

---

 - Prev: Installation ⏵ [Setting Up the Frontend](setupfrontend.md)
 - Next: Installation ⏵ [Setting Up the Backend](setupbackend.md)

