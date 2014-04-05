/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.fr/
 * 
 * Copyright (c) 2012-2014 Nathanaël Jourdane
 * 
 * Drawall is free software : you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * \file	drawall.h
 * \author  Nathanaël Jourdane
 * \brief   Fichier d'en-tête de la bibliothèque
 * \details L'ensemble du code a été indenté avec le programme indent (sudo apt-get install indent) avec les paramètres suivants :
indent -kr -bad -bbb -sc -ncdw -ss -bfda -blf -ts4 -ut -sob -nlp -ci12 -fc1 drawall.cpp drawall.h pins.h

// finit le dernier trajet au cas ou ça ne tombe pas juste

 * \mainpage Présentation

Drawall est un projet libre de robot autonome qui dessine sur les murs.

Ce projet est libre : vous pouvez le redistribuer ou le modifier suivant les termes de la GNU GPL. L'ensemble du projet est publié sous cette licence, ce qui inclut l'intégralité du code-source, les schémas électroniques, les schémas du matériel et toute la documentation. Pour plus de détails, consultez la GNU General Public License, dont vous trouverez une copie sur le fichier COPYING.txt dans le dépot GitHub. La documentation détaillée du code source est disponible sur http://drawall.cc/. 

Ce robot utilise une carte Arduino et nécessite donc le logiciel Arduino pour fonctionner. Vous trouverez de l'aide pour son installation et utilisation sur le site officiel http://arduino.cc/fr/.

La partie logicielle est une librairie Arduino. Elle est composée d'un fichier principal drawall.cpp, d'un fichier header drawall.h et d'un fichier de paramètres params.h. Ce dernier permet de spécifier tous les paramètres concernant le robot. Vous devrez l'éditer avant d'utiliser la librairie.
La librairie est utilisée par l'intermédiaire d'un "sketch" Arduino, (fichier .ino), dont vous trouverez des exemples dans le répertoire de la librairie.

La librairie contient tous les calculs nécessaire à l'execution du robot, les sketchs ne servent qu'à le commander, ils sont très courts et simples à utiliser.

Il est possible de commander le robot par des fonctions simples (lignes, courbes, ...), ou par l'intermédiaire d'un fichier svg qu'il va interpréter.
Les fonctions svg ne sont pas encore toutes interprétées, certains dessins ne seront don pas correctement reproduits. Vous pouvez vous référer au fichier d'exemple drawbot.svg dans le dossier examples.

Le projet comporte également un simulateur, permetant de tester le bon
fonctionnement d'un programe de dessin avant de le reproduire et faciliter le développement du projet.
Il nécessite l'environnement de développement Processing : http://www.processing.org/. Ce simulateur reproduit le dessin que réalise le robot, en interpretant en temps réel les impulsions envoyées aux moteurs.

Pour le faire fonctionner il vous faut donc connecter à votre ordinateur au minimum une carte arduino munie d'un lecteur SD et y insérer une carte contenant une image svg valide. Toutes les fonctions svg ne sont pas encore interprétées. Pour plus d'informations sur la conformité du fichier svg, référez-vous au document documentation/valid_svg.txt du dépot GitHub. Une aide à l'installation sur Linux est également disponible sur le dépot.

Ce projet est libre et évoluera en grâce aux retours des utilisateurs. Questions, demande d'informations et suggestions sont donc les bienvenues.

Copyright (c) 2012-2013 Nathanaël Jourdane

Adresse de contact : nathanael[AT]jourdane[DOT]net.

Site du projet : http://roipoussiere.github.io/Drawall

Dépôt GitHub : https://github.com/roipoussiere/Drawall

Documentation du projet : http://drawall.cc/

Vidéos de démonstration du robot : http://www.youtube.com/watch?v=ewhZ9wcrR2s - 2ème version : http://www.youtube.com/watch?v=p4oQWtzjBI0&feature=youtu.be

\image html thedrawall.jpg
 */

#ifndef drawall
#define drawall

#include "config.h"
#include <Arduino.h>
#include <math.h>
#include <SD.h>
#include <Servo.h>

