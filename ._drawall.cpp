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
 * \file	drawall.cpp
 * \author  Nathanaël Jourdane
 * \brief   Fichier principal de la bibliothèque.
 */

#include <drawall.h>

Drawall::Drawall()
{
}

void Drawall::begin(
			const char *fileName)
{
	// Début de la communication série.
	// La vitesse du port est définie dans le fichier pins.h.
	Serial.begin(SERIAL_BAUDS);

	// Pins d'entrée

	#ifdef BUTTONS
		/* Étrange, ces lignes font foirer la communication série...
		// Gestion de l'interruption à l'appui sur le BP pause.
		SREG |= 128;				// Interruptions globales ok
		EIMSK = B01;				// Interruption INT0 déclenchée à l'appui sur le BP pause : INT0 (pin2) = B01 ou INT1 (pin3) = B10 sur Atmega328.
		EICRA = B0011;				// Interruption déclenchée sur le front montant pour INT0 : état bas = B00, changement d'état = B01, front descendant = B10, front montant = B11
		*/

		pinMode(PIN_LEFT_CAPTOR, INPUT);
		pinMode(PIN_RIGHT_CAPTOR, INPUT);
		pinMode(PIN_REMOTE, INPUT);

		Activation des pullup internes pour les boutons
		digitalWrite(2, HIGH);		// INT0 (pour BP pause) est sur pin 2
		digitalWrite(PIN_LEFT_CAPTOR, HIGH);
		digitalWrite(PIN_RIGHT_CAPTOR, HIGH);
	#endif

	// Pins de sortie
	pinMode(PIN_OFF_MOTORS, OUTPUT);
	pinMode(PIN_LEFT_MOTOR_STEP, OUTPUT);
	pinMode(PIN_LEFT_MOTOR_DIR, OUTPUT);
	pinMode(PIN_RIGHT_MOTOR_STEP, OUTPUT);
	pinMode(PIN_RIGHT_MOTOR_DIR, OUTPUT);

	#ifdef SCREEN
		pinMode(PIN_SCREEN_SCE, OUTPUT);
		pinMode(PIN_SCREEN_RST, OUTPUT);
		pinMode(PIN_SCREEN_DC, OUTPUT);
		pinMode(PIN_SCREEN_SDIN, OUTPUT);
		pinMode(PIN_SCREEN_SCLK, OUTPUT);
	#endif

	pinMode(PIN_SD_CS, OUTPUT);

	mServo.attach(PIN_SERVO);

	if (!SD.begin(PIN_SD_CS)) {
		error(CARD_NOT_FOUND);
	}

	mWriting = true;			// Pour que write() fonctionne la 1ere fois

	// Chargement des paramètres à partir du fichier de configuration
	loadParameters(fileName);
	//printParameters();

	mDrawingScale = 1;
	mDrawingOffsetX = 0;
	mDrawingOffsetY = 0;

	// Vérification de la distance entre les 2 moteurs.
	if (mpSpan < mpSheetWidth + mpSheetPositionX) {
		error(TOO_SHORT_SPAN);
	}

	initStepLength();

	setPosition(mpDefaultPosition);

	// Calcul de la longueur des courroies au début
	mLeftLength = positionToLeftLength(mPositionX, mPositionY);
	mRightLength = positionToRightLength(mPositionX, mPositionY);

	setSpeed(mpDefaultSpeed);

	#ifdef SERIAL
		String line = "";
		while (line != "START") {
			line = readLine();
		}

		// pinMode(13, OUTPUT);
		// for(int i=0; i<10; i++) {
		// digitalWrite(13, HIGH);
		// delay(1000);
		// digitalWrite(13, LOW);
		// delay(1000);			
		// }
		
		// Send init data to computer
		Serial.println("START_INIT");
		Serial.println(mpSpan);
		Serial.println(mpSheetPositionX);
		Serial.println(mpSheetPositionY);
		Serial.println(mpSheetWidth);
		Serial.println(mpSheetHeight);
		Serial.println(mLeftLength);
		Serial.println(mRightLength);
		Serial.println(mStepLength * 1000);
		Serial.println("END_INIT");
	#else
		delay(mpInitialDelay);
	#endif

	#ifdef BUTTONS
		// Pause until button pushed.
		// Serial.println("_Press button to begin...");
		// while(digitalRead(PIN_BP) == LOW) {}
	#endif

	power(true);
	
	write(false); // Servo initialization
	mPositionZ = 10;
}

