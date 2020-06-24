# ![Lucinda Logo](../../rsc/lucindalogo_klein.png) Lucinda
 
Ein Dokumentenverwaltungssystem (nicht nur) für die Arztpraxis.

## Einleitung

Mit dem Aufkommen der elektronischen Datenverarbeitung stellte sich auch in Arztpraxen die Frage der elektronischen Dokumentenverwaltung. 

An die Stelle sperriger Karteischränke, bei denen aber immerhin jederzeit klar war, was wo gelagert war und wie man es wieder auffinden konnte, traten nun mehr oder weniger undurchsichtige Konstruktionen aus mehreren Praxiscomputern, einem oder mehreren Servern und möglicherweise gar einer "Cloud" und verschiedenen Speicherstationen und Backup-Geräte. Schon nach kurzer Zeit ist es nicht mehr jederzeit klar, wo Dokumente sich physikalisch wirklich befinden, wieviele Kopien eines Dokuments im System und im Umlauf sind, und wer darauf Zugriff hat. Gleichzeitig nahmen Menge und Umfang der Dokumente gewaltig zu. 

Ein Dokumentenverwaltungssystem sollte folgende Ansprüche erfüllen können:

* Das Ablegen von Dokumenten aus Fax, Scanner, E-Mails und so weiter soll möglichst einfach sein.

* Jedes Dokument soll im aktiven System nur einmal existieren, aber von jedem Dokument soll es mehrere inaktive Datensicherungen (backups) geben.

* Sowohl die Dokumente im aktiven System, als auch die Backups müssen vor unbefugtem Zugriff geschützt sein.

* Dokumente sollten sich leicht anhand von Stichworten oder sonstigen Kriterien wieder auffinden lassen. Dabei soll nicht nur der Titel, sondern auch der Inhalt betrachtet werden.

