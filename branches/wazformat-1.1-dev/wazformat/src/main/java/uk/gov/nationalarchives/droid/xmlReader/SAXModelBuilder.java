/*
 * � The National Archives 2005-2006.  All rights reserved.
 * See Licence.txt for full licence details.
 *
 * Developed by:
 * Tessella Support Services plc
 * 3 Vineyard Chambers
 * Abingdon, OX14 3PX
 * United Kingdom
 * http://www.tessella.com
 *
 * Tessella/NPD/4305
 * PRONOM 4
 *
 * SAXModelBuilder.java
 *
 * $Id: SAXModelBuilder.java,v 1.7 2006/03/13 15:15:29 linb Exp $
 *
 * $Log: SAXModelBuilder.java,v $
 * Revision 1.7  2006/03/13 15:15:29  linb
 * Changed copyright holder from Crown Copyright to The National Archives.
 * Added reference to licence.txt
 * Changed dates to 2005-2006
 *
 * Revision 1.6  2006/02/09 15:31:23  linb
 * Updates to javadoc and code following the code review
 *
 * Revision 1.5  2006/01/31 16:47:30  linb
 * Added log messages that were missing due to the log keyword being added too late
 *
 * Revision 1.4  2006/01/31 16:21:20  linb
 * Removed the dollars from the log lines generated by the previous message, so as not to cause problems with subsequent commits
 *
 * Revision 1.3  2006/01/31 16:19:07  linb
 * Added Log and Id tags to these files
 *
 * Revision 1.2  2006/01/31 16:11:37  linb
 * Add support for XML namespaces to:
 * 1) The reading of the config file, spec file and file-list file
 * 2) The writing of the config file and file-list file
 * - The namespaces still need to be set to their proper URIs (currently set to example.com...)
 * - Can still read in files without namespaces*
 *
 */
package uk.gov.nationalarchives.droid.xmlReader;

import java.lang.reflect.Method;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import uk.gov.nationalarchives.droid.MessageDisplay;



/**
 * reads and parses data from an XML file
 *
 * @version 4.0.0
 */
public class SAXModelBuilder extends DefaultHandler {
    Stack stack = new Stack();
    SimpleElement element;
    String myObjectPackage = "uk.gov.nationalarchives.droid.signatureFile";
    StringBuffer textBuffer;
    String namespace = "";
    boolean useNamespace = false;
    boolean allowGlobalNamespace = true;


    public void setObjectPackage(String theObjectPackage) {
        myObjectPackage = theObjectPackage;
    }

    /**
     * Set up XML namespace handling.
     * <p/>
     * <p>If <code>allowGlobalNamespace</code> is set to <code>true</code>, elements
     * that do not have a namespace specified are parsed; attributes that don't
     * have a namespace specified are parsed.  If it is <code>false</code>, for
     * it to be parsed, an element must have a namespace specifed (by default or
     * with a prefix); an attribute must have a namespace specified with a prefix.
     *
     * @param namespace            the XML namespace to use
     * @param allowGlobalNamespace allow the parser to recognise elements/ attributes that aren't in any namespace
     */
    public void setupNamespace(String namespace, boolean allowGlobalNamespace) {
        if (namespace == null) {
            throw new IllegalArgumentException("Namespace cannot be null");
        }

        this.namespace = namespace;
        this.useNamespace = true;
        this.allowGlobalNamespace = allowGlobalNamespace;

    }

    /**
     * Handle names in a namespace-aware fashion.
     * <p/>
     * <p>If an element/ attribute is in a namespace, qname is not required to be set.
     * We must, therefore, use the localname if the namespace is set, and qname if it isn't.
     *
     * @param namespace the namespace uri
     * @param localname the local part of the name
     * @param qname     a qualified name
     * @return the local part or the qualified name, as appropriate
     */
    private String handleNameNS(String namespace, String localname, String qname) {
        if (this.useNamespace && this.namespace.equals(namespace)) {
            // Name is in the specified namespace
            return localname;
        } else if (this.allowGlobalNamespace && "".equals(namespace)) {
            // Name is in the global namespace
            return qname;
        } else {
            // Ignore 
            return null;
        }
    }

    public void startElement(String namespace, String localname, String qname, Attributes atts)
            throws SAXException {
        String elementName = handleNameNS(namespace, localname, qname);
        if (elementName == null) {
            return;
        }
        SimpleElement element = null;
        try {
            element = (SimpleElement) Class.forName(myObjectPackage + "." + elementName).newInstance();
        } catch (Exception e) {
        }
        if (element == null) {
            element = new SimpleElement();
        }
        for (int i = 0; i < atts.getLength(); i++) {
            String attributeName = handleNameNS(atts.getURI(i), atts.getLocalName(i), atts.getQName(i));
            if (attributeName == null) {
                continue;
            }
            element.setAttributeValue(attributeName, atts.getValue(i));
        }
        stack.push(element);
    }

    public void endElement(String namespace, String localname, String qname)
            throws SAXException {
        String elementName = handleNameNS(namespace, localname, qname);
        if (elementName == null) {
            return;
        }
        element = (SimpleElement) stack.pop();
        element.completeElementContent();
        if (!stack.empty()) {
            try {
                setProperty(elementName, stack.peek(), element);
            } catch (SAXException e) {
                throw new SAXException(e);
            }
        }
    }

    public void characters(char[] ch, int start, int len) {
        if (!stack.empty()) { // Ignore character data if we don't have an element to put it in.
            String text = new String(ch, start, len);
            ((SimpleElement) (stack.peek())).setText(text);
        }
    }

    void setProperty(String name, Object target, Object value) throws SAXException {
        Method method = null;
        try {
            method = target.getClass().getMethod(
                    "add" + name, new Class[]{value.getClass()});
        } catch (NoSuchMethodException e) {
        }
        if (method == null) {
            try {
                method = target.getClass().getMethod(
                        "set" + name, new Class[]{value.getClass()});
            } catch (NoSuchMethodException e) {
            }
        }
        if (method == null) {
            try {
                value = ((SimpleElement) value).getText();
                method = target.getClass().getMethod(
                        "add" + name, new Class[]{String.class});
            } catch (NoSuchMethodException e) {
            }
        }
        try {
            if (method == null) {
                method = target.getClass().getMethod(
                        "set" + name, new Class[]{String.class});
            }
            method.invoke(target, new Object[]{value});
        } catch (NoSuchMethodException e) {
            MessageDisplay.unknownElementWarning(name, ((SimpleElement) target).getElementName());
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    public SimpleElement getModel() {
        return element;
    }

}