// Interrupt routine
#ifdef BUTTONS
	ISR(INT0_vect)
	{
		EIMSK = 0;					// Bloquage de INT0

		while (PIND & 4) {
		};							// Attente que le pin soit à '1'
		while (!(PIND & 4)) {
		};							// Attente que le pin soit à '0'
		// (Pour un front descendant, inverser les 2 lignes).

		EIMSK = 1;					// Réautorisation de INT0
		EIFR = 1;					// Flag de INT0 remis à '0'
	};
#endif

String Drawall::readLine() {
	String message = "";

	while (Serial.available()) {
		delay(3);
		if (Serial.available() > 0) {
			char c = Serial.read();
			message += c;
			if (c == '\n') {
				break;
			}
		}
	}
	delay(50);
	return message;	
}

void Drawall::setPosition(
			float positionX,
			float positionY)
{
	mPositionX = positionX;
	mPositionY = positionY;
}

void Drawall::setPosition(
			Position position)
{
	setPosition(positionToX(position), positionToY(position));
}

float Drawall::positionToX(
			Position position)
{
	float x = 0;

	switch (position) {
	case UPPER_LEFT:
	case LEFT_CENTER:
	case LOWER_LEFT:
		x = 0;
		break;

	case UPPER_CENTER:
	case CENTER:
	case LOWER_CENTER:
		x = (float) (mpSheetWidth) / 2;
		break;

	case UPPER_RIGHT:
	case RIGHT_CENTER:
	case LOWER_RIGHT:
		x = (float) (mpSheetWidth);
		break;

	default:
		break;
	}

	return x;
}

float Drawall::positionToY(
			Position position)
{
	float y = 0;

	switch (position) {
	case UPPER_LEFT:
	case UPPER_CENTER:
	case UPPER_RIGHT:
		y = 0;
		break;

	case LEFT_CENTER:
	case CENTER:
	case RIGHT_CENTER:
		y = (float) (mpSheetHeight) / 2;
		break;

	case LOWER_LEFT:
	case LOWER_CENTER:
	case LOWER_RIGHT:
		y = (float) (mpSheetHeight);
		break;

	default:
		break;
	}

	return y;
}

void Drawall::initStepLength(
			)
{
	// mpSteps*2 car c'est seulement le front montant qui contôle le moteur
	mStepLength = (PI * float (mpDiameter)) /float (
				mpSteps * 2);
}

void Drawall::setSpeed(
			unsigned int speed)
{
	mDelay = 1000000 * mStepLength / float (
				speed);
}

long Drawall::positionToLeftLength(
			float positionX,
			float positionY)
{
	float width = ((float) mpSheetPositionX + positionX) / mStepLength;
	float height = ((float) mpSheetPositionY + positionY) / mStepLength;

	return sqrt(pow(width, 2) + pow(height, 2));
}

long Drawall::positionToRightLength(
			float positionX,
			float positionY)
{
	float width =
				((float) mpSpan - (float) mpSheetPositionX -
				positionX) / mStepLength;
	float height = ((float) mpSheetPositionY + positionY) / mStepLength;

	return sqrt(pow(width, 2) + pow(height, 2));
}

void Drawall::power(
			bool alimenter)
{
	if (alimenter) {
		digitalWrite(PIN_OFF_MOTORS, LOW);
		Serial.write(ENABLE_MOTORS);	// Processing: a = alimenter
	} else {
		digitalWrite(PIN_OFF_MOTORS, HIGH);
		Serial.write(DISABLE_MOTORS);	// Processing: b = désalimenter
		write(false);
	}
}

