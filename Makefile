PREFIX ?= /opt/jmxcli

default:
	@echo "try `make install' to install jmxcli into /opt/jmxcli"
	@echo "or, `make PREFIX=/usr/local install' etc."

build: jmxcli.jar

jmxcli.jar: src/Main.java build.xml
	chmod 0600 passwd
	ant

install:
	mkdir -p      $(PREFIX)/bin
	cp bin/jmxcli $(PREFIX)/bin/jmxcli
	chmod 0755    $(PREFIX)/bin/jmxcli

	mkdir -p      $(PREFIX)/lib
	cp jmxcli.jar $(PREFIX)/lib/jmxcli.jar
	chmod 0644    $(PREFIX)/lib/jmxcli.jar
