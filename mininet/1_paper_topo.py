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

izv1 = net.addHost("izv1") # izv1
izv2 = net.addHost("izv2") # izv2
izv3 = net.addHost("izv3") # izv3
izv4 = net.addHost("izv4") # izv4

pon1 = net.addHost("pon1") # pon1
pon2 = net.addHost("pon2") # pon2
pon3 = net.addHost("pon3") # pon3
pon4 = net.addHost("pon4") # pon4

s1 = net.addSwitch("s1")
s2 = net.addSwitch("s2")
s3 = net.addSwitch("s3")
s4 = net.addSwitch("s4")
s5 = net.addSwitch("s5")
s6 = net.addSwitch("s6")
s7 = net.addSwitch("s7")
s8 = net.addSwitch("s8")

# Add links
net.addLink(h1, s1, **linkopts)
net.addLink(h2, s1, **linkopts)
net.addLink(h3, s1, **linkopts)
net.addLink(h4, s1, **linkopts)
 
net.addLink(h5, s3, **linkopts)

net.addLink(s1, s2, **linkopts);
net.addLink(s1, s4, **linkopts);
net.addLink(s1, s6, **linkopts);
net.addLink(s2, s4, **linkopts);
net.addLink(s2, s5, **linkopts);
net.addLink(s2, s3, **linkopts);
net.addLink(s3, s5, **linkopts);
net.addLink(s3, s8, **linkopts);
net.addLink(s4, s5, **linkopts);
net.addLink(s4, s6, **linkopts);
net.addLink(s4, s7, **linkopts);
net.addLink(s5, s7, **linkopts);
net.addLink(s5, s8, **linkopts);
net.addLink(s6, s7, **linkopts);
net.addLink(s7, s8, **linkopts);

net.addLink(izv1, s2, **linkopts);
net.addLink(pon1, s7, **linkopts);

net.addLink(izv2, s2, **linkopts);
net.addLink(pon2, s7, **linkopts);

net.addLink(izv3, s2, **linkopts);
net.addLink(pon3, s7, **linkopts);

net.addLink(izv4, s2, **linkopts);
net.addLink(pon4, s7, **linkopts);

# Start
net.addController('c', controller=RemoteController,ip='127.0.0.1',port=6633)
net.build()
net.start()
 
# CLI
# CLI( net )
sleep(5)

'''
izv1.sendCmd('iperf -u -s -p 6000')
izv2.sendCmd('iperf -u -s -p 8000')
izv3.sendCmd('iperf -u -s -p 10000')
izv4.sendCmd('iperf -u -s -p 12000')

sleep(15)

pon1.sendCmd('iperf -u -c %s -p 6000 -b10m -t60' % (izv1.IP()))
pon2.sendCmd('iperf -u -c %s -p 8000 -b10m -t60' % (izv2.IP()))
pon3.sendCmd('iperf -u -c %s -p 10000 -b10m -t60' % (izv3.IP()))
pon4.sendCmd('iperf -u -c %s -p 12000 -b10m -t60' % (izv3.IP()))
'''

sleep(1)
net.iperf((h4, h5), l4Type='UDP', port=12000, udpBw='5M')
sleep(1)
net.iperf((h1, h5), l4Type='UDP', port=6000, udpBw='80k')
sleep(1)
net.iperf((h2, h5), l4Type='UDP', port=8000, udpBw='5M')
sleep(1)
net.iperf((h3, h5), l4Type='UDP', port=10000, udpBw='1M')

'''
print "--------------------------------------------"
print pon1.waitOutput()
print "--------------------------------------------"
print pon2.waitOutput()
print "--------------------------------------------"
print pon3.waitOutput()
'''
# Clean up
net.stop()
