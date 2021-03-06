/**
 *
 * COPYRIGHT (C) 2010, 2011, 2012, 2013, 2014 AGETO Innovation GmbH
 *
 * Authors Christian Kahlo, Ralf Wondratschek
 *
 * All Rights Reserved.
 *
 * Contact: PersoApp, http://www.persoapp.de
 *
 * @version 1.0, 30.07.2013 13:50:47
 *
 *          This file is part of PersoApp.
 *
 *          PersoApp is free software: you can redistribute it and/or modify it
 *          under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation, either version 3 of the
 *          License, or (at your option) any later version.
 *
 *          PersoApp is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details.
 *
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with PersoApp. If not, see
 *          <http://www.gnu.org/licenses/>.
 *
 *          Diese Datei ist Teil von PersoApp.
 *
 *          PersoApp ist Freie Software: Sie können es unter den Bedingungen der
 *          GNU Lesser General Public License, wie von der Free Software
 *          Foundation, Version 3 der Lizenz oder (nach Ihrer Option) jeder
 *          späteren veröffentlichten Version, weiterverbreiten und/oder
 *          modifizieren.
 *
 *          PersoApp wird in der Hoffnung, dass es nützlich sein wird, aber OHNE
 *          JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 *          Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN
 *          ZWECK. Siehe die GNU Lesser General Public License für weitere
 *          Details.
 *
 *          Sie sollten eine Kopie der GNU Lesser General Public License
 *          zusammen mit diesem Programm erhalten haben. Wenn nicht, siehe
 *          <http://www.gnu.org/licenses/>.
 *
 */

/**
 * XXX: Most code is originally part of the Cryptix lib as
 * cryptix.util.core.Hex. Additionally most code is part of the GNU classpath
 * library within gnu.java.security.util.Util.
 * 
 * Below is the original copyright message.
 * 
 */

/**
 * Static methods for converting to and from hexadecimal strings.
 * <p>
 * 
 * <b>Copyright</b> &copy; 1995-1997 <a
 * href="http://www.systemics.com/">Systemics Ltd</a> on behalf of the <a
 * href="http://www.systemics.com/docs/cryptix/">Cryptix Development Team</a>. <br>
 * All rights reserved.
 * 
 * <p>
 * <b>$Revision: 1.2 $</b>
 * 
 * @author David Hopwood
 * @author Raif Naffah
 * @author Systemics Ltd
 * @since Cryptix 2.2.0a, 2.2.2
 */
package de.persoapp.core.util;

/**
 * Static functions for converting to and from hexadecimal strings.
 * 
 * @author Christian Kahlo
 * @author Rico Klimsa - added javadoc comments.
 */
public class Hex {
	
	/**
	 * Utility functions. Constructor is not used.
	 */
	private Hex() {
	}

	/**
	 * A char array of the common hexadecimal digits.
	 */
	private static final char[]	hexDigits	= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
			'E', 'F'						};

	/**
	 * <p>
	 * Converts the contents of the given byte buffer in an hexadecimal string.
	 * The length of the returned string is 2 * length.
	 * </p>
	 * <p>
	 * If offset and length are null, the whole array is used.
	 * </p>
	 * 
	 * @param ba
	 *            - The byte buffer to convert.
	 * @param offset
	 *            - The starting offset.
	 * @param length
	 *            - The number of characters of the byte array, which will be
	 *            converted.
	 * 
	 * @return The string which contains the hexadecimal digits. The length of
	 *         the returned string is 2 * length.
	 */
	public static String toString(final byte[] ba, final int offset, final int length) {
		final char[] buf = new char[length * 2];
		int j = 0;
		int k;

		for (int i = offset; i < offset + length; i++) {
			k = ba[i];
			buf[j++] = hexDigits[k >>> 4 & 0x0F];
			buf[j++] = hexDigits[k & 0x0F];
		}
		return new String(buf);
	}

	/**
	 * Converts the contents of the given byte buffer in an hexadecimal string.
	 * 
	 * @param ba
	 *            - The byte buffer, to convert.
	 * 
	 * @return The created String. The length of the returned string is twice
	 *         the length of the inserted byte array.
	 */
	public static String toString(final byte[] ba) {
		return toString(ba, 0, ba.length);
	}

	/**
	 * <p>
	 * Returns a byte array from a string of hexadecimal digits. Two hexadecimal
	 * digits will be one byte and thus the returned byte array is half of the
	 * size of the inserted string.
	 * </p>
	 * 
	 * @param hex
	 *            - The {@link String}, to convert.
	 * @return The byte array with the hexadecimal digits. The returned byte
	 *         array is half of the size of the inserted string.
	 */
	public static byte[] fromString(final String hex) {
		final int len = hex.length();
		final byte[] buf = new byte[(len + 1) / 2];

		int i = 0, j = 0;
		if (len % 2 == 1) {
			buf[j++] = (byte) fromDigit(hex.charAt(i++));
		}

		while (i < len) {
			buf[j++] = (byte) (fromDigit(hex.charAt(i++)) << 4 | fromDigit(hex.charAt(i++)));
		}
		return buf;
	}

	/**
	 * Decodes the given hex digit into an decimal integer.
	 * 
	 * @param ch
	 *            - The hex digit, to decode.
	 * @return Returns the decimal value.
	 * 
	 * @throws IllegalArgumentException
	 *             If the given hex digit is invalid.
	 */
	public static int fromDigit(final char ch) {
		if (ch >= '0' && ch <= '9') {
			return ch - '0';
		}
		if (ch >= 'A' && ch <= 'F') {
			return ch - 'A' + 10;
		}
		if (ch >= 'a' && ch <= 'f') {
			return ch - 'a' + 10;
		}

		throw new IllegalArgumentException("invalid hex digit '" + ch + "'");
	}

	/**
	 * Encodes the given value as a hex string.
	 * 
	 * @param n
	 *            - The value, to encode
	 * 
	 * @return Returns the hexadecimal value.
	 */
	public static String byteToString(final int n) {
		final char[] buf = { hexDigits[n >>> 4 & 0x0F], hexDigits[n & 0x0F] };
		return new String(buf);
	}

	/**
	 * Encodes the given value as a hex string.
	 * 
	 * @param n
	 *            - The value, to encode
	 * 
	 * @return Returns the hexadecimal value.
	 */
	public static String shortToString(final int n) {
		final char[] buf = { hexDigits[n >>> 12 & 0x0F], hexDigits[n >>> 8 & 0x0F], hexDigits[n >>> 4 & 0x0F],
				hexDigits[n & 0x0F] };
		return new String(buf);
	}

}
