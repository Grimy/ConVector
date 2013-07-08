/*
This file is part of Drawall, a project for a robot which draws on walls.
See http://drawall.cc/ and https://github.com/roipoussiere/Drawall/.

Copyright (c) 2012-2013 Nathanaël Jourdane

Drawall is free software : you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
  // *** acquisition des données d'init ***
  print("*** ports détectés (le 1er sera le port utilisé) ***\n");
  println(Serial.list()); // listing des ports dispos

  // init la communication avec l'arduino
  arduino = new Serial(this, Serial.list()[0], mBaudRate);
  
  // tableau récupéré sur la liason série qui contiendra les variables d'initialisation
  float tabInit[] = new float[8];

  String msg = null;
  
  // attends la réception
  while (msg == null)
  {
    // récupère tout jusqu'au caractère de début d'init.
    msg = arduino.readStringUntil('\n');
  }

  println("\n*** données d'initialisation ***");
  
  //récupère tout jusqu'au retour à la ligne
  msg = null;
  
  while (msg == null)
  {
    // récupère tout jusqu'au caractère de fin d'init.
    msg = arduino.readStringUntil('\n');
  }
  
  // retire les espaces, separe et stoque les param. dans un tableau
  tabInit = float( split( trim(msg) , ',') );
  
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

  majPos();
  println("Position : " + posX + " , " + posY);
  
  println("\n*** lancement de l'interface ***");

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
  
  char numErr;
  
  rectOut(mSheetPositionX, mSheetPositionY,
        mSheetWidth, mSheetHeight,
        mDistanceBetweenMotors, mSheetHeight + mSheetPositionY, colLim);

  echelle(areaX + 6, areaY + 4, round(mSheetWidth/10));
  barre();
  
  while (arduino.available() > 0)
  {
    char mvt = arduino.readChar();
    
/************************
Les caractères envoyés sur le port série
correspondent aux différentes actions
à effectuer par le simulateur:

f = mLeftLength--
h = mLeftLength++
c = mRightLength--
e = mRightLength++
a = alimenter
b = désalimenter
w = ecrire
x = pas ecrire
E = erreur (suivi du code d'erreur)
_ = Message arduino
************************/

    switch(mvt)
    {
      case 'l':
        mLeftLength--;
      break;

      case 'L':
        mLeftLength++;
      break;

      case 'r':
        mRightLength--;
      break;

      case 'R':
        mRightLength++;
      break;

      case 'w':
        msgBarre = "Dessin en cours...";
        stroke(colEcrire);
      break;

      case 'x':
        msgBarre = "Déplacement en cours...";
        if (mDrawMoves) {
          stroke(colPasEcrire);
        } else {
          noStroke();
        }
      break;
        
      case 'a':
        etat = 0;
        barre();
      break;
      
      case 'b':
        etat = -1;
        barre();
      break;

      case 'n':
        etat = 1;
        msgBarre = "Le dessin a été reproduit avec succès.";
        barre();
      break;
      
      case '_':
        String msg = null;
  
        while (msg == null)
        {
          msg = arduino.readStringUntil('\n');
        }
        print(msg);
      break;
      
      // Si on a envoyé une erreur
      case 'E':
        numErr = arduino.readChar();
        // Appelle la fonction erreur()
        // qui va afficher l'erreur en print et sur l'interface.
        erreur(numErr);
        println();
      break;

      // Si on a envoyé un warning
      case 'W':
        numErr = arduino.readChar();
        // Appelle la fonction erreur()
        // qui va afficher l'erreur en print et sur l'interface.
        erreur(numErr);
        println();
      break;

      default:
        erreur(char(0));
        println(mvt + "'.");
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

void erreur(char code)
{
        println("\n\n*************************");
        
        if (code < 50) {
          println("Error " + int(code) + " : ");
        } else {
          print("Warning " + int(code) + " : ");          
        }
        
        switch (code)
        {
          case 0 :
            msgBarre = "Un caractère non-attendu a été reçu : '";
          break;
          
          case 1 :
            msgBarre = "Carte absente ou non reconnue";
          break;

          case 2 :
            msgBarre = "Le fichier ne peut pas être ouvert.";
          break;

          case 11 :
            msgBarre = "Le fichier svg est incomplet.";
          break;

          case 12 :
            msgBarre = "Le fichier n'est pas un fichier svg.";
          break;

          case 13 :
            msgBarre = "Le fichier svg n'inclut aucune donnée de dessin.";
          break;

          case 30 :
            msgBarre = "La distance entre les 2 moteurs est inférieure à la largeur de la feuille + position.";
          break;
          
          case 50 :
            msgBarre = "Le crayon a ateint la limite gauche.";
          break;

          case 51 :
            msgBarre = "Le crayon a ateint la limite droite.";
          break;

          case 52 :
            msgBarre = "Le crayon a ateint la limite haute.";
          break;

          case 53 :
            msgBarre = "Le crayon a ateint la limite basse.";
          break;

          case 60 :
            msgBarre = "Fonction SVG non reconnue : ";
          break;

          default :
            msgBarre = "Non répertorié.";
          break;
          
        }
        
        // On imprime le descriptif de l'erreur
        print(msgBarre);
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
    " | X: " + round(posX) + " Y:" + round(posY) +
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
