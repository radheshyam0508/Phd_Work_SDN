#!/usr/bin/python

# autor: js
# simple topology 1 switch , 2 hosts, 1 Access point, 2 stations, internal controller
# 2 clients (h1 and h2) sending to the same server (sta2) using iperf3 and logging results. This allows to see that it is not working.
#


from mininet.node import Controller, OVSKernelSwitch, RemoteController
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
    h2=  net.addHost('h2')

    ap1 = net.addAccessPoint('ap1', ssid='ssid-ap1', mode='g', channel='1',
                            position='10,10,1', range=17)
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
 
    info("*** Stabilizing setup\n")
    time.sleep(15)
    info("*** Starting traffic generation\n")
    sta2.cmd("iperf3 -s -D --logfile /home/student/DTUsCodeTesting/MNScripts/traffictest1_Sr.log")
    info("*** IperfSrv Ready\n")
    h1.cmd("iperf3 -c 10.0.0.4 -P 1 -b 2M -u --logfile /home/student/DTUsCodeTesting/MNScripts/traffictest1_Cl1.log &")
    info("*** IperfCl1 test launched\n")
    h2.cmd("iperf3 -c 10.0.0.4 -P 1 -b 2M -u --logfile /home/student/DTUsCodeTesting/MNScripts/traffictest1_Cl2.log &")
    info("*** IperfCl2 test launched\n")
 
    info("*** Running CLI\n")
    CLI(net)

    info("*** Stopping network\n")
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    topology()
