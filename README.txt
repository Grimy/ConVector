Drawall
=======

Drawall est un projet libre de robot autonomme qui dessine sur les murs.
Ce projet est libre : vous pouvez le redistribuer ou le modifier suivant les termes de la GNU GPL. Pour plus de détails, consultez la GNU General Public License, dont vous trouverez une copie sur le fichier COPYING.txt dans le dépot GitHub.

L'ensemble du projet est publié sous cette licence, comprenant l'intégralité du code-source, les schémas électroniques, les schémas du matériel et la documentation.

Ce robot utilise une carte Arduino et nécessite donc le logiciel Arduino pour fonctionner. Vous trouverez de l'aide pour son installation et utilisation sur le site officiel http://arduino.cc/fr/.

La partie logicielle est une librairie Arduino. Elle est composée d'un fichier principal drawall.cpp, d'un fichier header drawall.h et d'un fichier de paramètres params.h. Ce dernier permet de spécifier tous les paramètres concernant le robot. Vous devrez l'éditer avant d'utiliser la librairie.
La librairie est utilisée par l'intermédiaire d'un "sketch" Arduino, (fichier .ino), dont vous trouverez des exemples dans le répertoire de la librairie.

La librairie contient tous les calculs nécessaire à l'execution du robot, les sketchs ne servent qu'à le commander, ils sont très courts et simples à utiliser.

Il est possible de commander le robot par des fonctions simples (lignes, courbes, ...), ou par l'intermédiaire d'un fichier svg qu'il va interpréter.
Les fonctions svg ne sont pas encore toutes interprétées, certains dessins ne seront don pas correctement reproduits. Vous pouvez vous référer au fichier d'exemple drawbot.svg dans le dossier examples.

Le projet comporte également un simulateur, qui permet de simuler le fonctionnement du robot sans le brancher. En augmentant la vitesse de traçé, une estimation du dessin est rapidement visualisable.
Ce simulateur interprette directement les impulsions envoyées aux moteurs, il vous faut donc un minimum de matériel pour lancer une simulation : une carte arduino munie d'un port sd, avec une carte contenant une image svg.
Ce simulteur utilise l'environnement de développement Processing, qu'il vous faudra aussi installer : https://www.processing.org/download/. Une aide à l'installation sur Linux est disponible sur le dépot GitHub.

Ce projet est libre et évoluera en fonction des retours des utilisateurs. Questions, demande d'informations et suggestions sont donc les bienvenues.

Copyright (c) 2012-2013 Nathanaël Jourdane

Adresse de contact : nathanael[AT]jourdane[DOT]net.
Lien vers dépôt GitHub : https://github.com/roipoussiere/Drawall
Site web du projet : http://drawall.cc/
Une vidéo de démonstration du robot : http://www.youtube.com/watch?v=ewhZ9wcrR2s
 */
