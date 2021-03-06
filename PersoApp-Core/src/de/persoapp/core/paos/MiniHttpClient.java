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
package de.persoapp.core.paos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import de.persoapp.core.util.ChunkingInputStream;

/**
 * This class provides a small HTTP-capable client ensuring connection re-use.
 * (uses only one connection per instance).
 * 
 * @author Christian Kahlo
 * @author Rico Klimsa - added javadoc comments.
 */
public class MiniHttpClient {
	
	/**
	 * The URL of the {link MiniHttpClient}.
	 */
	private final URL					url;
	
	/**
	 * The used socket factory of the {link MiniHttpClient}.
	 */
	private SocketFactory				sf;
	
	/**
	 * The {link Socket} of the {link MiniHttpClient} which he uses to set up a
	 * connection.
	 */
	private Socket						socket;
	
	/**
	 * The stored requestHeaders of the {link MiniHttpClient}.
	 */
	private final Map<String, String>	requestHeaders	= new HashMap<String, String>();
	
	/**
	 * The stored responseHeaders of the {link MiniHttpClient}.
	 */
	private final Map<String, String>	responseHeaders	= new HashMap<String, String>();

	/**
	 * The used HTTP_PROTOCOL.
	 */
	private final String				HTTP_PROTOCOL	= "HTTP/1.1";

	/**
	 * The carriage return.
	 */
	private static final char			CR				= 13;
	
	/**
	 * The line feed.
	 */
	private static final char			LF				= 10;

	/**
	 * Reads a number of characters from a line of a given {@link InputStream}.
	 * The number is set up through the <strong>limit</strong> argument.
	 * 
	 * @param input
	 *            - The given {@link InputStream}.
	 * @param limit
	 *            - The number of characters which will be read from the line of
	 *            the {@link InputStream}.
	 * @return The read line.
	 */
	private static final String readLine(final InputStream input, final long limit) {
		long read = 0;
		final StringBuilder line = new StringBuilder();
		int c = -1;
		do {
			try {
				c = input.read();
			} catch (final IOException ioe) {
			}

			if (c != -1 && c != CR && c != LF) {
				line.append((char) c);
			}
		} while (c != -1 && c != LF && (limit < 0 || ++read < limit));
		return c == -1 && line.length() == 0 ? null : line.toString();
	}

	/**
	 * Reads the entire line of the given {@link InputStream}.
	 * 
	 * @param input
	 *            - The given {@link InputStream}, which contains the characters to read.
	 * 
	 * @return The read line.
	 */
	private static final String readLine(final InputStream input) {
		return readLine(input, -1);
	}

	/**
	 * Constructs a {@link MiniHttpClient} with the given {@link URL}.
	 * 
	 * @param url - The given {@link URL}.
	 */
	public MiniHttpClient(final URL url) {
		this.url = url;
	}

	/**
	 * Sets the {@link SocketFactory}.
	 * 
	 * @param sf - The given {@link SocketFactory}.
	 */
	public final void setSocketFactory(final SocketFactory sf) {
		this.sf = sf;
	}

