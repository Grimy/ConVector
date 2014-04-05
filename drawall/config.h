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
 * \file	config.h
 * \author  Nathanaël Jourdane
 * \brief   Affectation des pins, des modes de compilation et de la vitesse de communication série.
 */

// *** Modes de compilation ***
// Décommenter la ligne si la fonction n'est pas désirée

/// Active le support des boutons.
// #define BUTTONS true

/// Active l'envoi d'informations par liason série
#define SERIAL true

/// Active l'envoi d'informations de deboguage.
// #define DEBUG true

/// Active le support de l'écran.
// #define SCREEN true

// *** Autre ***

/// Vitesse de la communication série.
#define SERIAL_BAUDS 57600

// *** Affectation des pins ***

// Pins 0 et 1 : utilisées par le port série (communication avec Processing)

// Pin 2 utilisé par l'interruption du bouton pause

/// Pin du capteur fin de course du moteur gauche.
#define PIN_LEFT_CAPTOR 3

/// Pin du capteur fin de course du moteur droit.
#define PIN_RIGHT_CAPTOR 4

/// Pin de commande du servo-moteur.
#define PIN_SERVO 5

/// Pin DIR (la direction) du moteur gauche.
#define PIN_LEFT_MOTOR_DIR 6

/// Pin STEP (les pas) du moteur gauche.
#define PIN_LEFT_MOTOR_STEP 7

/// Pin DIR (la direction) du moteur droit.
#define PIN_RIGHT_MOTOR_DIR 8

/// Pin STEP (les pas) du moteur droit.
#define PIN_RIGHT_MOTOR_STEP 9

/// Pin CS de la carte SD.
/// Snootlab et Adafruit : 10 - Sparkfun : 8
#define PIN_SD_CS 10

// Pins 11, 12 et 13 : utilisés par la carte SD (MOSI, MISO, SCK)

// Pin de la diode infra-rouge pour télécommande.
// #define PIN_REMOTE A0

/// Pin pour desactivation des moteurs (nommé /en sur pololu).
#define PIN_OFF_MOTORS A0

/// Pin SCE de l'écran LCD
#define PIN_SCREEN_SCE A1

/// Pin RESET de l'écran LCD
#define PIN_SCREEN_RST A2

/// Pin DC de l'écran LCD
#define PIN_SCREEN_DC A3

/// Pin SDIN de l'écran LCD
#define PIN_SCREEN_SDIN A4

/// Pin SCLK de l'écran LCD
#define PIN_SCREEN_SCLK A5
