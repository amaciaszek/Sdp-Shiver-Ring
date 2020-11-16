import time
import board
import busio
import adafruit_mpu6050
from digitalio import DigitalInOut, Direction, Pull
import array
from fft import spectrogram
from math import sqrt
import gc

switch = DigitalInOut(board.D2)
switch.direction = Direction.INPUT
switch.pull = Pull.UP
calculating = False
currentButtonState = switch.value
i2c = busio.I2C(board.SCL, board.SDA)
mpu = adafruit_mpu6050.MPU6050(i2c)
blocksize = 0
delay_length = 0.002
samples = []
data=[]
gc.enable()
start=0
stop=0

while True:
    lastButtonState = currentButtonState
    currentButtonState = switch.value

    if lastButtonState is True and currentButtonState is False:
        if calculating is True:
            '''
            with open('datafile.txt', 'w') as filehandle:
                for listitem in data:
                    filehandle.write('%s\n' % listitem)
            '''
            calculating = False
        else:
            calculating = True
    if calculating:
        if blocksize > 255:
            output=[0,0,0]
            test_fft = spectrogram([ x[0] for x in samples])
            maxi=0
            maxid=1
            for i in range(1,129):
                if (test_fft[i] > maxi):
                    maxi=test_fft[i]
                    maxid=i
            if test_fft[maxid] is test_fft[maxid+1]:
                maxid = maxid + 0.5
            period= 1/(maxid/(256*(delay_length+0.0051)))
            freq= 0.42387727*pow(maxid,0.97923666)
            output[0]=round(freq, 0)
            #data.append(str(time.monotonic())+","+str(round(freq, 1)))
            test_fft = []
            gc.collect()
            period=1
            freq=1
            test_fft = spectrogram([ x[1] for x in samples])
            maxi=0
            maxid=1
            for i in range(1,129):
                if (test_fft[i] > maxi):
                    maxi=test_fft[i]
                    maxid=i
            if test_fft[maxid] is test_fft[maxid+1]:
                maxid = maxid + 0.5
            period= 1/(maxid/(256*(delay_length+0.0051)))
            freq= 0.42387727*pow(maxid,0.97923666)
            output[1]=round(freq, 0)

            #data.append(str(time.monotonic())+","+str(round(freq, 1)))
            test_fft = []
            gc.collect()
            period=1
            freq=1

            test_fft = spectrogram([ x[2] for x in samples])
            maxi=0
            maxid=1
            for i in range(1,129):
                if (test_fft[i] > maxi):
                    maxi=test_fft[i]
                    maxid=i
            if test_fft[maxid] is test_fft[maxid+1]:
                maxid = maxid + 0.5
            period= 1/(maxid/(256*(delay_length+0.0051)))
            freq= 0.42387727*pow(maxid,0.97923666)
            output[2]=round(freq, 0)
            test_fft = []
            samples = []
            blocksize = 0
            print(output)
            gc.collect()
        else:
            #magnitude = sqrt(pow(mpu.acceleration[0],2)+pow(mpu.acceleration[1],2)+pow(mpu.acceleration[2],2))
            #magnitude = mpu.acceleration[2]
            #print(magnitude)
            gc.collect()
            num=mpu.acceleration
            samples.append((num[0]-1 + 0.0j,num[1]-1 + 0.0j,num[2]-1 + 0.0j))
            blocksize=blocksize+1
    time.sleep(delay_length)