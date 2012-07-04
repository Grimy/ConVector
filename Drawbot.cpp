#include <Drawbot.h>

Draw::Draw(float surfaceL, float surfaceH, char * nomFichier)
{
	// pins des moteurs en sortie
	DDRC = B001111;
	DDRD = B00111100;

	// dimentions de la surface
	this->surfaceL = surfaceL;
	this->surfaceH = surfaceH;

	this->nomFichier = nomFichier;

	// pour que ecrireOk() fonctionne la 1ere fois
	this->ecrireOk = true;

	// On passe en mode de dessin svg
	this->modeSVG = true;
}

Draw::Draw(float surfaceL, float surfaceH)
{
	// pins des moteurs en sortie
	DDRC = B001111;
	DDRD = B00111100;

	// dimentions de la surface
	this->surfaceL = surfaceL; //surfaceL;
	this->surfaceH = surfaceH; //surfaceH;

	// pour que ecrireOk() fonctionne la 1ere fois
	this->ecrireOk = true;

	// On n'est pas en mode de dessin svg
	this->modeSVG = false;
}

void Draw::commencer(void)
{
	// float echL;
	// float echH;

	Serial.print("surface: ");
	Serial.print(this->surfaceL);
	Serial.print(" * ");
	Serial.println(this->surfaceH);

	// fixe les limites par defaut que le stylo ne franchira pas
	// pour chaque coté de la surface
	setlimG(180);
	setlimD(180);
	setlimH(250);
	setlimB(10);

	// initialise la position du stylo au centre de la surface
	this->aX = this->largeur / 2;
	this->aY = this->hauteur / 2;

	Serial.print("position: ");
	Serial.print(this->aX);
	Serial.print(" , ");
	Serial.println(this->aY);

	setRatioDist(1.00);

	// calcul de la longueur des fils au début
	this->aG = filG(this->aX, this->aY);
	this->aD = filD(this->aX, this->aY);

	// le point de départ de la 1ere figure est le point courant
	this->ptDepartX = this->aX;
	this->ptDepartY = this->aY;

	setRatioVitesse(1.00);
	
	// une petite vitesse par défaut
	// pour que le moteur bouge même si on ne spécifie rien
	setVitesse(10);

	// *** envoi des données d'initialisation ***

	// caractère pour commencer l'init
	Serial.print('\t');
	
	Serial.print(this->surfaceL);
	Serial.print(',');
	Serial.print(this->surfaceH);
	
	Serial.print(',');
	Serial.print(this->aG);
	Serial.print(',');
	Serial.print(this->aD);
	
	Serial.print(',');
	Serial.print(this->ratioDist);

	Serial.print(',');
	Serial.print(this->limG);
	Serial.print(',');
	Serial.print(this->limD);

	Serial.print(',');
	Serial.print(this->limH);
	Serial.print(',');
	Serial.print(this->limB);
	
	// caractère de fin d'init
	Serial.println();

	// si on est en mode SVG
	if (this->modeSVG)
	{
		// initialisation SD et stoque le code d'erreur
		int err = initSD(this->nomFichier);

		// S'il y a eu une erreur au constructeur,
		// Affiche l'erreur et quitte
		if (err != 0)
		{
			Serial.print('E');
			Serial.print('0');
			Serial.print(err);
			return;
		}
		

/*		echL = this->surfaceL / valNb("width");
		echH = this->surfaceH / valNb("height");

		if (echL > echH)
		{
			setRatioDist(echH);
		}
		else
		{
			setRatioDist(echL);
		}
*/
	}
	// Si on est pas en mode SVG
	else
	{
		// calcule le ratio pour le rapport pas/distance
		// setRatioDist(1.00); déplaçé plus haut
	}

	// associe le servo-moteur au bon pin
	// servo.attach(PIN_SERVO);
	pinMode(PIN_SERVO, OUTPUT);
	digitalWrite(PIN_SERVO, LOW);
	
	// alimente le moteur
	alimenter(true);
}

float Draw::getaX(void)
{
	return this->aX;
}

float Draw::getaY(void)
{
	return this->aY;
}

// renvoie la longueur du fil (en pas) en fonction de la position actuelle
long Draw::getA(void)
{
	return this->aG;
}

long Draw::getB(void)
{
	return this->aD;
}

