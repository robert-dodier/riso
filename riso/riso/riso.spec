%define prefix   /usr/local

Summary:	RISO, an implementation of distributed belief networks
Name:		riso
Version:	20061015
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
files plus one Python file (the RISO binding for Jython).
The source rpm contains all the Java and Python source code,
along with supplementary files such as a makefile.

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
install ACM-LICENSE.html $RPM_BUILD_ROOT%{prefix}/%{name}
install GPL-LICENSE.txt $RPM_BUILD_ROOT%{prefix}/%{name}
install LICENSE $RPM_BUILD_ROOT%{prefix}/%{name}

# REMOVE COMPILED CLASS FILES TO AVOID "UNPACKAGED FILES" ERROR
rm -rf $RPM_BUILD_ROOT/java

%clean
rm -rf $RPM_BUILD_ROOT

%files 

%{prefix}/%{name}/%{name}.jar
%{prefix}/%{name}/riso_binding.py
%{prefix}/%{name}/ACM-LICENSE.html
%{prefix}/%{name}/GPL-LICENSE.txt
%{prefix}/%{name}/LICENSE
