#! /bin/sh

convert $1 $1.cnv.tiff
tesseract $1.cnv.tiff $1-cnv pdf
