package de.aitools.aq.geolocating.jaxb;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class Jaxbs {
  
  private Jaxbs() {}
  
  public static <T> String toString(final T object)
  throws JAXBException {
    return Jaxbs.toString(object,
        object.getClass().getSimpleName().toLowerCase());
  }
  
  public static <T> String toString(final T object, final String qname)
  throws JAXBException {
    return Jaxbs.toString(object, new QName(qname));
  }
  
  public static <T> String toString(final T object, final QName qname)
  throws JAXBException {
    final StringWriter stringWriter = new StringWriter();
    try {
      @SuppressWarnings("unchecked")
      final Class<T> classType = (Class<T>) object.getClass();
      final XMLStreamWriter writer = XMLOutputFactory.newFactory()
          .createXMLStreamWriter(stringWriter);
      final JAXBContext context = JAXBContext.newInstance(object.getClass());
      final Marshaller marshaller = context.createMarshaller();

      final JAXBElement<T> jaxbElement =
          new JAXBElement<T>(qname, classType, null, object);
      marshaller.marshal(jaxbElement, writer);
      return stringWriter.toString();
    } catch (final XMLStreamException | FactoryConfigurationError e) {
      // Should not be possible
      throw new RuntimeException(e);
    }
    
  }

}
