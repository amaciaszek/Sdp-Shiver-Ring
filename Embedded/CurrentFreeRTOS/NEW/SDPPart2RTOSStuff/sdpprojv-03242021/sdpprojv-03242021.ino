#include <FreeRTOS_SAMD51.h> //samd51
#include <MemoryFree.h>
#include "Wire.h"
#include "I2Cdev.h"
#include "MPU6050.h"
MPU6050 accelgyro;
int16_t ax, ay, az;
int16_t gx, gy, gz;
//**************************************************************************
// Type Defines and Constants
//**************************************************************************
#define M_PI 3.14159265358979323846264338327950288
#define  ERROR_LED_PIN  13 //Led Pin: Typical Arduino Board

#define ERROR_LED_LIGHTUP_STATE  HIGH // the state that makes the led light up on your board, either low or high

#define DEVICE_TOTAL_RAM  192000 // bytes of ram for this processor

// Select the serial port the project should use and communicate over
// Some boards use SerialUSB, some use Serial
#define SERIAL          Serial //Adafruit, other Samd21 Boards

//**************************************************************************
// global variables
//**************************************************************************
TaskHandle_t Handle_aTask;
TaskHandle_t Handle_bTask;
TaskHandle_t Handle_cTask;
TaskHandle_t Handle_monitorTask;
bool sendEmergencyText = false;
bool raiseAlarm = false;

struct complexNum {
  double r;
  double i;

complexNum operator*(double c){
  complexNum p;
  p.r = this->r * c;
  p.i = this->i * c;
  return p;
}

complexNum operator*(int c){
  complexNum p;
  p.r = this->r * c;
  p.i = this->i * c;
  return p;
}

complexNum operator*(const complexNum& c){
  complexNum p;
  p.r = this->r*c.r - this->i*c.i;
  p.i = this->r*c.i + c.r*this->i;
  return p;
}

complexNum operator+(const complexNum& c){
  complexNum p;
  p.r = this->r + c.r;
  p.i = this->i + c.i;
  return p;
}

complexNum operator-(const complexNum& c){
  complexNum p;
  p.r = this->r - c.r;
  p.i = this->i - c.i;
  return p;
}
};
// takes in the input and output, with the arguments N being the size of the input/output array, and s=1 initially
void fft(complexNum *funcInput, complexNum *output, int N, int s)
{
  
  if (s < N) 
  {
    // Recursive Functionality 
    fft(output, funcInput, N,s * 2); // even recursive branch
    fft(output + s, funcInput + s, N, s * 2); // odd recursive branch

    double bing = 0.0;
    double bong = 0.0;
    double b5 = 0.0;
    double b6 = 0.0;
    // For loop for k cycles
    for (int k = 0; k < N; k += 2 * s)
    {
      bing=cos(-M_PI*k/N);
      bong=sin(-M_PI*k/N);
      b5=bong*output[k+s].r+bing*output[k+s].i;
      b6=bing*output[k+s].r-bong*output[k+s].i;
      complexNum out{b6, b5};

      funcInput[k / 2] = output[k] + out;
      funcInput[(k + N) / 2] = output[k] - out;
    }
  }
}

void spectrogram(complexNum *inputData, complexNum *outputData, int *spectrumOut, int len){
  fft(inputData, outputData, len, 1);
  float abs_val;
  for(int k = 0; k < len; k++){
    abs_val = sqrt(pow(inputData[k].r,2)+pow(inputData[k].i,2));
    if(abs_val != 0.0){
      spectrumOut[k] = int(log(abs_val));
    } 
    else {
      spectrumOut[k] = 0;
    }
  }  
}

//**************************************************************************
// Can use these function for RTOS delays
// Takes into account processor speed
// Use these instead of delay(...) in rtos tasks                      
//**************************************************************************
void myDelayUs(int us)
{
  vTaskDelay( us / portTICK_PERIOD_US );  
}
void myDelayMs(int ms)
{
  vTaskDelay( (ms * 1000) / portTICK_PERIOD_US );  
}
void myDelayMsUntil(TickType_t *previousWakeTime, int ms)
{
  vTaskDelayUntil( previousWakeTime, (ms * 1000) / portTICK_PERIOD_US );  
}

