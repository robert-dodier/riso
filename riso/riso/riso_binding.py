import riso.belief_nets.BeliefNetworkContext

bn_context = riso.belief_nets.BeliefNetworkContext ('mycontext')  # THIS NAME SHOULD BE CONFIGURABLE !!!

def shutdown ():
    # PROBABLY IT'S POSSIBLE TO DO THIS AUTOMATICALLY AT JYTHON EXIT !!!
    import java.rmi.server.UnicastRemoteObject
    java.rmi.server.UnicastRemoteObject.unexportObject (bn_context, 1)

class py_variable:
    def __init__ (self, java_variable, owner):
        self.java_variable = java_variable
        self.name = java_variable.get_name ()
        self.owner = owner
    def __str__ (self):
        return self.java_variable.format_string ('')
    def __getattr__ (self, name):
        if name == 'cpd':
            return self.java_variable.get_distribution ()
        elif name == 'posterior':
            return self.java_variable.get_posterior ()
        elif name == 'pi':
            return self.java_variable.get_pi ()
        elif name == 'lambda':
            return self.java_variable.get_lambda ()
        elif name == 'pi_messages':
            return self.java_variable.get_pi_messages ()
        elif name == 'lambda_messages':
            return self.java_variable.get_lambda_messages ()
        elif name == 'parents':
            # Attribute parents doesn't exist yet, so create it.
            # After it's created, this code won't be executed again.
            java_parents = self.java_variable.get_parents ()
            py_parents = []
            for i in range (len (java_parents)):
                # FOLLOWING WON'T WORK IF PARENT IS IN ANOTHER BN !!! HOW TO LOCATE ???
                py_parents.append (getattr (self.owner, java_parents[i].get_name ()))
            self.parents = py_parents
            return self.parents
        elif name == 'children':
            # Attribute children doesn't exist yet, so create it.
            # After it's created, this code won't be executed again.
            java_children = self.java_variable.get_children ()
            py_children = []
            for i in range (len (java_children)):
                # FOLLOWING WON'T WORK IF CHILD IS IN ANOTHER BN !!! HOW TO LOCATE ???
                py_children.append (getattr (self.owner, java_children[i].get_name ()))
            self.children = py_children
            return self.children
        else:
            return getattr (self.java_variable, name)

class py_bn:
    def __init__ (self, java_bn):
        self.java_bn = java_bn
        self.name = java_bn.get_name ()
    def __str__ (self):
        return self.java_bn.format_string ('')
    def __getattr__ (self, name):
        if name == 'nodes':
            # Attribute nodes doesn't exist yet, so create it.
            # After it's created, this code won't be executed again.
            java_nodes = self.java_bn.get_variables ()
            py_nodes = []
            for i in range (len (java_nodes)):
                py_nodes.append (getattr (self, java_nodes[i].get_name ()))
            self.nodes = py_nodes
            return self.nodes
        else:
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

def parse_network (s, c):
    java_bn = c.parse_network (s)
    b = assign_bn_attributes (java_bn)
    return b
    
def assign_bn_attributes (java_bn):
    # HMM, MAYBE THIS WANTS TO MOVE INTO THE py_bn CONSTRUCTOR !!!
    b = py_bn (java_bn)
    l = b.java_bn.get_variables ()
    for i in range (len (l)):
        v = py_variable (l [i], b)
        s = v.java_variable.get_name ()
        setattr (b, s, v)
    return b

def import_bn (s):
    '''s is a belief network description string'''
    bn = parse_network (s, bn_context)
    import_bn_ref (bn)

def import_bn_ref (bn):
    import sys
    setattr (sys.modules['__main__'], bn.name, bn)

def import_bn_file (bn_filename):
    '''bn_filename is something like: /home/robert/belief-nets/random-polytree/random8.riso'''
    f = open (bn_filename)
    s = f.read ()
    import_bn (s)

def import_bn_remote (bn_name):
    import java.rmi.Naming
    remote_bn = java.rmi.Naming.lookup ('rmi://'+bn_name)
    bn = assign_bn_attributes (remote_bn)
    import_bn_ref (bn)