/**
 * \brief Classe principale de la librairie
 * \todo Faire une simulation complète très rapide au début avant de commencer le traçé (possible ?) ou créer méthode test(bool testing) pour activer le mode de test.
 * \todo Ajouter méthode rect.
 * \todo Processing : Tracer les mouvement sur un calque séparé (derrière).
 * \todo Processing : Gestion des warnings.
 * \bug Processing : Affiche une mauvais position à la fin du traçé (même pendant ?)
 * \bug Processing : Afficher toujours déplacement en cours à la fin du traçé
 * \bug Système de limites très foireux.
 * \bug Processing : L'affichage de la position de tient pas compte de la position de la feuille.
 * \bug Le crayon va très en bas en fin de traçé (en dehors de limite basse).
 * \bug Moteurs gauche et droit non synchronisés pour les déplacements.
 * \bug Voir quel est le problème avec le dessin de Drawall, il fait n'importe quoi + non détection de la limite basse (cf. capture).
 * \bug Il y a une légère marge en haut du dessin.
 * \bug Pour les surfaces larges, pas de marge en bas du dessin, sans doute lié au bug ci-dessus.
 * \bug Dessin pas centré pour les surfaces larges.
 * \todo Afficher distance tracé + distance move + distance totale + taux d'optimisation (distance tracé / distance totale).
 * \todo Afficher durée + heure de fin estimée.
 * \todo Processing : Mettre au clair variables globales.
 * \todo Processing : Afficher erreur de communication série quand c'est le cas.
 * \todo Tester tous les fichiers avant de commencer à tracer.
 * \todo Possibilité d'insérer une variable dans les codes d'erreurs.
 * \todo Stoquer dans un fichier toutes les erreurs et warnings.
 * \todo Envoyer signal de pause sur le port série lors d'une pause.
 * \bug Lorsqu'on change la vitesse, le traçé se fait en 2 segments.
 */
class Drawall {

  private:

	/**
	 * \brief Liste des données envoyées au pc via le port série.
	*/
	enum SerialData {
		PUSH_LEFT,				// Relâche la courroie gauche d'un cran.
		PULL_LEFT,				// Tire la courroie gauche d'un cran.
		PUSH_RIGHT,				// Relâche la courroie droite d'un cran.
		PULL_RIGHT,				// Tire la courroie droite d'un cran.
		WRITING,				// Le stylo dessine.
		MOVING,					// Le stylo se déplace (ne dessine pas).
		START_MESSAGE,			// Début d'envoie d'un message à afficher.
		END_MESSAGE,			// Début d'envoie d'un message à afficher.
		ENABLE_MOTORS,			// Alimentation des moteurs.
		DISABLE_MOTORS,			// Désalimentation des moteurs.
		SLEEP,					// Mise en pause du programme.
		CHANGE_TOOL,			// Pause pour changement d'outil
		END_DRAWING,			// Fin du dessin.
		WARNING,				// Warning (suivi du code de warning).
		END_WARNING,			// Fin du message de warning.
		ERROR,					// Erreur (suivi du code d'erreur).
		END_ERROR,				// Fin du message d'erreur.
		
		START = 100,			// Amorçage par liason série
		START_INSTRUCTIONS, 	// Début d'envoi des données d'initialisation.
		END_INSTRUCTIONS,		// Fin d'envoi des données d'initialisation.
	};

	/**
	* \brief Liste des erreurs et warnings pouvant survenir pendant l'execution du programme.
	* \details Les erreurs commencent à l'indice 0 tandis que les warnings commencent à l'indice 100.
	* Les warnings sont des simples avertissement qui n'interfèrent pas la course du robot, tandis que
	* les erreurs sont des anomalies critiques qui empèchent ou stoppent la course du robot.
	*/
	enum Error {
		CARD_NOT_FOUND,		// La carte SD n'a pas été trouvée ou est illisible.
		FILE_NOT_FOUND,			// Le fichier n'existe pas.
		FILE_NOT_READABLE,		// Erreur d'ouverture du fichier.
		TOO_SHORT_SPAN,			// La distance entre les 2 moteurs est inférieure à la largeur de la feuille et sa position horizontale.

		// * Warnings *

