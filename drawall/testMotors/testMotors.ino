/*
 * This file is part of Drawall, a vertical tracer (aka drawbot) - see http://drawall.fr/
 * Drawall is free software and licenced under GNU GPL v3 : http://www.gnu.org/licenses/
 * Copyright (c) 2012-2014 Nathanaël Jourdane
 */

int dirL = 6;
int stepL = 7;

int dirR = 8;
int stepR = 9;

int motors_steps = 6400;
int speed_tpm = 60;

int step_delay = int(60000000.0/(float(motors_steps)*float(speed_tpm)))/2;

void setup() {
  Serial.begin(9600);
  pinMode(stepL, OUTPUT);
  pinMode(dirL, OUTPUT);

  pinMode(stepR, OUTPUT);
  pinMode(dirR, OUTPUT);
}

void loop() {
  Serial.write("Left motor is moving up...\n");
  digitalWrite(dirL, HIGH);
  move(stepL);

  Serial.write("Left motor is moving down...\n");
  digitalWrite(dirL, LOW);
  move(stepL);

  Serial.write("Right motor is moving up...\n");
  digitalWrite(dirR, HIGH);
  move(stepR);

  Serial.write("Right motor is moving down...\n");
  digitalWrite(dirR, LOW);
  move(stepR);
}

void move(int step_pin) {  
  for(int i=0 ; i<motors_steps ; i++) {
    digitalWrite(step_pin, HIGH);
    delayMicroseconds(step_delay);
    digitalWrite(step_pin, LOW);
    delayMicroseconds(step_delay);
  }
  delay(1000);
}
