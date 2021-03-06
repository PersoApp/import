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
package de.persoapp.core.card;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;

import de.persoapp.core.client.IMainView;
import de.persoapp.core.client.PropertyResolver;
import de.persoapp.core.client.SecureHolder;
import de.persoapp.core.util.ArrayTool;
import de.persoapp.core.util.Hex;
import de.persoapp.core.util.TLV;

/**
 * <p>
 * <tt>CardHandler</tt> is an implementation of {@link ICardHandler} for the
 * German national eID card.
 * </p>
 * 
 * @author Christian Kahlo
 * @author Rico Klimsa - added javadoc comments.
 */
public class CardHandler implements ICardHandler {

	/**
	 * The default cryptographic mechanism to be used while initiating the
	 * <em>PACE</em> protocol. TBD: make this dynamic by parsing
	 * EF.CardSecurity.
	 * 
	 */
	private static final String				PACE_AES128CBC	= "04007F00070202040202";

	/**
	 * Local instance of core message bundle for localized output.
	 */
	private final PropertyResolver.Bundle	textBundle		= PropertyResolver.getBundle("text_core");

	/**
	 * The certificate holder reference ("subject") of the last verified CVC.
	 */
	private byte[]							lastCertSubject;

	/**
	 * ephemeral key for terminal authentication
	 **/
	private byte[]							TAKey;

	/**
	 * Certificate Authority References of the PKIs known by the ECard. Usually
	 * the current root and its predecessor.
	 */
	private List<byte[]>					CAReferences;

	/**
	 * raw contents of EF.CardAccess. <br/>
	 * This file contains:
	 * <ul>
	 * <li>PACEInfo</li>
	 * <li>ChipAuthenticationInfo</li>
	 * <li>ChipAuthenticationDomainParameterInfo</li>
	 * <li>PrivilegedTerminalInfo</li>
	 * <li>TerminalAuthenticationInfo</li>
	 * <li>CardInfoLocator</li>
	 * </ul>
	 */
	private byte[]							EFCardAccess;

	/**
	 * IDPICC-value calculated while processing PACE (compressed base point of
	 * second ECDHE)
	 */
	private byte[]							IDPICC;

	/**
	 * (shadow) plain transport provider (direct IFD access)
	 * */
	private TransportProvider				tp0;

	/**
	 * current transport provider in use (logical access, i.e. with
	 * ISOSMTransport)
	 * */
	private TransportProvider				tp;

	/**
	 * Indicates if the card handler is initialized.
	 */
	private boolean							initialized		= false;

	/**
	 * Instance of main GUI for user interaction.
	 */
	private final IMainView					mainView;

	/**
	 * This is the default curve if the field is not present in EFCardAccess, or
	 * EFCardAccess doesn't exist at all -> brainpoolP256r1.
	 */
	private int								PACEv2_curveID	= 13;

	/**
	 * Create and initialize the {@link CardHandler} with the {@link IMainView}
	 * instance of the applications GUI.
	 * 
	 * @param mainView
	 *            - {@link IMainView} instance of the GUI
	 */
	public CardHandler(final IMainView mainView) {
		this.mainView = mainView;
	}

	/**
	 * Logs the given <tt>message</tt> to the console.
	 * 
	 * @param msg
	 *            - The given <tt>message</tt>.
	 */
	public void log(final String msg) {
		System.out.println(msg);
	}

	/**
	 * Builds an <em>APDU</em> from instruction class, instruction command,
	 * parameters and data. See ISO 7816-3 for details.
	 * 
	 * @param cla
	 *            - instruction class byte
	 * @param ins
	 *            - instruction command byte
	 * @param p1
	 *            - instruction parameter P1
	 * @param p2
	 *            - instruction parameter P2
	 * @param data
	 *            - data to be transferred
	 * @param le
	 *            - The expected length of the <em>APDU</em>-Response.
	 * 
	 * @return <em>APDU</em> to be sent to card
	 */
	private byte[] buildCmd(final byte cla, final byte ins, final byte p1, final byte p2, final byte[] data,
			final int le) {
		if (data == null) {
			if (le < 0) {
				return new byte[] { cla, ins, p1, p2, 0, 0, 0 };
			} else {
				return new byte[] { cla, ins, p1, p2, 0, 0, 0, (byte) (le >> 8 & 0xFF), (byte) (le & 0xFF) };
			}
		} else {
			if (le < 0) {
				return ArrayTool.arrayconcat(new byte[] { cla, ins, p1, p2, 0, (byte) (data.length >> 8 & 0xFF),
						(byte) (data.length & 0xFF) }, data);
			} else {
				return ArrayTool.arrayconcat(
						ArrayTool.arrayconcat(new byte[] { cla, ins, p1, p2, 0, (byte) (data.length >> 8 & 0xFF),
								(byte) (data.length & 0xFF) }, data), new byte[] { (byte) (le >> 8 & 0xFF),
								(byte) (le & 0xFF) });

			}
		}
	}

