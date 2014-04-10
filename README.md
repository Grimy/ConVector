DraWall
=======

Drawall est un traceur vertical. L'objectif principal du projet est de mettre sa réalisation et son utilisation à la portée de tous, au moyen d'une mise en œuvre simple et d'une documentation détaillée et partiellement en français (pour ce fichier et le fichier d'instructions).

**Pour obtenir de l'aide à la réalisation d'un prototype de traceur, reportez-vous au dossier [documentation][doc]**.

**Pour obtenir de l'aide sur l'installation et l'utilisation de la partie logicielle du robot, reportez-vous au [fichier d'instuctions][ins]**.

La librairie contient tous les calculs nécessaire à l’exécution du robot, le sketch ne sert qu'à le commander, il est donc très court et simple à utiliser (reportez-vous au fichier d'instruction pour plus de détails).

Ce projet comporte un simulateur qui affiche les déplacements du robot en temps réel sur une interface graphique. Il permet ainsi de tester la bonne prise en charge d'un dessin avant de lancer sa reproduction et facilite également le développement du projet.

Ce projet est libre et évoluera grâce aux retours des utilisateurs. Questions, demande d'informations et suggestions sont donc les bienvenues.

Description des dossiers et fichiers du dépôt
---------------------------------------------

- [documentation][doc] : Dossier contenant de la documentation relative à la réalisation d'un prototype du robot.
- [library][lib] : Dossier de la librairie, contenant les fichiers à charger sur la carte Arduino.
- [simulator][sim] : Dossier du programme simulateur du robot à lancer via le logiciel Processing.
- [computer][com] : Dossier du programme à exécuter sur le PC, permettant notamment de générer, à partir d'une image, le fichier GCode qui sera analysé par le robot.
- [instructions.md][ins] : Instructions d'installation et d'utilisation du robot, concernant la partie logicielle.
- [COPYING.txt][cop] : Texte de licence GPL v3, sous laquelle est publié ce projet.

Licence
-------

Ce projet est libre : vous pouvez le redistribuer ou le modifier suivant les termes de la GNU GPL v3. L'ensemble du projet est publié sous cette licence, ce qui inclut les schémas électroniques, les schémas des pièces matérielles, la documentation utilisateur et développeur, ainsi que l'intégralité du code-source (incluant le programme chargé dans la puce, le simulateur et le logiciel PC). Pour plus de détails, consultez la *GNU General Public License*, dont vous trouverez une copie sur le fichier COPYING.txt dans le dépôt GitHub.

Copyright (c) 2012-2014 Nathanaël Jourdane

Liens et contact
----------------

- [Dépôt GitHub](https://github.com/roipoussiere/Drawall)
- [Suivi de bugs et des améliorations](https://github.com/roipoussiere/Drawall/issues)
- [Documentation développeurs](https://doc.drawbot.cc)
- [Une vidéo du premier prototype](http://www.youtube.com/watch?v=ewhZ9wcrR2s)
- Contacter Nathanaël Jourdane : *contact@drawbot.cc* .

[doc]: https://github.com/roipoussiere/Drawall/tree/master/documentation
[ins]: http://instructions.drawbot.cc/
[sim]: https://github.com/roipoussiere/Drawall/tree/master/simulator
[lib]: https://github.com/roipoussiere/Drawall/tree/master/library
[com]: https://github.com/roipoussiere/Drawall/tree/master/computer
[cop]: https://github.com/roipoussiere/Drawall/blob/master/COPYING.txt

