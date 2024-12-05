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

import org.onlab.packet.*;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    ConcurrentHashMap<DeviceId, ConcurrentHashMap<IpAddress,PortNumber>> switchTables = new ConcurrentHashMap<>();
    ConcurrentHashMap<IpAddress, DeviceId> switchHost = new ConcurrentHashMap<>();

    ArrayList<IpAddress> network1 = new ArrayList<>();
    ArrayList<IpAddress> network2 = new ArrayList<>();

    ArrayList<IpAddress> movingStation = new ArrayList<>();

    static boolean forwardCanProceed = false;
    int switchCount = 0;
    int hostCount = 0;



    private ReactivePacketProcessor learnProcessor = new ReactivePacketProcessor();
    private ForwardReactivePacketProcessor forwardProcessor = new ForwardReactivePacketProcessor();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private ApplicationId appId;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.student.virtnetwork");
        TrafficSelector packetSelector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).build();
        packetService.requestPackets(packetSelector, PacketPriority.REACTIVE, appId);
        //packetService.addProcessor(learnProcessor, PacketProcessor.director(1));
        packetService.addProcessor(forwardProcessor, PacketProcessor.director(1));
        intializeSwitchTables();
        initializeTables();
        startTime = System.currentTimeMillis();
        log.info("VirtNetwork Started");
    }

    @Deactivate
    protected void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(forwardProcessor);
        forwardProcessor = null;
        //packetService.removeProcessor(learnProcessor);
        //learnProcessor = null;
        log.info("VirtNetwork Stopped");
    }

    private void intializeSwitchTables(){
        // switch1
        ConcurrentHashMap<IpAddress,PortNumber> switch1Table = new ConcurrentHashMap<>();
        switch1Table.put(IpAddress.valueOf("10.0.0.1"), PortNumber.portNumber(1));
        switch1Table.put(IpAddress.valueOf("10.0.0.2"), PortNumber.portNumber(1));
        switch1Table.put(IpAddress.valueOf("10.0.0.3"), PortNumber.portNumber(1));
        switch1Table.put(IpAddress.valueOf("10.0.0.4"), PortNumber.portNumber(3));
        switch1Table.put(IpAddress.valueOf("10.0.0.5"), PortNumber.portNumber(2));
        switch1Table.put(IpAddress.valueOf("10.0.0.6"), PortNumber.portNumber(1));
        switch1Table.put(IpAddress.valueOf("10.0.0.7"), PortNumber.portNumber(1));

        // switch1
        ConcurrentHashMap<IpAddress,PortNumber> switch2Table = new ConcurrentHashMap<>();
        switch2Table.put(IpAddress.valueOf("10.0.0.1"), PortNumber.portNumber(1));
        switch2Table.put(IpAddress.valueOf("10.0.0.2"), PortNumber.portNumber(1));
        switch2Table.put(IpAddress.valueOf("10.0.0.3"), PortNumber.portNumber(1));
        switch2Table.put(IpAddress.valueOf("10.0.0.4"), PortNumber.portNumber(2));
        switch2Table.put(IpAddress.valueOf("10.0.0.5"), PortNumber.portNumber(2));
        switch2Table.put(IpAddress.valueOf("10.0.0.6"), PortNumber.portNumber(3));
        switch2Table.put(IpAddress.valueOf("10.0.0.7"), PortNumber.portNumber(4));

        // switch1
        ConcurrentHashMap<IpAddress,PortNumber> switch3Table = new ConcurrentHashMap<>();
        switch3Table.put(IpAddress.valueOf("10.0.0.1"), PortNumber.portNumber(2));
        switch3Table.put(IpAddress.valueOf("10.0.0.2"), PortNumber.portNumber(3));
        switch3Table.put(IpAddress.valueOf("10.0.0.3"), PortNumber.portNumber(2));
        switch3Table.put(IpAddress.valueOf("10.0.0.4"), PortNumber.portNumber(1));
        switch3Table.put(IpAddress.valueOf("10.0.0.5"), PortNumber.portNumber(1));
        switch3Table.put(IpAddress.valueOf("10.0.0.6"), PortNumber.portNumber(1));
        switch3Table.put(IpAddress.valueOf("10.0.0.7"), PortNumber.portNumber(1));

        // switch1
        ConcurrentHashMap<IpAddress,PortNumber> switch4Table = new ConcurrentHashMap<>();
        switch4Table.put(IpAddress.valueOf("10.0.0.1"), PortNumber.portNumber(1));
        switch4Table.put(IpAddress.valueOf("10.0.0.2"), PortNumber.portNumber(2));
        switch4Table.put(IpAddress.valueOf("10.0.0.3"), PortNumber.portNumber(1));
        switch4Table.put(IpAddress.valueOf("10.0.0.4"), PortNumber.portNumber(2));
        switch4Table.put(IpAddress.valueOf("10.0.0.5"), PortNumber.portNumber(2));
        switch4Table.put(IpAddress.valueOf("10.0.0.6"), PortNumber.portNumber(2));
        switch4Table.put(IpAddress.valueOf("10.0.0.7"), PortNumber.portNumber(2));

        // switch1
        ConcurrentHashMap<IpAddress,PortNumber> switch5Table = new ConcurrentHashMap<>();
        switch5Table.put(IpAddress.valueOf("10.0.0.1"), PortNumber.portNumber(1));
        switch5Table.put(IpAddress.valueOf("10.0.0.2"), PortNumber.portNumber(1));
        switch5Table.put(IpAddress.valueOf("10.0.0.3"), PortNumber.portNumber(2));
        switch5Table.put(IpAddress.valueOf("10.0.0.4"), PortNumber.portNumber(2));
        switch5Table.put(IpAddress.valueOf("10.0.0.5"), PortNumber.portNumber(2));
        switch5Table.put(IpAddress.valueOf("10.0.0.6"), PortNumber.portNumber(2));
        switch5Table.put(IpAddress.valueOf("10.0.0.7"), PortNumber.portNumber(2));

        // switch1
        ConcurrentHashMap<IpAddress,PortNumber> switch6Table = new ConcurrentHashMap<>();
        switch6Table.put(IpAddress.valueOf("10.0.0.1"), PortNumber.portNumber(2));
        switch6Table.put(IpAddress.valueOf("10.0.0.2"), PortNumber.portNumber(2));
        switch6Table.put(IpAddress.valueOf("10.0.0.3"), PortNumber.portNumber(2));
        switch6Table.put(IpAddress.valueOf("10.0.0.4"), PortNumber.portNumber(2));
        switch6Table.put(IpAddress.valueOf("10.0.0.5"), PortNumber.portNumber(1));
        switch6Table.put(IpAddress.valueOf("10.0.0.6"), PortNumber.portNumber(2));
        switch6Table.put(IpAddress.valueOf("10.0.0.7"), PortNumber.portNumber(2));

        // switch1
        ConcurrentHashMap<IpAddress,PortNumber> switch7Table = new ConcurrentHashMap<>();
        switch7Table.put(IpAddress.valueOf("10.0.0.1"), PortNumber.portNumber(2));
        switch7Table.put(IpAddress.valueOf("10.0.0.2"), PortNumber.portNumber(2));
        switch7Table.put(IpAddress.valueOf("10.0.0.3"), PortNumber.portNumber(2));
        switch7Table.put(IpAddress.valueOf("10.0.0.4"), PortNumber.portNumber(1));
        switch7Table.put(IpAddress.valueOf("10.0.0.5"), PortNumber.portNumber(2));
        switch7Table.put(IpAddress.valueOf("10.0.0.6"), PortNumber.portNumber(2));
        switch7Table.put(IpAddress.valueOf("10.0.0.7"), PortNumber.portNumber(2));

        // push into forwardTable hash map
        DeviceId switch1Id = DeviceId.deviceId("of:0000000000000002");
        DeviceId switch2Id = DeviceId.deviceId("of:0000000000000003");
        DeviceId switch3Id = DeviceId.deviceId("of:0000000000000004");
        DeviceId switch4Id = DeviceId.deviceId("of:1000000000000001");
        DeviceId switch5Id = DeviceId.deviceId("of:1000000000000002");
        DeviceId switch6Id = DeviceId.deviceId("of:1000000000000003");
        DeviceId switch7Id = DeviceId.deviceId("of:1000000000000004");

        switchTables.put(switch1Id, switch1Table);
        switchTables.put(switch2Id, switch2Table);
        switchTables.put(switch3Id, switch3Table);
        switchTables.put(switch4Id, switch4Table);
        switchTables.put(switch5Id, switch5Table);
        switchTables.put(switch6Id, switch6Table);
        switchTables.put(switch7Id, switch7Table);

        switchHost.put(IpAddress.valueOf("10.0.0.1"),switch4Id);
        switchHost.put(IpAddress.valueOf("10.0.0.2"),switch5Id);
        switchHost.put(IpAddress.valueOf("10.0.0.3"),switch4Id);
        switchHost.put(IpAddress.valueOf("10.0.0.4"),switch7Id);
        switchHost.put(IpAddress.valueOf("10.0.0.5"),switch6Id);
        switchHost.put(IpAddress.valueOf("10.0.0.6"),switch2Id);
        switchHost.put(IpAddress.valueOf("10.0.0.7"),switch2Id);
    }

    private void initializeTables() {
        // network1
        network1.add(IpAddress.valueOf("10.0.0.1"));
        network1.add(IpAddress.valueOf("10.0.0.3"));
        network1.add(IpAddress.valueOf("10.0.0.5"));
        network1.add(IpAddress.valueOf("10.0.0.7"));
// network2
        network2.add(IpAddress.valueOf("10.0.0.2"));
        network2.add(IpAddress.valueOf("10.0.0.4"));
        network2.add(IpAddress.valueOf("10.0.0.6"));


        movingStation.add(IpAddress.valueOf("10.0.0.1"));
        //network2.add(IpAddress.valueOf("10.0.0.6"));


    }
    short refreshCount = 0;
    Long currentTime = System.currentTimeMillis();
    Long startTime = System.currentTimeMillis();
    int totalHost = 7;
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            //log.info("froward forwardCanProcedd:"+forwardCanProceed);
            currentTime = System.currentTimeMillis();

            //log.info("CurrentInterval:"+(currentTime-startTime));
            if(currentTime-startTime>=1000*60*3 && refreshCount++==0){
                log.info("Values are Refreshing");
                forwardCanProceed = false;
                //switchHost.clear();
                //switchTables.clear();
                startTime = System.currentTimeMillis();

                switchTables.get(DeviceId.deviceId("of:0000000000000002")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:0000000000000003")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:0000000000000004")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(1));
                switchTables.get(DeviceId.deviceId("of:1000000000000001")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:1000000000000002")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:1000000000000003")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(1));
                switchTables.get(DeviceId.deviceId("of:1000000000000004")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));

                switchHost.put(IpAddress.valueOf("10.0.0.1"),DeviceId.deviceId("of:1000000000000003"));