		UNKNOWN_SERIAL_CODE = 100, // Caractère envoyé au port série non reconnu. Utilisé uniquement sur le programme PC, jamais sur l'Arduino (mais il est préférable que les listes soient identiques).
		WRONG_CONFIG_LINE,		// Ligne mal formée dans le fichier de configuration. Param : n° de la ligne.
		TOO_LONG_CONFIG_LINE,	// Ligne trop longue dans le fichier de configuration. Param : n° de la ligne.
		UNKNOWN_CONFIG_KEY,		// Clé inconnue dans le fichier de configuration. Params : clé, n° de la ligne.
		UNKNOWN_GCODE_FUNCTION,	// Fonction gcode inconnue dans le fichier.
		UNKNOWN_GCODE_PARAMETER,// Paramètre gcode inconnu.
		WRONG_GCODE_PARAMETER,	// Erreur lors de la lecture d'un paramètre. Params : paramètre, n° de la ligne.
		LEFT_LIMIT,				// Le crayon a atteint la limite gauche.
		RIGHT_LIMIT,			// Le crayon a atteint la limite droite.
		UPPER_LIMIT,			// Le crayon a atteint la limite haute.
		LOWER_LIMIT,			// Le crayon a atteint la limite basse.
		FORWARD_LIMIT,			// Le crayon est sorti au maximum.
		REARWARD_LIMIT			// Le crayon est rentré au maximum.
	};

	/**
	 * \brief Position sur la zone de dessin
	 * \details Les différentes position pour accès rapide, correspondant aux 8 points cardinaux, plus le centre.
	 * \bug Ne fonctionne pas dans le sketch.
	 */
	enum Position {
		CENTER,
		UPPER_CENTER,
		LOWER_CENTER,

		LEFT_CENTER,
		RIGHT_CENTER,
		UPPER_LEFT,

		UPPER_RIGHT,
		LOWER_LEFT,
		LOWER_RIGHT
	};

	/************
	* Attributs *
	************/
	// Commencent par m, pour "member".

	/// Objet pour manipuler le servo-moteur, utilisé avec la librairie \a Servo.
	Servo mServo;

	/// Longueur de la courroie gauche, en pas.
	unsigned long mLeftLength;

	/// Longueur de la courroie droite, en pas.
	unsigned long mRightLength;

	/// Le fichier svg contenant le dessin vectoriel à reproduire.
	File mFile;

	/// Échelle générée par les attributs width et height du fichier svg, afin que le dessin s'adapte aux dimentions de la zone de dessin.
	float mDrawingScale;

	/// Offset horizontal généré par l'attribut width du fichier svg, afin que le dessin soit centré sur la zone de dessin.
	unsigned int mDrawingOffsetX;

	/// Offset vertical généré par l'attribut height du fichier svg, afin que le dessin soit centré sur la zone de dessin.
	unsigned int mDrawingOffsetY;

	/// Longueur d'un pas (distance parcourue par la courroie en 1 pas, en mm.
	float mStepLength;

	/// Delai initial entre chaque pas du moteur qui a la plus grande distance à parcourir, (en µs).
	/// \details Le délai de l'autre moteur est calculé pour que les 2 moteurs arrivent au point de destination simultanément.
	float mDelay;

	/// Indique si le robot est en train d'écrire (\a true) ou non (\a false).   
	bool mWriting;

	/// Fonction actuellement en cours d'execution dans le fichier Gcode, utile lorsque le nom de fonction n'est pas spécifié de nouveau (ex : G1 X10 X20). Lorsque aucune fonction n'est en cours, la valeur est 255.
	byte mFunction;

	/************
	* Positions *
	************/

	/// Position horizontale actuelle du crayon sur le plan.
	float mPositionX;

	/// Position verticale actuelle du crayon sur le plan.
	float mPositionY;

	/// Position verticale actuelle du crayon sur le plan.
	float mPositionZ;

	/*************
	* Paramètres *
	*************/

	// Initialisation avant loadParameters()
	// Commencent par mp, pour "member" et "parameter".

	// * Dessin *

	/// Nom du fichier ouvert par defaut.
	char* mpFileName = "drawall.svg";

	/// Distance entre les 2 moteurs.
	unsigned int mpSpan = 1000;

	/// Largeur de la feuille.
	unsigned int mpSheetWidth = 650;

	/// Hauteur de la feuille.
	unsigned int mpSheetHeight = 500;

	/// Position horizontale du coin supérieur gauche de la feuille par rapport au moteur gauche.
	unsigned int mpSheetPositionX = 175;