// vitesse du moteur (en tr/min)
void Draw::setVitesse(float vitesse)
{
	// delai entre chaque pas, en micro-secondes
	this->delaiBase = (this->ratioVitesse)*60000000 / (vitesse*float(NBPAS));
}

void Draw::setlimG(float limG)
{
	this->limG = limG;

	// mise à jour de la largeur
	this->largeur = this->surfaceL - this->limG - this->limD;
}

void Draw::setlimD(float limD)
{
	this->limD = limD;

	// mise à jour de la largeur
	this->largeur = this->surfaceL - this->limG - this->limD;
}

void Draw::setlimH(float limH)
{
	this->limH = limH;

	// mise à jour de la hauteur
	this->hauteur = this->surfaceH - this->limH - this->limB;
}

void Draw::setlimB(float limB)
{
	this->limB = limB;

	// mise à jour de la hauteur
	this->hauteur = this->surfaceH - this->limH - this->limB;
}

void Draw::setaXY(float aX, float aY)
{
	this->aX = aX;
	this->aY = aY;
}

// xx(mm)*ratio --> xx(pas)
// xx(pas)/ratio --> xx(mm)
void Draw::setRatioDist(float ratioDist)
{
	// ratio calculé en fonction du diametre moteur et du nb de pas
	// *2 car en mode demi-pas il faut 2 fois plus de pas
	this->ratioDist = ratioDist * float(NBPAS*2) / (3.1415*DIAMETRE);
}

void Draw::setRatioVitesse(float ratioVitesse)
{
	this->ratioVitesse = ratioVitesse;
}

// renvoie la longueur du fil (en pas) en fonction de la position donnée (en mm)
long Draw::filG(float x, float  y)
{
	return sqrt ( pow((x + this->limG) * this->ratioDist, 2)
	+ pow((y + this->limH) * this->ratioDist, 2) );
}

long Draw::filD(float x, float  y)
{
	return sqrt ( pow((this->surfaceL - x - limG) * this->ratioDist, 2)
	+ pow((y + this->limH) * this->ratioDist, 2) );
}

void Draw::alimenter(bool alimenter)
{
	if (alimenter)
	{
		this->masqueAlim = B1111;

		// Processing: a=alimenter
		Serial.print('a');
	}
	else
	{
		this->masqueAlim = B0000;
		// éloigne le stylo avant de couper tout
		ecrire(false);
		
		// Processing: b=désalimenter
		Serial.print('b');
	}

	PORTC = B001111 | this->masqueAlim;
	PORTD = B00111100 | this->masqueAlim << 2;

}

// approche ou eloigne le stylo pour écrire ou non
void Draw::ecrire(bool ecrireOk)
{
	// si on veut ecrire et que le stylo n'ecrit pas
	if (ecrireOk && !this->ecrireOk)
	{
		// servo.write(MIN_SERVO);
		digitalWrite(PIN_SERVO, HIGH);
		delay(1000);
		digitalWrite(PIN_SERVO, LOW);

		// Processing: w=ecrire
		Serial.print('w');

		this->ecrireOk = true;
	}
	
	// si on ne veut pas ecrire et que le stylo ecrit
	else if (!ecrireOk && this->ecrireOk)
	{
		// servo.write(MAX_SERVO);
		digitalWrite(PIN_SERVO, HIGH);
		delay(1000);
		digitalWrite(PIN_SERVO, LOW);

		// Processing: x=pas ecrire	
		Serial.print('x');
		this->ecrireOk = false;
	}
}

