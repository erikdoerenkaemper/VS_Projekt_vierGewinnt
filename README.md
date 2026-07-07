# LetterBoard

## Idee

Auf einem Gitter von Zellen können Nutzer formatierte (Font, Farbe) Buchstaben eintragen und damit ein Wort bilden.
Das Gitter wird von einem Server verwaltet und der aktuelle Zustand an alle Clienten (Browser) übermittelt.

## Aufbau der Applikation

Das Projekt besteht aus einer Html-Datei `index.html` als Frontend und einem 
Servlet `LetterBoardServlet` als Backend, das ein Web-API implementiert. Im Frontend werden (asynchrone) JavaScript-Funktionen 
benutzt, um die API-Methoden aufzurufen.

## Erzeugung und Nutzung

Die Applikation liegt als Maven-Projekt vor. Es kann in einem IDE (IntelliJ, Netbeans, Eclipse oder VS Code)
geöffnet, editiert und gestartet werden. Das Projekt kann ohne grafisches IDE auch per Maven direkt von der Console erzeugt werden (hier: Linux):

> mvn clean

> mvn package

Für das Deployment sollten die Mechanismen des IDE zur Einrichtung und Verwaltung des
Applikationsservers genutzt werden. Des Weiteren ist auch die testweise eines Containers mit dem Server zur Ausführung der Applikation via `Maven Cargo` möglich.
Beispiel (Installation in einen Tomcat 10-Container hinein) unter Linux:

> mvn verify org.codehaus.cargo:cargo-maven3-plugin:run -Dcargo.maven.containerId=tomcat10x -Dcargo.maven.containerUrl=https://repo.maven.apache.org/maven2/org/apache/tomcat/tomcat/11.0.18/tomcat-11.0.18.zip

Zugriff auf die App im Browser: http://localhost:8080/VS_P08_LetterBoard/

Es kann auch eine Run-Configuration für den Start via IntelliJ erstellt werden. Dazu sollte ggf. vorher
der Ordner .idea gelöscht und durch Neustart von IntelliJ erstellt werden.

## Versionen

### Version v0

Beinhaltet nur die die Basis-Funktionalität: GET und POST sind ausgeführt. 