// À supprimer plus tard, mais garder pour l'instant
void Drawall::write(
			bool write)
{
	// Si on souhaite écrire et que le stylo n'ecrit pas
	if (write && !mWriting) {
		delay(mpPreServoDelay);
		mServo.write(mpMinServoAngle);
		delay(mpPostServoDelay);

		Serial.write(WRITING);	// Processing: w = ecrire
		mWriting = true;
	}
	// si on ne veut pas ecrire et que le stylo ecrit
	else if (!write && mWriting) {
		delay(mpPreServoDelay);
		mServo.write(mpMinServoAngle);
		delay(mpPostServoDelay);

		Serial.write(MOVING);		// Processing: x = ne pas ecrire
		mWriting = false;
	}
}

void Drawall::leftStep(
			bool pull)
{
	if (pull) {
		mLeftLength--;
		Serial.write(PULL_LEFT);
	} else {
		mLeftLength++;
		Serial.write(PUSH_LEFT);
	}

	digitalWrite(PIN_LEFT_MOTOR_STEP, mLeftLength % 2);
}

void Drawall::rightStep(
			bool pull)
{
	if (pull) {
		mRightLength--;
		Serial.write(PULL_RIGHT);
	} else {
		mRightLength++;
		Serial.write(PUSH_RIGHT);
	}

	digitalWrite(PIN_RIGHT_MOTOR_STEP, mRightLength % 2);
}

void Drawall::line(
			float x,
			float y)
{
	int longmax = 5;

	float longX = abs(x - mPositionX);
	float longY = abs(y - mPositionY);

	float miniX;
	float miniY;
	int boucle;

	if (longX > longmax || longY > longmax) {

		if (longX > longY) {
			boucle = ceil(longX / longmax);
		} else {
			boucle = ceil(longY / longmax);
		}

		miniX = ((x - mPositionX) / boucle);
		miniY = ((y - mPositionY) / boucle);

		for (int i = 0; i < boucle; i++) {
			line(mPositionX + miniX, mPositionY + miniY, true);
		}
	}
	line(x, y, true);
}

void Drawall::area(
			)
{
	move(0, 0);
	line(mPositionX + mpSheetWidth, 0);
	line(0, mPositionY + mpSheetHeight);
	line(mPositionX - mpSheetWidth, 0);
	line(0, mPositionY - mpSheetHeight);
}

void Drawall::drawingArea(
			const char *fileName)
{
	/*sdInit(fileName);
	float width = getNumericAttribute("width");
	float height = getNumericAttribute("height");

	setDrawingScale(width, height);
	move(0, 0);
	rectangle(width, height);

	mFile.close();*/
}

void Drawall::move(
			float x,
			float y)
{
	line(x, y, false);
}

void Drawall::move(
			Position position)
{
	move(positionToX(position), positionToY(position));
}

bool Drawall::isNumeric(
			char car)
{
	int i;
	const char *chiffres = "-.0123456789";

	for (i = 0; chiffres[i] != '\0'; i++) {
		if (car == chiffres[i]) {
			return true;
		}
	}

	return false; // Pas de chiffre trouvé donc renvoie faux
}

