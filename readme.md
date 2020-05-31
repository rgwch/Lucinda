![](rsc/lucinda.gif)
# Lucinda

(The name is short for "lucene powered file indexa")
Version 3.0 is a complete rewrite, uses Solr now in place of the earlier "handmade" Lucene-handlers.

A program to index, store and retrieve files in many formats.


## Features

* Indexes a directory recursively in-place
* watches that directory and indexes new or modified files in-place
* Retrieves documents with flexible queries

## Install

It's recommended to use docker-compose. The docker-compose.yaml takes care of prestarting solr and tika and connecting everything. So, if you have docker-compose installed, Lucinda's install is a matter of:

`docker-compose up -d`

which will launch Lucinda with default features. Probably you want to use an existing documrnts directory. If so, set an environment variable LUCINDA_DOCBASE before the call to docker-compose. e.g:

~~~~~
export LUCINDA_DOCBASE=/srv/praxis/berichte
docker-compose up -d
~~~~~

Optionally, you can change the docbase in docker-compose.yaml directly (and more permanently), or you can create a file called .env in the same directory as the docker-compose.yaml and state there something like: `LUCINDA_DOCBASE=/srv/praxis/berichte`

## Use

Lucinda offers a simple REST api to search and fetch documents. There's an openapi.yaml with the exact specification, but the most important things are simply:

* `GET http://server-address:9997/lucinda/3.0/query/some-expression? - searches for documents matching the expression and returns a JSON Array with their metadata

* `GET http://server-address:9997/lucinda/3.0/get/id - fetches the contents of the file with the given id in binary form. The id is part of the metadata as fetched with query.

You might call this API directy with a web browser, but you'll rather use some sort of frontend, e.g. the Elexis Lucinda Plugin. And of course, since Solr is behind the scenes, you can always as well use Solr's API to retrieve Documents.


## Remarks

While retrieval ist blazing fast, indexing of files can be a slow process. Lucinda does it's best to achieve a good result. This means especially: When importing scanned PDF files (which normally do not contain indexable text but only an image of the original paper), Lucinda runs an OCR to extract text and the overlays that text to the PDF, making a searchable PDF from it. This process can take up to several minutes for a complex document. So, if you store a new file in the docbase, don't expect it to be immediately available.

If you run Lucinda first on a large existing docbase (even one managed by an earlier version of Lucinda), the first start can take several hours.

Subsequent starts will be quite fast. Lucinda wil still check the whole docbase for new files, but this process won't take too long, if there are no new files.

## Technology

Lucinda uses the following programs:

* Extracting text from many document formats: [Apache Tika](https://tika.apache.org/)
* Indexing and retrieval by sophisticated query technique: [Apache Lucene](https://lucene.apache.org)
* Management of the index: [Apache Solr](https://lucene.apache.org/solr/)
* OCR of PDFs without text layer: [OCRmyPDF](https://github.com/jbarlow83/OCRmyPDF) and [Tesseract](https://tesseract-ocr.github.io)
