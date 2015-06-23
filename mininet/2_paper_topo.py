#!/usr/bin/env python
import random
from time import sleep

from mininet.net  import Mininet
from mininet.node import RemoteController
from mininet.link import TCLink
from mininet.node import OVSSwitch
from mininet.cli  import CLI
from mininet.util import quietRun
 
net = Mininet(link=TCLink, switch=OVSSwitch);
# set link speeds to 10Mbit/s
linkopts = dict(bw=5, loss=0)

h1 = net.addHost("h1") # video
h2 = net.addHost("h2") # audio
h3 = net.addHost("h3") # data
h4 = net.addHost("h4") # other

h5 = net.addHost("h5") #server

s1 = net.addSwitch("s1")
s2 = net.addSwitch("s2")
s3 = net.addSwitch("s3")
s4 = net.addSwitch("s4")
s5 = net.addSwitch("s5")
s6 = net.addSwitch("s6")
s7 = net.addSwitch("s7")
s8 = net.addSwitch("s8")
s9 = net.addSwitch("s9")
s10 = net.addSwitch("s10")
s11 = net.addSwitch("s11")
s12 = net.addSwitch("s12")
s13 = net.addSwitch("s13")
s14 = net.addSwitch("s14")

# Add links
net.addLink(h1, s1, **linkopts)
net.addLink(h2, s1, **linkopts)
net.addLink(h3, s1, **linkopts)
net.addLink(h4, s1, **linkopts)
 
net.addLink(h5, s3, **linkopts)

net.addLink(s1, s2, **linkopts);
net.addLink(s1, s4, **linkopts);
net.addLink(s1, s6, **linkopts);
net.addLink(s1, s9, **linkopts);

net.addLink(s2, s4, **linkopts);
net.addLink(s2, s5, **linkopts);
net.addLink(s2, s3, **linkopts);

net.addLink(s3, s5, **linkopts);
net.addLink(s3, s8, **linkopts);
net.addLink(s3, s14, **linkopts);

net.addLink(s4, s5, **linkopts);
net.addLink(s4, s6, **linkopts);
net.addLink(s4, s7, **linkopts);

net.addLink(s5, s7, **linkopts);
net.addLink(s5, s8, **linkopts);

net.addLink(s6, s7, **linkopts);
net.addLink(s6, s9, **linkopts);
net.addLink(s6, s10, **linkopts);
net.addLink(s6, s11, **linkopts);

net.addLink(s7, s8, **linkopts);
net.addLink(s7, s11, **linkopts);
net.addLink(s7, s12, **linkopts);

net.addLink(s8, s12, **linkopts);
net.addLink(s8, s13, **linkopts);
net.addLink(s8, s14, **linkopts);

net.addLink(s9, s10, **linkopts);
net.addLink(s10, s11, **linkopts);
net.addLink(s11, s12, **linkopts);
net.addLink(s12, s13, **linkopts);
net.addLink(s13, s14, **linkopts);

# Start
net.addController('c', controller=RemoteController,ip='127.0.0.1',port=6633)
net.build()
net.start()
 
# CLI
# CLI( net )
sleep(5)

sleep(1)
net.iperf((h4, h5), l4Type='UDP', port=12000, udpBw='5M')
sleep(1)
net.iperf((h1, h5), l4Type='UDP', port=6000, udpBw='80k')
sleep(1)
net.iperf((h2, h5), l4Type='UDP', port=8000, udpBw='5M')
sleep(1)
net.iperf((h3, h5), l4Type='UDP', port=10000, udpBw='1M')

# Clean up
net.stop()
