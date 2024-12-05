#! /usr/bin/env bash
NS="ns1"
VETH="v1"
VPEER1="v2"
VADD="10.0.0.1"
#Create namespace
ip netns add $NS

#Create veth
ip link add ${VETH} type veth peer name ${VPEER1}

#Move veth to namespace
ip link set ${VETH} netns ${NS}

#Define networking in the namespace
ip netns exec ${NS} ip link set lo up
ip netns exec ${NS} ip link set ${VETH} up
ip netns exec ${NS} ip addr add ${VADD}/16 dev v1
ip netns exec ${NS} ip route add 10.0.0.0/24 via ${VADD}
