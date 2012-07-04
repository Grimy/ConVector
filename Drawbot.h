#ifndef Drawbot
#define Drawbot

#include <math.h>
#include <SD.h>
#include <Servo.h> 

#include "Arduino.h"

// définition des pins
#define PIN_CS 10
#define PIN_SERVO A5

// position du servo-moteur (en degrés)
// lorsque le robot n'écrit pas (MIN) - lorsque il écrit (MAX)
#define MIN_SERVO 81
#define MAX_SERVO 91

// spécifications mécaniques du robot
#define NBPAS 200
#define DIAMETRE 20

// différentes étapes des pins du moteur en mode demi-pas:
const int tabMot[8] = {B0101, B0100, B0110, B0010, B1010, B1000, B1001, B0001};

class Draw {
	public:

	/****************
	** construceurs **
	****************/

	// dimentions de la surface (en mm)
	Draw(float surfaceL, float surfaceH);

	// Ajout du nom du fichier .svg si en mode svg
	Draw(float surfaceL, float surfaceH, char *nomFichier);

	/************
	** getters **
	************/

	// renvoie la position courante en X du crayon
	float getaX(void);
	// renvoie la position courante en Y du crayon
	float getaY(void);
	// renvoie la longueur courante de la chaine gauche
	long getA(void);
	// renvoie la longueur courante de la chaine droite
	long getB(void);
	
	/************
	** setters **
	************/

	// initialise la position du curseur, à n'utiliser qu'au début.
	// Par défaut: au centre de la surface
	void setaXY(float aX, float aY);
    
	// initialise le ratio pour le calcul de distance, à n'utiliser qu'au début.
	// Par défaut: 100
	void setRatioDist(float ratioDist);

	// initialise le ratio pour le calcul de vitesse, à n'utiliser qu'au début).
	// Par défaut: 100
	void setRatioVitesse(float ratioVitesse);

	// initialise la vitesse du crayon
	// Par défaut: 100
    void setVitesse(float vitesse);

	// initialise les limites que le crayon ne franchira pas, de chaque cotés de la surface
	// Limite haute: Prévoir une bonne marge, minimum 1/10 de la hauteur de la surface, 
	// pour éviter que le crayon ne se fasse pas tirer dans 2 sens opposés.
	// Une valeur proche de 0 entrainerait un traçé imprécis et risquerait de déteriorer le crayon.
	// Limite gauche et droite: Prévoir une légère marge pour qu'aucune des 2 chaines ne soient
	// jamais à la verticale (dans ce cas l'autre chaine ne serait pas tendue).
	void setlimG(float limG);	// Par défaut: 10
	void setlimD(float limD);	// Par défaut: 10
	void setlimH(float limH);	// Par défaut: 50
	void setlimB(float limB);	// Par défaut: 10
	
	/*****************
	** alimentation **
	*****************/
	
	// alimente ou désalimente le moteur.
	// Attention, selon le poids du crayon, la désalimentation peut entrainer la chute du robot.
	void alimenter(bool alimenter);

	// obligatoire, à placer en début de programme:
	// réalise les procédures d'initialisation nécessaires au fonctionnement du robot.
	void commencer(void);
    
	/*****************
	** deplacements **
	*****************/

	// Quasiment toutes ces fonctions sont doublée,
	// de manière à effectuer le traçé: 
	// - en position absolue (ABS)
	// - en position relative (REL)

	// deplacements (sans écrire)

	// place le crayon au point [x, y]
	void deplacerABS(float x, float y);
	void deplacerREL(float x, float y);

	// place le crayon au centre de la surface.
	void centrer(void);

	// lignes droite:
	
	// trace une ligne droite, du point courant au point [x, y]
	void ligneABS(float x, float y);
	void ligneREL(float x, float y);
	
	// fini le traçé courant:
	// Revient au point cible du dernier déplacement.
	// Ceci permet de faire des formes "finies"
	void finir(void);

	// lignes horizontales/verticales

	// trace une ligne horizontale sur toute la largeur de la surface, à la coordonée y.
	void horizABS(float y);
	void horizREL(float y);

	// trace une ligne verticale sur toute la hauteur de la surface,
	// à la coordonée x.
	void vertiABS(float x);
	void vertiREL(float x);

	// courbes de Bezier cubiques
	
