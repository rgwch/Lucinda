### Lucinda Dockerfile

#### Usage

The container ist hosted on dockerhub, so no need to download anything. Just install [Docker](https://www.docker.com), and then:

    docker run -d -v <host-dir>:/var/lucinda -p 2016:2016 rgwch/lucinda-server:1.0.0
    
The host dir should be either empty or contain folders named '`files`' and '`index`'. '`Files`' can contain subfolders with files to integrate,
 and documents dropped there will be integrated continuously while lucinda-server runs, or when lucinda-server is started the next time.
  '`Index`'  is managed entirely by lucinda-server and should not be modified in any way.

The container does not hold any internal state. So if you stop it and run it (or another instance) on the same &lt;host-dir&gt;,
 it will behave exaxtly the same way as if it runs continuously. You should, however, never run more than one instance at the same
  time on the same &lt;host-dir&gt;, since Lucene (the engine inside lucinda-server) needs to lock the '`index`' directory.

*Note for Mac and Windows*: At the time of this writing, docker containers can only reach directories within the home directory of the user who started the container.

#### Test

Open a web browser and navigate to `http://localhost:2016/api/1.0/ping`. The browser should display 'pong' if lucinda
is working correctly.

#### Debug

If you want to see console output of the lucinda server, either run the container without the -d option, or find out
the name of the running container with `docker ps` and then attach a console with `docker attach <name>` to it.

#### Stop

To stop a background container, find out its name with `docker ps` and stop it with `docker stop <name>`. Lucinda will
shut down gracefully on stop signal and close the index properly.
