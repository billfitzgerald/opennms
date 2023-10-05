#!/bin/bash

source /usr/share/opennms/etc/confd.custom.cfg

if [[ ${ENABLE_SYSLOGD} == "true" ]]; then
  echo "Enabling Syslogd..."
  sed -i 'N;s/service.*\n\(.*Syslogd\)/service enabled="true">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
if [[ ${ENABLE_SYSLOGD} == "false" ]]; then
  echo "Disabling Syslogd..."
  sed -i 'N;s/service.*\n\(.*Syslogd\)/service enabled="false">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi


if [[ ${ENABLE_SNMPPOLLER} == "true" ]]; then
  echo "Enabling SNMP Interface Poller..."
  sed -i 'N;s/service.*\n\(.*SnmpPoller\)/service enabled="true">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
if [[ ${ENABLE_SNMPPOLLER} == "false" ]]; then
  echo "Disabling SNMP Interface Poller..."
  sed -i 'N;s/service.*\n\(.*SnmpPoller\)/service enabled="false">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi

if [[ ${ENABLE_TELEMETRYD} == "true" ]]; then
  echo "Enabling Telemetryd..."
  sed -i 'N;s/service.*\n\(.*Telemetryd\)/service enabled="true">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
if [[ ${ENABLE_TELEMETRYD} == "false" ]]; then
  echo "Disabling Telemetryd..."
  sed -i 'N;s/service.*\n\(.*Telemetryd\)/service enabled="false">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi

if [[ ${ENABLE_CORRELATOR} == "true" ]]; then
  echo "Enabling Event Correlator..."
  sed -i 'N;s/service.*\n\(.*Correlator\)/service enabled="true">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
if [[ ${ENABLE_CORRELATOR} == "false" ]]; then
  echo "Disabling Event Correlator..."
  sed -i 'N;s/service.*\n\(.*Correlator\)/service enabled="false">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi

if [[ ${ENABLE_TICKETER} == "true" ]]; then
  echo "Enabling Ticketer..."
  sed -i 'N;s/service.*\n\(.*Ticketer\)/service enabled="true">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
if [[ ${ENABLE_TICKETER} == "false" ]]; then
  echo "Disabling Ticketer..."
  sed -i 'N;s/service.*\n\(.*Ticketer\)/service enabled="false">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi

if [[ ${ENABLE_TRAPD} == "true" ]]; then
  echo "Enabling Trapd..."
  sed -i 'N;s/service.*\n\(.*Trapd\)/service enabled="true">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
if [[ ${ENABLE_TRAPD} == "false" ]]; then
  echo "Disabling Trapd..."
  sed -i 'N;s/service.*\n\(.*Trapd\)/service enabled="false">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi

if [[ ${ENABLE_ENLINKD} == "true" ]]; then
  echo "Enabling Enlinkd..."
  sed -i 'N;s/service.*\n\(.*EnhancedLinkd\)/service enabled="true">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
if [[ ${ENABLE_ENLINKD} == "false" ]]; then
  echo "Disabling Enlinkd..."
  sed -i 'N;s/service.*\n\(.*EnhancedLinkd\)/service enabled="false">\n\1/;P;D' /usr/share/opennms/etc/service-configuration.xml
fi
