#!/usr/bin/python

# autor: js
# simple topology 1 switch , 2 hosts, 1 Access point, 2 stations, external controller
#


from mininet.node import Controller, OVSKernelSwitch, RemoteController
from mininet.log import setLogLevel, info
from mn_wifi.node import OVSKernelAP
from mn_wifi.cli import CLI
from mn_wifi.net import Mininet_wifi


def topology():
    "Create a network."
    net = Mininet_wifi(controller=RemoteController, switch=OVSKernelSwitch, accessPoint=OVSKernelAP)

    info("*** Creating nodes\n")

    c1 = net.addController('c1', controller=RemoteController, ip="127.0.0.1", port=6653)

    s1 = net.addSwitch('s1', protocols="OpenFlow13")
    h1 = net.addHost('h1')
    h2=  net.addHost('h2')

    ap1 = net.addAccessPoint('ap1', ssid='ssid-ap1', mode='g', channel='1',
                            position='10,10,1', range=17, protocols="OpenFlow13")
    sta1 = net.addStation('sta1', position='10,11,2')
    sta2 = net.addStation('sta2', position='10,9,2')
    info("*** Configuring Propagation Model\n")
    net.setPropagationModel(model="logDistance", exp=5)
    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()

    info("*** Creating links\n")
    net.addLink(s1, ap1)
    net.addLink(s1, h1)
    net.addLink(s1, h2)
   
    info("*** Starting network\n")
    net.build()
    c1.start()
    ap1.start([c1])
    s1.start([c1])

    info("*** Running CLI\n")
    CLI(net)

    info("*** Stopping network\n")
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    topology()
