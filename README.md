# UGEGreed

Created by Axel BELIN and Thomas VELU for a course in Network programming in JAVA by M. Arnaud CARAYOL.

The [RFC](RFC/rfc-ugegreed-V3.txt) of UGEGreed.  

Check the [report](Rapport_BELIN_VELU.pdf) for more information about the project ! (French only)  

The project has been compiled for your convenience (in Java 19).

## Usage

> Launch in ROOT mode.

```
java -jar --enable-preview ugegreed.jar <LISTENING PORT>
```

> Launch in NODE mode.

```
java -jar --enable-preview ugegreed.jar <LISTENING PORT> <HOST ADDRESS> <HOST LISTENING PORT>
```

> Show route table.

```
route
```

> Disconnect.

```
disconnect
```

> Send work.

```
start url-jar=VALUE fqn=VALUE start-range=LONGVALUE end-range=LONGVALUE filename=FILENAME
```