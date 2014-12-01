#!/usr/bin/env python
 
from mininet.net  import Mininet
from mininet.node import RemoteController
from mininet.link import TCLink
from mininet.node import OVSSwitch
from mininet.cli  import CLI
from mininet.util import quietRun
 
net = Mininet(link=TCLink, switch=OVSSwitch);
 
# Add hosts and switches
Host1  = net.addHost('h1')
Host2  = net.addHost('h2')
 
Switch1 = net.addSwitch('s1')
Switch2 = net.addSwitch('s2')
Switch3 = net.addSwitch('s3')
Switch4 = net.addSwitch('s4')
Switch5 = net.addSwitch('s5')
 
# Add links
# set link speeds to 10Mbit/s
linkopts = dict(bw=10)
net.addLink(Host1,   Switch1,    **linkopts )
net.addLink(Switch1,  Switch2,    **linkopts )
net.addLink(Switch1,  Switch3,    **linkopts )
net.addLink(Switch3,  Switch4,    **linkopts )
net.addLink(Switch2,  Switch5,    **linkopts )
net.addLink(Switch4,  Switch5,    **linkopts )
net.addLink(Switch4,  Host2,     **linkopts )
 
# Start
net.addController('c', controller=RemoteController,ip='127.0.0.1',port=6633)
net.build()
net.start()
 
# CLI
CLI( net )
 
# Clean up
net.stop()