float Drawall::readParameter(char parameter)
{
	char c; // Le caractère lu
	int bufferSize = 10; // Taille du buffer
	char buffer[bufferSize + 1]; // Buffer contenant le nombre
	float value;
	int i = 0;

	// Ne lis pas le caractère, au cas où il y a moins de 3 arguments
	c = mFile.peek();
	
	if (c == ' ') {
		c = mFile.read(); // Passe le 1er espace
	
	} else if (c == parameter) { // Si le car. correspond à celui attendu
		c = mFile.read(); // Fait avancer le curseur
		while (isNumeric(c)) { // Tant qu'on lit des chiffres
			if (c != -1 || i < bufferSize) {
				error(WRONG_GCODE_PARAMETER, c); // Ajouter num ligne
			}
			buffer[i] = c; // Remplit le buffer
			c = mFile.read();
			i++;
		}
		buffer[i] = c;
		buffer[i+1] = '\0';	// Finis la chaine

		value = atof(buffer);
		if (!mpMetricUnit) { // Si unité en pouces, convertit
			value *= 25.4;
		}

		if(mpAbsolutePosition) { // Si position absolue, convertit
			if(parameter == 'X') {
				value += mPositionX;
			} else if(parameter == 'Y') {
				value += mPositionY;
			} else if(parameter == 'Z') {
				value += mPositionZ;
			}
		}

		return value;

	} else { // Soit mauvais paramètre (Y au lieu de X), soit \n
		return 0;
	}
}
/*
char* Drawall::readStringUntil(char end, byte bufferSize)
{
	char car; // Caractère utilisé pour parcourir la chaine
	char buffer[bufferSize + 1]; // Buffer contenant le nombre
	int i = 0;

	// Ignore les espaces.
	do {
		car = mFile.read();
	} while (car == ' ');

	// Tant qu'on peut lire et qu'on ne détecte pas le caractère de fin
	while (car != end) {
		if (mFile.available() && i < bufferSize) {
			buffer[i] = car; // On a déjà lu le 1er car.
			car = mFile.read();
			i++;
		}
	}

	// Remplace le caractère de fin par le caractère de fin de chaine.
	buffer[i] = '\0';

	return buffer;
}*/

int* Drawall::readNumber()
{
	int pos[] = {100, 100, 25};
	return pos;
}

void Drawall::readFunction()
{
	// Lis le nom de la fonction
	char functionName[4];
	for(int i=0 ; i<3 ; i++) {
		functionName[i] = mFile.read();
	}
	functionName[3] = '\0';

	Serial.print("functionName: '");
	Serial.print(functionName);
	Serial.println("'");


	// Interprète le code
	if (!strcmp(functionName, "G00")) {
		int* pos = readNumber();
		Serial.print("Deplacement rapide: ");
		Serial.print(pos[0]);
		Serial.print(';');
		Serial.print(pos[1]);
		Serial.print(';');
		Serial.println(pos[2]);
		fastline(pos[0], pos[1], pos[2]);
	} else if(!strcmp(functionName, "G01")) {
		//line(readNumber(), readNumber(), readNumber());
	} else if(!strcmp(functionName, "G04")) {
		delay(1000 * 1);
	} else if(!strcmp(functionName, "G20")) {
		mpMetricUnit = false;
	} else if(!strcmp(functionName, "G21")) {
		mpMetricUnit = true;
	} else if(!strcmp(functionName, "G90")) {
		mpAbsolutePosition = true;
	} else if(!strcmp(functionName, "G91")) {
		mpAbsolutePosition = false;
	} else {
		warning(UNKNOWN_GCODE_FUNCTION, functionName); // Ajouter ligne
	}
}

// TODO
void Drawall::fastline(float x, float y, float z)
{
	line(x, y, true);
}

