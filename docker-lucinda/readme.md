### Lucinda Dockerfile

#### usage

    docker run -v <host-dir>:/var/lucinda -p 2016:2016 rgwch/lucinda-server:1.0.1
    
The host dir should be either empty or contain folders named `files` and `index`. `Files` can contain subfolders with files to integrate, and documents dropped there will bew integrated continuously while lucinda-server runs, or when lucinda-server is started the next time. `Index`  is managed entirely by lucinda-server and should not be modified in any way.

*Note* for Mac and Windows: At the time of this writing, docker containers can only reach directories within the home directory of the user who started the container.
