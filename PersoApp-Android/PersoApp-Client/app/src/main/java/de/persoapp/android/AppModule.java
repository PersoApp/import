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
package de.persoapp.android;

import android.content.Context;
import android.nfc.NfcManager;

import net.vrallev.android.base.BaseAppModule;
import net.vrallev.android.base.ForApplication;
import net.vrallev.android.base.settings.SettingsMgr;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import de.persoapp.android.core.adapter.MainViewFacade;
import de.persoapp.android.core.adapter.MainViewFragment;
import de.persoapp.android.core.adapter.NfcTransportProvider;

/**
 * Provides dependencies with {@link android.app.Application} scope.
 *
 * @author Ralf Wondratschek
 */
@SuppressWarnings("UnusedDeclaration")
@Module(
        overrides = true,
        library = true,
        includes = {
                BaseAppModule.class
        },
        injects = {
                AppExtension.class,

                MainViewFragment.class
        }
)
public class AppModule {

    @Provides
    @Singleton
    EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @Singleton
    SettingsMgr provideSettingsMgr(@ForApplication Context context) {
        return new SettingsMgr(context);
    }

    @Provides
    @Singleton
    NfcTransportProvider provideNfcTransportProvider(NfcManager nfcManager, EventBus eventBus) {
        return new NfcTransportProvider(nfcManager, eventBus);
    }

    @Provides
    @Singleton
    MainViewFacade provideMainViewFacade() {
        return new MainViewFacade();
    }
}
