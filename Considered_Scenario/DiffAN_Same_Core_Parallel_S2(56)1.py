#!/usr/bin/python

'UPDATED Example for Handover for Difference Access and Same Core_Parallel _S2(5/6)1'

import sys
import time

from mininet.log import setLogLevel, info
from mn_wifi.cli import CLI
from mn_wifi.net import Mininet_wifi


from mininet.node import Controller, RemoteController, OVSController
from mininet.node import CPULimitedHost, Host, Node
from mininet.node import OVSKernelSwitch, UserSwitch
from mininet.node import IVSSwitch

from mininet.log import setLogLevel, info
from mininet.link import TCLink, Intf
from subprocess import call


def topology(args):
    "Create a network."
    net = Mininet_wifi( topo=None,
                   build=False,
                   ipBase='192.168.0.0/24')

    info( '*** Adding controller\n' )
    c1=net.addController(name='c1',
                      controller=RemoteController,
                      ip='127.0.0.1',
                      protocol='tcp',
                      port=6653)

    kwargs = {'protocols':'OpenFlow13','txpower':'49dBm','range': 110 }#'failMode': 'standalone', 'datapath': 'user'}

    info( '*** Add switches\n')
    s11 = net.addSwitch('s11', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s22 = net.addSwitch('s22', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s33 = net.addSwitch('s33', cls=OVSKernelSwitch,protocols="OpenFlow13")

    
    info("*** Creating nodes\n")
    Train1_args, Train3_args, Car1_args = dict(), dict(), dict()
    if '-s' in args:
        Train1_args['position'], Train3_args['position'],  Car1_args['position'] = '100,50,0', '350,10,0' '30,320,0'

    Train1 = net.addStation('Train1', mac='00:00:00:00:00:01', ip='192.168.7.101/24', position='100,50,0',**Train1_args)
    Train2 = net.addStation('Train2', mac='00:00:00:00:00:03', ip='192.168.7.102/24', position='30,30,0')
    Train3 = net.addStation('Train3', mac='00:00:00:00:00:05', ip='192.168.7.103/24', position='380,40,0', **Train3_args)
    Train4 = net.addStation('Train4', mac='00:00:00:00:00:09', ip='192.168.7.105/24', position='380,10,0')

    Car1 = net.addStation('Car1', mac='00:00:00:00:00:02', ip='192.168.0.201/24', position='30,320,0',**Car1_args)
    Car2 = net.addStation('Car2', mac='00:00:00:00:00:04', ip='192.168.0.202/24', position='30,300,0')
    Car3 = net.addStation('Car3', mac='00:00:00:00:00:06', ip='192.168.0.203/24', position='350,320,0')
    Car4 = net.addStation('Car4', mac='00:00:00:00:00:10', ip='192.168.0.205/24', position='350,340,0')
  

    
    info( '*** Add hosts\n')
    CarServer = net.addHost('CarServer', cls=Host, ip='192.168.0.204/24', mac='00:00:00:00:00:08')
    TrainServer = net.addHost('RailServer', cls=Host, ip='192.168.7.104/24', mac='00:00:00:00:00:07')

    ap1 = net.addAccessPoint('ap1', ssid='ssid-ap1', mode='a', channel='36', position='100,300,0', **kwargs)
    ap2 = net.addAccessPoint('ap2', ssid='ssid-ap2', mode='a', channel='40', position='300,300,0', **kwargs)
    
    ap3 = net.addAccessPoint('ap3', ssid='ssid-ap3', mode='a', channel='36', position='100,50,0', **kwargs)
    ap4 = net.addAccessPoint('ap4', ssid='ssid-ap4', mode='a', channel='40', position='300,50,0', **kwargs)
    
    
    net.setPropagationModel(model="logDistance", exp=5)

    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()

    info("*** Creating links\n")

    s11s22 = {'bw':1000}
    net.addLink(s11, s22, cls=TCLink , **s11s22)
    
    s22s33 = {'bw':1000}
    net.addLink(s22, s33, cls=TCLink , **s22s33)
    
        
    CarServers22 = {'bw':1000}
    net.addLink(CarServer, s22, cls=TCLink , **CarServers22)

    TrainServers22 = {'bw':1000}
    net.addLink(TrainServer, s22, cls=TCLink , **TrainServers22)

    s11ap1 = {'bw':1000}
    net.addLink(s11, ap1, cls=TCLink , **s11ap1)

    s11ap3 = {'bw':1000}
    net.addLink(s11, ap3, cls=TCLink , **s11ap3)

    s33ap2 = {'bw':1000}
    net.addLink(s33, ap2, cls=TCLink , **s33ap2)

    s33ap4 = {'bw':1000}
    net.addLink(s33, ap4, cls=TCLink , **s33ap4)

   


    if '-p' not in args:{
        net.plotGraph(max_x=450, min_x=-50, min_y=-50, max_y=450)}

    if '-s' not in args:
        net.startMobility(time=0, AC= 'ssf')
        net.mobility(Train1, 'start', time=60, position='30,10,0')
        #net.mobility(Train1, 'start', time=1500, position='100,50,0')
        net.mobility(Car1, 'start', time=60, position='30,320,0')
        net.mobility(Train1, 'stop', time=65, position='380,60,0')
        net.mobility(Car1, 'stop', time=65, position='350,250,0')
        net.stopMobility(time=67)

    info("*** Starting network\n")
    net.build()
    info( '*** Starting controllers\n')
    for controller in net.controllers:
        controller.start()

    #c1.start()
    ap1.start([c1])
    ap2.start([c1])
    ap3.start([c1])
    ap4.start([c1])
    
    
    net.get('s11').start([c1])
    net.get('s22').start([c1])
    net.get('s33').start([c1])
    
    
    info("****Make it self working like iperf and other command\n")

    
    ##PING Command
    #Car1.cmd("xterm -hold -e \"ping 192.168.0.203 \" &")
    #Train1.cmd("xterm -hold -e \"ping 192.168.7.102 \" &")
    
    #Train1.cmd("xterm &")
    #xterm -hold -e "python3 "
    #CarServer.cmd("xterm -hold -e \"sudo python Scapy1.py\" &")
    #TrainServer.cmd("xterm -hold -e \"iperf3 -s \" &")
    #time.sleep(5)
    #Train2.cmd("xterm -hold -e \"iperf3 -c 192.168.7.104 \" &")

    ##Latency Test
    #time.sleep(3)
    #Car2.cmd("xterm -hold -e \"mtr -r -n -c 10 192.168.0.204 -u \" &")

    info("*** Running CLI\n")
    CLI(net)

    info("*** Stopping network\n")
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    topology(sys.argv)








