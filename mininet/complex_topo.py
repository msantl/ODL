#!/usr/bin/env python
import random

from mininet.net  import Mininet
from mininet.node import RemoteController
from mininet.link import TCLink
from mininet.node import OVSSwitch
from mininet.cli  import CLI
from mininet.util import quietRun
 
net = Mininet(link=TCLink, switch=OVSSwitch);
 
# number of switches/hosts
S = 8

switch = []
host = []

# Add hosts and switches
for i in xrange(S):
    name = 'h{}'.format(i + 1)
    print "Adding new host ", name
    host.append(net.addHost(name))

for i in xrange(S):
    name = 's{}'.format(i + 1)
    print "Adding new switch ", name
    switch.append(net.addSwitch(name))

 
# Add links
# set link speeds to 10Mbit/s
linkopts = dict(bw=10)

for i in xrange(S):
    net.addLink(host[i],   switch[i], **linkopts )

for i in xrange(S):
    for j in xrange(i + 1, S):
        if random.random() > 0.5: continue

        print "Connecting switch s{} with s{}".format(i + 1, j + 1)
        net.addLink(switch[i], switch[j], **linkopts);
 
# Start
net.addController('c', controller=RemoteController,ip='127.0.0.1',port=6633)
net.build()
net.start()
 
# CLI
CLI( net )
 
# Clean up
net.stop()