void Draw::ligne(float bX, float bY, bool ecrit)
{
	// aX, aY: Position du point courant
	// aG, aD: Longueur actuelle des fils.

	// bX, bY: position du point de destination.
	// bG, bD: Longueur des fils à la destination.

	// nbPasG, nbPasD: Nombre de pas à faire pour aller à destination.

	// delaiG, delaiD: Delai avant chaque pas.

	// 1) on calcule la longueur des fils à la destination
	// 2) on fait la différence par rapport à la longueur actuelle
	// 3) Calcule le délai en fonction du rapport de longeur des fils

	// stoque la position voulue
	// avant qu'elle soit modifiée par les limites
	this->aXf = bX;
	this->aYf = bY;

	// contrôle des limites, n'ecris pas si en dehors
	if (bX < 0 || bX > this->largeur || bY < 0 || bY > this->hauteur)
	{
		if (bX < 0)
			bX = 0;
		if (bX > this->largeur)
			bX = this->largeur;
		if (bY < 0)
			bY = 0;
		if (bY > this->hauteur)
			bY = this->hauteur;

		ecrire(false);
	}
	else
	{
		// s'il est dedans et qu'il doit ecrire, écrit
		if (ecrit)
		{
			ecrire(true);
		}
		else
			ecrire(false);
	}

	// longueur fils à la destination (en pas)
	long bG = filG(bX, bY);
	long bD = filD(bX, bY);

	// nombre de pas à faire
	long nbPasG = bG-this->aG;
	long nbPasD = bD-this->aD;

	int sensG = 0;
	int sensD = 0;

	float delaiG;
	float delaiD;

	long dernierTempsG;
	long dernierTempsD;

	// calcul de la direction
	if (nbPasG > 0)
		sensG = 1;
	else if(nbPasG < 0)
		sensG = -1;

	if (nbPasD > 0)
		sensD = 1;
	else if(nbPasD < 0)
		sensD = -1;

	// on a le sens
	// donc on peut retirer le signe pour simplifier les calculs
	nbPasG = fabs(nbPasG);
	nbPasD = fabs(nbPasD);

	if (nbPasG > nbPasD)
	{
		delaiG = this->delaiBase;
		delaiD = this->delaiBase * (float(nbPasG) / float(nbPasD));
	}
	else
	{
		delaiD = this->delaiBase;
		delaiG = this->delaiBase * (float(nbPasD) / float(nbPasG));
	}

	dernierTempsG = micros();
	dernierTempsD = micros();

	while(nbPasG > 0 || nbPasD > 0)
	{
		// si le delai est franchi et qu'il reste des pas à faire
		if ((nbPasG > 0) && (micros() - dernierTempsG >= delaiG))
		{
			// stoque le temps actuel dans lastTimer
			dernierTempsG = micros();

			// incremente ou decremente (en fonction de la direction)
			this->aG += sensG;
			
			// decremente le nb de pas restants
			nbPasG--;
			
			// on écrit que sur les 4 bits & coupe tout si c'est pas alimenté & le code de déplacement
			PORTC = B001111 & tabMot[this->aG%8];

			// Processing: f=motG-- ; h=motG++
			Serial.print(char('g' + sensG));
		}

		if ((nbPasD > 0) && (micros() - dernierTempsD >= delaiD))
		{
			// stoque le temps actuel dans lastTimer
			dernierTempsD = micros();

			// incremente ou decremente (en fonction de la direction)
			this->aD += sensD;
			
			// decremente le nb de pas restants
			nbPasD--;

			// on écrit que sur les 4 bits & coupe tout si c'est pas alimenté & le code de déplacement
			PORTD = B00111100 & (tabMot[this->aD%8] << 2);

			// Processing: c=motD-- ; e=motD++
			Serial.print(char('d' + sensD));
		}
	}

	this->aX = bX;
	this->aY = bY;
}

void Draw::ligneABS(float x, float y)
{
	this->ptBezierQuadX = 0;
	this->ptBezierQuadY = 0;
	this->ptBezierCubX = 0;
	this->ptBezierCubY = 0;
	
	int longmax = 20;
	
	float longX = abs(x-this->aX);
	float longY = abs(y-this->aY);

	float miniX;
	float miniY;
	int boucle;

	if (longX > longmax || longY > longmax)
	{
		if( longX > longY )
			boucle = ceil(longX/longmax);
		else
			boucle = ceil(longY/longmax);
			
		miniX = ((x-this->aX) / boucle);
		miniY = ((y-this->aY) / boucle);
		
		for(int i=0; i<boucle; i++)
		{
			ligne(this->aX + miniX, this->aY + miniY, true);
		}
	}
	
	// si c'est une petite longueur, on y va directement
	// et puis pour finir au cas ou ça tombe pas juste avec les boucles
	ligne(x, y, true);
}

void Draw::ligneREL(float x, float y)
{
	ligneABS(this->aX + x , this->aY + y);
}

void Draw::finir(void)
{
	ligneABS(this->ptDepartX, this->ptDepartY);
}

