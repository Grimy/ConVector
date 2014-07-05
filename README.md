DraWall
=======

DraWall est un traceur vertical. Il permet de reproduire une image sur un mur ou une autre surface verticale de n'importe quelle largeur (testé de 4cm à 5m).

Les principaux objectifs de ce projet sont :
- de simplifier l'utilisation du robot, au moyen d'une mise en œuvre simple et rapide et via un programme facilitant la création du dessin : aucune connaissance en programmation n'est nécessaire ;
- de faciliter sa réalisation, grâce à une documentation détaillée en français ;
- de s'appuyer sur une conception modulaire, en dissociant chaque étape du processus de réalisation d'un dessin, afin de laisser à l'utilisateur la liberté d'intervenir sur chacune d'entre elles s'il le souhaite ;
- de faire évoluer constamment ce projet en ajoutant régulièrement de nouvelles fonctionnalités et en améliorant celles déjà implémentées, en se basant sur les retours des utilisateurs ;
- de publier tous les éléments de ce projet sous licence libre afin que chacun puisse comprendre comment il fonctionne, le copier, le redistribuer et le modifier ;
- par ces différents critères, de développer autour de ce projet une communauté composée d'artistes, de *makers* et toute les autres personnes trouvant ce projet intéressant.

**Pour obtenir de l'aide sur l'installation et l'utilisation de la partie logicielle du robot, reportez-vous au [fichier d’instruction][ins].**

**Pour obtenir de l'aide à la réalisation d'un prototype de traceur, reportez-vous au dossier [documentation][doc]**.

Ce projet comporte un simulateur qui affiche les déplacements du robot en temps réel sur une interface graphique. Il permet ainsi de tester la bonne prise en charge d'un dessin avant de lancer sa reproduction et facilite également le développement du projet.

Ce projet est libre et évoluera grâce aux retours des utilisateurs. Questions, demande d'informations et suggestions sont donc les bienvenues.

Principe de fonctionnement
--------------------------

Le projet est composé d'un programme qui convertit une image vectorielle ou bitmap en fichier G-code exploitable par le robot. G-code est un format de fichier utilisé dans l'industrie pour commander les machines-outil à commande numérique (plus d'information [ici](http://fr.wikipedia.org/wiki/G-code)). Une fois ce fichier généré, il est nécessaire de le copier sur une carte SD, puis d'insérer celle-ci dans le lecteur de carte du robot. Ensuite, le robot reproduit l'image en interprétant les instructions du fichier G-code. Le fait de passer par un fichier intermédiaire permet une très grande liberté quand à la manière dont est dessinée l'image.

Description des dossiers et fichiers du dépôt
---------------------------------------------

- [documentation][doc] : Dossier contenant de la documentation relative à la réalisation d'un prototype du robot.
- [library][lib] : Dossier de la librairie, contenant les fichiers à charger sur la carte Arduino.
- [simulator][sim] : Dossier du programme simulateur du robot à lancer via le logiciel Processing.
- [computer][com] : Dossier du programme à exécuter sur le PC, permettant notamment de générer, à partir d'une image, le fichier G-code qui sera analysé par le robot.
- [instructions.md][ins] : Instructions d'installation et d'utilisation du robot, concernant la partie logicielle.
- [COPYING.txt][cop] : Texte de licence GPL v3, sous laquelle est publié ce projet.
- [SD_files][sd] : Dossier contenant les fichiers à placer sur la carte SD :
	- [drawall.dcf][dcf] : fichier de configuration du dessin à éditer régulièrement en fonction de vos besoin ;
	- [drawing.ngc][ngc] : fichier G-code de test, permettant de tester le bon fonctionnement du traceur.
	_**Note :** Dans une prochaine version du programme, il sera possible d'envoyer ces fichiers sur la carte SD directement à travers la liaison série._

Contact et conditions d'utilisations
------------------------------------

Contact : contact@drawall.cc.

Ce projet est libre : vous pouvez le redistribuer ou le modifier suivant les termes de la GNU GPLv3. L'ensemble du projet est publié sous cette licence, ce qui inclut les schémas électroniques, les schémas des pièces matérielles, la documentation utilisateur et développeur, ainsi que l'intégralité du code-source (incluant le programme chargé dans la puce, le simulateur et le logiciel PC). Pour plus de détails, consultez la [*GNU General Public License*][cop].

Copyright (c) 2012-2014 Nathanaël Jourdane

Remerciements
-------------

- Le *[FabLab de Toulouse](http://www.artilect.fr/)*, pour m'avoir permis de partager mes expériences ;
- L'équipe du projet *[Datacentre d'art](http://www.ordigami.net/circuit-beant.php)*, pour ma première collaboration avec une équipe tierce et pour avoir intégré mon projet à un véritable projet artistique.

[doc]: https://github.com/roipoussiere/Drawall/tree/master/documentation
[dev]: https://github.com/roipoussiere/DraWall/blob/master/instructions.md#note-aux-d%C3%A9veloppeurs
[ins]: http://instructions.drawbot.cc/
[sim]: https://github.com/roipoussiere/Drawall/tree/master/simulator
[lib]: https://github.com/roipoussiere/Drawall/tree/master/library
[com]: https://github.com/roipoussiere/Drawall/tree/master/computer
[cop]: https://github.com/roipoussiere/Drawall/blob/master/COPYING.txt
[sd]: https://github.com/roipoussiere/Drawall/blob/master/library/SD_files
[dcf]: library/SD_files/drawall.dcf
[ngc]: library/SD_files/drawall.ngc