void Drawall::line(float x, float y, bool write)
{
	// échelle
	float sX = (float) mpScaleX * mDrawingScale;
	float sY = (float) mpScaleY * mDrawingScale;

	// offset
	int oX = (int) mpOffsetX + mDrawingOffsetX;
	int oY = (int) mpOffsetY + mDrawingOffsetY;

	unsigned long bG = positionToLeftLength(x * sX + oX, y * sY + oY);
	unsigned long bD = positionToRightLength(x * sX + oX, y * sY + oY);

	// nombre de pas à faire
	long nbPasG = bG - mLeftLength;
	long nbPasD = bD - mRightLength;

	bool pullLeft = false;
	bool pullRight = false;

	float delaiG;
	float delaiD;

	unsigned long dernierTempsG;
	unsigned long dernierTempsD;

	// calcul de la direction
	if (nbPasG < 0) {
		pullLeft = true;
	}

	if (nbPasD < 0) {
		pullRight = true;
	}
	// On a le sens, donc on peut retirer le signe pour simplifier les calculs
	nbPasG = fabs(nbPasG);
	nbPasD = fabs(nbPasD);

	if (nbPasG > nbPasD) {
		delaiG = mDelay;
		delaiD = mDelay * ((float) nbPasG / (float) nbPasD);
	} else {
		delaiD = mDelay;
		delaiG = mDelay * ((float) nbPasD / (float) nbPasG);
	}

	dernierTempsG = micros();
	dernierTempsD = micros();

	if (pullLeft) {
		digitalWrite(PIN_LEFT_MOTOR_DIR, mpLeftDirection);
	} else {
		digitalWrite(PIN_LEFT_MOTOR_DIR, !mpLeftDirection);
	}

	if (pullRight) {
		digitalWrite(PIN_RIGHT_MOTOR_DIR, mpRightDirection);
	} else {
		digitalWrite(PIN_RIGHT_MOTOR_DIR, !mpRightDirection);
	}

	while (nbPasG > 0 || nbPasD > 0) {
		// Si le delai est franchi et qu'il reste des pas à faire
		if ((nbPasG > 0) && (micros() - dernierTempsG >= delaiG)) {
			dernierTempsG = micros();	// Stoque le temps actuel dans lastTimer
			leftStep(pullLeft);	// Effectue le pas
			nbPasG--;			// Décremente le nb de pas restants
		}

		if ((nbPasD > 0) && (micros() - dernierTempsD >= delaiD)) {
			dernierTempsD = micros();	// stoque le temps actuel dans lastTimer   
			nbPasD--;			// decremente le nb de pas restants    
			rightStep(pullRight);	// Effectue le pas
		}
	}

	mPositionX = x;
	mPositionY = y;
}

/*
void Drawall::loadTool(int toolNumber)
{

}
*/

void Drawall::error(Error errorNumber, char* msg)
{
	#ifdef SERIAL
		Serial.write(ERROR);
		Serial.write((byte) errorNumber);
		Serial.println(msg);
		Serial.write(END_ERROR);
	#endif
	delay(1000);
	write(false);
	while (true) ;
}

void Drawall::warning(Error warningNumber, char* msg)
{
	#ifdef SERIAL
		Serial.write(WARNING);
		Serial.write((byte) warningNumber);
		Serial.println(msg);
		Serial.write(END_WARNING);
	#endif
}

void Drawall::setDrawingScale(
			int width,
			int height)
{
	float scaleX = float (
				mpSheetWidth) / float (
				width);
	float scaleY = float (
				mpSheetHeight) / float (
				height);

	if (scaleX > scaleY) {
		mDrawingScale = scaleY;
		mDrawingOffsetX = float (
					mpSheetWidth) / 2 - float (
					width) * scaleY / 2;
	} else {
		mDrawingScale = scaleX;
		mDrawingOffsetY = float (
					mpSheetHeight) / 2 - float (
					height) * scaleX / 2;
	}
}

void Drawall::draw(
			const char *fileName)
{
	mDrawingScale = 1;
	mDrawingOffsetX = 0;
	mDrawingOffsetY = 0;

	mFile = SD.open(fileName);

	if (!mFile) {
		error(FILE_NOT_FOUND);
	}
	mFile.seek(0);				// Se positionne en début de fichier.

	// Tant qu'on peut lire
	while (mFile.available()) {
		readFunction();
	}

	// Fin du dessin
	mFile.close();
	Serial.write(END_DRAWING);
}

void Drawall::end(
			)
{
	move(mpEndPosition);
	power(false);
	while (true) ;
}

bool Drawall::atob(char* value) {
	bool booleanValue = false;

	if (!strcmp(value, "true") || !strcmp(value, "yes")) {
		booleanValue = true;
	}
	return booleanValue;
}