	/// Position verticale du coin supérieur gauche de la feuille par rapport au moteur gauche.
	unsigned int mpSheetPositionY = 250;

	// * Servo-moteur *

	/// Angle minimal du servo-moteur (en degrés).
	unsigned int mpMinServoAngle = 36;

	/// Angle maximal du servo-moteur (en degrés).
	unsigned int mpMaxServoAngle = 50;

	/// Avancée minimale du crayon (en mm) : nombre négatif quand le crayon est éloigné du mur, positif lorsqu'il le touche et 0 le point de contact avec le mur.
	int mpMinPen = -10;

	/// Avancée maximale du crayon (en mm) : nombre négatif quand le crayon est éloigné du mur, positif lorsqu'il le touche et 0 le point de contact avec le mur.
	int mpMaxPen = 10;

	/// Pause avant le déplacement du servo (en ms).
	unsigned int mpPreServoDelay = 100;

	/// Pause après le déplacement du servo (en ms).
	unsigned int mpPostServoDelay = 500;

	// * Moteurs *

	/// Nombre de pas (prendre en compte les micros-pas effectués par le driver moteur).
	unsigned int mpSteps = 800;

	/// Diamètre du pignon (en µm).
	float mpDiameter = 17.51;

	/// Direction du moteur gauche : \a true pour relâcher la courroie lorsque le moteur tourne dans le sens horaire et \a false dans le cas contraire.
	boolean mpLeftDirection = false;

	/// Direction du moteur droit : \a true pour relâcher la courroie lorsque le moteur tourne dans le sens horaire et \a false dans le cas contraire.
	boolean mpRightDirection = true;

	// * Divers *

	/// Pause avant de commencer à desiner (en ms)
	unsigned int mpInitialDelay = 5000;

	/// Échelle horizontale appliquée au dessin, permetant de le calibrer.
	float mpScaleX = 1;

	/// Échelle verticale appliquée au dessin, permetant de le calibrer.
	float mpScaleY = 1;

	/// Offset horizontal appliqué au dessin, permetant de le décaler.
	int mpOffsetX = -64;

	/// Offset vertical appliqué au dessin, permetant de le décaler.
	int mpOffsetY = 3;

	/// Vitesse par défaut du crayon, avant l'appel éventuel de setSpeed() (en m/s).
	unsigned int mpDefaultSpeed = 20;

	/// Unité utilisée par défaut pour le dessin (si aucune n'est spécifiée dans le fichier G-Code).
	/// Métrique (milimètres) : \a true ; ou anglo-saxone (pouces) : \a false.
	bool mpMetricUnit = true;

	/// Indique si le robot se déplace de manière absolue (par rapport au coin supérieur gauche de la feuille) : \a true, ou relative (par rapport à la dernière position) : false.
	bool mpAbsolutePosition = true;

	/// Affiche (\a true) ou non (\a false) les commentaires du fichier gcode.
	bool mpDisplayComments = false;

	/// Affiche (\a true) ou non (\a false) ce que fait le robot.
	bool mpVerbose = true;

	/// Position par défaut du point de départ du crayon, avant l'appel éventuel de setPosition().
	/// Todo Prendre en charge dans le fichier de paramètres.
	Position mpDefaultPosition = CENTER;

	/// Position du point d'arrivée du crayon
	/// Todo Prendre en charge dans le fichier de paramètres.
	Position mpEndPosition = CENTER;

	/***********
	* Méthodes *
	***********/

	/**
	 */
	void waitUntil(char msg);

	/**
	 * \brief Détermine si la position désirée se trouve dans la zone de dessin (\a true) ou non (\a false).
	 * \details Envoie un warning si c'est le cas.
	 * \return \a true si la position désirée se trouve dans la zone de dessin, \a false sinon
	 * \todo Possibilité de désactiver la correction de trajectoire si en dehors des limites.
	 */
	bool positionInsideSheet(
				float x,
				float y);

	/**
	 * \brief Initialise le ratio entre le nombre de pas et la distance.
	 * \details Le ratio est calculé en fonction du diametre moteur et du nombre de pas.
	 * xx(mm)*ratio --> xx(pas)
	 * xx(pas)/ratio --> xx(mm)
	 * \todo Inverser le ratio car le nombre de pas est une valeur entière, éviter de le diviser.
	 */
	void initStepLength(
				);

