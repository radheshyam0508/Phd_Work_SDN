import sys


from mininet.log import setLogLevel, info, error
from mn_wifi.cli import CLI
from mn_wifi.net import Mininet_wifi
from mn_wifi.link import wmediumd
from mn_wifi.wmediumdConnector import interference
from mininet.node import Controller, RemoteController, OVSController
from mininet.node import Host
from mininet.node import OVSKernelSwitch
from mn_wifi.sumo.runner import sumo

from mininet.link import TCLink
from subprocess import call
from mn_wifi.link import wmediumd, ITSLink


import re
import logging
from sys import exit


import testbed_utils as tu
import time

from mininet.term import makeTerm
from mn_wifi.node import OVSKernelAP
from mininet.util import quietRun


def topology(args):
    
    trains_file = "Barcelona/osm.tram.trips.xml"
    cars_file = "Barcelona/osm.passenger.trips.xml"

    info("Loading trains and cars informations")
    trains_ids = tu.getIdFromXml(trains_file,"trip")
    cars_ids = tu.getIdFromXml(cars_file, "trip")

    "Create a network."

    net = Mininet_wifi(link=wmediumd, wmediumd_mode=interference)

    kwargs = {'ssid':'wifi','mode':'n','protocols':'OpenFlow13', 
        'txpower':'50dBm','range': 200, 'failMode': 'standalone', 'datapath': 'user'}



    info( '*** Add switches\n')
    s3 = net.addSwitch('s3', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s2 = net.addSwitch('s2', cls=OVSKernelSwitch,protocols="OpenFlow13")
    s4 = net.addSwitch('s4', cls=OVSKernelSwitch,protocols="OpenFlow13")


    for id in range(25):
        cars_id = 'C%s'%cars_ids[id]
        ip_address = '192.168.0.{}/24'.format(id+201)
        net.addCar(cars_id, ip=ip_address, defaultRoute='via 192.168.0.1/24')
    
    for id in range(43):
        trains_id = 'T%s'%trains_ids[id]
        p_address = '192.168.7.{}/24'.format(id+101)
        net.addCar(trains_id, ip=p_address, defaultRoute='via 192.168.7.1/24')

    for car in net.cars:
        print(car.name)
    
    


    info( '*** Add hosts\n')
    CarServer = net.addHost('CarServer', cls=Host, ip='192.168.0.242/24', mac='00:00:00:00:01:06')
    RailServer = net.addHost('RailServer', cls=Host, ip='192.168.7.140/24', mac='00:00:00:00:01:07')
    h2 = net.addHost("CarHost", ip="192.168.0.250/24")
    h3 = net.addHost("RailHost", ip="192.168.7.240/24")
    #CarHost = net.addStation('CarHost', ip='192.168.0.250/24', mac='00:00:00:00:01:50',position='570,568,0')
    #RailHost = net.addStation('RailHost', ip='192.168.7.240/24', mac='00:00:00:00:01:40',position='570,568,0')

    # create access points
    info("Access points creation")

    ap_1 = net.addAccessPoint('ap_1', channel=1, position='90,1950,5',
                              mac="00:00:00:11:00:01",**kwargs)
    ap_2 = net.addAccessPoint('ap_2', channel=6, position='137,1824,5',
                              mac="00:00:00:11:00:02", **kwargs)
    ap_3 = net.addAccessPoint('ap_3', channel=11, position='135,1686,5',
                              mac="00:00:00:11:00:03", **kwargs)
    ap_4 = net.addAccessPoint('ap_4',channel=1, position='135,1594,5',
                              mac="00:00:00:11:00:04", **kwargs)
    ap_5 = net.addAccessPoint('ap_5',channel=6, position='140,1480,5',
                              mac="00:00:00:11:00:05", **kwargs)
    ap_6 = net.addAccessPoint('ap_6',channel=9, position='142,1334,5',
                              mac="00:00:00:11:00:06", **kwargs)
    ap_7 = net.addAccessPoint('ap_7',channel=13, position='142,1188,5',
                              mac="00:00:00:11:00:07", **kwargs)
    ap_8 = net.addAccessPoint('ap_8',channel=6, position='147,1000,5',
                              mac="00:00:00:11:00:08", **kwargs)
    ap_9 = net.addAccessPoint('ap_9',channel=1, position='190,876,5',
                              mac="00:00:00:11:00:09", **kwargs)
    ap_10 = net.addAccessPoint('ap_10',channel=6, position='250,750,5',
                              mac="00:00:00:11:00:10", **kwargs)
    ap_11 = net.addAccessPoint('ap_11',channel=9, position='348,663,5',
                              mac="00:00:00:11:00:11", **kwargs)
    ap_12 = net.addAccessPoint('ap_12',channel=1, position='410,570,5',
                              mac="00:00:00:11:00:12", **kwargs)
    ap_13 = net.addAccessPoint('ap_13',channel=6, position='499,469,5',
                              mac="00:00:00:11:00:13", **kwargs)
    ap_14 = net.addAccessPoint('ap_14',channel=1, position='605,320,5',
                              mac="00:00:00:11:00:14", **kwargs)
    ap_15 = net.addAccessPoint('ap_15',channel=9, position='635,244,5',
                              mac="00:00:00:11:00:15", **kwargs)
    ap_16 = net.addAccessPoint('ap_16',channel=6, position='639,133,5',
                              mac="00:00:00:11:00:16", **kwargs)
    ap_17 = net.addAccessPoint('ap_17',channel=11, position='618,40,5',
                              mac="00:00:00:11:00:17", **kwargs)
 

    net.setPropagationModel(model="logDistance", exp=3.8)
    info( '*** Adding controller\n' )
    c1=net.addController(name='c1',
                      controller=RemoteController,
                      ip='127.0.0.1',
                      protocol='tcp',
                      port=6653)

    info("*** Configuring wifi nodes\n")
    net.configureWifiNodes()


    info("*** Creating links\n")

    s3CarServer = {'bw':1000}
    net.addLink(s3, CarServer, cls=TCLink , **s3CarServer)

    s3RailServer = {'bw':1000}
    net.addLink(s3, RailServer, cls=TCLink , **s3RailServer)

    #net.addLink(s4, s3)
    net.addLink(s2, s3)
    net.addLink(s3, s4)


    s2ap_1 = {'bw':1000}
    net.addLink(s2, ap_1, cls=TCLink , **s2ap_1)


    net.addLink(ap_1, ap_2)

    net.addLink(ap_2, ap_3)
    net.addLink(ap_3, ap_4)
    net.addLink(ap_4, ap_5)
    net.addLink(ap_5, ap_6)
    net.addLink(ap_6, ap_7)
    net.addLink(ap_7, ap_8)
    net.addLink(ap_8, ap_9)
    net.addLink(ap_9, ap_10)
    net.addLink(ap_10, ap_11)
    net.addLink(ap_11, ap_12)
    net.addLink(ap_12, ap_13)
    net.addLink(ap_13, ap_14)
    net.addLink(ap_14, ap_15)
    net.addLink(ap_15, ap_16)
    net.addLink(ap_16, ap_17)
    ap_17h2 = {'bw':1000}
    net.addLink(ap_17, h2, cls=TCLink , **ap_17h2)
    ap_17h3 = {'bw':1000}
    net.addLink(ap_17, h3, cls=TCLink , **ap_17h3)

    
    info("**** Starting network and connecting to traci")
    info('Connecting to traci - sumo')
    
    net.useExternalProgram(program=sumo, port=8813, config_file='osm.sumocfg',
                            extra_params=["--start --delay 1500"])
    
    
        #for car in net.cars:
        #net.addLink(car, intf=car.wintfs[0].name,
                    #cls=ITSLink, band=20, channel=181)


    info("*** Starting network\n")
    net.build()


    info( '*** Starting controllers\n')



    for controller in net.controllers:
        controller.start()

    #c1.start()
    #c0.start()
    for ap in net.aps:
       ap.start([c1])
    
    

    nodes = net.cars + net.aps

    net.telemetry(nodes=nodes, data_type='position',
                min_x=-450, min_y=-500,
                max_x=850, max_y=2100)
    
    for c in net.cars:
        print("c.name :"+c.name)
        start_cmd=f"bash -c'./wifi_reconnect_timer.sh/{c.name}&"
        makeTerm(c, cmd=start_cmd)


    #info("*** Running PingAll Command\n")
    #net.pingAll()
    #Tra1.cmd("ping 192.168.7.103")


    #default_route_cmd = 'ip route add default via 11.0.0.1'
    #makeTerm(Car1, cmd=f"bash -c '{default_route_cmd}'")
    #start_cmd = f"iperf3 -c "+ "'python3 host_edge_service_start_edit.py " + iperf_server +"'"
    #makeTerm(Car1, cmd=start_cmd)
    #time.sleep(10)

    #Make it self working like iperf and other command
    #RailServer.cmd("xterm -hold -e \"iperf3 -s \" &")
    #CarServer.cmd("xterm -hold -e \"iperf3 -s \" &")
    #time.sleep(10)
    #Tra1.cmd("xterm -hold -e \"iperf3 -c 192.168.7.242 \" &")
    #Car45.cmd("xterm -hold -e \"iperf3 -c 192.168.0.242 \" &")
    #Car2.cmd("xterm &")
    #xterm -hold -e "python3 "
    net.start()

    info("*** Running CLI\n")
    CLI(net)


    info("*** Stopping network\n")
    net.stop()



if __name__ == '__main__':
    setLogLevel('info')
    topology(sys.argv)



