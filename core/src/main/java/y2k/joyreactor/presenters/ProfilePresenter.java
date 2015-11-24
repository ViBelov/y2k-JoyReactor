package y2k.joyreactor.presenters;

import y2k.joyreactor.platform.Navigation;
import y2k.joyreactor.Profile;
import y2k.joyreactor.http.HttpClient;
import y2k.joyreactor.services.requests.ProfileRequest;

/**
 * Created by y2k on 9/30/15.
 */
public class ProfilePresenter {

    private final View view;

    public ProfilePresenter(View view) {
        this.view = view;

        this.view.setBusy(true);
        new ProfileRequest()
                .request()
                .subscribe(profile -> {
                    this.view.setProfile(profile);
                    this.view.setBusy(false);
                }, e -> {
                    e.printStackTrace();
                    Navigation.getInstance().switchProfileToLogin();
                });
    }

    public void logout() {
        HttpClient.getInstance().clearCookies();
        Navigation.getInstance().switchProfileToLogin();
    }

    public interface View {

        void setProfile(Profile profile);

        void setBusy(boolean isBusy);
    }
}