//**************************************************************************
// Print how much ram is free on the device
// Useful to see how much ram is available at startup with current heap size settings, and after initializing all classes
// freeMemory() gives wrong answers after starting the rtos for unknown reasons, only use before rtos start
//**************************************************************************
void printRamFree()
{
  SERIAL.print("Ram Remaining : (");
  SERIAL.print( freeMemory() );
  SERIAL.print(" / ");
  SERIAL.print(DEVICE_TOTAL_RAM);
  SERIAL.print(") bytes  ");
  double percentage = ((double)freeMemory() / (double)DEVICE_TOTAL_RAM) * 100;
  SERIAL.print( percentage );
  SERIAL.println("%");
  SERIAL.flush();
}

//*****************************************************************
// Create a thread that prints out A to the screen every two seconds
// this task will delete its self after printing out afew messages
//*****************************************************************
static void threadA( void *pvParameters ) 
{
  int threshold =0;
  int space =0;
  while(1){
  int N = 1024;
  complexNum* buff_x;
  buff_x = new complexNum[N];
  complexNum* buff_y;
  buff_y = new complexNum[N];
  complexNum* buff_z;
  buff_z = new complexNum[N];
  int start=0;
  int stops=0;
  for (int i = 0; i < N; i++) {
    accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
    buff_x[i]={ax,0.0};
    buff_y[i]={ay,0.0};
    buff_z[i]={az,0.0};
    myDelayMs(4);
  }
  
  complexNum* outputs;
  outputs = new complexNum[N];
  for(int i = 0; i<N; i++){outputs[i] = buff_x[i];}
  int* spectrum_out_x;
  spectrum_out_x = new int[N];
  int* spectrum_out_y;
  spectrum_out_y = new int[N];
  int* spectrum_out_z;
  spectrum_out_z = new int[N];
  spectrogram(buff_x, outputs, spectrum_out_x, N);
  delete[] buff_x;
  spectrogram(buff_y, outputs, spectrum_out_y, N);
  delete[] buff_y;
  spectrogram(buff_z, outputs, spectrum_out_z, N);
  delete[] buff_z;
  delete[] outputs;

  int high_index_x=1;int high_index_y=1;int high_index_z=1;
  int maxi = N/2+1;
  for(int i = 1; i<maxi; i++){
    if(spectrum_out_x[i] > spectrum_out_x[high_index_x] && spectrum_out_x[i] >7){high_index_x=i;}
    if(spectrum_out_y[i] > spectrum_out_y[high_index_y] && spectrum_out_y[i] >7){high_index_y=i;}
    if(spectrum_out_z[i] > spectrum_out_z[high_index_z] && spectrum_out_z[i] >7){high_index_z=i;}
  }
  if(high_index_x >100){high_index_x=1;}
  if(high_index_y >100){high_index_y=1;}
  if(high_index_z >100){high_index_z=1;}
  
  double freqx = high_index_x*0.19518+0.02465;
  double freqy = high_index_y*0.19518+0.02465;
  double freqz = high_index_z*0.19518+0.02465;


  SERIAL.print(freqx);
  SERIAL.print(" , ");
  SERIAL.print(freqy);
  SERIAL.print(" , ");
  SERIAL.print(freqz);

  
  delete[] spectrum_out_x;
  delete[] spectrum_out_z;
  delete[] spectrum_out_y;
  SERIAL.println("\nDetection:");
  if(freqx > 7.7 && freqx < 12.3){threshold=threshold+1; space=0; SERIAL.print(" X is bad \n");}
  else{space=space+1;}
  
  if(freqy > 7.7 && freqy < 12.3){threshold=threshold+1; space=0; SERIAL.print(" Y is bad \n");}
  else{space=space+1;}
  
  if(freqz > 7.7 && freqz < 12.3){threshold=threshold+1; space=0; SERIAL.print(" Z is bad \n");}
  else{space=space+1;}

  if(space > 12){threshold = 0;space=0;}
  
  if(threshold > 5){raiseAlarm=true; threshold=0;SERIAL.print(" Alarm Raised! ");}
  myDelayMs(100);
  
  }
  
  myDelayMs(500);
  // delete ourselves.
  // Have to call this or the system crashes when you reach the end bracket and then get scheduled.
  SERIAL.println("Detection Thread Exiting");
  SERIAL.flush();
  vTaskDelete( NULL );
}

