%define prefix   /usr/local

Summary:	RISO, an implementation of distributed belief networks
Name:		riso
Version:	20040607
Release:	1
License:	GPL
Group:		Mathematics/Probability
Buildroot:	/tmp/%{name}-%{version}
Source:     %{name}-%{version}.src.tar.gz
BuildArch:  noarch
URL:        http://sourceforge.net/projects/riso/

%description
RISO is an implementation of distributed belief networks in Java.
The binary rpm contains an archive (jar) containing the compiled class
files plus one Python file (the RISO binding for Jython). The class
files were produced by the IBM Java2 version 1.3 compiler (javac)
for Linux; the results should be compatible with other Java2 
implementations. The source rpm contains all the Java and Python 
source code, along with supplementary files such as a makefile.

%prep
%setup -q -n riso

%build
export CLASSDIR=$RPM_BUILD_ROOT/java
sh compile.sh

%install
pushd $RPM_BUILD_ROOT/java
jar cvf %{name}.jar `find . -name \*.class`
mkdir -p $RPM_BUILD_ROOT%{prefix}/%{name}
install %{name}.jar $RPM_BUILD_ROOT%{prefix}/%{name}
popd
install riso_binding.py $RPM_BUILD_ROOT%{prefix}/%{name}

%clean
rm -rf $RPM_BUILD_ROOT

%files 

%{prefix}/%{name}/%{name}.jar
%{prefix}/%{name}/riso_binding.py

%changelog
* Mon Jun 07 2004 Robert Dodier <robert_dodier@yahoo.com>

Updated the description a little bit, and added riso_binding.py
to the list of files for the binary rpm.

* Wed Jul 10 2002 Robert Dodier <robert_dodier@yahoo.com>

First attempt to construct rpm file.
Contains only riso.jar.

