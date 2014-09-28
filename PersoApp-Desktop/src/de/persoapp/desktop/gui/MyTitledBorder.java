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
 *          PersoApp ist Freie Software: Sie k�nnen es unter den Bedingungen der
 *          GNU Lesser General Public License, wie von der Free Software
 *          Foundation, Version 3 der Lizenz oder (nach Ihrer Option) jeder
 *          sp�teren ver�ffentlichten Version, weiterverbreiten und/oder
 *          modifizieren.
 * 
 *          PersoApp wird in der Hoffnung, dass es n�tzlich sein wird, aber OHNE
 *          JEDE GEW�HRLEISTUNG, bereitgestellt; sogar ohne die implizite
 *          Gew�hrleistung der MARKTF�HIGKEIT oder EIGNUNG F�R EINEN BESTIMMTEN
 *          ZWECK. Siehe die GNU Lesser General Public License f�r weitere
 *          Details.
 * 
 *          Sie sollten eine Kopie der GNU Lesser General Public License
 *          zusammen mit diesem Programm erhalten haben. Wenn nicht, siehe
 *          <http://www.gnu.org/licenses/>.
 * 
 */
package de.persoapp.desktop.gui;

import java.awt.Font;

import javax.swing.border.TitledBorder;

import de.persoapp.desktop.Configuration;

/**
 * <p>
 * The MyTitledBorder sets up an separating border for components with
 * the purpose to make the layout of the PersoApp Application more
 * understandable to the user.
 * </p>
 * 
 * @author Christian Kahlo
 * @author Rico Klimsa - added javadoc comments.
 */
public class MyTitledBorder extends TitledBorder {

	private static final long	serialVersionUID	= -7009403230565728382L;

	/**
	 * <p>
	 * Constructs a new instance of {@link MyTitledBorder}. The given string is
	 * set as the title of the border. The font for setting up the style of the
	 * title is predefined.
	 * </p>
	 * 
	 * @param title
	 *            - The title to set.
	 */
	public MyTitledBorder(final String title) {
		super(title);
		this.setTitleFont(new Font(Configuration.FONT, Font.BOLD, 12));
	}

}