void Draw::deplacerABS(float x, float y)
{
	ligne(x, y, false);
	
	this->ptDepartX = x;
	this->ptDepartY = y;
}

void Draw::deplacerREL(float x, float y)
{
	deplacerABS(this->aX+x, this->aY+y);
}

void Draw::centrer(void)
{
	deplacerABS(this->largeur/2 , this->hauteur/2);
}

void Draw::horizABS(float y)
{
	deplacerABS(0, y);
	ligneABS(this->largeur, y);
}

void Draw::horizREL(float y)
{
	horizABS(this->aY+y);
}

void Draw::vertiABS(float x)
{
	deplacerABS(x, 0);
	ligneABS(x, this->hauteur);
}

void Draw::vertiREL(float x)
{
	vertiABS(this->aX+x);
}

void Draw::bezierCubABS(float x1, float y1, float x2, float y2, float x, float y)
{
	// B(t) = P0(1-t)^3 + 3*P1*t(1-t)^2 + 3*P2*t^2*(1-t) + P3*t^3
	// t=[0,1]

	// P0 = pt source
	// P1-P2 = pts de controles
	// P3 = pt cible

	float x0 = this->aX;
	float y0 = this->aY;
	float ptx, pty;
	float t;

	for(t=0; t<=1; t+=0.01)
	{
		ptx = x0*pow((1-t),3) + 3*x1*t*pow(1-t, 2) + 3*x2*pow(t, 2)*(1-t) + x*pow(t,3);
		pty = y0*pow((1-t),3) + 3*y1*t*pow(1-t, 2) + 3*y2*pow(t, 2)*(1-t) + y*pow(t,3);
		ligne(ptx, pty, true);
	}
	
	// finit le dernier trajet au cas ou ça ne tombe pas juste
	ligne(x, y, true);

	this->ptBezierQuadX = 0;
	this->ptBezierQuadY = 0;
	this->ptBezierCubX = x2;
	this->ptBezierCubY = y2;
}

void Draw::bezierCubABS(float x2, float y2, float x, float y)
{
	float x1, y1;
	
	if(this->ptBezierCubX == 0 && this->ptBezierCubY == 0)
	{
		x1 = x;
		y1 = y;
	}
	else
	{
		x1 = 2*this->aXf-this->ptBezierCubX;
		y1 = 2*this->aYf-this->ptBezierCubY;
	}
	
	bezierCubABS(x1, y1, x2, y2, x, y);
}

void Draw::bezierCubREL(float x1, float y1, float x2, float y2, float x, float y)
{
	bezierCubABS(this->aX+x1, this->aY+y1, this->aX+x2, this->aY+y2, this->aX+x, this->aY+y);
}

void Draw::bezierCubREL(float x2, float y2, float x, float y)
{
	bezierCubABS(this->aX+x2, this->aY+y2, this->aX+x, this->aY+y);
}

void Draw::bezierQuadABS(float x1, float y1, float x, float y)
{
	// B(t) = P0*(1-t)^2 + 2*P1*t(1-t) + P2*t^2
	// t=[0,1]

	// P0 = pt source
	// P1 = pt de controle
	// P2 = pt cible

	float x0 = this->aX;
	float y0 = this->aY;
	float ptx, pty;

	for(float t=0; t<=1; t+=0.01)
	{
	    ptx = x0*pow(1-t, 2) + 2*x1*t*(1-t) + x*pow(t, 2);
	    pty = y0*pow(1-t, 2) + 2*y1*t*(1-t) + y*pow(t, 2);
		ligne(ptx, pty, true);
	}
	ligne(x, y, true);

	this->ptBezierCubX = 0;
	this->ptBezierCubY = 0;
	this->ptBezierQuadX = x1;
	this->ptBezierQuadY = y1;
}

void Draw::bezierQuadABS(float x, float y)
{
	float x1, y1;
	if(this->ptBezierQuadX == 0 && this->ptBezierQuadY == 0)
	{
		x1 = x;
		y1 = y;
	}
	else
	{
		x1 = 2*this->aXf-this->ptBezierQuadX;
		y1 = 2*this->aYf-this->ptBezierQuadY;
	}

	bezierQuadABS(x1, y1, x, y);
}

