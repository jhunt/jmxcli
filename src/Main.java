/*
  Copyright (c) 2016 James Hunt.  All Rights Reserved.

  This file is part of jmxcli.

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to
  deal in the Software without restriction, including without limitation the
  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
  sell copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software..

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
  IN THE SOFTWARE.
 */

package jmxcli;

import java.lang.System;
import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class Main {

	private static void dump(ObjectName mbean, String key, Object value) {
		if (value == null) {
			System.out.printf("%s\t%s\t~\n", mbean, key);
			return;
		}

		if (value instanceof Collection) {
			value = ((Collection)value).toArray();
		}

		if (value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++) {
				dump(mbean, key + "." + i, Array.get(value, i));
			}
			return;
		}

		if (value instanceof CompositeData) {
			CompositeData data = (CompositeData)value;
			for (String k : data.getCompositeType().keySet()) {
				dump(mbean, key + "." + k, data.get(k));
			}
			return;
		}

		if (value instanceof TabularData) {
			TabularData data = (TabularData)value;

			List<String> keys = data.getTabularType().getIndexNames();
			for (Object k : data.keySet()) {
				Object[] values = ((List<?>)k).toArray();
				for (int i = 0; i < keys.size(); i++) {
					dump(mbean, key + "." + keys.get(i), values[i]);
				}
			}
			return;
		}

		if (value instanceof Map) {
			for (Entry<Object,Object> e : ((Map<Object,Object>)value).entrySet()) {
				dump(mbean, key + "." + e.getKey(), e.getValue());
			}
			return;
		}

		System.out.printf("%s\t%s\t%s\n", mbean, key, value);
	}

	public static void main(final String[] args) throws Exception {
		String HOST   = "";
		String PORT   = "9999";
		String FILTER = "java.lang:*";
		String USER   = null;
		String PASS   = null;

		int i;
		for (i = 0; i < args.length; i++) {
			if (args[i].equals("-h") || args[i].equals("--host")) {
				HOST = args[++i];
				continue;
			}

			if (args[i].equals("-p") || args[i].equals("--port")) {
				PORT = args[++i];
				continue;
			}

			if (args[i].equals("-u") || args[i].equals("--user")) {
				USER = args[++i];
				continue;
			}

			if (args[i].equals("-p") || args[i].equals("--pass")) {
				PASS = args[++i];
				continue;
			}

			if (args[i].charAt(0) != '-') {
				FILTER = args[i];
			}
		}

		HashMap env = null;
		if (USER == null) {
			System.err.printf("connecting to host %s, port %s filter '%s'\n", HOST, PORT, FILTER);
		} else {
			if (PASS == null) {
				System.err.printf("--pass required for --user!\n");
				System.exit(1);
			}
			System.err.printf("connecting to host %s, port %s filter '%s' as %s\n", HOST, PORT, FILTER, USER);

			env = new HashMap();
			String[] creds = new String[] {USER, PASS};
			env.put(JMXConnector.CREDENTIALS, creds);
		}
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + HOST + ":" + PORT + "/jmxrmi");
		JMXConnector conn = JMXConnectorFactory.connect(url, env);
		MBeanServerConnection mbsc = conn.getMBeanServerConnection();

		for (ObjectName mbean : mbsc.queryNames(new ObjectName(FILTER), null)) {
			for (MBeanAttributeInfo attr : mbsc.getMBeanInfo(mbean).getAttributes()) {
				Object value = null;
				try {
					value = mbsc.getAttribute(mbean, attr.getName());
				} catch (Exception x) {
					value = x;
				}
				dump(mbean, attr.getName(), value);
			}
		}
	}
}
