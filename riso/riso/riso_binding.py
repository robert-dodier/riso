class py_variable:
    def __str__ (self):
        return str (self.java_variable)
    def __getattr__ (self, name):
        return getattr (self.java_variable, name) ()

class py_bn:
    def __str__ (self):
        return str (self.java_bn)
    def __getattr__ (self, name):
        return getattr (self.java_bn, name) ()

import riso.belief_nets.BeliefNetworkContext
c = riso.belief_nets.BeliefNetworkContext('mycontext')
f = open('/home/robert/belief-nets/random-polytree/random8.riso')
s = f.read()
java_bn = c.parse_network(s)
b = py_bn()
b.java_bn = java_bn
l = b.java_bn.get_variables ()
for i in range(len(l)):
    v = py_variable ()
    v.java_variable = l [i]
    s = v.java_variable.get_name ()
    setattr (b, s, v)