void Draw::bezierQuadREL(float x1, float y1, float x, float y)
{
	bezierQuadABS(this->aX+x1, this->aY+y1, this->aX+x, this->aY+y);
}

void Draw::bezierQuadREL(float x, float y)
{
	bezierQuadABS(this->aX+x, this->aY+y);
}

void Draw::arcABS(float a1, float a2, float a3, float a4, float a5, float a6, float a7)
{
	// à compléter
}

void Draw::arcREL(float a1, float a2, float a3, float a4, float a5, float a6, float a7)
{
	// à compléter
}

void Draw::ellipse(float rx, float ry)
{
	// ratio pour déterminer
	// la distance du point de controle en fonction du rayon
	float k = 0.551915;

	// pour simplifier les formules
	float x = this->aX;
	float y = this->aY;

	// se déplace à gauche du cercle
	deplacerABS(x-rx, y);

	bezierCubABS(x-rx,y-ry*k , x-rx*k,y-ry , x,y-ry);
	bezierCubABS(x+rx,y-ry*k , x+rx,y);
	bezierCubABS(x+rx*k,y+ry , x,y+ry);
	bezierCubABS(x-rx,y+ry*k , x-rx,y);

	// revient au centre du cercle
	deplacerABS(x, y);
}

void Draw::cercle(float r)
{
	ellipse(r, r);
}

/******************************
* fonctions pour lire les svg *
******************************/

int Draw::initSD(char * nomFichier)
{
	// pin 10 en sortie pour etre sur qu'il ne sera pas utilisé
	pinMode(PIN_CS, OUTPUT);

	if (!SD.begin(PIN_CS))
	{
		// Err. 01 : Carte absente ou non reconnue.
		return 01;
	}

	// ouvre le fichier
	this->fichier = SD.open(nomFichier);

	// si erreur d'ouverture de fichier, arrete.
	if (!this->fichier)
	{
		// Err. 02 : Erreur d'ouverture de fichier.
		return 02;
	}

	return 0;
}

// recuperation des valeurs sous forme de chaine
void Draw::val(const char * requette, char * valeur)
{
	// requette: ce que l'on veut obtenir, ex: (svg)
	// requ: requette + (=") , ex: (svg=")
	// valeur: valeur de la requette
	// ex: (21cm) ou (fill:none;stroke:#000000)

	// crée une chaine de la meme taille que valeur (+2 pour (=") )
	char requ[strlen(requette)+2];
	
	char car = ' ';
	int i = 0;
	
	// copie la chaine cherchée et ajoute le (=")	
	strcpy(requ, requette);
	strcat(requ, "=\"");

	// se positionne à la fin de la requette cherchée + (=")
	// pour récupérer la valeur de la requette
	trouveSD(requ);
	
	// tant qu'il y a qqch à lire
	// et qu'on n'est pas rendu au (") correspondant à la fin de la valeur
	while( this->fichier.available() && car != '"')
	{
		// lis
		car = this->fichier.read();

		// stocke le car.
		valeur[i] = car;

		i++;
	}

	// quand on a fini, remplace le (") par le car. de fin de chaine.
	valeur[i-1] = '\0';
}

// Renvoie la valeur du nombre.
// Pre-condition : Le curseur doit être positionné au début d'un nombre
// Parcours le nombre jusqu'au prochain caractère qui n'est pas un chiffre.
// Converti le nombre en float

bool Draw::estChiffre(char car)
{
	int i;

	// caractères considérés comme étant des chiffres
	const char * chiffres = "-.0123456789";

	// parcours le tableau de chiffres
	for(i=0; chiffres[i] != '\0'; i++)
	{
		// si c'est un chiffre, renvoie vrai
		if (car == chiffres[i])
			return true;
	}
	
	// si on est là c'est qu'aucun chiffre n'a été trouvé, renvoie faux
	return false;
}

