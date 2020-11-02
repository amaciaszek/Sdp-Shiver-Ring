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
delay_length = 0.01
samples = []
gc.enable()
while True:
    lastButtonState = currentButtonState
    currentButtonState = switch.value

    if lastButtonState is True and currentButtonState is False:
        if calculating is True:
            calculating = False
        else:
            calculating = True
    if calculating:
        if blocksize > 255:
            test_fft = spectrogram(samples)
            maxi=1
            maxid=1
            for i in range(1,129):
                if (test_fft[i] > maxi):
                    maxi=test_fft[i]
                    maxid=i
            if test_fft[maxid] is test_fft[maxid+1]:
                maxid = maxid + 0.5
            period= 1/(maxid/(256*(delay_length+0.0051)))
            freq= 1.369567297*pow(period,-1.001297824)
            print(round(freq, 1))
            test_fft = []
            samples = []
            gc.collect()
            blocksize = 0
        else:
            #magnitude = sqrt(pow(mpu.acceleration[0],2)+pow(mpu.acceleration[1],2)+pow(mpu.acceleration[2],2))
            magnitude = mpu.acceleration[2]
            samples.append(magnitude-1 + 0.0j)
            blocksize=blocksize+1

    time.sleep(delay_length)