//*****************************************************************
// Create a thread that prints out B to monitor before sending bluetooth
//*****************************************************************
static void threadB( void *pvParameters ) 
{
  while(1)
  {
    if(sendEmergencyText==true)
    {
    SERIAL.println("\nEMERGENCY: SENDING ALERTS\n");
  
    Serial1.print("EMERGENCY");
    myDelayMs(3000);
    while(Serial1.available())
    {
      SERIAL.print((char)Serial1.read());
    }
    SERIAL.print("\n");
    sendEmergencyText=false;
                                                    //  SERIAL.println(Serial1.print("AT"));
                                                    //  delay(1000);
                                                    //  while(Serial1.available())        // Disconnect phone if connected, or basic test of device AT Command
                                                    //  {
                                                    //    SERIAL.print((char)Serial1.read());
                                                    //  }
                                                    //  SERIAL.print("\n");
                                                      
                                                    //  SERIAL.println(Serial1.print("WAKE\r\n"));   //  SERIAL.println(Serial1.print("AT+MODE?"));   //  SERIAL.println(Serial1.print("AT+TYPE?"));
                                                    //  delay(200);
                                                    //  SERIAL.println(Serial1.print("AT+NAME?"));
                                                    //  delay(500);                   // Get name of device, can be used in future to rename
                                                    //  while(Serial1.available())
                                                    //  {
                                                    //    SERIAL.print((char)Serial1.read());
                                                    //  }
                                                    //  SERIAL.print("\n");
                                                    
    }
    else
    {
      SERIAL.println("NOEVENT");
    }
    myDelayMs(1000);
  }
  SERIAL.println("Alert Thread:Exiting\n");
  SERIAL.flush();
  vTaskDelete( NULL );
}

static void threadC( void *pvParameters ){
  int msresetcount=0;
  int msalarmcount=0;
 
  myDelayMs(2000);
 
  while(1)
  {
    if(raiseAlarm == true)
    {
       SERIAL.println("\nSTARTING ALARM\n");
       digitalWrite(7, HIGH);   // turn the LED on (HIGH is the voltage level)
       while(raiseAlarm == true)
       {
          int buttonState = digitalRead(6);
          if(buttonState == HIGH){
            msresetcount = msresetcount + 1;
            msalarmcount = msalarmcount + 1;
            myDelayMs(1);
          }
          else
          {
            msresetcount=0;
            msalarmcount = msalarmcount + 1;
            myDelayMs(1);
          }
          if(msresetcount >= 1000)
          {
            raiseAlarm=false;
            msresetcount=0;
            msalarmcount=0;
            digitalWrite(7, LOW);   // turn the LED on (HIGH is the voltage level)
            SERIAL.println("\nEnding Alarm\n");
          }
          if(msalarmcount >= 9000)
          {
            sendEmergencyText = true;
            myDelayMs(1);
          }
       }
    }
    myDelayMs(1000);
  }
  
}

//*****************************************************************
// Task will periodically print out useful information about the tasks running
// Is a useful tool to help figure out stack sizes being used
// Run time stats are generated from all task timing collected since startup
// No easy way yet to clear the run time stats yet
//*****************************************************************
static char ptrTaskList[400]; //temporary string buffer for task stats