Lucinda ist eine universelle Dokumentenverwaltungslösung auf Basis industriebewährter Komponenten wie Apache Lucene und Solr, welches sowohl allein, als auch in Kombination mit der [Elexis](http://www.elexis.ch)&trade;  Praxissoftware eingesetzt werden kann.

## Installation

Es gibt mehrere Möglichkeiten, Lucinda zu installieren.  

* Sie können Lucinda in einer virtuellen Maschine laufen lassen. Eine virtuelle Maschine (VM) ist praktisch ein Computer in ihrem Computer. Wir verwenden hier das kostenlose Programm [VirtualBox](https://www.virtualbox.org) von Oracle, um die benötigte virtuelle Maschine zu kontrollieren. Der Vorteil dieser Lösung ist, dass VirtualBox auf allen relevanten Betriebssystemen sehr einfach einzurichten ist. Und danach die Lucinda VM in VirtualBox zu installieren und zu starten, klappt mit wenigen Mausklicks. Der Nachteil ist, dass eine virtuelle Maschine, da sie ja praktisch ein zweiter Computer in Ihrem Computer ist, relativ viele Ressourcen (Speicher, Rechenzeit) konsumiert. In den meisten Fällen ist das bei heutigen Computern und dem typischen Büro-Nutzungsprofil kein grosses Problem. Die VM Variante ist empfehlenswert, um Lucinda mal auszuprobieren, oder wenn Sie es unbedingt auf einem Windows-Computer haben möchten. Wie Sie Lucinda als VM installieren können, lesen Sie [hier](installvbox.md).

* Sie können Lucinda als Docker-Komposition betreiben. [Docker](https://www.docker.com) ist eine Technologie, die es erlaubt, eine leichtgewichtigere Form von virtuellen Maschinen laufen zu lassen, als dies bei VirtualBox möglich ist. der Vorteil dieser Lösung ist ein geringerer Ressourcenverbrauch. Docker wird Ihren Computer nicht wesentlich ausbremsen und eignet sich auch sehr gut zum Betrieb auf einem Server ohne Bildschirm und Tastatur. Der Nachteil ist, dass Docker auf Windows nicht ganz so reibungslos läuft - wobei dies auf neueren Versionen von Windows 10 deutlich besser geworden ist. Dennoch ist diese Variante eher für den Serverbetrieb, idealerweise auf einem Linux- oder Mac-Server empfehlenswert. Wie Sie Lucinda mit Docker-Compose starten können, lesen Sie [hier](installdocker.md).

* Der Vollständigkeit halber sei auch die Möglichkeit erwähnt, Lucinda direkt, ohne Zwischenschicht auf dem Computer zu installieren. Dies geht auf allen Betriebssystemen, auf denen Java und NodeJS laufen kann (also praktisch allen), braucht aber ein wenig erweiterte Computerkennntisse. Ich gehe daher hier nicht näher darauf ein, sondern verweise aufs [Readme](https://github.com/rgwch/Lucinda/blob/master/readme.md).


## Struktur der Ablage

Es hat sich gezeigt, dass es grundsätzlich sinnvoll ist, Dokumente so abzulegen, dass sie auch ohne Spezialsoftware oder gar -hardware lesbar bleiben. Lucinda speichert daher Dokumente ganz normal im Dateisystem und benötigt nur für seinen eigenen Index spezielle Speicherorte.

Erstellen Sie also ein Verzeichnis, welches von allen Arbeitsstationen aus erreichbar ist (z.B. durch eine Netzwerkfreigabe). Innerhalb dieses Verzeichnisses empfiehlt sich eine logische Ordnerstruktur, der man auch ohne Programmkenntnisse "ansieht", was wo ist. Für die Ablage von Klienten- bzw. Patienten-Dokumenten hat sich eine Ordnerstruktur wie folgt bewährt:

````
Freigabename 
             -  aaa_Eingangsfach 
             -  a 
                  - Adliger_Heinrich_20.04.1948
                  - Andlikoser_Miguela_13.12.1976
             -  b 
                  - Bartlos_Kuno_24.04.2001
             -  ...
               
usw.

````

Auf diese Weise kann auch in einer Ablage mit mehreren tausend Patienten jede Kartei innert nützlicher Frist auch mit den Dateisystemwerkzeugen des Betriebssystems aufgesucht werden. Dies wird vor allem beim Ablegen von Dokumenten aus dem Scanner oder der E-Mail wichtig. Das Eingangsfach hat eine spezielle Bedeutung, die weiter unten erklärt wird. Die Präfix aaa_ wurde einfach darum vorangestellt, damit es als Erstes im Verzeichnis steht. Grundsaätzlich sind Sie nicht an diese Strukturempfehlung gebunden - Lucinda kommt mit jeder Einteilung zurecht. 

## Dokumente ablegen

Es genügt, Dokumente, die Sie in den Index aufnehmen wollen, im Dokumentenverzeichnis abzulegen. Wenn Beispielsweise ein Dokument für Adliger Heinrich gescannt wird, kann man es mit dem Scanprogramm direkt in `Freigabename\a\Adliger_Heinrich_20.04.1948` abspeichern. Lucinda wird innerhalb weniger Sekunden erkennen, dass ein neues Dokument eingetroffen ist, und dieses wenn nötig durch eine Texterkennung (OCR) schicken und indizieren.

Mit zunehmendem Datenaustausch per E-Mail sinkt allerdings die Bedeutung der OCR. Auch PDFs, die per Mail eintreffen, sind oft bereits durchsuchbar und können somit direkt indiziert werden. Unter gewissen Bedingungen kann Lucinda auch selbst erkennen, zu welchem Patienten/Klienten ein Dokument gehört, auch wenn man es nicht explizit im passenden Ordner ablegt.

Hierfür dient der Ordner aaa_Eingangsfach (den man selbstverständlich auch anders nennen kann, wenn man Lucinda entsprechend konfiguriert.) Jedes Dokument, das in diesem Verzeichnis abgelegt wird, wird anhand bestimmter konfigurierbarer Regeln analysiert, und wenn so der richtige Ablageort eruiert werden konnte, wird es dort hin verschoben.

## Dokumente finden

Das ist natürlich der Hauptzweck eines Dokumentenverwaltungssystems (Wann hatte Herr Meier die Magenspiegelung? Wie hiess die Patienten mit dem Rattenbiss?). Und genau hier glänzt Lucinda und zeigt ihre Vorzüge gegenüber einer reinen Dateiablage.

