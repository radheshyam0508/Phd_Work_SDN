#!/usr/bin/python

'Level Crossing using S4(56)4 perpendicular Scenario'

import os
import sys
import time
from mn_wifi.replaying import ReplayingMobility

from mininet.log import setLogLevel, info
from mn_wifi.cli import CLI
from mn_wifi.net import Mininet_wifi
from mn_wifi.link import wmediumd

from mininet.node import Controller, RemoteController, OVSController
from mininet.node import CPULimitedHost, Host, Node
from mininet.node import OVSKernelSwitch, UserSwitch
from mininet.node import IVSSwitch

from mininet.log import setLogLevel, info
from mininet.link import TCLink, Intf
from subprocess import call

from mn_wifi.link import wmediumd, ITSLink
from mn_wifi.wmediumdConnector import interference
from mininet.net import Mininet



def topology(args):
    "Create a network."
    net = Mininet_wifi(topo=None,
                   build=False,
                   ipBase='192.168.0.0/24')
    
    info( '*** Adding controller\n' )
    c1=net.addController(name='c1',
                      controller=RemoteController,
                      ip='127.0.0.1',
                      protocol='tcp',
                      port=6653)


    kwargs = {'protocols':'OpenFlow13','txpower':'49dBm','range': 150 }#'failMode': 'standalone', 'datapath': 'user'}

    info( '*** Add switches\n')
    s11 = net.addSwitch('s11', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s22 = net.addSwitch('s22', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s33 = net.addSwitch('s33', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s44 = net.addSwitch('s44', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s55 = net.addSwitch('s55', cls=OVSKernelSwitch,protocols="OpenFlow13")


    info("*** Creating nodes\n")
    
    Tra1 = net.addStation('Tra1', mac='00:00:00:00:00:01', ip='192.168.7.101/24', speed=3)
    Tra2 = net.addStation('Tra2', mac='00:00:00:00:00:03', ip='192.168.7.102/24', speed=4)
    Tra3 = net.addStation('Tra3', mac='00:00:00:00:00:05', ip='192.168.7.103/24', speed=4)

    Car1 = net.addStation('Car1', mac='00:00:00:00:00:02', ip='192.168.0.201/24', speed=2)
    Car2 = net.addStation('Car2', mac='00:00:00:00:00:04', ip='192.168.0.202/24', speed=2)
    Car3 = net.addStation('Car3', mac='00:00:00:00:00:06', ip='192.168.0.203/24', speed=4)

    info( '*** Add hosts\n')
    #r1 = net.addHost( 'r1', mac="00:00:00:00:01:00" )
    CarServer = net.addHost('CarServer', cls=Host, ip='192.168.0.204/24', mac='00:00:00:00:00:08')
    RailServer = net.addHost('RailServer', cls=Host, ip='192.168.7.104/24', mac='00:00:00:00:00:07')
    CarEmer = net.addHost('CarEmer', cls=Host, ip='192.168.0.205/24', mac='00:00:00:00:00:44')
    RailEmer = net.addHost('RailEmer', cls=Host, ip='192.168.7.105/24', mac='00:00:00:00:00:55')

    ap1 = net.addAccessPoint('ap1', ssid='ssid-ap1', mode='a', channel='40', position='120,70,0',  **kwargs)
    
    
    ap2 = net.addAccessPoint('ap2', ssid='ssid-ap2',  mode='a', channel='36', position='350,70,0',  **kwargs)
   
    

    info("*** Configuring Propagation Model\n")
    net.setPropagationModel(model="logDistance", exp=5)

    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()

    info("*** Creating links\n")
    
    s11s22 = {'bw':1000}
    net.addLink(s11, s22, cls=TCLink , **s11s22)
    s22s33 = {'bw':1000}
    net.addLink(s22, s33, cls=TCLink , **s22s33)
    s33s44 = {'bw':1000}
    net.addLink(s33, s44, cls=TCLink , **s33s44)
    s44s55 = {'bw':1000}
    net.addLink(s44, s55, cls=TCLink , **s44s55)
    
    s33CarServer = {'bw':1000}
    net.addLink(s33, CarServer, cls=TCLink , **s33CarServer)

    s33RailServer = {'bw':1000}
    net.addLink(s33, RailServer,   cls=TCLink , **s33RailServer)

    
    s22CarEmer = {'bw':1000}
    net.addLink(s22, CarEmer, cls=TCLink , **s22CarEmer)

    s44RailEmer = {'bw':1000}
    net.addLink(s44, RailEmer,   cls=TCLink , **s44RailEmer)

    
    s11ap1 = {'bw':1000}
    net.addLink(s11, ap1, cls=TCLink, **s11ap1)

    s55ap2 = {'bw':1000}
    net.addLink(s55, ap2, cls=TCLink , **s55ap2)

    net.isReplaying = True
    path = os.path.dirname(os.path.abspath(__file__)) + '/replayingMobility/'
    get_trace(Car1, '{}node1.dat'.format(path))
    get_trace(Car2, '{}node2.dat'.format(path))
    get_trace(Car3, '{}node3.dat'.format(path))
    get_trace(Tra1, '{}node4.dat'.format(path))
    get_trace(Tra2, '{}node5.dat'.format(path))
    get_trace(Tra3, '{}node6.dat'.format(path))

    if '-p' not in args:{
         net.plotGraph(max_x=550, min_x=-50, max_y=250, min_y=-100)}
    
    info("*** Starting network\n")
    net.build()

    Tra1 = net.get('Tra1')
    Tra2 = net.get('Tra2')
    Tra3 = net.get('Tra3')
    
    Car1 = net.get('Car1')
    Car2 = net.get('Car2')
    Car3 = net.get('Car3')
    
    Tra1.cmd("route add default gw 192.168.7.1 dev Tra1-wlan0")
    Tra1.cmd("arp -i Tra1-wlan0 -s 192.168.7.1 08:01:11:01:11:01")

    Tra2.cmd("route add default gw 192.168.7.1 dev Tra2-wlan0")
    Tra2.cmd("arp -i Tra2-wlan0 -s 192.168.7.1 08:01:11:01:11:01")

    Tra3.cmd("route add default gw 192.168.7.1 dev Tra3-wlan0")
    Tra3.cmd("arp -i Tra3-wlan0 -s 192.168.7.1 08:01:11:01:11:01")

    Car1.cmd("route add default gw 192.168.0.1 dev Car1-wlan0")
    Car1.cmd("arp -i Car1-wlan0 -s 192.168.0.1 08:01:22:01:22:01")

    Car2.cmd("route add default gw 192.168.0.1 dev Car2-wlan0")
    Car2.cmd("arp -i Car2-wlan0 -s 192.168.0.1 08:01:22:01:22:01")

    Car3.cmd("route add default gw 192.168.0.1 dev Car3-wlan0")
    Car3.cmd("arp -i Car3-wlan0 -s 192.168.0.1 08:01:22:01:22:01")
   
    CarEmer.cmd("ip route add default via 192.168.0.1")
    RailEmer.cmd("ip route add default via 192.168.7.1")
    
 

    info( '*** Starting controllers\n')
    
    for controller in net.controllers:
        controller.start()

    ap1.start([c1])
  
    ap2.start([c1])

    net.get('s11').start([c1])
    net.get('s22').start([c1])
    net.get('s33').start([c1])
    net.get('s44').start([c1])
    net.get('s55').start([c1])

    info("****\n\nMake it self working like iperf and other command\n")

   
    ##PING Command
    #Car3.cmd("xterm -hold -e \"ping 192.168.0.201 \" &")
    #Tra1.cmd("xterm -hold -e \"ping 192.168.7.103 \" &")
    ##Latency Test
    #Car2.cmd("xterm -hold -e \"mtr -r -n -c 10 192.168.0.204 -u \" &")
    #CarServer.cmd("xterm -hold -e \"sudo python Scapy1.py\" &")

    #RailServer.cmd("xterm -hold -e \"iperf3 -s \" &")
    #time.sleep(8)
    #Tra2.cmd("xterm -hold -e \"iperf3 -c 192.168.7.104 \" &")


    info("*** Replaying Mobility\n")
    ReplayingMobility(net)

    info("*** Running CLI\n")
    CLI(net)

    info("*** Stopping network\n")
    net.stop()


def get_trace(sta, file_):
    file_ = open(file_, 'r')
    raw_data = file_.readlines()
    file_.close()

    sta.p = []
    pos = (0, 0, 0)
    sta.position = pos

    for data in raw_data:
        line = data.split()
        x = line[0]  # First Column
        y = line[1]  # Second Column
        pos = float(x), float(y), 0.0
        sta.p.append(pos)


if __name__ == '__main__':
    setLogLevel('info')
    topology(sys.argv)