void taskMonitor(void *pvParameters)
{
    int x;
    int measurement;
    SERIAL.println("Task Monitor: Started");
    // run this task afew times before exiting forever
    for(x=0; x<10; ++x)
    {
    SERIAL.flush();
    SERIAL.println("");
      SERIAL.println("****************************************************");
      SERIAL.print("Free Heap: ");
      SERIAL.print(xPortGetFreeHeapSize());
      SERIAL.println(" bytes");

      SERIAL.print("Min Heap: ");
      SERIAL.print(xPortGetMinimumEverFreeHeapSize());
      SERIAL.println(" bytes");
      SERIAL.flush();

      SERIAL.println("****************************************************");
      SERIAL.println("Task            ABS             %Util");
      SERIAL.println("****************************************************");

      vTaskGetRunTimeStats(ptrTaskList); //save stats to char array
      SERIAL.println(ptrTaskList); //prints out already formatted stats
      SERIAL.flush();

    SERIAL.println("****************************************************");
    SERIAL.println("Task            State   Prio    Stack   Num     Core" );
    SERIAL.println("****************************************************");

    vTaskList(ptrTaskList); //save stats to char array
    SERIAL.println(ptrTaskList); //prints out already formatted stats
    SERIAL.flush();

    SERIAL.println("****************************************************");
    SERIAL.println("[Stacks Free Bytes Remaining] ");

    measurement = uxTaskGetStackHighWaterMark( Handle_aTask );
    SERIAL.print("Thread A: ");
    SERIAL.println(measurement);

    measurement = uxTaskGetStackHighWaterMark( Handle_bTask );
    SERIAL.print("Thread B: ");
    SERIAL.println(measurement);

    measurement = uxTaskGetStackHighWaterMark( Handle_monitorTask );
    SERIAL.print("Monitor Stack: ");
    SERIAL.println(measurement);

    SERIAL.println("****************************************************");
    SERIAL.flush();

      myDelayMs(10000); // print every 10 seconds
    }

    // delete ourselves.
    // Have to call this or the system crashes when you reach the end bracket and then get scheduled.
    SERIAL.println("Task Monitor: Deleting");
    vTaskDelete( NULL );
}

//*****************************************************************
void setup() 
{
  SERIAL.flush();
  Wire.begin();      // join I2C bus
  SERIAL.begin(115200);
  Serial1.begin(115200);
  accelgyro.initialize();
  accelgyro.setFullScaleAccelRange(MPU6050_ACCEL_FS_16);

  delay(1000); // prevents usb driver crash on startup, do not omit this
  while (!SERIAL) ;  // Wait for serial terminal to open port before starting program

  pinMode(7, OUTPUT);
  pinMode(6, INPUT);
  
  // show the amount of ram free at startup
  /*printRamFree();*/
  
  SERIAL.println("");
  SERIAL.println("******************************");
  SERIAL.println("        Program start         ");
  SERIAL.println("******************************");
  SERIAL.flush();      

  // Set the led the rtos will blink when we have a fatal rtos error
  // RTOS also Needs to know if high/low is the state that turns on the led.
  // Error Blink Codes:
  //    3 blinks - Fatal Rtos Error, something bad happened. Think really hard about what you just changed.
  //    2 blinks - Malloc Failed, Happens when you couldn't create a rtos object. 
  //               Probably ran out of heap.
  //    1 blink  - Stack overflow, Task needs more bytes defined for its stack! 
  //               Use the taskMonitor thread to help gauge how much more you need
  vSetErrorLed(ERROR_LED_PIN, ERROR_LED_LIGHTUP_STATE);

  // sets the serial port to print errors to when the rtos crashes
  // if this is not set, serial information is not printed by default
  vSetErrorSerial(&SERIAL);

  // Create the threads that will be managed by the rtos
  // Sets the stack size and priority of each task
  // Also initializes a handler pointer to each task, which are important to communicate with and retrieve info from tasks
  xTaskCreate(threadA,     "Task A",       1512, NULL, tskIDLE_PRIORITY + 3, &Handle_aTask);
  xTaskCreate(threadB,     "Task B",       256, NULL, tskIDLE_PRIORITY + 2, &Handle_bTask);
  xTaskCreate(threadC,     "Task C",       256, NULL, tskIDLE_PRIORITY + 1, &Handle_cTask);
  /*xTaskCreate(taskMonitor, "Task Monitor", 256, NULL, tskIDLE_PRIORITY + 1, &Handle_monitorTask);*/

  // show the amount of ram free after initializations
  /*printRamFree();*/

  // Start the RTOS, this function will never return and will schedule the tasks.
  vTaskStartScheduler();

  // error scheduler failed to start
  // should never get here
  while(1)
  {
    SERIAL.println("Scheduler Failed! \n");
    SERIAL.flush();
    delay(1000);
  }

}

//*****************************************************************
// This is now the rtos idle loop
// No rtos blocking functions allowed!
//*****************************************************************
void loop() 
{
  // Optional commands, can comment/uncomment below
  SERIAL.print("."); //print out dots in terminal, we only do this when the RTOS is in the idle state
  SERIAL.flush();      
  delay(100); //delay is interrupt friendly, unlike vNopDelayMS
}
//*****************************************************************
