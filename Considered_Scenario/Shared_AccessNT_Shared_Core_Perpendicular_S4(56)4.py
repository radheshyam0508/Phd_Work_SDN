#!/usr/bin/python

'Example for Handover_ Shared Access Network Shared Core_Perpendicular_S4(5/6)4'

import sys
import time

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
    #net = Mininet_wifi()
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

    info("*** Creating nodes\n")
    Train1_args, Car1_args, Car3_args= dict(), dict(), dict()
    if '-s' in args:
        Train1_args['position'],  Car1_args['position'], Car3_args['position']= '130,180,0', '20,50,0', '120,30,0'

    Train1 = net.addStation('Train1', mac='00:00:00:00:00:01', ip='192.168.7.101/24', position='130,180,0', **Train1_args)
    Train2 = net.addStation('Train2', mac='00:00:00:00:00:03', ip='192.168.7.102/24', position='180,-35,0')
    Train3 = net.addStation('Train3', mac='00:00:00:00:00:05', ip='192.168.7.103/24', position='270,-20,0')

    Car1 = net.addStation('Car1', mac='00:00:00:00:00:02', ip='192.168.0.201/24', position='10,70,0',**Car1_args)
    Car2 = net.addStation('Car2', mac='00:00:00:00:00:04', ip='192.168.0.202/24', position='10,50,0')
    Car3 = net.addStation('Car3', mac='00:00:00:00:00:06', ip='192.168.0.203/24', position='450,80,0', **Car3_args)

    info( '*** Add hosts\n')
    CarServer = net.addHost('CarServer', cls=Host, ip='192.168.0.204/24', mac='00:00:00:00:00:08')
    TrainServer = net.addHost('RailServer', cls=Host, ip='192.168.7.104/24', mac='00:00:00:00:00:07')

    ap1 = net.addAccessPoint('ap1', ssid='ssid-ap1', mode='a', channel='40', position='120,70,0',  **kwargs)
    #ap2 = net.addAccessPoint('ap2', ssid='ssid-ap2', mode='a', channel='40', position='50,10,0',protocols="OpenFlow13")
    
    ap2 = net.addAccessPoint('ap2', ssid='ssid-ap2',  mode='a', channel='36', position='350,70,0',  **kwargs)
    #ap4 = net.addAccessPoint('ap4', ssid='ssid-ap4', mode='a', channel='40', position='100,90,0',protocols="OpenFlow13")
    
    

    net.setPropagationModel(model="logDistance", exp=5)

    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()

    info("*** Creating links\n")
    
    s11s22 = {'bw':1000}
    net.addLink(s11, s22, cls=TCLink , **s11s22)
    s22s33 = {'bw':1000}
    net.addLink(s22, s33, cls=TCLink , **s22s33)
    
    s22CarServer = {'bw':1000}
    net.addLink(s22, CarServer, cls=TCLink , **s22CarServer)

    s22TrainServer = {'bw':1000}
    net.addLink(s22, TrainServer,   cls=TCLink , **s22TrainServer)

    
    s33ap2 = {'bw':1000}
    net.addLink(s33, ap2, cls=TCLink, **s33ap2)

    s11ap1 = {'bw':1000}
    net.addLink(s11, ap1, cls=TCLink , **s11ap1)


    if '-p' not in args:{
        net.plotGraph(max_x=550, min_x=-90, max_y=250, min_y=-90)}
    
   # Strongest Signal First
    #net.associationControl('ssf')

    if '-s' not in args:
        net.startMobility(time=0)
        net.mobility(Train1, 'start', time=60, position='130,180,0')
        net.mobility(Car1, 'start', time=62, position='10,70,0')
        net.mobility(Train1, 'stop', time=67, position='130,-35,0')
        net.mobility(Car1, 'stop', time=69, position='450,60,0')
        net.stopMobility(time=70)

    info("*** Starting network\n")
    net.build()

    Train1 = net.get('Train1')
    Train2 = net.get('Train2')
    Train3 = net.get('Train3')
    
    Car1 = net.get('Car1')
    Car2 = net.get('Car2')
    Car3 = net.get('Car3')
    
    Train1.cmd("route add default gw 192.168.7.1 dev Train1-wlan0")
    Train1.cmd("arp -i Train1-wlan0 -s 192.168.7.1 08:01:11:01:11:01")

    Train2.cmd("route add default gw 192.168.7.1 dev Train2-wlan0")
    Train2.cmd("arp -i Train2-wlan0 -s 192.168.7.1 08:01:11:01:11:01")

    Train3.cmd("route add default gw 192.168.7.1 dev Train3-wlan0")
    Train3.cmd("arp -i Train3-wlan0 -s 192.168.7.1 08:01:11:01:11:01")

    Car1.cmd("route add default gw 192.168.0.1 dev Car1-wlan0")
    Car1.cmd("arp -i Car1-wlan0 -s 192.168.0.1 08:01:22:01:22:01")

    Car2.cmd("route add default gw 192.168.0.1 dev Car2-wlan0")
    Car2.cmd("arp -i Car2-wlan0 -s 192.168.0.1 08:01:22:01:22:01")

    Car3.cmd("route add default gw 192.168.0.1 dev Car3-wlan0")
    Car3.cmd("arp -i Car3-wlan0 -s 192.168.0.1 08:01:22:01:22:01")

    info( '*** Starting controllers\n')
    #Train1.cmd("ping 192.168.7.103")


    for controller in net.controllers:
        controller.start()

    #c1.start()
    ap1.start([c1])
    #ap2.start([c1])
    ap2.start([c1])
    #ap4.start([c1])
    
    
    #net.get('s4').start([c1])
    net.get('s11').start([c1])
    net.get('s22').start([c1])
    net.get('s33').start([c1])

  
    info("****Make it self working like iperf and other command\n")

   
    ##PING Command
    Car3.cmd("xterm -hold -e \"ping 192.168.0.201 \" &")
    Train1.cmd("xterm -hold -e \"ping 192.168.7.103 \" &")
    ##Latency Test
    Car2.cmd("xterm -hold -e \"mtr -r -n -c 10 192.168.0.204 -u \" &")
    CarServer.cmd("xterm -hold -e \"sudo python Scapy1.py\" &")

    TrainServer.cmd("xterm -hold -e \"iperf3 -s \" &")
    time.sleep(5)
    Train2.cmd("xterm -hold -e \"iperf3 -c 192.168.7.104 \" &")

    info("*** Running CLI\n")
    CLI(net)
    

    info("*** Stopping network\n")
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    topology(sys.argv)








