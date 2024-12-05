#!/usr/bin/python

# autor: js
# simple topology 1 switch , 1 host
# connecting to external interface: a tup interface create on the MN host to do so in the VM, previoulsy to launching the script:
#
#     sudo ip tuntap add mode tap dev mytap
#     sudo id addr add 11.11.11.11/24 mytap
#     sudo ip link set dev mytap up
#
#  It did not work with a tup. But it works with a veth. Check example 5


from mininet.node import Controller, OVSKernelSwitch, RemoteController
from mininet.link import Intf
from mininet.log import setLogLevel, info
from mn_wifi.node import OVSKernelAP
from mn_wifi.cli import CLI
from mn_wifi.net import Mininet_wifi
import time


def topology():
    "Create a network."
    net = Mininet_wifi(controller=Controller, switch=OVSKernelSwitch, accessPoint=OVSKernelAP)

    info("*** Creating nodes\n")

    c1 = net.addController('c1')
    s1 = net.addSwitch('s1')
    h1 = net.addHost('h1')
    h2 = net.addHost('h2')

    info("*** Creating links\n")
    net.addLink(s1, h1)
    net.addLink(s1, h2)
    intf_=Intf("mytap", node=s1) # s1.attach("mytap")  # This is the line that enables the external interface to be attached as a port to the switch.
    
   
    info("*** Starting network\n")
    net.build()
    c1.start()
    s1.start([c1])
 
    info("*** Running CLI\n")
    CLI(net)

    info("*** Stopping network\n")
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    topology()
