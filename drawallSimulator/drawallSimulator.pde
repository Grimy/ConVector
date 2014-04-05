/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.fr/
 * Drawall is free software and licenced under GNU GPL v3 : http://www.gnu.org/licenses/
 * Copyright (c) 2012-2014 Nathanaël Jourdane
 */

/*
Ce programme est un simulateur pour le robot Drawall, permetant de tester le bon
fonctionnement d'un programe de dessin avant de le reproduire et faciliter le développement
du projet.
Il nécessite l'environnement de développement Processing : http://www.processing.org/

Ce simulateur reproduit le dessin que réalise le robot, en interpretant en temps réel
les impulsions envoyées aux moteurs. Pour le faire fonctionner il vous faut donc
connecter à votre ordinateur au minimum une carte arduino munie d'un lecteur SD
et y insérer une carte contenant une image svg valide.
Toutes les fonctions svg ne sont pas encore interprétées. Pour plus d'informations
sur la conformité du fichier svg, référez-vous au document documentation/valid_svg.txt
du dépot GitHub. Une aide à l'installation sur Linux est également disponible sur le dépot.

Si les moteurs ne sont pas branchés, vous pouvez augmenter la vitesse de traçé pour
accélérer la simulation.

Si Processing affiche l'erreur ArrayIndexOutOfBoundsException, c'est qu'il n'a pas reçu
les données n'initialisation de la carte. Initialisez la carte Arduino et resayez.
Si rien ne change, il y a probablement une erreur de communication Arduino <-> PC.

*/

import processing.serial.*;

Serial arduino; // le port série

color colEcrire = #000000;
color colPasEcrire = #3969ec;
color colLim = #EEEEEE;
color colBarreAlim = #CCFFAF;
color colBarreDesalim = #FFAFAF;
color colBarreFin = #8aeda2;
color colFond = #AAAAAA;
color colCurseur = #FC2E31;
color colRectArea = #C7CC10;

float mDistanceBetweenMotors;
float mSheetPositionX, mSheetPositionY;
float mSheetWidth, mSheetHeight;
int mLeftLength, mRightLength;
float mStepLength;

float mScale;
float posX, posY; // position du stylo

// -1 : desalimenté
// 0 : alimenté
// 1 : fin
int etat = -1;

boolean mDrawMoves = true;
int barreH = 15;
String msgBarre = "";
long mStart;
int mBaudRate = 57600;

void setup()
{
  String port = Serial.list()[0];
  float tabInit[] = new float[8];
  String msg = null;
  
  println("*** Wait for Arduino feedback on " + port + " at " + mBaudRate + " bauds... ***");
  arduino = new Serial(this, port, mBaudRate);

  // attends le car. de début d'init
  while (msg == null)
  {
    msg = arduino.readStringUntil(101);
  }

  println("\n*** données d'initialisation ***");
  
  //récupère tout jusqu'au car de fin d'init
  msg = null;
  while (msg == null)
  {
    // récupère tout jusqu'au caractère de fin d'init.
    msg = arduino.readStringUntil(102);
  }


  // waitUntil("READY");
  // println("Arduino is ready.");
  // arduino.write('d');
  
  /*arduino.readStringUntil(101); // START_INSTRUCTIONS

  println("\n*** Données d'initialisation ***");
  
  while (msg == null)
  {
    // Récupère tout jusqu'au caractère de fin d'init.
    msg = arduino.readStringUntil(102); // END_INSTRUCTIONS
  }*/
  
  // Retire les espaces, separe et stoque les paramètres dans un tableau.
  tabInit = float(split(trim(msg), '\n'));
  
  mDistanceBetweenMotors = tabInit[0];
  mSheetPositionX = tabInit[1];
  mSheetPositionY = tabInit[2];
  mSheetWidth = tabInit[3];
  mSheetHeight = tabInit[4];
  mLeftLength = int(tabInit[5]);
  mRightLength = int(tabInit[6]);
  mStepLength = tabInit[7];
  
  println("Distance inter-moteurs : " + mDistanceBetweenMotors);
  println("Position de la zone de dessin : " + mSheetPositionX + " , " + mSheetPositionY);
  println("Taille de la zone de dessin : " + mSheetWidth + " * " + mSheetHeight);
  println("Longueur des câbles : gauche = " + mLeftLength + " , droit = " + mRightLength);
  println("1 pas = " + mStepLength + "µm");

/*
  for (float param : tabInit) {
    if (param == null)
      println("Error: all initialization datas have not been recieved.");
      while(true) {};
  }
*/
  majPos();

  println("Position : " + posX + " , " + posY);
  
  println("\n*** Lancement de l'interface ***");

  // *** texte ***
  PFont maTypo = loadFont("Garuda-12.vlw"); // choix de la typo
  textFont(maTypo, 12); // Définition de la taille de la typo

  // *** creation fenêtre ***
  background(colFond);
  
  size (800, 600);
  frame.setResizable(true);
  
  println("Heure de début : " + hour() + "h" + minute() + ":" + second());
  
  mStart = millis();
}

