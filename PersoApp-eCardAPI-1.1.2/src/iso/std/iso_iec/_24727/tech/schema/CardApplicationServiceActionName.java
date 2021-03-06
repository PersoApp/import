
package iso.std.iso_iec._24727.tech.schema;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CardApplicationServiceActionName.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CardApplicationServiceActionName">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CardApplicationList"/>
 *     &lt;enumeration value="CardApplicationCreate"/>
 *     &lt;enumeration value="CardApplicationDelete"/>
 *     &lt;enumeration value="CardApplicationServiceList"/>
 *     &lt;enumeration value="CardApplicationServiceCreate"/>
 *     &lt;enumeration value="CardApplicationServiceLoad"/>
 *     &lt;enumeration value="CardApplicationServiceDelete"/>
 *     &lt;enumeration value="CardApplicationServiceDescribe"/>
 *     &lt;enumeration value="ExecuteAction"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CardApplicationServiceActionName")
@XmlEnum
public enum CardApplicationServiceActionName {

    @XmlEnumValue("CardApplicationList")
    CARD_APPLICATION_LIST("CardApplicationList"),
    @XmlEnumValue("CardApplicationCreate")
    CARD_APPLICATION_CREATE("CardApplicationCreate"),
    @XmlEnumValue("CardApplicationDelete")
    CARD_APPLICATION_DELETE("CardApplicationDelete"),
    @XmlEnumValue("CardApplicationServiceList")
    CARD_APPLICATION_SERVICE_LIST("CardApplicationServiceList"),
    @XmlEnumValue("CardApplicationServiceCreate")
    CARD_APPLICATION_SERVICE_CREATE("CardApplicationServiceCreate"),
    @XmlEnumValue("CardApplicationServiceLoad")
    CARD_APPLICATION_SERVICE_LOAD("CardApplicationServiceLoad"),
    @XmlEnumValue("CardApplicationServiceDelete")
    CARD_APPLICATION_SERVICE_DELETE("CardApplicationServiceDelete"),
    @XmlEnumValue("CardApplicationServiceDescribe")
    CARD_APPLICATION_SERVICE_DESCRIBE("CardApplicationServiceDescribe"),
    @XmlEnumValue("ExecuteAction")
    EXECUTE_ACTION("ExecuteAction");
    private final String value;

    CardApplicationServiceActionName(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CardApplicationServiceActionName fromValue(String v) {
        for (CardApplicationServiceActionName c: CardApplicationServiceActionName.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