	/**
	 * Retrieves the currently running {@link SSLSession}.
	 * 
	 * @return The currently running {@link SSLSession}.
	 */
	public final SSLSession getSSLSession() {
		if (socket == null || socket.isClosed()) {
			try {
				socket = getSocket();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		if (socket != null && socket instanceof SSLSocket) {
			return ((SSLSocket) socket).getSession();
		} else {
			return null;
		}
	}

	/**
	 * Set up a new {@link Socket} and returns it.
	 * 
	 * @return The new {@link Socket}.
	 * 
	 * @throws IOException
	 *             If the {@link Socket} can't connected.
	 */
	private final Socket getSocket() throws IOException {
		if (this.sf == null) {
			this.sf = SocketFactory.getDefault();
		}

		int port = url.getPort();
		if (port < 0) {
			port = url.getDefaultPort();
		}

		return sf.createSocket(url.getHost(), port);
	}

	/**
	 * Stores the request header.
	 * 
	 * @param key
	 *            - The key under them the header will be saved.
	 * @param value
	 *            - The request header which will be saved.
	 */
	public final void setRequestHeader(final String key, final String value) {
		requestHeaders.put(key, value);
	}

	/**
	 * Stores the request header under the given key. Is already a request
	 * header under this key, in the requestHeader-Map stored, the given header
	 * will be appended to the currently stored header. The delimiting character
	 * is a ";".
	 * 
	 * @param key
	 *            - The key under them the header will be saved.
	 * @param value
	 *            - The request header which will be saved.
	 */
	public final void addRequestHeader(final String key, final String value) {
		if (!requestHeaders.containsKey(key)) {
			setRequestHeader(key, value);
		} else {
			requestHeaders.put(key, requestHeaders.get(key) + ";" + value);
		}
	}

	/**
	 * Retrieves the response header which is stored under the given key.
	 * 
	 * @param key - The key of the requested response header.
	 *
	 * @return The retrieved response header.
	 */
	public final String getResponseHeader(final String key) {
		return responseHeaders.get(key);
	}

	/**
	 * Appends the header, which will be identified through the given
	 * key/value pair at the given {@link StringBuilder}.
	 * 
	 * @param sb
	 *            - The {@link StringBuilder}, which contains the result.
	 * @param key
	 *            - The key of the requested header.
	 * @param value
	 *            - The requested header.
	 * 
	 * @return The {@link StringBuilder} with the appended header.
	 */
	private static final StringBuilder appendHeader(final StringBuilder sb, final String key, final String value) {
		return sb.append(key).append(": ").append(value).append("\r\n");
	}

	/**
	 * Transmits the given byte-array to the connection endpoint of the
	 * {@link MiniHttpClient}.
	 * 
	 * @param in
	 *            - The given byte-array.
	 * @return The collected response of the send request. If something went
	 *         wrong with the request, the given <strong>byte-array</strong> is
	 *         returned. If the given byte-array was empty,
	 *         <strong>null</strong> is returned.
	 * @throws IOException
	 *             If something went wrong with the connection.
	 */
	public final byte[] transmit(final byte[] in) throws IOException {
		if (socket == null || socket.isClosed()) {
			socket = getSocket();
		}

		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		final StringBuilder request = new StringBuilder().append("POST ").append(url.getFile()).append(" ")
				.append(HTTP_PROTOCOL).append("\r\n");
		appendHeader(request, "Host", url.getHost());
		appendHeader(request, "Connection", "keep-alive");
		appendHeader(request, "Accept-Encoding", "gzip");

		if (in != null && in.length > 0) {
			appendHeader(request, "Content-Length", Long.toString(in.length));
		} else {
			appendHeader(request, "Content-Length", Long.toString(0));
		}

		for (final String headerKey : requestHeaders.keySet()) {
			request.append(headerKey).append(": ").append(requestHeaders.get(headerKey)).append("\r\n");
		}
		request.append("\r\n");

		try {
			os.write(request.toString().getBytes("UTF-8"));
		} catch (final IOException ioe) {
			// connection gone bad, try to set up new one
			try {
				socket = getSocket();
				os = socket.getOutputStream();
				is = socket.getInputStream();
				os.write(request.toString().getBytes("UTF-8"));
			} catch (final IOException ioe2) {
				ioe2.printStackTrace();
				throw ioe;
			}
		}

		if (in != null && in.length > 0) {
			os.write(in);
		}
		os.flush();

		String response = readLine(is);
		int responseCode = -1;

		if (response == null || !response.startsWith(HTTP_PROTOCOL.substring(0, HTTP_PROTOCOL.length() - 1))) {
			throw new IOException("protocol violation in response: " + response);
		} else {
			response = response.substring(HTTP_PROTOCOL.length() + 1);
			final int n = response.indexOf(' ');
			responseCode = Integer.parseInt(response.substring(0, n));
			response = response.substring(n + 1);
		}

		responseHeaders.clear();
		String temp;
		while ((temp = readLine(is)) != null && temp.length() > 0) {
			final int n = temp.indexOf(':');
			responseHeaders.put(temp.substring(0, n).trim(), temp.substring(n + 1).trim());
		}

		long contentLength = -1;
		try {
			contentLength = Long.parseLong(responseHeaders.get("Content-Length"));
		} catch (final Exception e) {
		}
		boolean connectionClose = false;
		try {
			connectionClose = "close".equals(responseHeaders.get("Connection").toLowerCase());
		} catch (final Exception e) {
		}

		if ("chunked".equalsIgnoreCase(responseHeaders.get("Transfer-Encoding"))) {
			is = new ChunkingInputStream(is);
		}

		final String contentEncoding = responseHeaders.get("Content-Encoding");
		if ("gzip".equalsIgnoreCase(contentEncoding)) {
			is = new GZIPInputStream(is);
		}
		if ("deflate".equalsIgnoreCase(contentEncoding)) {
			final Inflater inf = new Inflater(true);
			is = new InflaterInputStream(is, inf);
		}

		if (contentLength > 0) {
			final byte[] buffer = new byte[(int) contentLength];
			int read = 0;
			while (buffer.length - read > 0) {
				read += is.read(buffer, read, buffer.length - read);
			}

			return buffer;
		} else if (contentLength < 0) {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			do {
				int c = -1;
				try {
					c = is.read();
				} catch (final IOException ioe) {
				}

				if (c != -1) {
					baos.write(c);
				} else {
					break;
				}
			} while (true);

			return baos.toByteArray();
		}

		return null;
	}
}
