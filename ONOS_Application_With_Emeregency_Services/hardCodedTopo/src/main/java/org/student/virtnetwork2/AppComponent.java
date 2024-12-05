/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.student.virtnetwork2;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.*;

import org.onlab.util.PredictableExecutor;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.*;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;

import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample reactive forwarding application.
 */
@Component(
        immediate = true//,
        //service = AppComponent.class,
        //property = {}
)
public class AppComponent {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private ApplicationId appId;


    private final TopologyListener topologyListener = new InternalTopologyListener();

    ArrayList<IpAddress> network1 = new ArrayList<>();

    ArrayList<IpAddress> network2 = new ArrayList<>();
    ArrayList<IpAddress> network3 = new ArrayList<>();

    private ExecutorService blackHoleExecutor;

    static final int FLOW_TIMEOUT_DEFAULT = 10;

    static final int FLOW_PRIORITY_DEFAULT = 5000;

    private int flowTimeout = FLOW_TIMEOUT_DEFAULT;

    private int flowPriority = FLOW_PRIORITY_DEFAULT;
    /*@Activate
    protected void activate() {
        appId = coreService.registerApplication("org.student.virtnetwork");
        TrafficSelector packetSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).build();
        hostEventExecutors = new PredictableExecutor(DEFAULT_BUCKETS, groupedThreads("onos/mn-wifi",
                "event-host-%d", log));

        hostService.addListener(hostListener);
        packetService.requestPackets(packetSelector, PacketPriority.REACTIVE, appId);
        packetService.addProcessor(learnProcessor, PacketProcessor.director(1));
        packetService.addProcessor(forwardProcessor, PacketProcessor.director(1));
        initializeTables();
        startTime = System.currentTimeMillis();
        log.info("VirtNetwork Started");


    }*/

    @Activate
    public void activate(/*ComponentContext ctxt*/) {

        /*blackHoleExecutor = newSingleThreadExecutor(groupedThreads("onos/radhe/fwd",
                "black-hole-fixer",
                log));*/
        //hostService.addListener(hostListener);

        //cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.radhe.fwd");

        initializeTables();
        packetService.addProcessor(processor, PacketProcessor.director(1));
        topologyService.addListener(topologyListener);
        requestIntercepts();

        log.info("Started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        withdrawIntercepts();
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        topologyService.removeListener(topologyListener);
        blackHoleExecutor.shutdown();
        blackHoleExecutor = null;
        processor = null;
        //hostService.removeListener(hostListener);
        log.info("Stopped");
    }

    private void initializeTables() {
        // network1
        for(int i=101;i<=140;i++)
            network1.add(IpAddress.valueOf("192.168.7."+i));
        network1.add(IpAddress.valueOf("192.168.7.240"));

// network2

        for(int i=201;i<=240;i++)
            network2.add(IpAddress.valueOf("192.168.0."+i));
        network2.add(IpAddress.valueOf("192.168.0.250"));



        //network3
        network3.add(IpAddress.valueOf("192.168.0.205"));
        //network3.add(IpAddress.valueOf("192.168.0.260"));
        network3.add(IpAddress.valueOf("192.168.7.105"));

        //network3.add(IpAddress.valueOf("192.168.0.7"));
        //network3.add(IpAddress.valueOf("192.168.7.1"));


    }

    @Modified
    public void modified(ComponentContext context) {
        requestIntercepts();
    }


    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_IPV6);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    private VlanId getVlanForNetwork(IpAddress ip,IpAddress ip2,int payLoadPortNumber){
        if((network3.contains(ip) && payLoadPortNumber == 135) ||(network3.contains(ip2) && payLoadPortNumber == 135))
        //if((ip.toString().equals(IpAddress.valueOf("192.168.0.204").toString()) && payLoadPortNumber == 135)|| (ip.toString().equals(IpAddress.valueOf("192.168.7.105").toString())) && payLoadPortNumber ==135)
            return VlanId.vlanId((short) 5);
        else if (network1.contains(ip)) {
            return VlanId.vlanId((short) 3);
        } else if (network2.contains(ip)) {
            return VlanId.vlanId((short) 4);
        }
        else {
            return VlanId.NONE;
        }
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        private int ipsInSameNetwork(IpAddress srcIp, IpAddress dstIp) {
            if (network1.contains(srcIp) && network1.contains(dstIp)) {
                return 3;
            } else if (network2.contains(srcIp) && network2.contains(dstIp)) {
                return 4;
            } else if (network3.contains(srcIp) && network3.contains(dstIp)) {
                return 5;
            } else {
                return -1;
            }
        }

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.

            /*if (context.isHandled()) {
                return;
            }*/

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            if (ethPkt == null) {
                return;
            }

            if(ethPkt.getEtherType() == Ethernet.TYPE_IPV6){
                return;
            }

            if(ethPkt.getEtherType() != Ethernet.TYPE_IPV4) return;

            MacAddress macAddress = ethPkt.getSourceMAC();

            if (isControlPacket(ethPkt)) {
                return;
            }

            IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
            //log.info("Payload:"+ethPkt.getPayload());

            MacAddress srcMac = ethPkt.getSourceMAC();
            MacAddress dstMac = ethPkt.getDestinationMAC();
            IpAddress srcIp = IpAddress.valueOf(ipv4Packet.getSourceAddress());
            IpAddress dstIp = IpAddress.valueOf(ipv4Packet.getDestinationAddress());
            short pktVlan = ethPkt.getVlanID();


            //log.info("pktVlan:"+pktVlan);
            //if(pktVlan==-1) pktVlan = (short) ipsInSameNetwork(srcIp,dstIp);

            HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());
            //HostId id = HostId.hostId(ethPkt.getDestinationMAC(), VlanId.vlanId(pktVlan));
            HostId srcId = HostId.hostId(ethPkt.getSourceMAC());