/*                for(IpAddress ip: movingStation){
                    //remvoving host from switchhost
                    //DeviceId protTable = switchHost.get(ip);
                    for(Map.Entry<DeviceId,ConcurrentHashMap<IpAddress,PortNumber>> portMap : switchTables.entrySet()){
                        //portMap.getValue().get(10.0.0.1)
                        portMap.getValue().remove(ip);
                    }
                    switchHost.remove(ip);
                }*/

            }



            boolean test  = true;
            for (Map.Entry<DeviceId, ConcurrentHashMap<IpAddress,PortNumber>> entry : switchTables.entrySet()) {
                if (entry.getValue().size()==totalHost) continue;
                test = false;
            }
           // if(switchHost.size()==totalHost){
                if(switchHost.size()==totalHost && test){
                forwardCanProceed = true;
                return;
            }


            //return false;

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            //log.info("learner counter is :"+counter);
            //Discard if  packet is null.
            if (ethPkt == null) {
                log.info("Discarding null packet");
                return;
            }

            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4) return;
            log.info("Proccesing packet request.");
            IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();

            DeviceId deviceId = pkt.receivedFrom().deviceId();
            PortNumber inputPort = pkt.receivedFrom().port();
            MacAddress srcMac = ethPkt.getSourceMAC();
            MacAddress dstMac = ethPkt.getDestinationMAC();
            IpAddress srcIp = IpAddress.valueOf(ipv4Packet.getSourceAddress());
            IpAddress dstIp = IpAddress.valueOf(ipv4Packet.getDestinationAddress());
            short pktVlan = ethPkt.getVlanID();

            log.info("DeviceID / Port: " + deviceId + " / " + inputPort);
            log.info("Source MAC / IP: " + srcMac + " / " + srcIp);
            log.info("Destination Mac / IP: " + dstMac + " / " + dstIp);
            log.info("VLAN ID: " + pktVlan);

            // First step is to check if the packet came from a newly discovered switch.
            // Create a new entry if required.
            //DeviceId deviceId = pkt.receivedFrom().deviceId();
            log.info("device id:" + deviceId);
            if((!switchHost.containsKey(srcIp))  && pktVlan==-1){
                //if((!switchHost.containsKey(srcIp))  && pktVlan==-1){
                switchHost.put(srcIp,deviceId);
            }

            if (!switchTables.containsKey(deviceId)) {
                log.info("Adding new switch: " + deviceId.toString());
                ConcurrentHashMap<IpAddress, PortNumber> hostTable = new ConcurrentHashMap<>();
                switchTables.put(deviceId, hostTable);
            }

            // Now lets check if the source host is a known host. If it is not add it to the switchTable.
            ConcurrentHashMap<IpAddress, PortNumber> hostTable = switchTables.get(deviceId);
            //MacAddress srcMac = ethPkt.getSourceMAC();
            if (!hostTable.containsKey(srcIp)) {
                log.info("Adding new host: " + srcIp.toString() + " for switch " + deviceId.toString());
                hostTable.put(srcIp, pkt.receivedFrom().port());
                switchTables.replace(deviceId,hostTable);
            }

            // To take care of loops, we must drop the packet if the port from which it came from does not match the port that the source host should be attached to.
            if (!hostTable.get(srcIp).equals(pkt.receivedFrom().port())) {
                log.info("Dropping packet to break loop");
                return;
            }

            // Now lets check if we know the destination host. If we do asign the correct output port.
            // By default set the port to FLOOD.
            //MacAddress dstMac = ethPkt.getDestinationMAC();
            PortNumber outPort = PortNumber.FLOOD;
            if (hostTable.containsKey(dstIp)) {
                outPort = hostTable.get(dstIp);
                log.info("Setting output port to: " + outPort);

            }

            //Generate the traffic selector based on the packet that arrived.
            TrafficSelector packetSelector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(ipv4Packet.getProtocol())
                    .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                    .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH))
                    //.matchEthDst(dstIp);
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outPort).build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(packetSelector)
                    .withTreatment(treatment)
                    .withPriority(5000)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(5)
                    .add();

            if (outPort != PortNumber.FLOOD) flowObjectiveService.forward(deviceId, forwardingObjective);
            context.treatmentBuilder().addTreatment(treatment);
            context.send();

            log.info("switchTable:" + switchTables);
            log.info("switchHost:" + switchHost);
        }
    }

    private class ForwardReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            refreshTables();

            //Discard if  packet is null.
            if (ethPkt == null) {
                log.info("Discarding null packet");
                return;
            }

            if(ethPkt.getEtherType() != Ethernet.TYPE_IPV4) return;
            IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
            log.info("");
            log.info("Proccesing packet request.");

            DeviceId deviceId = pkt.receivedFrom().deviceId();
            PortNumber inputPort = pkt.receivedFrom().port();
            MacAddress srcMac = ethPkt.getSourceMAC();
            MacAddress dstMac = ethPkt.getDestinationMAC();
            IpAddress srcIp = IpAddress.valueOf(ipv4Packet.getSourceAddress());
            IpAddress dstIp = IpAddress.valueOf(ipv4Packet.getDestinationAddress());
            short pktVlan = ethPkt.getVlanID();



            log.info("DeviceID / Port: " + deviceId + " / " + inputPort);
            log.info("Source MAC / IP: " + srcMac + " / " + srcIp);
            log.info("Destination Mac / IP: " + dstMac + " / " + dstIp);
            log.info("VLAN ID: " + pktVlan);

            //log.info("Inbound packet: {}", pkt);
            //log.info("IPv4 packet: {}", ipv4Packet);


            //deteremine host connected to switch
            if(pktVlan==-1){

            }
            log.info("switchHost:"+switchHost);
            log.info("switchTable:"+switchTables);

            // Determine whether packet should be forwarded or dropped
            PortNumber outputPort = null;
            int outputVlan = ipsInSameNetwork(srcIp, dstIp);
            if (outputVlan != -1) {
                log.info("IPs in same network.");
                ConcurrentHashMap<IpAddress,PortNumber> switchTable = switchTables.get(deviceId);
                if (switchTable != null) {
                    if (switchTable.containsKey(dstIp)) {
                        outputPort = switchTable.get(dstIp);
                        log.info("Output port: " + outputPort.toString() + "in Switch: "+ deviceId.toString());
                    } else {
                        log.info("Error: IP not found in table.");
                    }
                } else {
                    log.info("Error: Switch not found in table.");
                }

            } else {
                log.info("IPs NOT in same network.");
            }

            // IPs belong to same network - install flow rule
            if (outputPort != null) {
                // forward flow
                log.info("Install forward flows.");

                // check if packet is untagged
                if (pktVlan == -1) {
                    //switch
                    log.info("Packet is untagged.");
                    //Generate the traffic selector based on the packet that arrived.
                    TrafficSelector.Builder packetSelector = DefaultTrafficSelector.builder()
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPProtocol(ipv4Packet.getProtocol())
                            .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                            .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH));

                    //Generate the traffic selector for the return route
                    TrafficSelector.Builder packetSelector2 = DefaultTrafficSelector.builder()
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPProtocol(ipv4Packet.getProtocol())
                            .matchIPSrc(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH))
                            .matchIPDst(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                            .matchVlanId(VlanId.vlanId((short) outputVlan));

                    // eg h1 to h2 with same switch s1 by radhe
                    // check if hosts are connected to the same device - in this case, no tagging is needed
                    if (hostsConnectedToSameDevice(srcIp, dstIp)) {
                        log.info("Hosts are connected to same device.");

                        // install flow rules
                        installForwardRule(packetSelector.build(), context, outputPort);
                        installForwardRule(packetSelector2.build(), context, inputPort);
                        // forward the received packet
                        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(outputPort).build();
                        context.treatmentBuilder().addTreatment(treatment);
                        context.send();
                    } else {
                        log.info("Hosts are NOT connected to same device.");

                        // install flow rules
                        installForwardTagRule(packetSelector.build(), context, outputPort, outputVlan);
                        installForwardUntagRule(packetSelector2.build(), context, inputPort);

                        // tag and forward the first packet manually here
                        Ethernet newEthPkt = new Ethernet();
                        newEthPkt.setDestinationMACAddress(dstMac);
                        newEthPkt.setSourceMACAddress(srcMac);
                        newEthPkt.setEtherType(Ethernet.TYPE_IPV4);
                        newEthPkt.setVlanID((short) outputVlan);
                        newEthPkt.setPayload(ipv4Packet);
                        ByteBuffer newPktBuffer = ByteBuffer.wrap(newEthPkt.serialize());

                        //log.info("New tagged packet: {}", newEthPkt.toString());

                        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(outputPort).build();
                        OutboundPacket packetOut = new DefaultOutboundPacket(deviceId, treatment, newPktBuffer);
                        packetService.emit(packetOut);
                    }

                }
                //for switch which get tagged data eg. switch 2
                else {
                    log.info("Packet is tagged - VLAN {}.", pktVlan);

                    if (pktVlan == outputVlan) {
                        // vlans match
                        // eg h1 to h2 with same switch s1 by radhe
                        // check if hosts are connected to the same device - in this case, no tagging is needed
                        boolean isHostInDevice = false;
                        //ArrayList<IpAddress> currentSwitch = switchHost.get(deviceId);
                        boolean isHostInDeviceTest = switchHost.containsKey(dstIp);
                        if(isHostInDeviceTest==true) {
                            isHostInDevice = (switchHost.get(dstIp).toString().equalsIgnoreCase(deviceId.toString()));
                        }
                        log.info("--else switch:"+deviceId +", dspId:"+dstIp +" isHostInDivice:"+isHostInDevice+ "isHostnDevicTest:"+isHostInDeviceTest);
                        if (isHostInDevice) {
                            log.info("last switch for host");

                            //Generate the traffic selector based on the packet that arrived.
                            TrafficSelector.Builder packetSelector = DefaultTrafficSelector.builder()
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPProtocol(ipv4Packet.getProtocol())
                                    .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                    .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                    .matchVlanId(VlanId.vlanId((short) outputVlan));

                            installForwardUntagRule(packetSelector.build(), context, outputPort);

                            //Generate the traffic selector for the return route
                            TrafficSelector.Builder packetSelector2 = DefaultTrafficSelector.builder()
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPProtocol(ipv4Packet.getProtocol())
                                    .matchIPSrc(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                    .matchIPDst(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH));

                            installForwardTagRule(packetSelector2.build(), context, inputPort, outputVlan);

                            // untag and forward the first packet manually here
                            Ethernet newEthPkt = new Ethernet();
                            newEthPkt.setDestinationMACAddress(dstMac);
                            newEthPkt.setSourceMACAddress(srcMac);
                            newEthPkt.setEtherType(Ethernet.TYPE_IPV4);
                            newEthPkt.setPayload(ipv4Packet);
                            ByteBuffer newPktBuffer = ByteBuffer.wrap(newEthPkt.serialize());

                            //log.info("New untagged packet: {}", newEthPkt.toString());

                            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(outputPort).build();
                            OutboundPacket packetOut = new DefaultOutboundPacket(deviceId, treatment, newPktBuffer);
                            packetService.emit(packetOut);

                        } else {
                            log.info("core switches.");

                            //Generate the traffic selector based on the packet that arrived.
                            TrafficSelector.Builder packetSelector = DefaultTrafficSelector.builder()
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPProtocol(ipv4Packet.getProtocol())
                                    .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                    .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH));

                            //Generate the traffic selector for the return route
                            TrafficSelector.Builder packetSelector2 = DefaultTrafficSelector.builder()
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPProtocol(ipv4Packet.getProtocol())
                                    .matchIPSrc(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                    .matchIPDst(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                    .matchVlanId(VlanId.vlanId((short) outputVlan));

                            installForwardRule(packetSelector.build(), context, outputPort);
                            installForwardRule(packetSelector2.build(), context, inputPort);


                            // forward the received packet
                            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(outputPort).build();
                            context.treatmentBuilder().addTreatment(treatment);
                            context.send();

                        }

                    } else {
                        // vlans does not match
                        // We did not get this scenario till now
                        log.info("VLAN IDs does not match. Received: {}, Should be: {}.", pktVlan, outputVlan);
                        // ignore packet

                        //Generate the traffic selector based on the packet that arrived.
                        TrafficSelector.Builder packetSelector = DefaultTrafficSelector.builder()
                                .matchEthType(Ethernet.TYPE_IPV4)
                                .matchIPProtocol(ipv4Packet.getProtocol())
                                .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH));

                        //Generate the traffic selector for the return route
                        TrafficSelector.Builder packetSelector2 = DefaultTrafficSelector.builder()
                                .matchEthType(Ethernet.TYPE_IPV4)
                                .matchIPProtocol(ipv4Packet.getProtocol())
                                .matchIPSrc(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                .matchIPDst(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                                .matchVlanId(VlanId.vlanId((short) outputVlan));

                        // install flow rules
                        installForwardRule(packetSelector.build(), context, outputPort);
                        installForwardRule(packetSelector2.build(), context, inputPort);
                        // forward the received packet
                        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(outputPort).build();
                        context.treatmentBuilder().addTreatment(treatment);
                        context.send();

                        return;
                    }
                }



            } else {
                // drop flow
                log.info("Install drop flow.");
                TrafficSelector.Builder packetSelector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPProtocol(ipv4Packet.getProtocol())
                        .matchIPSrc(IpPrefix.valueOf(srcIp, IpPrefix.MAX_INET_MASK_LENGTH))
                        .matchIPDst(IpPrefix.valueOf(dstIp, IpPrefix.MAX_INET_MASK_LENGTH));

                installDropRule(packetSelector.build(), context);
                // block received packet
                context.block();
            }

        }

        private void refreshTables(){
            currentTime = System.currentTimeMillis();
            if(currentTime-startTime>=1000*60*3 && refreshCount++==0){
                log.info("Values are Refreshing");
                startTime = System.currentTimeMillis();
                switchTables.get(DeviceId.deviceId("of:0000000000000002")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:0000000000000003")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:0000000000000004")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(1));
                switchTables.get(DeviceId.deviceId("of:1000000000000001")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:1000000000000002")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));
                switchTables.get(DeviceId.deviceId("of:1000000000000003")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(1));
                switchTables.get(DeviceId.deviceId("of:1000000000000004")).put(IpAddress.valueOf("10.0.0.1"),PortNumber.portNumber(2));

                switchHost.put(IpAddress.valueOf("10.0.0.1"),DeviceId.deviceId("of:1000000000000003"));
            }

        }

        private int ipsInSameNetwork(IpAddress srcIp, IpAddress dstIp) {
            if (network1.contains(srcIp) && network1.contains(dstIp)) {
                return 3;
            } else if (network2.contains(srcIp) && network2.contains(dstIp)) {
                return 4;
            } else {
                return -1;
            }
        }

        private boolean hostsConnectedToSameDevice(IpAddress srcIp, IpAddress dstIp) {
            return (switchHost.get(srcIp).toString().equalsIgnoreCase(switchHost.get(dstIp).toString()));
        }

        // Install flow rule in switch
        public void installForwardRule(TrafficSelector packetSelector, PacketContext context, PortNumber outputPort) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outputPort).build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(packetSelector)
                    .withTreatment(treatment)
                    .withPriority(100)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(50) //5
                    .add();
            flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
            log.info("Installed flow for " + packetSelector.toString() + " / port " + outputPort);
            return;
        }

        public void installForwardTagRule(TrafficSelector packetSelector, PacketContext context, PortNumber outputPort, int outVlan) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .pushVlan().setVlanId(VlanId.vlanId((short) outVlan)).setOutput(outputPort).build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(packetSelector)
                    .withTreatment(treatment)
                    .withPriority(100)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(50) //5
                    .add();
            flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
            log.info("Installed flow for " + packetSelector.toString() + " / port " + outputPort + " / push vlan " + outVlan);
            return;
        }

        public void installForwardUntagRule(TrafficSelector packetSelector, PacketContext context, PortNumber outputPort) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .popVlan().setOutput(outputPort).build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(packetSelector)
                    .withTreatment(treatment)
                    .withPriority(100)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(50) //5
                    .add();
            flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
            log.info("Installed flow for " + packetSelector.toString() + " / port " + outputPort + " / pop vlan ");
            return;
        }

        public void installDropRule(TrafficSelector packetSelector, PacketContext context) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().drop().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .withSelector(packetSelector)
                    .withTreatment(treatment)
                    .withPriority(100)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .fromApp(appId)
                    .makeTemporary(50) //5
                    .add();
            flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);
            log.info("Installed flow for " + packetSelector.toString() + " / drop");
            return;
        }
    }


}