void waitUntil(String text)
{
  String msg = "";
  char c;

  while(!msg.contains(text)) {
    while (arduino.available() <= 0);
    c = (char)arduino.read(); // Lit l'octet
    msg += c;
  }
}

void initScale()
{
  float scaleX = width / mDistanceBetweenMotors;
  float scaleY = (height - barreH) / (mSheetPositionY + mSheetHeight);
  float scale;
  
  if(scaleX > scaleY) {
    mScale = scaleY;
  } else {
    mScale = scaleX;
  }
}

void draw() // Appelé tout le temps
{  
  initScale();  
  
  float areaX = (width - mDistanceBetweenMotors * mScale) / 2;
  float areaY = (height - barreH - (mSheetPositionY + mSheetHeight) * mScale) / 2;
  
  rectOut(mSheetPositionX, mSheetPositionY,
        mSheetWidth, mSheetHeight,
        mDistanceBetweenMotors, mSheetHeight + mSheetPositionY, colLim);

  echelle(areaX + 6, areaY + 4, round(mSheetWidth/10));
  barre();
  
  while (arduino.available() > 0)
  {
    int data = arduino.readChar();

	// En java on ne peut pas convertir des int en struct (lorsque on lit sur le port série), part ailleurs on ne peut pas mettre de variable dans les cases, donc tous les codes sont écrits en dur -_- ... Du coup c'est vraiment très moche, vivement que je passe en C++ ou en python.

    switch(data)
    {
      case 0 : // PUSH_LEFT
        mLeftLength++;
      break;

      case 1 : // PULL_LEFT
        mLeftLength--;
      break;

      case 2 : // PUSH_RIGHT
        mRightLength++;
      break;

      case 3 : // PULL_RIGHT
        mRightLength--;
      break;

      case 4 : // WRITING
        msgBarre = "Dessin en cours...";
        stroke(colEcrire);
      break;

      case 5 : // MOVING
        msgBarre = "Déplacement en cours...";
        if (mDrawMoves) {
          stroke(colPasEcrire);
        } else {
          noStroke();
        }
      break;
      
      case 6 : // START_MESSAGE
        println(arduino.readStringUntil(7)); // until END_MESSAGE
      break;

      case 8 : // ENABLE_MOTORS
        etat = 0;
        barre();
      break;
      
      case 9 : // DISABLE_MOTORS
        etat = -1;
        barre();
      break;

      case 10 : // SLEEP
        println("Pause.");
      break;

      case 11 : // CHANGE_TOOL
        println("Changez le stylo.");
      break;

      case 12 : // END_DRAWING
        etat = 1;
        msgBarre = "Le dessin a été reproduit avec succès.";
        println("Heure de fin : " + hour() + "h" + minute() + ":" + second());
        int s = int((millis() - mStart) / 1000);
        int m = s / 60;
        int h = m / 60;
        println("Durée totale : " + h + "h" + floor(m%60) + "m" + floor(s%3600) + "s.");
        barre();
      break;

      case 13 : //WARNING
        println("Warning: " + arduino.readStringUntil(14)); // until END_WARNING
        //error(arduino.readChar(), arduino.readChar(), arduino.readChar());
      break;

      case 15 : // ERROR
        println("Error: " + arduino.readStringUntil(16)); // until END_ERROR
      break;
           
      default:
        error(100, data, 0); // UNKNOWN_SERIAL_CODE
      break;
    }

    majPos();
    point(posX*mScale + areaX, posY*mScale + areaY);
    
    barre();
  }
}

void majPos()
{
  posX = (pow(float(mLeftLength) * mStepLength/1000, 2) - pow(float(mRightLength) * mStepLength/1000, 2)
      + pow(mDistanceBetweenMotors, 2) ) / (2*mDistanceBetweenMotors);
  posY = sqrt( pow(float(mLeftLength) * mStepLength/1000, 2) - pow(posX, 2) );
}

