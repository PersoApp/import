
package iso.std.iso_iec._24727.tech.schema;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PasswordTypeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="PasswordTypeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="bcd"/>
 *     &lt;enumeration value="ascii-numeric"/>
 *     &lt;enumeration value="utf8"/>
 *     &lt;enumeration value="half-nibble-bcd"/>
 *     &lt;enumeration value="iso9564-1"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "PasswordTypeType")
@XmlEnum
public enum PasswordTypeType {

    @XmlEnumValue("bcd")
    BCD("bcd"),
    @XmlEnumValue("ascii-numeric")
    ASCII_NUMERIC("ascii-numeric"),
    @XmlEnumValue("utf8")
    UTF_8("utf8"),
    @XmlEnumValue("half-nibble-bcd")
    HALF_NIBBLE_BCD("half-nibble-bcd"),
    @XmlEnumValue("iso9564-1")
    ISO_9564_1("iso9564-1");
    private final String value;

    PasswordTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static PasswordTypeType fromValue(String v) {
        for (PasswordTypeType c: PasswordTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
