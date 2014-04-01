/**
 *
 * COPYRIGHT (C) 2010, 2011, 2012, 2013 AGETO Innovation GmbH
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
package de.persoapp.core.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsCredentials;

/**
 * @author ckahlo
 * 
 */
public class BCTlsAuthentication implements TlsAuthentication {

	List<Certificate>	serverCertList;

	@Override
	public void notifyServerCertificate(final org.bouncycastle.crypto.tls.Certificate serverCertificate)
			throws IOException {
		System.out.println(serverCertificate.getCertificateList()[0].getSubject());

		if (this.serverCertList == null) {
			final org.bouncycastle.asn1.x509.Certificate[] serverCertList = serverCertificate.getCertificateList();
			if (serverCertList != null) {
				this.serverCertList = new ArrayList<Certificate>();
				try {
					final CertificateFactory x509cf = CertificateFactory.getInstance("X509");
					for (final org.bouncycastle.asn1.x509.Certificate cert : serverCertList) {
						this.serverCertList
								.add(x509cf.generateCertificate(new ByteArrayInputStream(cert.getEncoded())));
					}
				} catch (final CertificateException e) {
					throw new IOException(e);
				}
			}
		}
	}

	final List<Certificate> getServerCertList() {
		return this.serverCertList;
	}

	@Override
	public TlsCredentials getClientCredentials(final CertificateRequest certificateRequest) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

};
