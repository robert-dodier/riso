class py_variable:
    def __str__ (self):
        return str (self.java_variable)
    def __getattr__ (self, name):
        # COULD LOOK FOR name == 'name' OR 'fullname' AND RETURN THAT
        return getattr (self.java_variable, name)

class py_bn:
    def __str__ (self):
        return str (self.java_bn)
    def __getattr__ (self, name):
        # COULD LOOK FOR name == 'name' OR 'fullname' AND RETURN THAT
        return getattr (self.java_bn, name)
    def __setattr__ (self, name, value):
        try:
            a = self.__dict__[name]
            print name+' is in self.__dict__'
            try:
                self.java_bn.assign_evidence (a.java_variable, value)
                print name+' has java_variable attribute'
            except KeyError:
                print name+' doesnt have java_variable'
                a = value
        except KeyError:
            print name+' not in self.__dict__'
            self.__dict__[name] = value

def parse_network (s):
    b = py_bn ()
    b.java_bn = c.parse_network (s)
    l = b.java_bn.get_variables ()
    for i in range (len (l)):
        v = py_variable ()
        v.java_variable = l [i]
        s = v.java_variable.get_name ()
        setattr (b, s, v)
    # MIGHT WANT TO IMPORT A VARIABLE WITH NAME == b.get_name() INTO
    # globals() OR SOMETHING -- SEE dn.py
    return b

import riso.belief_nets.BeliefNetworkContext
# BNC WORKS FINE EXCEPT THAT THERE IS AT LEAST ONE THREAD LEFT RUNNING
# NEED TO IMPLEMENT SOME KIND OF SHUTDOWN MECHANISM SO JYTHON EXITS
c = riso.belief_nets.BeliefNetworkContext ('mycontext')
f = open ('/home/robert/belief-nets/random-polytree/random8.riso')
s = f.read ()
b = parse_network (s)
