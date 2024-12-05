#! /usr/bin/env python

# The following line will import all Scapy modules
from scapy.all import *
import sys
import time

time.sleep(60)

i = 1
while i < 50:
  #send(IP(src="192.168.0.204" ,dst="192.168.0.203")/UDP()/"STOP THE CAR")
  send(IP(src="192.168.0.205" ,dst="192.168.0.201")/UDP(sport=135,dport=135)/"Collision Happend: Car is Stopped")
  #send(IP(src="192.168.7.105" ,dst="192.168.7.101")/UDP(sport=135,dport=135)/"STOP THE CAR")
  i += 1
  print("Car1 is sending msg to CarEmergency Server:Collision Happend: Car is Stopped")
  time.sleep(2)



