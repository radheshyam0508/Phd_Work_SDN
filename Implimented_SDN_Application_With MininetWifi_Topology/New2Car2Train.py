#!/usr/bin/python

'Example for Handover'

import sys

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
    #net = Mininet_wifi()

    net = Mininet_wifi( topo=None,
                   build=False,
                   ipBase='10.0.0.0/8')

    info( '*** Adding controller\n' )
    c1=net.addController(name='c1',
                      controller=RemoteController,
                      ip='127.0.0.1',
                      protocol='tcp',
                      port=6653)

    info("*** Creating nodes\n")
    Tra1_args, Car2_args = dict(), dict()
    if '-s' in args:
        Tra1_args['position'],  Car2_args['position'] = '10,10,0', '100,90,0'

    Tra1 = net.addStation('Tra1', mac='00:00:00:00:00:01', ip='10.0.0.1/8', position='10,10,0', **Tra1_args)
    Tra2 = net.addStation('Tra2', mac='00:00:00:00:00:03', ip='10.0.0.3/8', position='10,10,0')
    Car1 = net.addStation('Car1', mac='00:00:00:00:00:02', ip='10.0.0.2/8', position='10,80,0')
    

    
    
    Car2 = net.addStation('Car2', mac='00:00:00:00:00:04', ip='10.0.0.4/8', position='100,90,0',**Car2_args)
    Tra3 = net.addStation('Tra3', mac='00:00:00:00:00:05', ip='10.0.0.5/8', position='100,20,0')
  

    
    info( '*** Add hosts\n')
    h6 = net.addHost('h6', cls=Host, ip='10.0.0.6', mac='00:00:00:00:00:06', defaultRoute=None)
    h7 = net.addHost('h7', cls=Host, ip='10.0.0.7', mac='00:00:00:00:00:07', defaultRoute=None)

    ap1 = net.addAccessPoint('ap1', ssid='ssid-ap1', channel='1', position='10,10,0',protocols="OpenFlow13")
    ap2 = net.addAccessPoint('ap2', ssid='ssid-ap2', channel='1', position='10,80,0',protocols="OpenFlow13")
    
    ap3 = net.addAccessPoint('ap3', ssid='ssid-ap3', channel='6', position='100,20,0',protocols="OpenFlow13")
    ap4 = net.addAccessPoint('ap4', ssid='ssid-ap4', channel='6', position='100,90,0',protocols="OpenFlow13")
    
    
    

    info( '*** Add switches\n')
    s4 = net.addSwitch('s4', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s2 = net.addSwitch('s2', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s3 = net.addSwitch('s3', cls=OVSKernelSwitch,protocols="OpenFlow13")
    #s4 = net.addSwitch('s4', cls=OVSKernelSwitch,protocols="OpenFlow13")


    net.setPropagationModel(model="logDistance", exp=5)

    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()

    info("*** Creating links\n")

    net.addLink(s4, s3)
    net.addLink(s2, s3)
    #net.addLink(s3, s4)
    
    h6s3 = {'bw':1000}
    net.addLink(h6, s3, cls=TCLink , **h6s3)

    h7s3 = {'bw':1000}
    net.addLink(h7, s3, cls=TCLink , **h7s3)

    s4ap1 = {'bw':1000}
    net.addLink(s4, ap1, cls=TCLink , **s4ap1)

    s4ap2 = {'bw':1000}
    net.addLink(s4, ap2, cls=TCLink , **s4ap2)

    s2ap3 = {'bw':1000}
    net.addLink(s2, ap3, cls=TCLink , **s2ap3)

    s2ap4 = {'bw':1000}
    net.addLink(s2, ap4, cls=TCLink , **s2ap4)

   


    if '-p' not in args:{
        net.plotGraph(max_x=100, max_y=100)}

    if '-s' not in args:
        net.startMobility(time=0)
        net.mobility(Tra1, 'start', time=160, position='10,10,0')
        #net.mobility(sta4, 'start', time=90, position='100,100,0')
        net.mobility(Tra1, 'stop', time=162, position='100,20,0')
        #net.mobility(sta4, 'stop', time=130, position='10,100,0')
        net.stopMobility(time=163)

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
    
    
    net.get('s4').start([c1])
    net.get('s2').start([c1])
    net.get('s3').start([c1])
    #net.get('s4').start([c1])


    info("*** Running CLI\n")
    CLI(net)

    info("*** Stopping network\n")
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    topology(sys.argv)








