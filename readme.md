![](rsc/lucindalogo.png)
# Lucinda

A service to index, store and retrieve files in many formats. Lucinda is meant for install on a server and access over a LAN, but of course it's also possible to install it locally.

Version 3.0 is a complete rewrite, now based on [Solr](https://lucene.apache.org/solr/) (which in turn is based on [Lucene](https://lucene.apache.org)). 

(The name is short for "Lucene powered file indexa")

### Why not use Solr directly?

While Solr does a great job indexing and retrieving documents, it does not take care about the document files themselves. For an in-house document store it is often preferred to have the document management system handle adding, modifying and removing files as well. Lucinda adds this capability to Solr.

## Features

* Indexes a directory recursively in-place. Adds Text layers to image-only documents (e.g. from scanners), making them searchable and indexable. Resulting documents are stored as [PDF/A](https://en.wikipedia.org/wiki/PDF/A).
* watches that directory and all its subdirectories and indexes new or modified files in-place.
* Retrieves documents with flexible queries.
* Allows to insert and retrieve documents via REST interface.
* Optionally presents an easy to use Web-Interface.

## Install

It's highly recommended to use docker-compose. The docker-compose.yaml takes care of prestarting solr and tika correctly and connecting everything uniformly in many operating systems, such as macOS, Linux, or Windows. So, if you have [Docker](https://www.docker.com/get-started) and [docker-compose](https://docs.docker.com/compose/) ready, Lucinda's install is a just matter of downloading the [docker-compose.yaml](https://raw.githubusercontent.com/rgwch/Lucinda/lucinda3/docker-compose.yaml) file and entering:

`docker-compose up -d`

which will launch Lucinda with default features. Probably you want to use an existing documents directory. If so, set an environment variable LUCINDA_DOCBASE before the call to docker-compose. e.g:

~~~~~
export LUCINDA_DOCBASE=/srv/praxis/berichte
docker-compose up -d
~~~~~

Optionally, you can change the docbase in docker-compose.yaml directly (and more permanently), or you can create a file called .env in the same directory as the docker-compose.yaml and state there something like: `LUCINDA_DOCBASE=/srv/praxis/berichte`

## Use

Lucinda offers a simple REST api to search and fetch documents. There's an openapi.yaml with the exact specification, but the most important things are simply:

* `GET http://server-address:9997/lucinda/3.0/query/some-expression` - searches for documents matching the expression and returns a JSON Array with their metadata

* `GET http://server-address:9997/lucinda/3.0/get/id` - fetches the contents of the file with the given id in binary form. The id is part of the metadata as fetched with query.

You might call this API directy with a web browser, but you'll rather use some sort of frontend, e.g. the Elexis Lucinda Plugin. And of course, since Solr is active behind the scenes, you can always as well use Solr's API to retrieve Documents.

### Simple Web UI

While the Lucinda server is primarly intended for use via its REST API, there's also a simple Web UI for quick searches at http://serveraddr:9997/. **Warning** This is absolutely only for inhouse-use behind a firewall. There's no security at all: Anyone with access to the network can read all documents.
If you want to enable the Web-UI, set the environment-variable LUCINDA_SIMPLEWEB to 'enabled'. 

## Remarks

While retrieval ist extremely fast, indexing of files can be a slow process. Lucinda does it's best to achieve a good result. This means especially: When importing scanned PDF files (which normally do not contain indexable text but only an image of the original paper), Lucinda runs OcrMyPDF to extract text and overlay that text to the PDF, making a searchable PDF from it. This process can take up to several minutes for a complex document. So, if you store a new file in the docbase, don't expect it to be immediately available.

If you run Lucinda first on a large existing docbase (even one managed by an earlier version of Lucinda), the first start can take several hours or even days. You can use the omputer and even Lucinda during this time, but of course, only part of the documents are indexed until the first run finishes.

Subsequent starts will be quite fast. Lucinda wil still check the whole docbase for new files, but this process won't take too long, if there are no new files.

## Fine Tuning

As usual with Docker compositions, much of the confguration happens via environment variables. One example was shown in the install section for the configuration of the document store. There are three possibilities to change such variables:

* Change them directly in the docker-compose.yaml. As a convention, Environement variables are in CAPITAL_LETTERS.
* Set them manually. In Linux and macOS, this means usually one or more `export VAR=value` commands before launching docker-compose. In Windows, the commands are similarly `SET VAR=value`.
* Set them in a file called .env in the same directory as docker-compose.yaml.

Wichever you chose, following variables can possibly be modified:

* SOLR_DATA - Where the index data are stored. Default: A Docker volume called 'solrdata'
* SOLR_PORT - The port where Solr listens. Default 8983. Change this only, if you have a good reason to do so. It's possible that other things won't work then..
* TIKA_PORT - The port where Tika listens. Default 9998. Don't change normally.
* LUCINDA_DOCBASE - we saw this before. Default: A Docker volume called 'lucindadocs'.
* LUCINDA_ATTIC - Whenever Lucinda detects a new version of an already indexed file, it copies the old version with a date suffix at the name to the location denoted by this var. Default: A docker container called 'lucindamoved'.
* LUCINDA_PORT - the port Lucinde should listen on. Default 9997. Change as you like.
* LUCINDA_SIMPLEWEB - if set to 'enabled', allows Browser access to Lucinda's simple Web UI.

**important**: If you set host directories to SOLR_DATA, LUCINDA_DOCBASE and LUCINDA_ATTIC, you must make shure that the docker containers have the appropriate access rights. You can either make these directories 777 (i.e. world writeable and browseable), or give ownerhsip to the respective user ids (8983 for SOLR_DATA)


## Backup

It is important to backup files regularly. The files in LUCINDA_DOCBASE are possibly irreparable, so it's absolutely essential to backup the folder or volume regularly.

The contents of SOLR_DATA is strictly speaking not irretrievable - you can always launch a full scan over all documents. But this can be quite time consuming. So better backup this folder, too.

It's recommended to stop Lucinda and Solr before backing up those directories. A simple script will do, such as:

~~~~~
docker-compose stop -t 30
tar -cjf /path/to/solr_backup.tar.xz $SOLR_DATA
docker-compose start
~~~~~


## Technology

Lucinda uses the following programs:

* Extracting text content from many document formats: [Apache Tika](https://tika.apache.org/)
* Indexing and retrieval by sophisticated query technique: [Apache Lucene](https://lucene.apache.org)
* Management of the index: [Apache Solr](https://lucene.apache.org/solr/)
* OCR of PDFs without text layer: [OCRmyPDF](https://github.com/jbarlow83/OCRmyPDF) and [Tesseract](https://tesseract-ocr.github.io)

## License

This program is copyright (c) 2020 by G. Weirich and may be used and distributed according to the terms and conditions as stated
in the [Apache license, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

The Lucinda Icon ![Lucinda](https://github.com/rgwch/Lucinda/blob/master/rsc/lucinda.gif) is created and copyrighted by http://www.fatcow.com/free-icons
