#include <SD.h>
#include <Servo.h>

#include <Drawbot.h>

Draw drawbot(1000, 750, "logo1.svg");

void setup() {
  Serial.begin(57600);
  drawbot.commencer();
  drawbot.setVitesse(20);
  delay(2000);
}

void loop() {
  drawbot.svg();

  drawbot.centrer();
  drawbot.alimenter(false);
  
  while(true) {}
}
