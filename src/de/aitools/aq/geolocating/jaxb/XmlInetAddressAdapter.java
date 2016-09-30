package de.aitools.aq.geolocating.jaxb;

import java.net.InetAddress;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlInetAddressAdapter
extends XmlAdapter<String, InetAddress> {

  @Override
  public InetAddress unmarshal(final String address)
  throws Exception {
    return InetAddress.getByName(address);
  }

  @Override
  public String marshal(final InetAddress address)
  throws Exception {
    return address.getHostAddress();
  }

}
