Drawall
=======

Drawall est un traceur vertical. L'objectif principal du projet est de mettre sa réalisation et son utilisation à la portée de tous, au moyen d'une mise en oeuvre simple et d'une documentation détaillée et partiellemet en français (pour ce fichier et le fichier d'instructions).

**Pour obtenir de l'aide à la réalisation d'un protype de traceur, reportez-vous au dossier [documentation](documentation)**.

**Pour obtenir de l'aide sur l'installation et l'utilisation de la partie logicielle du robot, reportez-vous au [fichier d'instruction](instructions.md).**

La librairie contient tous les calculs nécessaire à l'execution du robot, le sketch ne sert qu'à le commander, il est donc très court et simple à utiliser (reportez-vous au [fichier d'instruction](instructions.md) pour plus de détails).

Ce projet comporte un simulateur qui affiche les déplacements du robot en temps réel sur une interface graphique. Il permet ainsi de tester la bonne prise en charge d'un dessin avant de lancer sa reproduction et facilite également le développement du projet.

Ce projet est libre et évoluera grâce aux retours des utilisateurs. Questions, demande d'informations et suggestions sont donc les bienvenues.

Description des dossiers et fichiers du dépot
---------------------------------------------

- [documentation](documentation) : Dossier contenant de la documentation relative à la réalisation d'un prototype du robot.
- [library](library) : Dossier de la librairie, contenant les fichiers à charger sur la carte Arduino.
- [simulator](simulator) : Dossier du programme simulateur du robot à lancer via le logiciel Processing.
- [computer](computer) : Dossier du programme à executer sur le PC, permetant notament de générer, à partir d'une image, le fichier GCode qui sera analysé par le robot.
- [instructions.md](instructions.md) : Instructions d'installation et d'utilisation du robot, concernant la partie logiciell.
- [COPYING.txt](COPYING.txt) : Texte de licence GPL v3, sous laquelle est publié ce projet.

Licence
-------

Ce projet est libre : vous pouvez le redistribuer ou le modifier suivant les termes de la GNU GPL v3. L'ensemble du projet est publié sous cette licence, ce qui inclut les schémas électroniques, les schémas des pièces matérielles, la documentation utilisateur et développeur, ainsi que l'intégralité du code-source (incluant le programme chargé dans la puce, le simulateur et le logiciel PC). Pour plus de détails, consultez la GNU General Public License, dont vous trouverez une copie sur le fichier COPYING.txt dans le dépot GitHub.

Copyright (c) 2012-2014 Nathanaël Jourdane

Contact et liens
----------------

- Nathanaël Jourdane : nathanael[AT]jourdane[DOT]net.
- [Dépôt GitHub](https://github.com/roipoussiere/Drawall)
- [Une vidéo du premier prototype](http://www.youtube.com/watch?v=ewhZ9wcrR2s)
