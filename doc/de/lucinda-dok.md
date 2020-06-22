# Lucinda

Ein Dokumentenverwaltungssystem (nicht nur) für die Arztpraxis.

## Einleitung

Mit dem Aufkommen der elektronischen Datenverarbeitung stellte sich auch in Arztpraxen das Problem der elektronischen Dokumentenverwaltung an die Stelle sperriger Karteischränke, bei denen aber immerhin jederzeit klar war, was wo gelagert war und wie man es wieder auffinden konnte, traten nun mehr oder weniger undurchsichtige Konstruktionen aus mehreren Praxiscomputern, einem oder mehreren Servern und möglicherweise gar einer "Cloud" und verschiedenen Speicherstationen und Backup-Geräte. Schon nach kurzer Zeit ist es nicht mehr jederzeit klar, wo Dokumente sich physikalisch wirklich befinden, wieviele Kopien eines Dokuments im System und im Umlauf sind, und wer darauf Zugriff hat. Gleichzeitig nahmen Menge und Umfang der Dokumente gewaltig zu. 

Ein Dokumentenverwaltungssystem sollte folgende Ansprüche erfüllen können:

* Das Ablegen von Dokumenten aus Fax, Scanner, E-Mails und so weiter soll möglichst einfach sein.

* Jedes Dokument soll im aktiven System nur einmal existieren, aber von jedem Dokument soll es mehrere inaktive Datensicherungen (backups) geben.

* Sowohl die Dokumente im aktiven System, als auch die Backups müssen vor unbefugtem Zugriff geschützt sein.

* Dokumente sollten sich leicht anhand von Stichworten oder sonstigen Kriterien wieder auffinden lassen. Dabei soll nicht nur der Titel, sondern auch der Inhalt betrachtet werden.

## Installation

Es gibt mehrere Möglichkeiten, Lucinda zu installieren. Ich zeige hier die zwei einfachsten:

### Als virtuelle Maschine

Hierzu brauchen Sie am Wirtscomputer überhaupt keine Änderungen machen. Sie müssen nur [VirtualBox](https://www.virtualbox.org) installieren. Danach laden Sie die LucindaBox herunter: <http://www.elexis.ch/ungrad/LucindaBox.ova>. Wählen Sie in VirtualBox Datei->Appliance importieren (Strg+I) und wählen Sie diese Datei. Folgen Sie dann den Hinweisen [hier](installvbox.md)



### Als Docker-Komposition

Auch das ändert nicht viel am Wirtscomputer und benötigt weniger Ressourcen, als eine komplette virtuelle Maschine. Unter Mac und Windows benötigen Sie dafür dn [Docker Desktop](http://docker.com/products/docker-desktop) (*ACHTUNG*: Bei Windows ist die mindestens benötigte Version Windows 10, Version 2004, Build 19018).

Unter Linux benötigen Sie die [Docker Engine](https://docs.docker.com/engine/install/) und [Docker-Compose](https://docs.docker.com/compose/install/). 

Das weitere Vorgehen wird [hier](installdocker.md) beschrieben.
 
Der erste Start wird relativ lang dauern, da drei Docker Container heruntergeladen und installiert werden müssen (Solr, Tika und Lucinda). Spätere Starts gehen dann schnell.

## Struktur der Ablage

Es hat sich gezeigt, dass es grundsätzlich sinnvoll ist, Dokumente so abzulegen, dass sie auch ohne Spezialsoftware oder gar -hardware lesbar bleiben. Lucinda speichert daher Dokumente ganz normal im Dateisystem und benötigt nur für seinen eigenen Index spezielle Speicherorte.

Erstellen Sie also ein Verzeichnis, welches von allen Arbeitsstationen aus erreichbar ist (z.B. durch eine Netzwerkfreigabe). Innerhalb dieses Verzeichnisses empfiehlt sich eine logische Ordnerstruktur, der man auch ohne Programmkenntnisse "ansieht", was wo ist. Für die Ablage von Klienten- bzw. Patienten-Dokumenten hat sich eine Ordnerstruktur wie folgt bewährt:


### Dokumente ablegen

Es genügt, Dokumente, die Sie in den Index aufnehmen wollen, im Dokumentenverzeichnis abzulegen