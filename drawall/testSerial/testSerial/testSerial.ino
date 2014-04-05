void setup() {
  Serial.begin(9600);
  String line = "";
  while (line != "START") {
    line = readLine();
  }
  Serial.println("START_INIT");
}

void loop() {

}

String readLine() {
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