            if (dstId.mac().isLldp()) {
                return;
            }

            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                if (dstId.mac().isMulticast()) {
                    return;
                }
            }
/*
            byte ipv4Protocol = ipv4Packet.getProtocol();
            int payLoadPortNumb = -1,payLoadDestPortNumb = -1;
            if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
                UDP udpPacket = (UDP) ipv4Packet.getPayload();
                payLoadPortNumb = udpPacket.getSourcePort();
                payLoadDestPortNumb = udpPacket.getDestinationPort();
            }*/


            int outputVlan = ipsInSameNetwork(srcIp,dstIp);

            if(outputVlan == -1){
                log.info(" ips not in same n/w or drop the packet:"+", srcip:"+srcIp+", descip:"+dstIp);
                return;
            }
            else log.info("ips is in same n/w : "+pkt.receivedFrom().deviceId()+", source:"+srcIp+ ",Destination:"+ dstIp, "VLAN ID:"+pktVlan);

            /*if(pktVlan==-1){
                // tag and forward the first packet manually here
                log.info("whennnnn pktVlan -1");
                createPacket(context,outputVlan,ethPkt,ipv4Packet,pkt,PortNumber.FLOOD);
                return;
            }*/

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(dstId);
            //dst.mac()
            Host src = hostService.getHost(srcId);
            if (dst == null) {
                log.info("1st flood call");
                flood(context);
                return;
            }

            // Are we on an edge switch that our destination is on? If so,
            // simply forward out to the destination and bail.
            if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                if (!context.inPacket().receivedFrom().port().equals(dst.location().port())) {
                    log.info("final last switchchchch");
                    installRule(context, dst.location().port());
                                   }
                return;
            }

            // Otherwise, get a set of paths that lead from here to the
            // destination edge switch.
            Set<Path> paths =
                    topologyService.getPaths(topologyService.currentTopology(),
                            pkt.receivedFrom().deviceId(),
                            dst.location().deviceId());
            //paths.iterator().next().links().size()
            if (paths.isEmpty()) {
                // If there are no paths, flood and bail.
                log.info("flood call");
                flood(context);
                return;
            }

            // Otherwise, pick a path that does not lead back to where we
            // came from; if no such path, flood and bail.
            Path path = pickForwardPathIfPossible(paths, pkt.receivedFrom().port());
            if (path == null) {
                log.warn("Don't know where to go from here {} for {} -> {}",
                        pkt.receivedFrom(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC());
                flood(context);
                return;
            }

            // Are we on an edge switch that our destination is on? If so,
            // simply forward out to the destination and bail.
            installRule(context, path.src().port());

        }

    }

    //create ethernet packet for first and last switch/access point
    private void createPacket(PacketContext context,int outputVlan,Ethernet ethPkt,IPv4 ipv4Packet,InboundPacket pkt,PortNumber portNumber){
        installRuleNew(context,portNumber);
        Ethernet newEthPkt = new Ethernet();
        newEthPkt.setDestinationMACAddress(ethPkt.getDestinationMAC());
        newEthPkt.setSourceMACAddress(ethPkt.getSourceMAC());
        newEthPkt.setEtherType(Ethernet.TYPE_IPV4);

        if(outputVlan != -1) newEthPkt.setVlanID((short) outputVlan);

        newEthPkt.setPayload(ipv4Packet);
        ByteBuffer newPktBuffer = ByteBuffer.wrap(newEthPkt.serialize());

        //log.info("New tagged packet: {}", newEthPkt.toString());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(portNumber).build();
        OutboundPacket packetOut = new DefaultOutboundPacket(pkt.receivedFrom().deviceId(), treatment, newPktBuffer);
        packetService.emit(packetOut);
    }

    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    // Selects a path from the given set that does not lead back to the
    // specified port if possible.
    private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
        for (Path path : paths) {
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return null;
    }

    // Floods the specified packet if permissible.
    private void flood(PacketContext context) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }


    // Install a rule forwarding the packet to the specified port.
    private void installRule(PacketContext context, PortNumber portNumber) {
        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        DeviceId currentDeviceId = context.inPacket().receivedFrom().deviceId();
        IPv4 tempIpv4Packet = (IPv4) inPkt.getPayload();

        IpAddress srcIPAddress = IpAddress.valueOf(tempIpv4Packet.getSourceAddress());
        IpAddress dstIPAddress = IpAddress.valueOf(tempIpv4Packet.getDestinationAddress());

        Set<Host> possibleSourceHosts = hostService.getHostsByIp(srcIPAddress);
        Set<Host> possibleDestinationHosts = hostService.getHostsByIp(dstIPAddress);

        selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                .matchEthSrc(inPkt.getSourceMAC())
                .matchEthDst(inPkt.getDestinationMAC());

        if(possibleSourceHosts.size() == 0){
            log.error("No hosts found with source IP address: '%s'", srcIPAddress.toString());
            return;
        }

        if(possibleSourceHosts.size() == 0){
            log.error("More than one host found in core database with source IP address: '%s'", srcIPAddress.toString());
            return;
        }

        if(possibleDestinationHosts.size() == 0){
            log.error("No hosts found with destination IP address: '%s'", dstIPAddress.toString());
            return;
        }

        if(possibleDestinationHosts.size() == 0){
            log.error("More than one host found in core database with destinationIP address: '%s'", dstIPAddress.toString());
            return;
        }

        Host sourceHost = possibleSourceHosts.iterator().next();
        Host destinationHost = possibleDestinationHosts.iterator().next();

        IPv4 ipv4Packet1 = (IPv4) inPkt.getPayload();
        byte ipv4Protocol1 = ipv4Packet1.getProtocol();
        int payLoadPortNumb = -1,payLoadDestPortNumb = -1;
        if (ipv4Protocol1 == IPv4.PROTOCOL_UDP) {
            UDP udpPacket = (UDP) ipv4Packet1.getPayload();
            payLoadPortNumb = udpPacket.getSourcePort();
            payLoadDestPortNumb = udpPacket.getDestinationPort();


            //IPacket iPacket = packetContext.inPacket().parsed().getPayload();
            if (ipv4Packet1 instanceof IPv4) {
                    IPacket payload = ipv4Packet1.getPayload();
                    Ethernet newEthPkt = new Ethernet();
                    if (payload instanceof UDP && ((UDP) payload).getDestinationPort()== 135)
                         {
                        if(dstIPAddress.toString().equals("192.168.0.205")){
                            // set src and destination IP address
                            ipv4Packet1.setSourceAddress("192.168.7.104");
                            ipv4Packet1.setDestinationAddress("192.168.7.105");

                            // set src and destination mac address
                            newEthPkt.setDestinationMACAddress(MacAddress.valueOf("00:00:00:00:00:55"));
                            newEthPkt.setSourceMACAddress(MacAddress.valueOf("00:00:00:00:00:07"));
                        }
                        else if(dstIPAddress.toString().equals("192.168.7.105")){
                            // set src and destination IP address
                            ipv4Packet1.setSourceAddress("192.168.0.204");
                            ipv4Packet1.setDestinationAddress("192.168.0.205");

                            // set src and destination mac address
                            newEthPkt.setDestinationMACAddress(MacAddress.valueOf("00:00:00:00:00:44"));
                            newEthPkt.setSourceMACAddress(MacAddress.valueOf("00:00:00:00:00:08"));
                        }

                    newEthPkt.setEtherType(Ethernet.TYPE_IPV4);
                    //newEthPkt.setVlanID((short) 5);
                    newEthPkt.setPayload(ipv4Packet1.getPayload());
                    ByteBuffer newPktBuffer = ByteBuffer.wrap(newEthPkt.serialize());
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.portNumber(2)).build();
                    OutboundPacket packetOut = new DefaultOutboundPacket(currentDeviceId, treatment, newPktBuffer);
                    packetService.emit(packetOut);
                    }
                }
        }

        if(!hostService.getConnectedHosts(currentDeviceId).contains(sourceHost)){
            VlanId vId = getVlanForNetwork(srcIPAddress,dstIPAddress,payLoadPortNumb);
            log.info(".....Matching VLAN... ");
            selectorBuilder.matchVlanId(vId);
        }

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            return;
        }

        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
        byte ipv4Protocol = ipv4Packet.getProtocol();
        Ip4Prefix matchIp4SrcPrefix =
                Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                        Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix matchIp4DstPrefix =
                Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                        Ip4Prefix.MAX_MASK_LENGTH);
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(matchIp4SrcPrefix)
                .matchIPDst(matchIp4DstPrefix);

        /*
        if (ipv4Protocol == IPv4.PROTOCOL_TCP) {
            TCP tcpPacket = (TCP) ipv4Packet.getPayload();
            selectorBuilder.matchIPProtocol(ipv4Protocol)
                    .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                    .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
        }
        if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            selectorBuilder.matchIPProtocol(ipv4Protocol)
                    .matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                    .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
        }
        if (ipv4Protocol == IPv4.PROTOCOL_ICMP) {
            ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();
            selectorBuilder.matchIPProtocol(ipv4Protocol);
        }*/




        boolean hasVlan = (inPkt.getVlanID() == Ethernet.VLAN_UNTAGGED);

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        if(hostService.getConnectedHosts(currentDeviceId).contains(sourceHost)){
            VlanId vId = getVlanForNetwork(srcIPAddress,dstIPAddress,payLoadPortNumb);

            if (vId.equals(VlanId.NONE)){
                //Means no Vlan returned to what to do?
                log.error("No VLAN Id for source IP: '%s', aborting treatment", srcIPAddress.toString());
                return;
            }

            log.info("Pushing VLAN...");

            treatmentBuilder.pushVlan().setVlanId(vId);
        }else if(hostService.getConnectedHosts(currentDeviceId).contains(destinationHost)){
            VlanId vId = getVlanForNetwork(dstIPAddress,srcIPAddress,payLoadPortNumb);

            if (vId.equals(VlanId.NONE)){
                //Means no Vlan returned to what to do?
                log.error("No VLAN Id for destination IP: '%s', aborting treatment", srcIPAddress.toString());
                return;
            }

            log.info("Poping VLAN...");
            treatmentBuilder.popVlan();
        }
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //assign the hard coded rule here
        //if(srcIPAddress is eual to 192.168.0.205 and destination ip is 192.168.7.105 or viceversa){
        //treatmentBuilder.setEthSrc(MacAddress.valueOf("destination host"))
        //}

        treatmentBuilder.setOutput(portNumber);

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                forwardingObjective);

        packetOut(context, portNumber);
    }
    // Install a rule forwarding the packet to the specified port.
    private void installRuleNew(PacketContext context, PortNumber portNumber) {
        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            return;
        }

        //
        // If matchDstMacOnly
        //    Create flows matching dstMac only
        // Else
        //    Create flows with default matching and include configured fields
        //

        selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                .matchEthSrc(inPkt.getSourceMAC())
                .matchEthDst(inPkt.getDestinationMAC());

        //VLAN TAG
        //selectorBuilder.matchVlanId(VlanId.vlanId(inPkt.getVlanID()));


        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
        byte ipv4Protocol = ipv4Packet.getProtocol();
        Ip4Prefix matchIp4SrcPrefix =
                Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                        Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix matchIp4DstPrefix =
                Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                        Ip4Prefix.MAX_MASK_LENGTH);
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(matchIp4SrcPrefix)
                .matchIPDst(matchIp4DstPrefix);
