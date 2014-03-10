%define prefix   /usr/local

Summary:	RISO, an implementation of distributed belief networks
Name:		riso
Version:	20061015
Release:	2
License:	GPL
Group:		Mathematics/Probability
Buildroot:	/tmp/%{name}-%{version}
Source:     %{name}-%{version}.src.tar.gz
BuildArch:  noarch
URL:        http://sourceforge.net/projects/riso/

%description
RISO is an implementation of distributed belief networks in Java.
The binary rpm contains the compiled class files plus one Python file
(the RISO binding for Jython).
The source rpm contains all the Java and Python source code,
along with supplementary files such as a makefile.
RISO does not require any other packages to be installed;
Jython is required to use the Jython binding, but in its
absence a pure-Java interface (riso.apps.RemoteQuery) can be used.

%prep
%setup -q -n riso

%build
export CLASSDIR=$RPM_BUILD_ROOT/classes
sh compile.sh

%install
mkdir -p $RPM_BUILD_ROOT%{prefix}/%{name}
mv $RPM_BUILD_ROOT/classes $RPM_BUILD_ROOT%{prefix}/%{name}

install riso_binding.py $RPM_BUILD_ROOT%{prefix}/%{name}
install ACM-LICENSE.html $RPM_BUILD_ROOT%{prefix}/%{name}
install GPL-LICENSE.txt $RPM_BUILD_ROOT%{prefix}/%{name}
install LICENSE $RPM_BUILD_ROOT%{prefix}/%{name}

%clean
rm -rf $RPM_BUILD_ROOT

%files 

%{prefix}/%{name}/riso_binding.py
%{prefix}/%{name}/ACM-LICENSE.html
%{prefix}/%{name}/GPL-LICENSE.txt
%{prefix}/%{name}/LICENSE
%{prefix}/%{name}/classes