void Drawall::loadParameters(
			const char *fileName)
{
	const byte BUFFER_SIZE = 32;	// Taille buffer

	char buffer[BUFFER_SIZE];	// Stocke une ligne du fichier
	char *key;					// Chaine pour la clé
	char *value;				// Chaine pour la valeur

	byte i;						// Itérateur
	byte line_lenght;			// Longueur de la ligne
	byte line_counter = 0;		// Compteur de lignes
	char buffer[3];				// Buffer pour affichage numéro de ligne si erreur


	// Ici, problème avec la communication avec Processing !!!

	// Test existence fichier
	/*if(!SD.exists(fileName)) {
	 * Serial.print("Le fichier '");
	 * Serial.print(fileName);
	 * Serial.println("' n'existe pas.");
	 * while(true){};
	 * } */

	// Ouvre le fichier de configuration
	File configFile = SD.open(fileName, FILE_READ);

	if (!configFile) {
		error(FILE_NOT_READABLE);
	}
	// Tant qu'on est pas à la fin du fichier
	while (configFile.available() > 0) {
		// Récupère une ligne entière dans le buffer
		i = 0;
		while ((buffer[i++] = configFile.read()) != '\n') {

			// Si la ligne dépasse la taille du buffer
			if (i == BUFFER_SIZE) {
				// On finit de lire la ligne mais sans stocker les données
				while (configFile.read() != '\n') {
				};
				break;			// Et on arrête la lecture de cette ligne
			}
		}

		// On garde de côté le nombre de char stocké dans le buffer
		line_lenght = i;

		// Finalise la chaine de caractéres ASCII en supprimant le \n au passage.
		buffer[--i] = '\0';

		++line_counter;			// Incrémente le compteur de lignes

		// Ignore les lignes vides ou les lignes de commentaires.
		if (buffer[0] == '\0' || buffer[0] == '#') {
			continue;
		}
		// Gestion des lignes trop grande
		if (i == BUFFER_SIZE) {
			sprintf(buffer, "%d", line_counter);
			warning(TOO_LONG_CONFIG_LINE, buffer);
		}
		// Cherche l'emplacement de la clé en ignorant les espaces et les tabulations en début de ligne.
		i = 0;
		while (buffer[i] == ' ' || buffer[i] == '\t') {
			if (++i == line_lenght) {
				break;
			}
			// Ignore les lignes contenant uniquement des espaces et/ou des tabulations.
		}

		if (i == line_lenght) {
			continue;			// Gère les lignes contenant uniquement des espaces et/ou des tabulations.
		}
		key = &buffer[i];

		// Cherche l'emplacement du séparateur = en ignorant les espaces et les tabulations apres la clé.
		while (buffer[i] != ' ' && buffer[i] != '\t') {
			if (++i == line_lenght) {
				sprintf(buffer, "%d", line_counter);
				warning(WRONG_CONFIG_LINE, buffer);
				break;			// Ignore les lignes mal formées
			}
		}

		if (i == line_lenght) {
			continue;			// Gère les lignes mal formées
		}
		buffer[i++] = '\0';		// Transforme le séparateur en \0

		// Cherche l'emplacement de la valeur en ignorant les espaces et les tabulations après le séparateur.
		while (buffer[i] == ' ' || buffer[i] == '\t') {
			if (++i == line_lenght) {
				sprintf(buffer, "%d", line_counter);
				warning(WRONG_CONFIG_LINE, line_counter);
				break;			// Ignore les lignes mal formées
			}
		}

		if (i == line_lenght) {
			continue;			// Gère les lignes mal formées
		}

		value = &buffer[i];

		// Transforme les données texte en valeur utilisable
		if (!strcmp(key, "fileName")) {
			strcpy(this->mpFileName, value);
		} else if (!strcmp(key, "span")) {
			mpSpan = atoi(value);
		} else if (!strcmp(key, "sheetWidth")) {
			mpSheetWidth = atoi(value);
		} else if (!strcmp(key, "sheetHeight")) {
			mpSheetHeight = atoi(value);
		} else if (!strcmp(key, "sheetPositionX")) {
			mpSheetPositionX = atoi(value);
		} else if (!strcmp(key, "sheetPositionY")) {
			mpSheetPositionY = atoi(value);
		} else if (!strcmp(key, "minServoAngle")) {
			mpMinServoAngle = atoi(value);
		} else if (!strcmp(key, "maxServoAngle")) {
			mpMaxServoAngle = atoi(value);
		} else if (!strcmp(key, "minPen")) {
			mpMinPen = atoi(value);
		} else if (!strcmp(key, "maxPen")) {
			mpMaxPen = atoi(value);
		} else if (!strcmp(key, "preServoDelay")) {
			mpPreServoDelay = atoi(value);
		} else if (!strcmp(key, "postServoDelay")) {
			mpPostServoDelay = atoi(value);
		} else if (!strcmp(key, "steps")) {
			mpSteps = atoi(value);
		} else if (!strcmp(key, "diameter")) {
			mpDiameter = atof(value);
		} else if (!strcmp(key, "leftDirection")) {
			mpLeftDirection = atob(value);
		} else if (!strcmp(key, "rightDirection")) {
			mpRightDirection = atob(value);
		} else if (!strcmp(key, "initialDelay")) {
			mpInitialDelay = atoi(value);
		} else if (!strcmp(key, "scaleX")) {
			mpScaleX = atoi(value);
		} else if (!strcmp(key, "scaleY")) {
			mpScaleY = atoi(value);
		} else if (!strcmp(key, "offsetX")) {
			mpOffsetX = atoi(value);
		} else if (!strcmp(key, "offsetY")) {
			mpOffsetY = atoi(value);
		} else if (!strcmp(key, "defaultSpeed")) {
			mpDefaultSpeed = atoi(value);
		} else if (!strcmp(key, "metricUnit")) {
			mpMetricUnit = atob(value);
		} else if (!strcmp(key, "absolutePosition")) {
			mpAbsolutePosition = atob(value);
		} else if (!strcmp(key, "displayComments")) {
			mpDisplayComments = atob(value);
		} else {
			sprintf(buffer, "%d", line_counter);
			warning(UNKNOWN_CONFIG_KEY, line_counter);
		}
	}

	configFile.close();			// Ferme le fichier de configuration
}