float Draw::valNb(const char * requette)
{
	// nombre de max 20 caractères
	char chaine[20+1];

	// se positionne en début de fichier
	this->fichier.seek(0);

	// recuperation des valeurs sous forme de chaine
	val(requette, chaine);

	// unité de mesure de la valeur : (cm) ou (mm)
	char unite[3];

	// chaine contenant le nombre : (-3456.345)
	char nbCh[strlen(chaine)];

	// chaine convertie en float
	float nbFl;

	int i;

	// taille des 2 chaines
	int tNbCh = 0;
	int tUnite = 0;

	char car;
	
	// parcourt toute la chaine.
	for(i=0; chaine[i] != '\0'; i++)
	{
		if ( estChiffre(chaine[i]) )
		{
			// remplit nbCh avec les chiffres
			nbCh[tNbCh] = chaine[i];

			tNbCh++;
		}

		else
		{
			unite[tUnite] = chaine[i];
			tUnite++;
		}
	}

	// met la fin de chaine
	nbCh[tNbCh] = '\0';
	unite[tUnite] = '\0';

	// converti la chaine en float
	nbFl = atof(nbCh);

	// On converti les unités en px (unités utilisateur) :

	// px (aucun changement)
	if (!strcmp(unite, "px"))
		nbFl *= 1;

	// pt (*1.25)
	else if (!strcmp(unite, "pt"))
		nbFl *= 1.25;
	
	// pc (*15)
	else if (!strcmp(unite, "pc"))
		nbFl *= 15;

	// mm (*3.543307)
	else if (!strcmp(unite, "mm"))
		nbFl *= 3.543307;

	// cm (*35.43307)
	else if (!strcmp(unite, "cm"))
		nbFl *= 35.43307;

	// in (*90)
	else if (!strcmp(unite, "in"))
		nbFl *= 90;

	// si l'unité n'est pas spécifiée, c'est l'unité utilisateur par défaut
	else
		nbFl *= 1;

	return nbFl;
}

boolean Draw::trouveSD(const char * mot)
{
	int i = 0;
	char car;
		
	// tant qu'il y a qqch à lire
	while( this->fichier.available() )
	{
		car = this->fichier.read();
		
		// si le caractère correspond, passe au suivant
		if (car == mot[i])
		{
			i++;

			// si fin du mot (mot trouvé)
			if(mot[i] == '\0')
				// on s'est bien positionné, opération réeussie! :)
				return true;
		}
		// si un des car. n'est pas bon on reprends depuis le debut
		else
			i=0;
	}

	// si on est là c'est qu'on a pas trouvé le mot, fail!
	return false;
}

