
package org.etsi.uri._02231.v2_1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TSPInformationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TSPInformationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="TSPName" type="{http://uri.etsi.org/02231/v2.1.1#}InternationalNamesType"/>
 *         &lt;element name="TSPTradeName" type="{http://uri.etsi.org/02231/v2.1.1#}InternationalNamesType" minOccurs="0"/>
 *         &lt;element name="TSPAddress" type="{http://uri.etsi.org/02231/v2.1.1#}AddressType"/>
 *         &lt;element name="TSPInformationURI" type="{http://uri.etsi.org/02231/v2.1.1#}NonEmptyMultiLangURIListType"/>
 *         &lt;element name="TSPInformationExtensions" type="{http://uri.etsi.org/02231/v2.1.1#}ExtensionsListType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TSPInformationType", propOrder = {
    "tspName",
    "tspTradeName",
    "tspAddress",
    "tspInformationURI",
    "tspInformationExtensions"
})
public class TSPInformationType {

    @XmlElement(name = "TSPName", required = true)
    protected InternationalNamesType tspName;
    @XmlElement(name = "TSPTradeName")
    protected InternationalNamesType tspTradeName;
    @XmlElement(name = "TSPAddress", required = true)
    protected AddressType tspAddress;
    @XmlElement(name = "TSPInformationURI", required = true)
    protected NonEmptyMultiLangURIListType tspInformationURI;
    @XmlElement(name = "TSPInformationExtensions")
    protected ExtensionsListType tspInformationExtensions;

    /**
     * Gets the value of the tspName property.
     * 
     * @return
     *     possible object is
     *     {@link InternationalNamesType }
     *     
     */
    public InternationalNamesType getTSPName() {
        return tspName;
    }

    /**
     * Sets the value of the tspName property.
     * 
     * @param value
     *     allowed object is
     *     {@link InternationalNamesType }
     *     
     */
    public void setTSPName(InternationalNamesType value) {
        this.tspName = value;
    }

    /**
     * Gets the value of the tspTradeName property.
     * 
     * @return
     *     possible object is
     *     {@link InternationalNamesType }
     *     
     */
    public InternationalNamesType getTSPTradeName() {
        return tspTradeName;
    }

    /**
     * Sets the value of the tspTradeName property.
     * 
     * @param value
     *     allowed object is
     *     {@link InternationalNamesType }
     *     
     */
    public void setTSPTradeName(InternationalNamesType value) {
        this.tspTradeName = value;
    }

    /**
     * Gets the value of the tspAddress property.
     * 
     * @return
     *     possible object is
     *     {@link AddressType }
     *     
     */
    public AddressType getTSPAddress() {
        return tspAddress;
    }

    /**
     * Sets the value of the tspAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link AddressType }
     *     
     */
    public void setTSPAddress(AddressType value) {
        this.tspAddress = value;
    }

    /**
     * Gets the value of the tspInformationURI property.
     * 
     * @return
     *     possible object is
     *     {@link NonEmptyMultiLangURIListType }
     *     
     */
    public NonEmptyMultiLangURIListType getTSPInformationURI() {
        return tspInformationURI;
    }

    /**
     * Sets the value of the tspInformationURI property.
     * 
     * @param value
     *     allowed object is
     *     {@link NonEmptyMultiLangURIListType }
     *     
     */
    public void setTSPInformationURI(NonEmptyMultiLangURIListType value) {
        this.tspInformationURI = value;
    }

    /**
     * Gets the value of the tspInformationExtensions property.
     * 
     * @return
     *     possible object is
     *     {@link ExtensionsListType }
     *     
     */
    public ExtensionsListType getTSPInformationExtensions() {
        return tspInformationExtensions;
    }

    /**
     * Sets the value of the tspInformationExtensions property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtensionsListType }
     *     
     */
    public void setTSPInformationExtensions(ExtensionsListType value) {
        this.tspInformationExtensions = value;
    }

}
