# Installation von Lucinda in einer Docker Umgebung

## Windows und Mac

Starten Sie Docker Desktop. Sie werden gebeten, Ihr Administratorpasswort einzugeben. Im Fall on Windows werden Sie noch gebeten, einen Linux Kernel im WSL2 (Windows Subsystem for Linux Version 2) zu installieren bzw. upzudaten. Folgen Sie einfach dem angezeigten Link und starten Sie den heruntergeladenen Installer. Danach müssen Sie Docker Desktop neu starten, was eine ganze Weile dauern wird. Sobald Docker Sie dazu auffordert, die Powershell zu öffnen, tun Sie das.

## Linux

Unter Linux können Sie direkt Docker engine und Docker-compose inszallieren.

## Alle Systeme

Laden Sie dann folgende Datei herunter und speichern Sie sie z.B. in "Downloads": <https://raw.githubusercontent.com/rgwch/Lucinda/master/docker-compose.yaml>. 

Geben Sie dann in der Powershell (Windows) bzw. im Terminal (Mac, Linux) ein:

`````
cd Downloads
docker-compose up -d
`````
Das ist soweit alles. Sie werden eine ganze Weile warten müssen, bis Docker alles heruntergeladen und installiert hat.

Am Ende werden Sie je nach Sicherheitseinstellungen noch die Erlaubnis geben zu müssen, aufs Netzwerk zuzugreifen (Das ist notwendig, da Lucinda ja übers Netzwerk bedient wird).

Warten Sie danach noch rund 5 Minuten, und versuchen Sie dann mit einem Browser oder curl auf <http://localhost:9997/luinda/3.0> zu gehen. Sie sollten eine kurze Antwort mit der Lucinda-Version erhalten. Dann sollten Sie auch auf http://localhost:9997 zugreifen können, und die Such-Oberfläche von Lucinda sehen.
