import serial
import time
from datetime import datetime

ser = serial.Serial('COM7', 115200)
time.sleep(2)

while(1<2):
    file1 = open("sdp_data.txt","a")
    b = ser.readline()
    string_n = b.decode() 
    string = str(datetime.now())[10:19]+" "+string_n.rstrip()+"\n" # remove \n and \r
    print(string)
    file1.write(string)
    file1.close()
    time.sleep(0.1)
ser.close()