	/**
	 * Manage Security Environment for authentication terminal or signature
	 * terminal (CAN, empty CHAT)
	 * 
	 * @param tp
	 *            - {@link TransportProvider} to be used
	 * @param cryptoMechanism
	 *            - the cryptographic mechanism to be set for the next
	 *            authentication step as hex string encoded OID
	 * @param keyReference
	 *            - reference ID of the key to be used in conjunction with the
	 *            cryptographic algorithm
	 * @param CHAT
	 *            - <em>Card Holder Authorization Template</em> to be used in
	 *            case of an authentication terminal
	 * @return the status word of the command
	 */
	private int setMSE_AT(final TransportProvider tp, final String cryptoMechanism, final byte keyReference,
			final byte[] CHAT) {
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(TLV.build(0x80, Hex.fromString(cryptoMechanism)));
			baos.write(TLV.build(0x83, new byte[] { keyReference }));

			if (CHAT != null) {
				baos.write(CHAT);
			}

			final byte[] data = baos.toByteArray();

			tp.transmit(buildCmd((byte) 0x00, (byte) 0x22, (byte) 0xC1, (byte) 0xA4, data, -1));
		} catch (final IOException e2) {
			e2.printStackTrace();
		}
		return tp.lastSW();
	}

	/**
	 * General Authenticate to process <tt>PACE</tt> protocol steps. The
	 * cryptographic mechanism and key reference is already set by Manage
	 * Security Environment.
	 * 
	 * @param tp
	 *            - {@link TransportProvider} to be used
	 * @param authData
	 *            - The auth data encoded as <em>BER-TLV</em>.
	 * @param lastCommand
	 *            - <em>true</em> if this is the last command, <em>false</em> if
	 *            more commands follow
	 * @return the result of the executed General Authenticate / PACE step.
	 */

	private byte[] generalAUTH(final TransportProvider tp, byte[] authData, final boolean lastCommand) {
		if (authData == null) {
			authData = new byte[0];
		}

		// apply command chaining indication if not last command
		authData = tp.transmit(buildCmd(!lastCommand ? (byte) 0x10 : (byte) 0x00, (byte) 0x86, (byte) 0x00,
				(byte) 0x00, TLV.build((byte) 0x7C, authData), 0));

		// if there is no error extract embedded protocol response
		if (tp.lastSW() == 0x9000) {
			return TLV.get(authData, (byte) 0x7C);
		}
		return null;
	}

	/**
	 * Search for a supported card and return a TransportProvider corresponding
	 * to the communication channel.
	 * 
	 * @see de.persoapp.core.card.ICardHandler#getECard()
	 */
	@Override
	public TransportProvider getECard() {
		if (this.tp0 != null) {
			final CardChannel cc = (CardChannel) tp0.getParent();
			if (cc != null) {
				try {
					// send 00 00 00 00 command to card to test connection
					// exchange with DF AID_NPA for selection ...
					if (cc.transmit(new CommandAPDU(new byte[4])) != null) {
						return this.tp0;
					}

					// option #2: select MF, test pin state, but resets current card internal state
					//					this.tp0.transmit(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C, 0x02, 0x3F, 0x00 });
					//					if (setMSE_AT(this.tp0, PACE_AES128CBC, (byte) 0x03, null) != -1) {
					//						return this.tp0;
					//					}
				} catch (final CardException e) {
					this.tp0 = null;
					e.printStackTrace();
				}
			}
		}

		final TransportProvider tpNew = getHALTransport();

		if (tpNew != null) {
			this.tp0 = tpNew;

			// select ROOT / MF in case the card accepted selection of the application instead of replying
			// with SW6982
			tpNew.transmit(new byte[] { (byte) 0x00, (byte) 0xA4, (byte) 0x00, (byte) 0x00, (byte) 0x00 });

			final String manufacturer = new String(tpNew.transmit(new byte[] { (byte) 0xFF, (byte) 0x9A, 0x01, 0x01,
					0x00 }));
			final String product = new String(tpNew.transmit(new byte[] { (byte) 0xFF, (byte) 0x9A, 0x01, 0x03, 0x00 }));

			log("Manufacturer: " + manufacturer);
			log("Product: " + product);

			if (mainView != null) {
				if (manufacturer.length() > 0 && manufacturer.length() > 0) {
					mainView.showMessage(textBundle.get("CardHandler_card_in_device") + manufacturer + " " + product,
							IMainView.SUCCESS);
				} else {
					mainView.showMessage(textBundle.get("CardHandler_device_not_verified") + getCCID(tpNew).getName(),
							IMainView.WARNING);
				}
			}

			final byte[] EF_DIR = readFile(tpNew, (short) 0x2F00);
			if (EF_DIR != null) {
				System.out.println("EF_DIR: " + Hex.toString(EF_DIR));
				// System.out.println("EF_DIR: " + new String(EF_DIR)); - Reminder
				final List<byte[]> apps = TLV.getM(EF_DIR, (byte) 0x61);

				for (final byte[] app : apps) {
					final byte[] appAID = TLV.get(app, (byte) 0x4F);
					final byte[] appDesc = TLV.get(app, (byte) 0x50);
					// byte[] appUnk = TLV.get(app, (byte)0x51); - Reminder
					final byte[] appLink = TLV.get(app, (byte) 0x73);
					final byte[] appLinkDest = TLV.get(appLink, (byte) 0x4F);

					System.out.println("APP AID = " + Hex.toString(appAID) + " / "
							+ (appDesc == null ? "no name" : new String(appDesc))
							+ (appLinkDest == null ? "" : " -> " + Hex.toString(appLinkDest)));
				}

			}

			final byte[] EF_ATR = readFile(tpNew, (short) 0x2F01);
			if (EF_ATR != null) {
				// 7F66 = ISO24727-3 Services
				final byte[] services = TLV.get(EF_ATR, (short) 0x7F66);
				System.out.println("EF_ATR: " + Hex.toString(EF_ATR)
						+ (services == null ? "" : " / ISO 24727-3 services = " + Hex.toString(services)));
			}

			tpNew.transmit(new byte[] { 0x00, (byte) 0xA4, 0x00, 0x0C, 0x02, 0x3F, 0x00 });
			setMSE_AT(tpNew, PACE_AES128CBC, (byte) 0x03, null);
		}

		return tpNew;
	}

	/**
	 * Searches for supported ECards through available TransportProviders. This
	 * method may be overriden, i.e. in Android as an interface to the hardware
	 * abstraction layer (USB, NFC, TCP/IP, Bluetooth).
	 * 
	 * @return connected {@link TransportProvider}
	 */
	protected TransportProvider getHALTransport() {
		TransportProvider tpNew = null;
		try {
			// try to find a PC/SC terminal
			tpNew = JSCIOTransport.open(Hex.fromString(AID_NPA));
		} catch (final Exception e) {

		}

		if (tpNew == null) {
			// try to connect to PersoSim (http://www.persosim.de)
			tpNew = PersoSimTransport.getInstance("localhost", 9876);

			if (tpNew != null) {
				final byte[] aid = Hex.fromString(AID_NPA);
				final byte[] selectAID = { (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x0C, (byte) aid.length };
				tpNew.transmit(ArrayTool.arrayconcat(selectAID, aid));
				if (tpNew.lastSW() == 0x9000 || tpNew.lastSW() == 0x6982) {
					//	tpNew = tpNew; - Reminder
				} else {
					tpNew = null;
				}
			}
		}

		return tpNew;
	}

	/**
	 * For transport providers supporting CCID features return the lowest level
	 * CCID implementation if available
	 * 
	 * @param transport
	 *            - the transport provider in question
	 * 
	 * @return the CCID instance
	 */
	private CCID getCCID(Object transport) {
		CCID ccid = null;
		if (transport == null) {
			throw new NullPointerException("transport == null");
		} else if (transport instanceof CCID) {
			ccid = (CCID) transport;
		} else if (transport instanceof TransportProvider) {
			while ((transport = ((TransportProvider) transport).getParent()) != null) {
				return getCCID(transport);
			}
		} else {
			System.out.println("Unsupported transport provider parent: " + transport);
		}

		return ccid;
	}

	/**
	 * Send PACE commands to the terminal if it supports CCID / PC/SC and the
	 * PACE feature.
	 * 
	 * @param transport
	 *            - {@link TransportProvider} to be used
	 * @param function
	 *            - PACE function (GetReaderPACECapabilities,
	 *            EstablishPACEChannel, DestroyPACEChannel)
	 * @param data
	 *            - optional data as input to the function
	 * 
	 * @return the PACE feature result message if successful, otherwise
	 *         <em>null<em>
	 */
	private byte[] sendPACECommand(final Object transport, final int function, byte[] data) {
		
		// The chip card interface device - card terminal.
		final CCID ccid = getCCID(transport);
		if (ccid != null && ccid.hasFeature(CCID.FEATURE_EXECUTE_PACE)) {
			if (data == null) {
				data = new byte[0];
			}
			int dataLen = data.length;
			data = ArrayTool.arrayconcat(new byte[] { (byte) function, (byte) dataLen, (byte) (dataLen >> 8) }, data);

			// response of executing PACE by the card terminal.
			final byte[] pace_res = ccid.transmitControlCommand(CCID.FEATURE_EXECUTE_PACE, data);

			final ByteBuffer bb = ByteBuffer.wrap(pace_res);
			bb.order(ByteOrder.LITTLE_ENDIAN);

			//The PACE status.
			final int pace_status = bb.getInt();

			if (pace_status == 0) {
				dataLen = bb.getShort() & 0xFFFF;
				data = new byte[dataLen];
				bb.get(data);
				return data;
			} else if (pace_status == 0xF0100002) {
				log("No card.");
			} else if (pace_status == 0xF0200001) {
				log("Canceled.");
			} else if (pace_status == 0xF0200002) {
				log("Timeout.");
			} else {
				log("Abort reason: " + pace_status + " / " + Integer.toHexString(pace_status));
			}
		}

		return null;
	}

	/**
	 * 
	 * @see de.persoapp.core.card.ICardHandler#hasPACE(java.lang.Object)
	 */
	@Override
	public int hasPACE(final Object transport) {
		// PACE capabilities: fn=0x01
		final byte[] pace_res = sendPACECommand(transport, 1, null);
		if (pace_res != null && pace_res.length > 0) {
			return pace_res[0] & 0xFF;
		}

		return 0;
	}

	/**
	 * Execute PACE inside the terminal if supported
	 * 
	 * @param keyReference
	 *            - referenced password, 1 = MRZ, 2 = CAN, 3 = PIN, 4 = PUK
	 * @param CHAT
	 *            - the <em>Card Holder Authorization Template</em> if
	 *            authentication terminal is used
	 * 
	 * @param termDesc
	 *            - the terminal description of the authentication terminal
	 * 
	 * @return <tt>0x9000</tt> if execution was successful, <tt>-1</tt>
	 *         otherwise.
	 */
	private int executeRemotePACE(final byte keyReference, final byte[] CHAT, final byte[] termDesc) {
		byte[] pace_res = null;

		if (keyReference == 0x03 && setMSE_AT(tp, PACE_AES128CBC, keyReference, null) == 0x63C1) {
			pace_res = sendPACECommand(tp, 2, new byte[] { 2, 0, 0, 0, 0 }); // request CAN before PIN
		}

		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(keyReference);
			if (CHAT != null) {
				baos.write((byte) CHAT.length);
				baos.write(CHAT);

				baos.write((byte) 0);
				final int cdLen = termDesc != null ? termDesc.length : 0;
				baos.write(new byte[] { (byte) (cdLen & 0xFF), (byte) (cdLen >> 8) });
				if (cdLen > 0) {
					baos.write(termDesc);
				}
			} else {
				baos.write(0);
				baos.write(0);
				baos.write(0);
				baos.write(0);
			}

			final byte[] data = baos.toByteArray();

			//Response of the remotely execution of pace.
			pace_res = sendPACECommand(tp, 2, data);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		if (pace_res != null) {
			if (pace_res.length > 0) {
				final ByteBuffer bb = ByteBuffer.wrap(pace_res);
				bb.order(ByteOrder.LITTLE_ENDIAN);

				final byte[] pace_sw = new byte[2];
				bb.get(pace_sw);
				final byte[] pace_efca = new byte[bb.getShort() & 0xFFFF];
				bb.get(pace_efca);
				EFCardAccess = pace_efca;

				// not applicable by sig-terminals
				if (bb.remaining() > 0) {
					byte[] temp = new byte[bb.get()];
					bb.get(temp); // current CA
					CAReferences.add(temp);

					temp = new byte[bb.get()];
					bb.get(temp); // previous CA
					CAReferences.add(temp);

					IDPICC = new byte[bb.getShort() & 0xFFFF];
					bb.get(IDPICC);
				}
			}

			return 0x9000;
		}

		return -1;
	}

	/**
	 * <p>
	 * The key derivation function uses the supplied secret (seed) and counter
	 * to derive keying material to be used by {@link ISOSMTransport}.
	 * </p>
	 * 
	 * @param md
	 *            - message digest algorithm for key derivation
	 * @param secret
	 *            - the secret used as seed
	 * @param counter
	 *            - counter for derivation of different keys from same secret
	 * @param limit
	 *            - maximum output size if message digest result size is too
	 *            large
	 * 
	 * @return Returns material that can be employed by cryptographic
	 *         algorithms.
	 */
	private byte[] KDF(final MessageDigest md, final byte[] secret, final int counter, final int limit) {
		// Temporary storage for key derivation.
		final ByteBuffer temp = counter != -1 ? ByteBuffer.allocate(secret.length + 4) : ByteBuffer
				.allocate(secret.length);
		temp.order(ByteOrder.BIG_ENDIAN);
		temp.put(secret);
		if (counter != -1) {
			temp.putInt(counter);
		}
		return ArrayTool.subArray(md.digest(temp.array()), 0, limit);
	}

	// MRZ
	// SHA-1(Serial Number || Date of Birth || Date of Expiry)
	// secretType: 0x00 = ??, 0x01 = MRZ, 0x02 = CAN, 0x03 = PIN, 0x04 = PUK,
	// ..??
	/**
	 * Execute PACE without external keypad.
	 * 
	 * @param cryptoMechanism
	 *            - cryptographic mechanism to be used
	 * @param keyReference
	 *            - reference password
	 * @param secret
	 *            - the password (CAN, PIN) or secret (MRZ)
	 * @param CHAT
	 *            - the <em>Card Holder Authorization Template</em> if
	 *            authentication terminal
	 * 
	 * @return ISO status word of failing instruction or -1 for internal error
	 */
	private int executeLocalPACE(final String cryptoMechanism, final byte keyReference, final byte[] secret,
			final byte[] CHAT) {
		int status = -1;

		if (secret == null) {
			return status;
		}

		status = setMSE_AT(tp, cryptoMechanism, keyReference, CHAT);
		if (keyReference == 0x03 && status == 0x63C1) {
			// request CAN before PACE
			final SecureHolder can = mainView.showCANDialog(textBundle.get("CardHandler_can_dialog_text"));
			status = executeLocalPACE(cryptoMechanism, (byte) 0x02, can.getValue(), null);
			if (status != 0x9000) {//Error by requesting the CAN.
				return status;
			}
			setMSE_AT(tp, cryptoMechanism, keyReference, CHAT);
		}

		MessageDigest mdSHA1 = null;
		try {
			mdSHA1 = MessageDigest.getInstance("SHA-1");
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
			return status;
		}
		// The initial randomized pace number for starting the pace protocol.
		BigInteger paceNonce = null; 
		try {
			//Data authentication data, which shows the result of the steps.
			final byte[] authData = generalAUTH(tp, null, false);
			if (tp.lastSW() == 0x9000 && authData != null) {
				final Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
				c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KDF(mdSHA1, secret, 0x00000003, 16), "AES"),
						new IvParameterSpec(new byte[16]));
				paceNonce = new BigInteger(1, c.doFinal(TLV.get(authData, (byte) 0x80)));
			} else {
				log("Error requesting challenge: " + Hex.shortToString(status));
				if (status == 0x9000) {
					return -1;
				}
				return status;
			}
		} catch (final GeneralSecurityException e1) {
			e1.printStackTrace();
			return status;
		}

		//The terminal can't process PACE, so the client has to do it.
		
		/* The specs of the elliptic curve */
		final ECParameterSpec ecSpec = EC_Globals.getCurve(PACEv2_curveID);
		
		/* The PACE protocol */
		final PACE pace = new PACE(ecSpec, paceNonce);

		/* send public key on base-curve to card */
		/* retrieve public key of key-pair on base-curve from card */
		final byte[] paceQA = TLV.get(generalAUTH(tp, TLV.build(0x81, pace.init()), false), (byte) 0x82);

		/* retrieve public point relative to G' */
		/* send new public key to card */
		final byte[] paceYB = pace.step(paceQA);

		final byte[] paceYA = TLV.get(generalAUTH(tp, TLV.build(0x83, paceYB), false), (byte) 0x84);

		/* calculate common secret point between A and B */
		final byte[][] paceRes = pace.finish(paceYA);
		IDPICC = paceRes[0];
		final byte[] sharedSecret = paceRes[1];

		//The key for encryption
		final byte[] kEnc = KDF(mdSHA1, sharedSecret, 0x0000001, 16);
		
		//The key for the message authentication code.
		final byte[] kMac = KDF(mdSHA1, sharedSecret, 0x0000002, 16);
		final byte[] kMac_ = kMac; // KDF(mdSHA1, sharedSecret, 0x0000003, 16);

		try {
			final Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kMac_, "AES"), new IvParameterSpec(new byte[16]));
			
			// The cipher based message authentication code.
			final CMac cmac = new CMac(c, 8);

			/*
			 * The authentication token.
			 */
			byte[] authToken = TLV.build(0x7F49, TLV.buildOID(cryptoMechanism, TLV.build(0x86, paceYA)));
			cmac.update(authToken, 0, authToken.length);

			final byte[] macResultA = generalAUTH(tp, TLV.build(0x85, cmac.doFinal()), true);
			status = tp.lastSW();

			if (status == 0x9000) {
				authToken = TLV.build(0x7F49, TLV.buildOID(cryptoMechanism, TLV.build(0x86, paceYB)));
				cmac.update(authToken, 0, authToken.length);

				authToken = cmac.doFinal();
				if (!ArrayTool.arrayequal(TLV.get(macResultA, (byte) 0x86), authToken)) {
					log("AuthToken does not match! " + Hex.toString(authToken));
					log("kEnc/kMac should be: " + Hex.toString(kEnc) + "," + Hex.toString(kMac) + " / resp = "
							+ Hex.toString(macResultA));
					return status;
				} else {
					byte[] CARef = TLV.get(macResultA, (byte) 0x87);
					if (CARef != null) {
						CAReferences.add(CARef);
					}
					CARef = TLV.get(macResultA, (byte) 0x88);
					if (CARef != null) {
						CAReferences.add(CARef);
					}
				}
			} else {
				log("auth result: " + Hex.shortToString(status));
			}
		} catch (final GeneralSecurityException e) {
			e.printStackTrace();
			return status;
		}

		if (status == 0x9000 && kEnc != null && kMac != null) {
			final ISOSMTransport smtp = new ISOSMTransport(tp);
			smtp.setupKeys(kEnc, kMac);
			this.tp = smtp;
		}

		return status;
	}

	/**
	 * Short-hand executePACE without terminal description
	 * 
	 * @param keyReference
	 *            - referenced password
	 * @param secret
	 *            - password (CAN, PIN) or secret (MRZ)
	 * @param CHAT
	 *            - <em>Card Holder Authorization Template</em> if
	 *            authentication terminal
	 * @return status word of failing instruction or -1 for internal error
	 */
	private int executePACE(final byte keyReference, final byte[] secret, final byte[] CHAT) {
		return executePACE(keyReference, secret, CHAT, null);
	}

	/**
	 * Execute the PACE protocol either remote (terminal with external pin-pad)
	 * or locally.
	 * 
	 * @param keyReference
	 *            - referenced password
	 * @param secret
	 *            - password (CAN, PIN) or secret (MRZ)
	 * @param CHAT
	 *            - <em>Card Holder Authorization Template</em> if
	 *            authentication terminal
	 * @param termDesc
	 *            - terminal description if authentication terminal
	 * 
	 * @return status word of failing instruction or -1 for internal error
	 */
	private int executePACE(final byte keyReference, final byte[] secret, final byte[] CHAT, final byte[] termDesc) {
		this.EFCardAccess = tp.transmit(new byte[] { 0x00, (byte) 0xB0, (byte) 0x9C, 0x00, 0x00, 0x00, 0x00 });
		if (tp.lastSW() != 0x9000) {
			this.EFCardAccess = readFile((short) 0x011C);
		}

		if (this.EFCardAccess != null) {
			log("EFCardAccess: (" + Integer.toHexString(this.EFCardAccess.length) + ") "
					+ Hex.toString(this.EFCardAccess));
			final byte[][] sets = TLV.getM(TLV.get(this.EFCardAccess, (byte) 0x31), (byte) 0x30).toArray(new byte[0][]);
			for (final byte[] t : sets) {
				final byte[] oid = TLV.get(t, (byte) 0x06);
				System.out.println(Hex.toString(oid) + ": " + Hex.toString(t));

				if ("04007F00070202040202".equals(Hex.toString(oid))) {
					final List<byte[]> data = TLV.getM(t, (byte) 0x02);
					final int paceVersion = data.get(0)[0];
					// 0.4.0.127.0.7.2.2.4.2.2 PACE Version
					if (paceVersion >= 2) {
						this.PACEv2_curveID = data.get(1)[0] & 0xFF;
						System.out.println("PACE: v" + data.get(0)[0] + " curve: " + this.PACEv2_curveID);
					} else {
						// what about PACE Version < 2?
					}
				} else if ("04007F0007020206".equals(Hex.toString(oid))) {
					System.out.println("CardInfoLocator: " + new String(TLV.get(t, (byte) 0x16)));
					// AwT ePA - BDr GmbH - Testkarte v1.0
					// ePA - BDr GmbH - Testkarte v2.0
					// http://bsi.bund.de/cif/npa.xml
					//
				} else if ("04007F0007020202".equals(Hex.toString(oid))) {
				}
				// 04007F0007020202 // ?
				// 04007F00070202030202 // KEY
				// 04007F00070202040202 // Version
				// 04007F000702020302 // KEY
				// 04007F0007020206 // CIFL
			}
		} else {
			log("EFCardAccess: null");
		}

		this.CAReferences = new ArrayList<byte[]>();

		// The chip card interface device - card terminal.
		final CCID ccid = getCCID(tp);
		if (ccid != null && hasPACE(ccid) > 0) {
			return executeRemotePACE(keyReference, CHAT, termDesc);
		} else {
			return executeLocalPACE(PACE_AES128CBC, keyReference, secret, CHAT);
		}
	}

	/**
	 * 
	 * @see de.persoapp.core.card.ICardHandler#startAuthentication(byte[],
	 *      de.persoapp.core.client.SecureHolder, byte[])
	 */
	@Override
	public synchronized boolean startAuthentication(final byte CHAT[], final SecureHolder secret, final byte[] termDesc) {
		if (!initialized) {
			try {
				tp = getECard();
				if (tp == null) {
					return initialized;
				}

				final long timeStamp = System.currentTimeMillis();

				CAReferences = null;
				final byte secretType = 0x03;
				final int status = executePACE(secretType, secret != null ? secret.getValue() : null, CHAT, termDesc);

				if (status == 0x9000 && CAReferences != null) {
					initialized = true;
					log("CA-Reference: " + CAReferences);
				} else if (status == 0x6982) {
					mainView.showError(textBundle.get("CardHandler_card_error_title"),
							textBundle.get("CardHandler_card_error_text"));
				}

				log("time spent: " + (System.currentTimeMillis() - timeStamp));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		return initialized;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.persoapp.core.card.ICardHandler#doPINUnblock(de.persoapp.core.card
	 * .TransportProvider, byte, de.persoapp.core.client.SecureHolder, byte)
	 */
	@Override
	public int doPINUnblock(final TransportProvider tp0, final byte verifySecret, final SecureHolder verifySecretInput,
			final byte unblockSecret) {
		tp = tp0;
		final int status = executePACE(verifySecret, verifySecretInput != null ? verifySecretInput.getValue() : null,
				null);
		if (status != 0x9000) {
			return status;
		}

		tp.transmit(new byte[] { 0x00, 0x2C, 0x03, unblockSecret });
		return tp.lastSW();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.persoapp.core.card.ICardHandler#doPINChange(de.persoapp.core.card.
	 * TransportProvider, byte, de.persoapp.core.client.SecureHolder, byte,
	 * de.persoapp.core.client.SecureHolder)
	 */
	@Override
	public int doPINChange(final TransportProvider tp0, final byte verifySecret, final SecureHolder verifySecretInput,
			final byte updateSecret, final SecureHolder updateSecretInput) {
		tp = tp0;
		int status = executePACE(verifySecret, verifySecretInput != null ? verifySecretInput.getValue() : null, null);
		if (status != 0x9000) {
			return status;
		}

		status = -1;
		if (hasPACE(tp) > 0) {
			try {
				final byte[] res = getCCID(tp).modifyPinDirect(
						new byte[] { 0x15, 0x00, (byte) 0x82, 0x06, 0x00, 0x00, 0x00, 0x06, 0x06, 0x01, 0x02, 0x02,
								0x07, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x2C,
								0x02, updateSecret });

				status = ((res[0] & 0xFF) << 8) + (res[1] & 0xFF);
			} catch (final CardException e) {
				e.printStackTrace();
			}
		} else {
			tp.transmit(buildCmd((byte) 0x00, (byte) 0x2C, (byte) 0x02, updateSecret, updateSecretInput.getValue(), -1));
			status = tp.lastSW();
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#getCAReferences()
	 */
	@Override
	public List<byte[]> getCAReferences() {
		return CAReferences;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#getEFCardAccess()
	 */
	@Override
	public byte[] getEFCardAccess() {
		return EFCardAccess;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#getIDPICC()
	 */
	@Override
	public byte[] getIDPICC() {
		return IDPICC;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#verifyCertificate(byte[])
	 */
	@Override
	public boolean verifyCertificate(byte[] data) {
		
		/*
		 * The certificate to be verified
		 */
		final byte[] cert = TLV.get(data, (short) 0x7F21);
		
		// Certificate data as BER-TLV
		data = TLV.get(cert, (short) 0x7F4E);

		lastCertSubject = TLV.get(data, (short) 0x5F20);
		data = TLV.build(0x83, TLV.get(data, (byte) 0x42));
		tp.transmit(buildCmd((byte) 0x00, (byte) 0x22, (byte) 0x81, (byte) 0xB6, data, -1));
		if (tp.lastSW() == 0x9000) {
			tp.transmit(buildCmd((byte) 0x00, (byte) 0x2A, (byte) 0x00, (byte) 0xBE, cert, -1));
			if (tp.lastSW() == 0x9000) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#initTA(byte[], byte[])
	 */
	@Override
	public void initTA(final byte[] ephemeralKey, final byte[] auxData) {
		this.TAKey = ephemeralKey;

		//The APDU-data for terminal authentication.
		byte[] data = ArrayTool.arrayconcat(TLV.build((byte) 0x80, Hex.fromString("04007F00070202020203")),
				TLV.build((byte) 0x83, lastCertSubject));
		data = ArrayTool.arrayconcat(data, TLV.build((byte) 0x91, ArrayTool.subArray(ephemeralKey, 0, 32)));
		data = ArrayTool.arrayconcat(data, auxData);

		tp.transmit(buildCmd((byte) 0x00, (byte) 0x22, (byte) 0x81, (byte) 0xA4, data, -1));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#getTAChallenge()
	 */
	@Override
	public byte[] getTAChallenge() {
		return tp.transmit(new byte[] { 0x00, (byte) 0x84, 0x00, 0x00, 0x08 });
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#verifyTASignature(byte[])
	 */
	@Override
	public boolean verifyTASignature(final byte[] signature) {
		tp.transmit(buildCmd((byte) 0x00, (byte) 0x82, (byte) 0x00, (byte) 0x00, signature, -1));
		if (0x9000 == tp.lastSW()) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#getEFCardSecurity()
	 */
	@Override
	public byte[] getEFCardSecurity() {
		return readFile((short) 0x011D);
	}

	/**
	 * Read elementary file from card using current transport provider.
	 * 
	 * @param FID
	 *            - file identifier
	 * @return contents of elementary file
	 */
	private byte[] readFile(final short FID) {
		return readFile(this.tp, FID);
	}

	/**
	 * Read elementary file using specified transport probider.
	 * 
	 * @param tp
	 *            - {@link TransportProvider} to be used
	 * @param FID
	 *            - file identifier
	 * @return contents of elementary file
	 */
	private byte[] readFile(final TransportProvider tp, final short FID) {
		tp.transmit(new byte[] { 0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) (FID >> 8), (byte) (FID & 0xFF) });
		if (tp.lastSW() != 0x9000) {
			return null;
		}

		final byte[] READ_BINARY = new byte[] { 0x00, (byte) 0xB0, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		short read = 0;
		try {
			do {
				READ_BINARY[2] = (byte) (read >> 8);
				READ_BINARY[3] = (byte) (read & 0xFF);
				baos.write(tp.transmit(READ_BINARY));
				read = (short) baos.size();
			} while (tp.lastSW() == 0x9000 && read >= 0x0100);
		} catch (final IOException ex) {
			Logger.getLogger(CardHandler.class.getName()).log(Level.SEVERE, null, ex);
		}

		return baos.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#execCA()
	 */
	@Override
	public byte[] execCA() {
		// TODO: make cryptographic mechanism dynamic
		tp.transmit(new byte[] { 0x00, 0x22, 0x41, (byte) 0xA4, 0x0C, (byte) 0x80, 0x0A, 0x04, 0x00, 0x7F, 0x00, 0x07,
				0x02, 0x02, 0x03, 0x02, 0x02 });
		return generalAUTH(tp, TLV.build(0x80, Hex.fromString("04" + Hex.toString(TAKey))), true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#reset()
	 */
	@Override
	public void reset() {
		initialized = false;
		if (tp != null) {
			tp0.close();
			tp.close();
			tp0 = tp = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.persoapp.core.card.ICardHandler#transmit(byte[])
	 */
	@Override
	public byte[] transmit(byte[] cmd) {
		cmd = tp0.transmit(cmd);
		return ArrayTool.arrayconcat(cmd, new byte[] { (byte) (tp0.lastSW() >> 8), (byte) (tp0.lastSW() & 0xFF) });
	}

	/**
	 * Initiate PACE with signature terminal and select signature application
	 * (DF_ESIGN).
	 * 
	 * @param tp0
	 *            - {@link TransportProvider} to be used
	 * @param verifySecret
	 *            - referenced password for signature application
	 * @return status word of application selection
	 */
	private int selectESIGN(final TransportProvider tp0, final byte verifySecret) {
		this.tp = tp0;
		// role Signature Terminal
		executePACE(verifySecret, null, Hex.fromString("7F4C0E060904007F000703010203530103"));

		//The application identifier for the electronical signing application
		final byte[] AID = Hex.fromString(AID_eSign);
		final byte[] capdu = { (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x0C, (byte) AID.length };
		tp0.transmit(ArrayTool.arrayconcat(capdu, AID));

		return tp0.lastSW();
	}

	/** password reference for signature PIN */
	private static final byte	ESIGN_PIN_ID	= (byte) 0x81;

	/** key reference for signature key */
	private static final byte	ESIGN_PRK_QES	= (byte) 0x84;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.persoapp.core.card.ICardHandler#doESignInit(de.persoapp.core.card.
	 * TransportProvider)
	 */
	@Override
	public int doESignInit(final TransportProvider tp0) {
		int status = selectESIGN(tp0, (byte) 0x03);
		if (status == 0x9000) {
			tp.transmit(new byte[] { (byte) 0x00, (byte) 0xE6, (byte) 0x10, ESIGN_PIN_ID });
			tp.transmit(new byte[] { (byte) 0x00, (byte) 0xE6, (byte) 0x21, (byte) 0x00, (byte) 0x05, (byte) 0xB6,
					(byte) 0x03, (byte) 0x84, (byte) 0x01, ESIGN_PRK_QES });

			try {
				final byte[] res = getCCID(tp).modifyPinDirect(
						new byte[] { 0x15, 0x00, (byte) 0x82, 0x08, 0x00, 0x00, 0x00, 0x08, 0x06, 0x01, 0x02, 0x02,
								0x07, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x24,
								0x01, ESIGN_PIN_ID });

				status = ((res[0] & 0xFF) << 8) + (res[1] & 0xFF);
			} catch (final CardException e) {
				status = -1;
				e.printStackTrace();
			}
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.persoapp.core.card.ICardHandler#doESignChange(de.persoapp.core.card
	 * .TransportProvider)
	 */
	@Override
	public int doESignChange(final TransportProvider tp0) {
		int status = selectESIGN(tp0, (byte) 0x02);
		if (status == 0x9000) {
			try {
				final byte[] res = getCCID(tp).modifyPinDirect(
						new byte[] { 0x15, 0x00, (byte) 0x82, 0x08, 0x00, 0x00, 0x00, 0x08, 0x06, 0x03, 0x02,
								(byte) 0xFF, 0x07, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00,
								0x00, 0x24, 0x00, ESIGN_PIN_ID });
				status = ((res[0] & 0xFF) << 8) + (res[1] & 0xFF);
			} catch (final CardException e) {
				status = -1;
				e.printStackTrace();
			}
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.persoapp.core.card.ICardHandler#doESignUnblock(de.persoapp.core.card
	 * .TransportProvider)
	 */
	@Override
	public int doESignUnblock(final TransportProvider tp0) {
		final int status = selectESIGN(tp0, (byte) 0x04);
		if (status == 0x9000) {
			tp.transmit(new byte[] { (byte) 0x00, (byte) 0x2C, (byte) 0x03, ESIGN_PIN_ID });
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.persoapp.core.card.ICardHandler#doESignTerminate(de.persoapp.core.
	 * card.TransportProvider)
	 */
	@Override
	public int doESignTerminate(final TransportProvider tp0) {
		final int status = selectESIGN(tp0, (byte) 0x03);
		if (status == 0x9000) {
			tp.transmit(new byte[] { (byte) 0x00, (byte) 0xE6, (byte) 0x10, ESIGN_PIN_ID });
			tp.transmit(new byte[] { (byte) 0x00, (byte) 0xE6, (byte) 0x21, (byte) 0x00, (byte) 0x05, (byte) 0xB6,
					(byte) 0x03, (byte) 0x84, (byte) 0x01, ESIGN_PRK_QES });
		}
		return status;
	}

	/**
	 * Select signature application and validate signature PIN (short hand).
	 * 
	 * @param tp0
	 *            - {@link TransportProvider} to be used
	 * @return last status word
	 */
	public int open_eSign(final TransportProvider tp0) {
		return open_eSign(tp0, true);
	}

	/**
	 * Select signature application and validate signature PIN.
	 * 
	 * @param tp0
	 *            - {@link TransportProvider} to be used
	 * @param validatePin
	 *            - <em>true</em> to (re-)validate PIN, false only select
	 *            signature applicatios
	 * @return last status word
	 */
	public int open_eSign(final TransportProvider tp0, final boolean validatePin) {
		int status = selectESIGN(tp0, (byte) 0x02);
		if (status == 0x9000 && validatePin) {
			status = validate_eSignPin(tp0);
		}
		return status;
	}

	/**
	 * Validate the signature PIN.
	 * 
	 * @param tp0
	 *            - {@link TransportProvider} to be used
	 * @return last status word
	 */
	public int validate_eSignPin(final TransportProvider tp0) {
		int status = -1;
		try {
			final byte[] res = getCCID(tp).verifyPinDirect(
					new byte[] { 0x15, 0x00, (byte) 0x82, 0x08, 0x00, 0x08, 0x06, 0x02, (byte) 0xFF, 0x07, 0x04, 0x00,
							0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, ESIGN_PIN_ID });
			status = ((res[0] & 0xFF) << 8) + (res[1] & 0xFF);
		} catch (final CardException e) {
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * Signs the given data.
	 * 
	 * @param dataTBS
	 *            - data to be signed (hash)
	 * @return raw signature
	 */
	public byte[] doESign(final byte[] dataTBS) {
		return tp.transmit(buildCmd((byte) 0x00, (byte) 0x2A, (byte) 0x9E, (byte) 0x9A, dataTBS, 0));
	}

	/**
	 * Read and return signature certificate chain if existent.
	 * 
	 * @return signature certificate chain
	 */
	public List<byte[]> doESignGetCertificates() {
		final List<byte[]> certs = new ArrayList<byte[]>();
		certs.add(readFile((short) 0xC000));
		certs.add(readFile((short) 0xC001));

		return certs;
	}
}