	// trace une courbe de Bézier cubique
	// dont les points de controle P1 et P2 sont respectivement
	// en [x1, y1] et [x2, y2],
	// et dont le point de destination est en [x, y].
	void bezierCubABS(float x, float y, float x1, float y1, float x2, float y2);
	void bezierCubREL(float x, float y, float x1, float y1, float x2, float y2);

	// idem, le point de controle P1 est l'image du point P2 de la dernière courbe de Bézier
	// (cubique ou quadratique) par symétrie avec le point courant.
	// Si la dernière figure n'était pas une courbe de Bézier, il correspond au point courant.
	void bezierCubABS(float x, float y, float x2, float y2);
	void bezierCubREL(float x, float y, float x2, float y2);

	//courbes de Bezier quadratiques

	// trace une courbe de Bézier quadratique dont le point de controle P1 est en [x1, y1]
	// et dont le point de destination est en [x, y].
	void bezierQuadABS(float x, float y, float x1, float y1);
	void bezierQuadREL(float x, float y, float x1, float y1);

	// idem, le point de controle est l'image du point de controle de la dernière courbe de Bézier
	// (cubique ou quadratique) par symétrie avec le point courant.
	// Si la dernière figure n'était pas une courbe de Bézier, il correspond au point courant.
	void bezierQuadABS(float x, float y);
	void bezierQuadREL(float x, float y);
	
	// arcs et cercles
	
	// trace un arc de cercle
	void arcABS(float a1, float a2, float a3, float a4, float a5, float a6, float a7); // non fini
	void arcREL(float a1, float a2, float a3, float a4, float a5, float a6, float a7);
	
	// trace une ellipse dont le centre est les point courant,
	// le rayon horizontal est rx et le rayon vertical est ry.
	void ellipse(float rx, float ry);

	// trace un cercle dont le centre est les point courant et de rayon r.
	void cercle(float r);
	
	void svg(void);

	
	
	private:
	
	
	
	/**********
	** noyau ** (classe à l'origine de tout déplacement)
	**********/

	Servo servo;

	// ligne() n'initialise pas les variables pour les courbes Bezier, à la différence de ligneABS()
	void ligne(float x, float y, bool ecrit);
	
	/*********************
	** lecture carte SD **
	*********************/
	
	// initialisation de la carte SD
	int initSD(char *nomFichier);

	char * nomFichier;

	// recupere une valeur du fichier XML
	// EX: width --> (21cm) OU style --> (fill:none;stroke:#000000)
	void val(const char *requette, char *valeur);

	// recupere une valeur du fichier XML qui est un nombre et le converti en float,
	// en prenant en compte les unités (cm, mm, ...)
	// EX: width --> 210 OU height --> 320
	float valNb(const char *requette);

	// teste si le car est un chiffre ou non
	bool estChiffre(char car);

	// trouve un mot dans le fichier
	// renvoie faux si non trouvé (le curseur sera à la fin du mot)
	boolean trouveSD(const char *mot);

	// récupère nbParams paramètres, les convertis en float et les stoquent dans le tableau tNb
	void params(float *tNb, int nbParams);

	// dessine les traçés en lisant le contenu du fichier .svg
	void dessiner(void);

	// le fichier XML à lire
	File fichier;

	long filG(float x, float y);
	long filD(float x, float y);

	unsigned int aG;
	unsigned int aD;

	// vrai si l'utilisateur souhaite dessiner un .svg
	// faux sinon
	bool modeSVG;

	// masque
	int masqueAlim;

	// si ecrire est vrai, le stylo est contre la paroi, sinon il est éloigné
	void ecrire(bool ecrire);

	//variables:

	float ratioVitesse;
	
	float aX;
	float aY; // position sur le plan
	
	float aXf; // position voulue (fictive) avant qu'elle soit modifiée par les limites
	float aYf;
	
	float surfaceH;
	float surfaceL; // taille du plan (en mm)
	
	float largeur; // surface où l'on peu écrire (limites comprises)
	float hauteur;
	
	int nbPas; // nb de pas des moteurs (on suppose qu'ils sont de même type)
	float diametre;
	
	float ratioDist; // ratio entre le nb de pas et les mm
	
	float delaiBase;
	
	bool ecrireOk;
	
	float ptDepartX;	
	float ptDepartY;
	
	float ptBezierCubX;
	float ptBezierCubY;
	float ptBezierQuadX;
	float ptBezierQuadY;
	
	float limG;
	float limD;
	float limH;
	float limB; //limites que le stylo ne franchira pas, de chaque coté de la surface
};

#endif