//installRuleNew()
        /*
        if (ipv4Protocol == IPv4.PROTOCOL_TCP) {
            TCP tcpPacket = (TCP) ipv4Packet.getPayload();
            selectorBuilder.matchIPProtocol(ipv4Protocol)
                    .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                    .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
        }
        if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
            UDP udpPacket = (UDP) ipv4Packet.getPayload();
            selectorBuilder.matchIPProtocol(ipv4Protocol)
                    .matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                    .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
        }
        if (ipv4Protocol == IPv4.PROTOCOL_ICMP) {
            ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();
            selectorBuilder.matchIPProtocol(ipv4Protocol);
        }*/

        TrafficTreatment treatment;
        treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
                //.setVlanId(VlanId.vlanId(outputVlan))
                .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                forwardingObjective);

        //packetOut(context, portNumber);
    }

    public void installForwardTagRule(PacketContext context, PortNumber outputPort, int outVlan) {

        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            return;
        }

        selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                .matchEthSrc(inPkt.getSourceMAC())
                .matchEthDst(inPkt.getDestinationMAC());

        //VLAN TAG
        //selectorBuilder.matchVlanId(VlanId.vlanId(inPkt.getVlanID()));


        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
        byte ipv4Protocol = ipv4Packet.getProtocol();
        Ip4Prefix matchIp4SrcPrefix = Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix matchIp4DstPrefix = Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        selectorBuilder.
                matchEthType(Ethernet.TYPE_IPV4).
                matchIPSrc(matchIp4SrcPrefix).
                matchIPDst(matchIp4DstPrefix);


        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan().setVlanId(VlanId.vlanId((short) outVlan)).setOutput(outputPort).build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();



        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
        log.info("Installed flow for "  /*+ selectorBuilder.toString()*/ + " / port " + outputPort + " / push vlan " + outVlan);
        //packetOut(context, outputPort);
    }

    public void installForwardUntagRuleOld(/*TrafficSelector packetSelector,*/ PacketContext context, PortNumber outputPort) {

        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            return;
        }

        selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                .matchEthSrc(inPkt.getDestinationMAC())
                .matchEthDst(inPkt.getSourceMAC());

        //VLAN TAG
        //selectorBuilder.matchVlanId(VlanId.vlanId(inPkt.getVlanID()));


        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
        byte ipv4Protocol = ipv4Packet.getProtocol();
        Ip4Prefix matchIp4SrcPrefix = Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix matchIp4DstPrefix = Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(matchIp4DstPrefix).matchIPDst(matchIp4SrcPrefix);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan().setOutput(outputPort).build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();
        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
        log.info("Installed flow for " + /*selectorBuilder.toString() + */" / port " + outputPort + " / pop vlan ");
        packetOut(context, outputPort);
    }
    public void installForwardUntagRule(/*TrafficSelector packetSelector,*/ PacketContext context, PortNumber outputPort) {

        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            return;
        }

        selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                .matchEthSrc(inPkt.getDestinationMAC())
                .matchEthDst(inPkt.getSourceMAC());

        //VLAN TAG
        //selectorBuilder.matchVlanId(VlanId.vlanId(inPkt.getVlanID()));


        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
        byte ipv4Protocol = ipv4Packet.getProtocol();
        Ip4Prefix matchIp4SrcPrefix = Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix matchIp4DstPrefix = Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(matchIp4SrcPrefix).matchIPDst(matchIp4DstPrefix);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan().setOutput(outputPort).build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();
        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
        log.info("Installed flow for " + /*selectorBuilder.toString() + */" / port " + outputPort + " / pop vlan ");
        //packetOut(context, outputPort);
    }

    public void installForwardUntagNew(/*TrafficSelector packetSelector,*/ PacketContext context, PortNumber outputPort) {

        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        if (inPkt.getEtherType() == Ethernet.TYPE_ARP) {
            return;
        }

        selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                .matchEthSrc(inPkt.getDestinationMAC())
                .matchEthDst(inPkt.getSourceMAC());

        //VLAN TAG
        //selectorBuilder.matchVlanId(VlanId.vlanId(inPkt.getVlanID()));


        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
        byte ipv4Protocol = ipv4Packet.getProtocol();
        Ip4Prefix matchIp4SrcPrefix = Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        Ip4Prefix matchIp4DstPrefix = Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),Ip4Prefix.MAX_MASK_LENGTH);
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPSrc(matchIp4SrcPrefix).matchIPDst(matchIp4DstPrefix);

        TrafficTreatment treatment;
        treatment = DefaultTrafficTreatment.builder()
                .setOutput(outputPort)
                .build();

        //TrafficTreatment treatment = DefaultTrafficTreatment.builder()
        //      .popVlan().setOutput(outputPort).build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();
        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
        log.info("Installed flow for " + /*selectorBuilder.toString() + */" / port " + outputPort + " ");
        //packetOut(context, outputPort);
    }



    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            List<Event> reasons = event.reasons();
            if (reasons != null) {
                reasons.forEach(re -> {
                    if (re instanceof LinkEvent) {
                        LinkEvent le = (LinkEvent) re;
                        if (le.type() == LinkEvent.Type.LINK_REMOVED && blackHoleExecutor != null) {
                            //blackHoleExecutor.submit(() -> fixBlackhole(le.subject().src()));
                        }
                    }
                });
            }
        }
    }

    private void fixBlackhole(ConnectPoint egress) {
        Set<FlowEntry> rules = getFlowRulesFrom(egress);
        Set<SrcDstPair> pairs = findSrcDstPairs(rules);

        Map<DeviceId, Set<Path>> srcPaths = new HashMap<>();

        for (SrcDstPair sd : pairs) {
            // get the edge deviceID for the src host
            Host srcHost = hostService.getHost(HostId.hostId(sd.src));
            Host dstHost = hostService.getHost(HostId.hostId(sd.dst));
            if (srcHost != null && dstHost != null) {
                DeviceId srcId = srcHost.location().deviceId();
                DeviceId dstId = dstHost.location().deviceId();
                log.trace("SRC ID is {}, DST ID is {}", srcId, dstId);

                cleanFlowRules(sd, egress.deviceId());

                Set<Path> shortestPaths = srcPaths.get(srcId);
                if (shortestPaths == null) {
                    shortestPaths = topologyService.getPaths(topologyService.currentTopology(),
                            egress.deviceId(), srcId);
                    srcPaths.put(srcId, shortestPaths);
                }
                backTrackBadNodes(shortestPaths, dstId, sd);
            }
        }
    }

    // Backtracks from link down event to remove flows that lead to blackhole
    private void backTrackBadNodes(Set<Path> shortestPaths, DeviceId dstId, SrcDstPair sd) {
        for (Path p : shortestPaths) {
            List<Link> pathLinks = p.links();
            for (int i = 0; i < pathLinks.size(); i = i + 1) {
                Link curLink = pathLinks.get(i);
                DeviceId curDevice = curLink.src().deviceId();

                // skipping the first link because this link's src has already been pruned beforehand
                if (i != 0) {
                    cleanFlowRules(sd, curDevice);
                }

                Set<Path> pathsFromCurDevice =
                        topologyService.getPaths(topologyService.currentTopology(),
                                curDevice, dstId);
                if (pickForwardPathIfPossible(pathsFromCurDevice, curLink.src().port()) != null) {
                    break;
                } else {
                    if (i + 1 == pathLinks.size()) {
                        cleanFlowRules(sd, curLink.dst().deviceId());
                    }
                }
            }
        }
    }

    // Removes flow rules off specified device with specific SrcDstPair
    private void cleanFlowRules(SrcDstPair pair, DeviceId id) {
        log.trace("Searching for flow rules to remove from: {}", id);
        log.trace("Removing flows w/ SRC={}, DST={}", pair.src, pair.dst);
        for (FlowEntry r : flowRuleService.getFlowEntries(id)) {
            boolean matchesSrc = false, matchesDst = false;
            for (Instruction i : r.treatment().allInstructions()) {
                if (i.type() == Instruction.Type.OUTPUT) {
                    // if the flow has matching src and dst
                    for (Criterion cr : r.selector().criteria()) {
                        if (cr.type() == Criterion.Type.ETH_DST) {
                            if (((EthCriterion) cr).mac().equals(pair.dst)) {
                                matchesDst = true;
                            }
                        } else if (cr.type() == Criterion.Type.ETH_SRC) {
                            if (((EthCriterion) cr).mac().equals(pair.src)) {
                                matchesSrc = true;
                            }
                        }
                    }
                }
            }
            if (matchesDst && matchesSrc) {
                log.trace("Removed flow rule from device: {}", id);
                flowRuleService.removeFlowRules((FlowRule) r);
            }
        }

    }

    // Returns a set of src/dst MAC pairs extracted from the specified set of flow entries
    private Set<SrcDstPair> findSrcDstPairs(Set<FlowEntry> rules) {
        ImmutableSet.Builder<SrcDstPair> builder = ImmutableSet.builder();
        for (FlowEntry r : rules) {
            MacAddress src = null, dst = null;
            for (Criterion cr : r.selector().criteria()) {
                if (cr.type() == Criterion.Type.ETH_DST) {
                    dst = ((EthCriterion) cr).mac();
                } else if (cr.type() == Criterion.Type.ETH_SRC) {
                    src = ((EthCriterion) cr).mac();
                }
            }
            builder.add(new SrcDstPair(src, dst));
        }
        return builder.build();
    }


    //Example for checking rules on a switch
    private Set<FlowEntry> getFlowRulesFrom(ConnectPoint egress) {
        ImmutableSet.Builder<FlowEntry> builder = ImmutableSet.builder();
        flowRuleService.getFlowEntries(egress.deviceId()).forEach(r -> {
            if (r.appId() == appId.id()) {
                r.treatment().allInstructions().forEach(i -> {
                    if (i.type() == Instruction.Type.OUTPUT) {
                        if (((Instructions.OutputInstruction) i).port().equals(egress.port())) {
                            flowRuleService.removeFlowRules(r);
                        }
                    }
                });
            }
        });

        return builder.build();
    }

    // Wrapper class for a source and destination pair of MAC addresses
    private final class SrcDstPair {
        final MacAddress src;
        final MacAddress dst;

        private SrcDstPair(MacAddress src, MacAddress dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SrcDstPair that = (SrcDstPair) o;
            return Objects.equals(src, that.src) &&
                    Objects.equals(dst, that.dst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }
    }
/*
//Updated by Radhe
    private void hostUpdated(Host host) {
        log.info(host.toString());
    }
    /**
     * Internal listener for host events.
     */
    /*
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
                case HOST_ADDED:
                    log.info("Host ({}) added, host event {}", event.subject().id(), event);
                    break;
                case HOST_UPDATED:
                    log.info("Host ({}) updated, host event {}", event.subject().id(), event);
                    break;
                case HOST_MOVED:
                    log.info("Host ({}) moved, host event {}", event.subject().id(), event);
                    //hostEventExecutors.execute(() -> hostUpdated(event.subject()), event.subject().id().hashCode());
                    break;
                case HOST_REMOVED:
                    log.info("Host ({}) removed, host event {}", event.subject().id(), event);
                    break;
                default:
                    break;
            }
        }
    }*/
}