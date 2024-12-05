#!/usr/bin/python

# autor: js
# simple topology 1 switch , 2 hosts, 1 Access point, 2 stations, internal controller
# Station with 2 interfaces as client, connecting to server in h1 simultaneously by both interfaces. The log allows to check that it is working.
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
    sta2 = net.addStation('sta2', position='10,9,2', wlans=2)
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
    sta2.setIP('10.0.0.5', intf="sta2-wlan1")
    #
    info("*** Stabilizing setup\n")
    time.sleep(10)
    info("*** Starting traffic generation\n")
    h1.cmd("date >> /home/student/DTUsCodeTesting/MNScripts/S.log")
    h1.cmd("iperf -s -e -P 2 >> /home/student/DTUsCodeTesting/MNScripts/S.log &")
    info("*** IperfSrv Ready\n")
    sta2.cmd("date >> /home/student/DTUsCodeTesting/MNScripts/CI1.log &")
    sta2.cmd("iperf -B 10.0.0.4 -c 10.0.0.1 -e -b 2M >> /home/student/DTUsCodeTesting/MNScripts/CI1.log &")
    info("*** Iperf Interface1 test launched\n")
    sta2.cmd("date >> /home/student/DTUsCodeTesting/MNScripts/CI2.log &")
    sta2.cmd("iperf -B 10.0.0.5 -c 10.0.0.1 -e -b 2M >> /home/student/DTUsCodeTesting/MNScripts/CI2.log &")
    info("*** Iperf Interface2 test launched\n")
 
    info("*** Running CLI\n")
    CLI(net)

    info("*** Stopping network\n")
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    topology()
