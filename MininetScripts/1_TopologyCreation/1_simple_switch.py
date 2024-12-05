#!/usr/bin/python

# autor: js
# simple topology 1 switch , 2 hosts, external controller
#


from mininet.node import Controller, OVSKernelSwitch, RemoteController
from mininet.log import setLogLevel, info
from mn_wifi.cli import CLI
from mn_wifi.net import Mininet_wifi


def topology():
    "Create a network."
    net = Mininet_wifi(controller=RemoteController, switch=OVSKernelSwitch)

    info("*** Creating nodes\n")
   
    c1 = net.addController('c1', controller=RemoteController, ip="127.0.0.1", port=6653)
    s1 = net.addSwitch('s1', protocols="OpenFlow13")
    h1 = net.addHost('h1')
    h2=  net.addHost('h2')

    info("*** Creating links\n")
    net.addLink(s1, h1)
    net.addLink(s1,h2)

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
