import board
import digitalio
import storage

switch1 = digitalio.DigitalInOut(board.D5)

switch1.direction = digitalio.Direction.INPUT
switch1.pull = digitalio.Pull.UP

# If the switch pin is connected to ground CircuitPython can write to the drive
storage.remount("/", switch1.value)