	/**
	 * \brief Modifie l'échelle pour s'adapter à la largeur \a width et la hauteur \a height du dessin.
	 * \details Les dimentions du dessin sont récupérées sur le fichier svg.
	 * \todo Changer le nom et retourner l'échelle plutôt que de la modifier directement.
	 */
	void setDrawingScale(
				int width,
				int height);

	/**
	 * \brief Fonction appelée lorsque une erreur se produit.
	 * \details Éloigne le stylo de la paroi et stoppe le programme. Envoie le code d'erreur \a errNumber au PC, qui se charge d'afficher sa description. 
	 * \param p1 1er paramètre du warning (facultatif).
	 * \param p2 2eme paramètre du warning (facultatif).
	 * \todo Mettre en pause le traçé, quand la pause sera opérationnelle.
	 */
	void error(
				Error errorNumber, char* msg = "");

	/**
	 * \brief Fonction appelée lorsque un warning se produit.
	 * \details Envoie le code de warning \a errNumber à Processing, qui se charge d'afficher sa description sans affecter le déroulement du programme.
	 * \param p1 1er paramètre du warning (facultatif).
	 * \param p2 2eme paramètre du warning (facultatif).
	 */
	void warning(
			Error warningNumber, char* msg = "");

	/***********************
	* Commande du matériel *
	***********************/

	/**
	 * \brief Rotation du moteur gauche d'un pas.
	 * \param pull Sens du pas a effectuer : \a true pour tirer, \a false pour relacher.
	 */
	void leftStep(
				bool pull);

	/**
	 * \brief Rotation du moteur droit d'un pas.
	 * \param pull Sens du pas a effectuer : \a true pour tirer, \a false pour relacher.
	 */
	void rightStep(
				bool pull);

	/**
	 * \brief Alimente ou désalimente les moteurs.
	 * \details Éloigne le stylo de la paroi avant la désalimentation pour éviter de dessiner pendant la chute éventuelle du moteur.
	 * \param power \a true pour alimenter le moteur, \a false pour le désalimenter.
 	* \todo Séparer la désactivation moteur gauche et moteur droit.
	 */
	void power(
				bool power);

	/**************
	* Convertions *
	**************/

	/**
	 * \brief Récupère la position horizontale de \a position.
	 * \details Cf. type enum \a Position.
	 * \param position La position a convertir, de type \a Position.
	 * \return La position verticale de \a position.
	 */
	float positionToX(
				Position position);

	/**
	 * \brief Récupère la position verticale de \a position.
	 * \details Cf. type enum \a Position.
	 * \param position La position a convertir, de type \a Position.
	 * \return La position verticale de \a position.
	 */
	float positionToY(
				Position position);

	/**
	 * \brief Calcule la longueur de la courroie gauche pour la position [\a x ; \a y]
	 * \param x La position absolue horizontale du point.
	 * \param y La position absolue horizontale du point.
	 * \return La longueur de la courroie gauche pour cette position, en nombre de pas.
	 */
	long positionToLeftLength(
				float x,
				float y);

	/**
	 * \brief Calcule la longueur de la courroie droite pour la position [\a x ; \a y]
	 * \param x La position absolue horizontale du point.
	 * \param y La position absolue horizontale du point.
	 * \return La longueur de la courroie droite pour cette position, en nombre de pas.
	 */
	long positionToRightLength(
				float x,
				float y);

	/*******************
	* Lecture carte SD *
	*******************/

	/**
	 * \brief Initialise de la carte SD.
	 * \details Peut générer l'erreur 01 : Carte absente ou non reconnue.
	 * \param fileName Le nom du fichier à lire.
	 * \todo : si erreur et nom fichier > 8 car, proposer de vérifer si carte formatée en fat16.
	 */
	void sdInit(
				char *fileName);

	/**
	 * \brief Interprète la fonction gcode passée en paramètre.
	 * \detail Le curseur doit-être devant une fonction g-code, sinon envoie un warning. Ignore les espaces avant la fonction.
	 */
	void processSDLine();


	/**

	*/
	float* get_parameters(String line, int number);

	/**

	*/
	void segment(float x, float y, bool write);

	// void loadTool(int toolNumber);