void error(int code, int p1, int p2)
{
  if (code < 100) {
    println("\n*************************");        
    print("Error " + int(code) + " : ");
  } else {
    print("\nWarning : ");
  }

        switch (code)
        {
          case 0 : // CARD_NOT_FOUND
            msgBarre = "La carte SD est absente ou illisible.";
          break;

          case 1 : // FILE_NOT_FOUND
            msgBarre = "Le fichier n'existe pas.";
          break;
          
          case 2 : // FILE_NOT_READABLE
            msgBarre = "Impossible d'ouvrir le fichier. Un nom de moins de 8 caractères peut corriger le problème.";
          break;

          case 3 : // TOO_SHORT_SPAN
            msgBarre = "La distance entre les 2 moteurs est inférieure à la largeur de la feuille et sa position horizontale.";
          break;

          // *** Warnings ***
          
          case 100 : // UNKNOWN_SERIAL_CODE
            msgBarre = "Le code ";
            msgBarre += p1;
            msgBarre += " ('";
            msgBarre += (char) p1;
            msgBarre += "') recu par la liason serie n'est pas reconnu.";
          break;
          
          case 101 : // WRONG_CONFIG_LINE
            msgBarre = "Erreur dans le fichier de configuration à la ligne ";
            msgBarre += p1;
          break;

          case 102 : // TOO_LONG_CONFIG_LINE
            msgBarre = "Une ligne est trop longue dans le fichier de configuration à la ligne ";
            msgBarre += p1;
            msgBarre += ".";
          break;

          case 103 : // UNKNOWN_FUNCTION
            msgBarre = "Fonction '";
            msgBarre += (char)p1;
            msgBarre += "' inconnue.";
          break;

          case 104 : // UNKNOWN_GCODE_FUNCTION
            msgBarre = "Fonction Gcode G";
            msgBarre += p1;
            msgBarre += " non prise en charge.";
          break;

          case 105 : // UNKNOWN_MCODE_FUNCTION
            msgBarre = "Fonction Mcode M";
            msgBarre += p1;
            msgBarre += " non prise en charge.";
          break;

          case 106 : // LEFT_LIMIT
            msgBarre = "Le crayon a atteint la limite gauche.";
          break;

          case 107 : // RIGHT_LIMIT
            msgBarre = "Le crayon a atteint la limite droite.";
          break;

          case 108 : // UPPER_LIMIT
            msgBarre = "Le crayon a atteint la limite haute.";
          break;

          case 109 : // LOWER_LIMIT
            msgBarre = "Le crayon a atteint la limite basse.";
          break;

          case 110 : // FORWARD_LIMIT
            msgBarre = "Le crayon est sorti au maximum.";
          break;

          case 111 : // REARWARD_LIMIT
            msgBarre = "Le crayon est rentré au maximum.";
          break;

          default :
            msgBarre = "Code d'erreur non répertorié.";
          break;
        }
        
        // On imprime le descriptif de l'erreur
        println(msgBarre);
        barre();
}

void barre()
{
  pushStyle();
  // *** barre de statut ***
  if(etat == 0) // couleur de la barre en fonction de l'état du robot
    fill(colBarreAlim);
  else if(etat == -1)
    fill(colBarreDesalim);
  else if(etat == 1)
    fill(colBarreFin);
  
  stroke(0); // contour noir
  
  rect(-1, height - barreH, width + 1, barreH);
  // rectangle de la barre (écrase l'ancien texte)
  
  fill(0); // couleur du texte
  String msg = "surface: " + round(mSheetWidth) + "x" + round(mSheetHeight) +
    " | X: " + round(posX - mSheetPositionX) + " Y:" + round(posY - mSheetPositionY) +
    " | motG: " + mLeftLength + " motD: " + mRightLength +
    " | 1 pas = " + mStepLength + "µm" +
    " | " + msgBarre;
  
  text(msg, 4, height - 3); // écriture du texte
  popStyle();
}

void rectOut( float x, float y, float w, float h, float limL, float limH, color fillCol) {
  x *= mScale;
  y *= mScale;
  w *= mScale;
  h *= mScale;
  limL *= mScale;
  limH *= mScale;
  
  pushStyle();
  noStroke();
  
  float debX = (width-limL)/2;
  float debY = (height - barreH - limH)/2;
  
  fill(fillCol);
  rect(debX + 1, debY - 1, x - 1, limH); // gauche
  rect(debX + 1, debY - 1, limL - 1, y); // haut
  rect(x + w + debX, debY - 1, limL - x - w, limH); //  droite
  rect(debX + 1, debY + y + h, limL - 1, limH - y - h); // bas

  noFill();
  stroke(0);

  rect(x + debX , y + debY - 1, w - 1, h + 1);

  //lignes verticales
  line(debX, debY, debX, debY + limH);
  line(debX + limL, debY - 1, debX + limL, debY + limH);

  //lignes horizontales
  line(debX, debY - 1, debX + limL - 1, debY - 1);
  line(debX, debY + limH, debX + limL - 1, debY + limH);
  
  popStyle();
}

void echelle(float x, float y, float ech)
{
  pushStyle();
  stroke(0);
  fill(0);

  int flecheL = 3;

  line(x, y, x + mScale*ech, y);
  line(x + mScale*ech, y, x + mScale*ech - flecheL, y - flecheL);
  line(x + mScale*ech, y, x + mScale*ech - flecheL, y + flecheL);
  
  line(x, y, x, y + mScale*ech);
  line(x, y + mScale*ech, x - flecheL, y + mScale*ech - flecheL);
  line(x, y + mScale*ech, x + flecheL, y + mScale*ech - flecheL);
  
  text( int(ech) + " mm", x + 4, y + 14);
  
  popStyle();
}

// fonction pour stopper le programme (en attendant de trouver mieux...)
void quit() { while(true) {} }
