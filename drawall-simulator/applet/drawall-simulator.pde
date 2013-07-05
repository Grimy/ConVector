import processing.serial.*;

Serial arduino;  // le port série

color colEcrire = 0;
color colPasEcrire = #B9B79F;
color colLim = #EEEEEE;
color colBarreAlim = #CCFFAF;
color colBarreDesalim = #FFAFAF;
color colFond = #AAAAAA;
color colCurseur = #FC2E31;

float surfaceL;
float surfaceH;

int barreH = 15;

int zoom = 4;

int motG, motD;

float posX, posY; // position du stylo
float ratioDist;

float limG;
float limD;
float limH;
float limB;

boolean alim = false;
void setup()
{
  // *** acquisition des données d'init ***
  print("*** ports détectés (le 1er sera le port utilisé) ***");
  println(Serial.list()); // listing des ports dispos

  // init la communication avec l'arduino
  arduino = new Serial(this, Serial.list()[0], 9600);
  
  println("\n*** informations pré-traitement ***");

  // tableau récupéré sur la liason série qui contiendra:
  float tabInit[] = new float[9]; // les variables d'initialisation
  // --> { surfaceL, surfaceH, aG, aD  }

  String msg = null;
  
  // attends la réception
  while (msg == null)
  {
    // récupère tout jusqu'au $
    msg = arduino.readStringUntil('$'); 
  }

  // on imprime les messages envoyés sur le port série
  // ils sont à titre informatifs et ne sont pas nécessaires au fonctionnement.
  println(msg);

  println("\n*** données d'initialisation ***");
  
  // retire les espaces, separe et stoque les param. dans un tableau
  tabInit = float(split(trim(arduino.readStringUntil('\n')), ","));
  /*
  Si on a l'erreur: 
  Exception in thread "Animation Thread"
  java.lang.NullPointerException
  c'est que l'envoi du message d'initialisation ne s'est pas fait correctement.
  */
  
  surfaceL = tabInit[0];
  surfaceH = tabInit[1];

  motG = int(tabInit[2]);
  motD = int(tabInit[3]);
  
  ratioDist = tabInit[4];
  
  limG = tabInit[5];
  limD = tabInit[6];

  limH = tabInit[7];
  limB = tabInit[8];
  
  println("surfaceL: " + surfaceL);
  println("surfaceH: " + surfaceH);
  println("motG: " + motG);
  println("motD: " + motD);
  println("ratioDist: " + ratioDist);
  println("limG: " + limG);
  println("limD: " + limD);
  println("limH: " + limH);
  println("limB: " + limB);
  
  println("\n*** lancement de l'interface ***");

  // *** texte ***
  PFont maTypo = loadFont("Garuda-12.vlw"); // choix de la typo
  textFont(maTypo, 12);  // Définition de la taille de la typo

  // *** creation fenêtre ***
  background(colFond);

  size (800, 600);
  frame.setResizable(true);
}

void draw()
{
  float eX = width/surfaceL;
  float eY = (height - barreH)/surfaceH;
  
  float eL, eH; // dimentions du rectangle qui conserve les proportions
  
  if(eX > eY)
    eX = eY;
  else
    eY = eX;

  float debX = (width-surfaceL*eX)/2;
  float debY = (height - barreH - surfaceH*eY)/2;
  
  rectOut(limG*eX, limH*eY, (surfaceL-limD-limG)*eX,
  (surfaceH-limB-limH)*eY, surfaceL*eX, surfaceH*eY, colLim);

  echelle(debX + 6, 5, eX, eY, 30, 3);
    
  while (arduino.available() > 0)
  {
    char mvt = arduino.readChar();
    
    /************************
    __________ mvt __________
    f = motG--
    h = motG++
    c = motD--
    e = motD++
    a = alimenter
    b = désalimenter
    w = ecrire
    x = pas ecrire
    ************************/

    switch(mvt)
    {
      case 'f':
        motG--;
      break;

      case 'h':
        motG++;
      break;

      case 'c':
        motD--;
      break;

      case 'e':
        motD++;
      break;

      case 'w':
        stroke(colEcrire);
      break;

      case 'x':
        stroke(colPasEcrire);
      break;
        
      case 'a':
        alim = true;
        barre();
      break;
      
      case 'b':
        alim = false;
        barre();
      break;

      case 't':
        println("test");
      break;
      
      case '_':
        print(arduino.readStringUntil('\n'));
      break;
      }

    // calcul position en pas
    posX = ( pow(float(motG)/ratioDist, 2) - pow(float(motD)/ratioDist, 2)
      + pow(surfaceL, 2) ) / (2*surfaceL);
    posY = sqrt( pow(float(motG)/ratioDist, 2) - pow(posX, 2) );

    barre();
    
    point(posX*eX + debX, posY*eY + debY);

  }
}

void barre()
{
  pushStyle();
  // *** barre de statut ***
  if(alim) // couleur de la barre en fonction de l'état du moteur
    fill(colBarreAlim);
  else
    fill(colBarreDesalim);
  
  stroke(0); // contour noir
  
  rect(-1, height - barreH, width + 1, barreH);
  // rectangle de la barre (écrase l'ancien texte)
  
  fill(0); // couleur du texte
  
  text(
    "surface: " + round(surfaceL-limG-limD) + "x" + round(surfaceH-limH-limB) +
    "  |  X: " + round(posX) + " Y:" + round(posY) +
    "  |  motG: " + motG + " motD: " + motD +
    "  |  ratio: " + ratioDist +
    "  |"
  , 4, height - 3); // écriture du texte
  popStyle();
}

void rectOut( float x, float y, float w, float h, float limL, float limH, color fillCol) {
  pushStyle();
  noStroke();
  
  float debX = (width-limL)/2;
  float debY = (height - barreH - limH)/2;
  
  fill( fillCol );
  rect(debX + 1, debY - 1, x - 1, limH);
  rect(debX + 1, debY - 1, limL - 1, y);
  rect(x + w + debX, debY - 1, limL - x - w, limH);
  rect(debX + 1, debY + y + h, limL - 1, limH - y - h);

  noFill();
  stroke(0);

  rect(x + debX , y + debY - 1, w - 1, h + 1);

  //lignes verticales
  line(debX, debY, debX, debY + limH);
  line(debX + limL - 1, debY - 1, debX + limL - 1, debY + limH);

  //lignes horizontales
  line(debX, debY - 1, debX + limL - 1, debY - 1);
  line(debX, debY + limH, debX + limL - 1, debY + limH);
  
  popStyle();
}

void echelle(float x, float y, float eX, float eY, float ech, int flecheL)
{
  pushStyle();
  stroke(0);
  fill(0);

  x = round(x);
  y = round(y);

  line(x, y, x + eX*ech, y);
  line(x + eX*ech, y, x + eX*ech - flecheL, y - flecheL);
  line(x + eX*ech, y, x + eX*ech - flecheL, y + flecheL);
  
  line(x, y, x, y + eY*ech);
  line(x, y + eY*ech, x - flecheL, y + eY*ech - flecheL);
  line(x, y + eY*ech, x + flecheL, y + eY*ech - flecheL);
  
  text( (ech/10) + " cm", x + 4, y + 14);
  
  popStyle();
}