	/**
	 * \brief Approche ou éloigne le crayon de la paroi.
	 * \param write \a true pour plaquer le crayon contre la paroi (traçé), \a false pour l'éloigner (déplacement)
	 */
	void write(
				bool write);

	/**
	 * \brief Convertit une chaine en valeur booléenne.
	 * \param value Chaine contenant soit "true", soit "false".
	 * \return \a true si la chaine est "true", \a false si la chaine est "false".
	*/
	bool atob(char* value);

	/**
	 * \Brief Charge les paramètres à partir d'un fichier de configuration présent sur la carte SD.
	 * \param fileName Le nom du fichier de configuration à charger.
	*/
	void loadParameters(
				char *fileName);

	/**
	 * \Brief Imprime sur la liaison série les paramètres actuels pour degug.
	*/
	void printParameters(
				);

  public:

	/**
	 * \brief Initialise la librairie.
	 * \bug Corriger Warning
	 */
	 Drawall(
				);

	/**
	 * \brief Démarre la librairie.
	 * \details \b Nécessaire au fonctionnement de la librairie. Réalise les procédures d'initialisation du robot.
	 * \param fileName Nom du fichier de configuration à charger.
	 */
	void begin(
				char *fileName);

	/**
	 * \brief Finit le traçé.
	 * \details Utilisé à la fin du programme. Cela positionne le crayon en bas de la zone de dessin, désalimente les moteurs et met en pause le programme.
	 */
	void end(
				);

	/********************
	* Getters & setters *
	********************/

	/**
	 * \brief Spécifie la position initiale du crayon.
	 * \details À utiliser avant de commencer à tracer.
	 * \param x La position horizontale du crayon.
	 * \param y La position verticale du crayon.
	 */
	void setPosition(
				float x,
				float y);

	/**
	 * \brief Spécifie la position initiale du crayon.
	 * \details À utiliser avant de commencer à tracer.
	 * \param position La position du crayon (voir type enum \a Position).
	 * \bug Ne fonctionne pas sur le sketch.
	 */
	void setPosition(
				Position position);

	/**
	 * \brief Spécifie la vitesse du traçé (en mm/s).
	 * \details Cette vitesse correspond à la vitesse de déplacement de la courroie qui effectue la plus grande distance entre 2 points (Cf. \a mDelay). La vitesse réelle du dessin sera donc plus lente.

	 * \param speed La vitesse du traçé.
	 * \bug La vitesse diminue si on augmente le nombre de pas.
	 */
	void setSpeed(
				unsigned int speed);

	/**********************
	* Fonctions de dessin *
	**********************/

	/**
	 * \brief Déplace le crayon à la position absolue [\a x ; \a y].
	 * \param x La position absolue horizontale du point de destination.
	 * \param y La position absolue verticale du point de destination.
	 */
	void move(
				float x,
				float y);

	/**
	 * \brief Déplace le crayon à la position absolue \a position.
	 * \param position La position absolue du point de destination (Cf type enum \a Position)
	 */
	void move(
				Position position);

	/**
	 * \brief Trace une ligne droite, de la position actuelle à la position absolue [\a x; \a y].
	 * \param x La position absolue horizontale du point de destination.
	 * \param y La position absolue verticale du point de destination.
	 * \bug Fait des escaliers dans certains cas (?).
	 */
	void line(
				float x,
				float y);

	/**
	 *
	 */
	void fastline(float x, float y);
	
	/**
	 * \brief Trace un rectangle avec pour coin supérieur gauche la position actuelle et pour coin inférieur droit la position absolue [\a x; \a y].
	 */
	void rectangle(
			float x,
			float y);

	/**
	 * \brief Trace un rectangle représentant les limites de la zone de dessin.
	 */
	void area(
				);

	/**
	 * \brief Trace un rectangle représentant les limites du dessin.
	 */
void drawingArea(
			char *fileName);

	/**
	 * \brief Trace un dessin correspondant au fichier gcode \a fileName de la carte SD.
	 * \details Le robot doit être muni d'un lecteur de carte SD contenant un fichier gcode.
	 * \param fileName Le nom du fichier gcode à dessiner.
	 * \todo Tester la présence du code M02 (fin du dessin) avant la fin du fichier.
	 */
	void draw(
				char *fileName);

};

#endif
