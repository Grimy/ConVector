Drawbot
=======

Drawbot est un projet de robot qui dessine sur les murs.

Ce robot utilise une carte Arduino et nécessite par conséquent le logiciel Arduino pour fonctionner.
Je vous invite à vous documenter sur internet pour l'installation de celui-ci, vous y trouverez beaucoup d'information et d'aide.

La partie logicielle se compose en un programme principal (le fichier .ino) et une librairie (les fichiers drawbot.cpp et drawbot.h).
La librairie contient tous les calculs a executer pour faire fonctionner le robot.
Le fichier .ino ne sert qu'à initialiser le robot et appeler la librairie.
Le programme principal est paramétré pour que le robot déssine sur une feuille au format 65*50cm en mode paysage.
Lors de l'installation, il vous faut aussi respecter des distances précises : un petit schéma d'instalation est présent sur le dépot.
Toutefois vous pouvez facilement l'éditer pour modifier les paramètres que vous voulez.

Le projet comporte également un simulateur, qui permet de simuler le fonctionnement du robot sans le brancher.
Attention toutefois, le simulateur lis directement les impulsions électriques envoyées aux moteurs.
Il vous faut donc connecter à vote ordinateur une carte arduino munie d'un port sd, avec une carte contenant une image svg dessus pour faire fonctionner ce simulateur.
Ce simulteur utilise l'environnement de développement Processing, il vous faut donc l'installer.
Une aide à l'installation sur Linux est disponible sur le dépot.

Le logiciel et le matériel ont été intégralement développé par Nathanaël Jourdane.
Ce projet évoluera en fonction des retours des utilisateurs. N'hésitez pas à poser des questions, demander d'avantage d'information pour le montage et éventuellement proposer des évoluions.
Pour toute question vous pouvez me contacter par mail à roipoussiere[at]gmail[dot]com .