void Drawall::printParameters()
{
	#ifdef DEBUG
		Serial.print("Span: ");
		Serial.println(mpSpan);
		Serial.print("SheetWidth: ");
		Serial.println(mpSheetWidth);
		Serial.print("SheetHeight: ");
		Serial.println(mpSheetHeight);
		Serial.print("SheetPositionX: ");
		Serial.println(mpSheetPositionX);
		Serial.print("SheetPositionY: ");
		Serial.println(mpSheetPositionY);
		Serial.print("MinServoAngle: ");
		Serial.println(mpMinServoAngle);
		Serial.print("MaxServoAngle: ");
		Serial.println(mpMaxServoAngle);
		Serial.print("MinPen: ");
		Serial.println(mpMinPen);
		Serial.print("MaxPen: ");
		Serial.println(mpMaxPen);
		Serial.print("PreServoDelay: ");
		Serial.println(mpPreServoDelay);
		Serial.print("PostServoDelay: ");
		Serial.println(mpPostServoDelay);
		Serial.print("Steps: ");
		Serial.println(mpSteps);
		Serial.print("Diameter: ");
		Serial.println(mpDiameter);
		Serial.print("LeftDirection: ");
		Serial.println(mpLeftDirection);
		Serial.print("RightDirection: ");
		Serial.println(mpRightDirection);
		Serial.print("InitialDelay: ");
		Serial.println(mpInitialDelay);
		Serial.print("ScaleX: ");
		Serial.println(mpScaleX);
		Serial.print("ScaleY: ");
		Serial.println(mpScaleY);
		Serial.print("OffsetX: ");
		Serial.println(mpOffsetX);
		Serial.print("OffsetY: ");
		Serial.println(mpOffsetY);
		Serial.print("DefaultSpeed: ");
		Serial.println(mpDefaultSpeed);
		Serial.print("MetricUnit: ");
		Serial.println(mpMetricUnit);
		Serial.print("AbsolutePosition: ");
		Serial.println(mpAbsolutePosition);
		Serial.print("DisplayComments: ");
		Serial.println(mpDisplayComments);
	#endif
}