void Draw::dessiner(void)
{
	char car;

	// tableau contenant les valeurs à lançer dans la requette
	// (max 7 valeurs avec la fonction arc() )
	float tNb[7];

	// tant qu'il y a qqch à lire
	while( this->fichier.available() )
	{
		car = this->fichier.read();

		// Appelle les fonctions de dessin svg en fonction du caractère détecté
		switch (car)
		{
			// deplacerABS (2 args)
			case 'M':
				// lis les paramètres tant qu'il y a des nombres
				do
				{
					// la fonction deplacerABS() attends 2 paramètres
					// on les récupère sous forme de tableau (tNb)
					params(tNb, 2);

					// appel de la fonction
					deplacerABS (tNb[0], tNb[1]);
				}
				// teste le prochain car (le curseur ne bouge pas)
				while ( estChiffre(this->fichier.peek()) );

			break;

			// deplacerREL (2 args)
			case 'm':
				do
				{
					params(tNb, 2);
					deplacerREL (tNb[0], tNb[1]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// finir (aucun arg)
			case 'Z':
				finir ();
			break;

			// finir (aucun arg)
			case 'z':
				finir ();
			break;

			// ligneABS (2 args)
			case 'L':

				do
				{
					params(tNb, 2);
					ligneABS (tNb[0], tNb[1]);
				}
				while ( estChiffre(this->fichier.peek()) );

			break;

			// ligneREL (2 args)
			case 'l':
				do
				{
					params(tNb, 2);
					ligneREL (tNb[0], tNb[1]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// horizABS (1 arg)
			case 'H':
				do
				{
					params(tNb, 1);
					horizABS (tNb[0]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// horizREL (1 arg)
			case 'h':
				do
				{
					params(tNb, 1);
					horizREL (tNb[0]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// vertiABS (1 arg)
			case 'V':
				do
				{
					params(tNb, 1);
					vertiABS (tNb[0]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// vertiABS (1 arg)
			case 'v':
				do
				{
					params(tNb, 1);
					vertiREL (tNb[0]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;
			
			// bezierCubABS (6 args)
			case 'C':
				do
				{
					params (tNb, 6);
					bezierCubABS (tNb[0], tNb[1], tNb[2], tNb[3], tNb[4], tNb[5]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// bezierCubREL (6 args)
			case 'c':
				do
				{
					params (tNb, 6);
					bezierCubREL (tNb[0], tNb[1], tNb[2], tNb[3], tNb[4], tNb[5]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;
			
			// bezierCubABS (4 args)
			case 'S':
				do
				{
					params (tNb, 4);
					bezierCubABS (tNb[0], tNb[1], tNb[2], tNb[3]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// bezierCubREL (4 args)
			case 's':
				do
				{
					params (tNb, 4);
					bezierCubREL (tNb[0], tNb[1], tNb[2], tNb[3]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;
			
			// bezierQuadABS (4 args)
			case 'Q':
				do
				{
					params (tNb, 4);
					bezierQuadABS (tNb[0], tNb[1], tNb[2], tNb[3]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// bezierQuadREL (4 args)
			case 'q':
				do
				{
					params (tNb, 4);
					bezierQuadREL (tNb[0], tNb[1], tNb[2], tNb[3]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;
			
			// bezierQuadABS (2 args)
			case 'T':
				do
				{
					params (tNb, 2);
					bezierQuadABS (tNb[0], tNb[1]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// bezierQuadREL (2 args)
			case 't':
				do
				{
					params (tNb, 2);
					bezierQuadREL (tNb[0], tNb[1]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;
			
			// arcABS (7 args)
			case 'A':
				do
				{
					params (tNb, 7);
					arcABS (tNb[0], tNb[1], tNb[2], tNb[3], tNb[4], tNb[5], tNb[6]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;

			// arcREL (7 args)
			case 'a':
				do
				{
					params (tNb, 7);
					arcREL (tNb[0], tNb[1], tNb[2], tNb[3], tNb[4], tNb[5], tNb[6]);
				}
				while ( estChiffre(this->fichier.peek()) );
			break;
			
			// si on détecte la fin du contenu de "d"
			// on a parcouru toute les données, ça a reeussi !!
			case '"':
				// On ferme le fichier
				this->fichier.close();

				return;
			break;
			
			// si ce n'est pas une lettre de déplacement
			default:
				// Serial.print(car);
			break;
		}
	
	}

	// si on est là c'est qu'on a parcouru tout le fichier
	// sans trouver le (") de fin de la balise (d) : fail

	// Err. 11 : Le fichier svg est incomplet.
	Serial.print("E11");
	return;
}

void Draw::svg(void)
{
	// si on est pas en mode svg, on a rien à faire ici,
	// donc on renvoie un erreur.
	if (!modeSVG)
	{
		// Err. 10 : Tentative d'utilisation d'un fichier SVG qui n'a pas été initialisé.
		Serial.print("E10");
		return;
	}

	// se positionne en début de fichier
	this->fichier.seek(0);

	// Se positionne jusqu'à la balise SVG
	// Si on ne la trouve pas, on renvoie une erreur
	if (! trouveSD("<svg") )
	{
		// Err. 12 : Le fichier n'est pas un fichier svg.
		Serial.print("E12");
		return;
	}

	// Se positionne jusqu'à la balise PATH
	// Si on ne la trouve pas, on renvoie une erreur
	if (! trouveSD("<path") )
	{
		// Err. 13 : Le fichier svg n'inclut aucune donnée de dessin.
		Serial.print("E13");
		return;
	}

	// tant que l'on trouve le début des données d'un traçé, on dessine
	while ( trouveSD("d=\"") )
	{
		dessiner();
	}	
}

void Draw::params(float * tNb, int nbParams)
{
	// chaine qui va contenir le nombre à convertir en float
	// ex: (23456.43532)
	char valeur[20];

	char car;

	int i, j;

	for (i=0; i < nbParams; i++)
	{
		// passe les espaces et virgules
		do
		{
			car = this->fichier.read();
		}
		while (car == ' ' || car == ',');

		// tant que les car. sont des chiffres, lis et stoque dans une chaine
		for (j=0; estChiffre(car); j++)
		{
			valeur[j] = car;
			car = this->fichier.read();
		}
		valeur[j] = '\0';

		tNb[i] = atof(valeur);
	}
}
