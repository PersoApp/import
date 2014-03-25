package de.persoapp.android;

import android.app.Activity;

import net.vrallev.android.base.BaseActivityModule;
import net.vrallev.android.base.BaseActivitySupport;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.persoapp.android.activity.ActivatePinActivity;
import de.persoapp.android.activity.AuthenticateActivity;
import de.persoapp.android.activity.ChangePinActivity;
import de.persoapp.android.activity.MainActivity;
import de.persoapp.android.activity.PinOptionsActivity;
import de.persoapp.android.activity.dialog.CanDialog;
import de.persoapp.android.activity.dialog.QuestionDialog;
import de.persoapp.android.activity.fragment.ActivatePinFragment;
import de.persoapp.android.activity.fragment.AuthenticateFragment;
import de.persoapp.android.activity.fragment.ConfirmPinFragment;
import de.persoapp.android.activity.fragment.InitializeAppFragment;
import de.persoapp.android.activity.fragment.NewPinFragment;
import de.persoapp.android.view.MenuHelper;
import de.persoapp.android.view.PinRow;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("UnusedDeclaration")
@Module(
        addsTo = AppModule.class,
        includes = {
                BaseActivityModule.class
        },
        injects = {
                MainActivity.class,
                AuthenticateActivity.class,
                PinOptionsActivity.class,
                ActivatePinActivity.class,
                ChangePinActivity.class,

                NewPinFragment.class,
                ConfirmPinFragment.class,
                QuestionDialog.class,
                CanDialog.class,
                AuthenticateFragment.class,
                InitializeAppFragment.class,
                ActivatePinFragment.class,

                PinRow.class
        }
)
public class ActivityModule {

    @Provides
    @Singleton
    MenuHelper provideMenuHelper(Activity activity) {
        return new MenuHelper((BaseActivitySupport) activity);
    }

}