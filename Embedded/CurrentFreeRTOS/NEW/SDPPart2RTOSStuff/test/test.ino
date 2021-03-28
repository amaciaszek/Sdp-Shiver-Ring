#define  ERROR_LED_PIN  13 //Led Pin: Typical Arduino Board

#define ERROR_LED_LIGHTUP_STATE  HIGH // the state that makes the led light up on your board, either low or high

#define DEVICE_TOTAL_RAM  192000 // bytes of ram for this processor

#define SERIAL Serial //Adafruit, other Samd21 Boards


void setup() {
  // put your setup code here, to run once:
  Serial1.flush();
  SERIAL.flush();
  SERIAL.begin(115200);
  Serial1.begin(9600);
  delay(1000); // prevents usb driver crash on startup, do not omit this
  while (!SERIAL) ;  // Wait for serial terminal to open port before starting program
  while (!Serial1);

  SERIAL.println("");
  SERIAL.println("******************************");
  SERIAL.println("        Program start         ");
  SERIAL.println("******************************");
  SERIAL.flush();

  SERIAL.println(Serial1.println("AT+BAUD?"));
  Serial1.flush();
  delay(100);
  SERIAL.println(Serial1.read());


  
}

void loop() {
  // put your main code here, to run repeatedly:
  SERIAL.print("."); //print out dots in terminal
  SERIAL.flush();      
  delay(100); //delay is interrupt friendly, unlike vNopDelayMS
}
