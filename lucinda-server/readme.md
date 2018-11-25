# Lucinda Server

## Features

Observes one or more directories for changes, and adds, modifies, or deletes files accordingly from its index. 
If you have [tesseract](https://github.com/tesseract-ocr) installed on your system, Lucinda will try to OCR image files (including  pdf-image files such as pdfs' produced by document scanners without postprocessing).
Queries are accepted from the console or via Messages over the distributed Vert.x bus.

## Usage

### 1. As a library

 * Copy *default.cfg* to *user.cfg* in the same directory
 * Edit *user.cfg* according to your system.
 * Use the API of ch.rgw.lucinda.Dispatcher as entry point to the library functions

### 2. As a standalone program
 
 * Copy *default.cfg* to *user.cfg* in the same directory
 * Edit user.cfg as needed (see below)
 * run java -jar Launcher
 * interact on the console with the program (see below)
 * communicate via REST interface

## Search terms

Since Lucinda is based on Apache Lucene, the classic query-parser of Lucene is used. 
Full documentation, is [here](http://lucene.apache.org/core/5_5_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description_).
 
In short, you can use:

 * simple words: *Meier*
 
 * simple Phrases: *"Hans Meier"*
 
 * Wildcards:   *M??er*
 
 * Regular Expressions: */M[ae][iy]er/*
 
 * Fuzzy:  *Meier~*
 
 * Proximity: *"Hans Meier"~5* (The two words must be within 5 words distance of each other) 

 * Field names: Title: *amazing*
 
 * Combinations of all of the above: *Title: amazing OR (Author: M[ea][iy]er AND Müller -Huber)* (Find documents with "amazing" in the title 
 or with some form of Meier as the author, and containing "Müller" but not "Huber" in the Text.) 
 
## Configuration
 
On program launch, the program first looks for "default.cfg" in the class path and loads all values from that file. Then, it looks for "user.cfg" and
again loads all values. Thus, if a value with the same name exists in both, default.cfg and in user.cfg, the latter will win.
 
The following configuration items are supported (values here are the default values)
 
### The language to use for the analyzer and tokenizer when importing new files

    default_language = de  
 
 
### Use the REST interface

    rest_use=yes
    rest_port=2016  

### The base directory for all operations

    fs_basedir = /some/path
  
  
### The directory where the index should be kept. Make sure to backup this directory regularly

    fs_indexdir = /some/path
  
  
### One or more directories to watch

    watchdirs = &lt;comma separated list of directories&gt;
  
 Lucinda watches these directories for added, changed or removed files. If a file is added, Lucinda will index it, but won't move it somewhere. If a file is
  deleted, its entry in the indes will also be deleted. If a file is changed, the index will be updated.
  
### Path to an ocr executable

    ocr=tesseract

If this parameter is given, Lucinda tries to run that program/script over image-only PDF 
files to retrieve text content. As of today, only Tesseract is tested.

  
## Tests
  
`mvn test` will run several tests. 

If you start the program from the command line, it will prompt for inputs. Try to copy some files into one of the watched directories and observe the console.
  Enter a query in the console, e.g. `lorem AND ipsum` and see the result.
  
  
## Command line switches

 * --config=&lt;filename&gt;: Use that file as config (instead of default.cfg and user.cfg)
 * --daemon or -d: Run in background. Don't accept input
 * --rescan or -r: rescan all watch directories immediately after launch.
 
  
## Credits and used technologies  

 * [Apache Lucene](http://lucene.apache.org)
 * [Apache Tika](http://tika.apache.org)
 * [Vert.x](http://vertx.io)
 * [Tesseract](https://github.com/tesseract-ocr/tesseract)
 
