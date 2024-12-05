#!/usr/bin/python

# autor: js
# simple topology 1 switch , 1 host
# connecting to external interface: an interface "v2" from the host VM
#
#      To test this a veth pair v1-v2 is created so that it is one side of the veth (v2) which is attched to the Mininet switch.
#      The other side of the veth (v1) can be moved to a separate network namespace and traffic send/receive from there towards Mininet.
#      The reason to move it to a separate network namespace is to avoid modifying the routing and iptables of the VM to enable the routing tyo from v1.
#      An accopanying shell script creates all this.
#


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
    h2 = net.addHost('h2', address="10.0.0.2")
    h3 = net.addHost('h3', address="10.0.0.3")

    info("*** Creating links\n")
    net.addLink(s1, h2)
    net.addLink(s1, h3)
    intf_=Intf("v2", node=s1) # s1.attach("mytap")
    
   
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
