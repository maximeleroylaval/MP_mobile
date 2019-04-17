package ca.ulaval.ima.mp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        boolean found = true;
        try {
            packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {

            found = false;
        }
        return found;
    }

    protected void launchBrowser(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    protected void launchLogin() {
        this.launchBrowser(SDK.getBotLoginURL());
    }

    protected void authorizeWorkflow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.launchLogin();
        } else {
            if (isPackageInstalled("org.mozilla.firefox", this.getPackageManager())) {
                SDK.displayMessage("Warning", "Make sure to select Firefox Browser on the next pop up", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchLogin();
                    }
                });
            } else {
                SDK.displayMessage("Warning", "Your device is old but don't worry, we will download the Firefox Browser apk for you, so make sure you install it and then restart the application", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String APK_86 = "http://37.187.118.60/apps/miniproject/mozilla_API15_X86.apk";
                        String APK_ARM_V7 = "http://37.187.118.60/apps/miniproject/mozilla_API15_armeabi-v7a.apk";
                        if(Build.CPU_ABI.equals("x86") || Build.CPU_ABI.equals("x86_64")
                                || Build.CPU_ABI2.equals("x86") || Build.CPU_ABI2.equals("x86_64")){
                            launchBrowser(APK_86);
                        }
                        else {
                            launchBrowser(APK_ARM_V7);
                        }
                        finishAffinity();
                    }
                });
            }
        }
    }

    protected void endLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (data != null) {
            final String code = data.getQueryParameter("code");
            final String guild_id = data.getQueryParameter("guild_id");
            if (code != null && !code.equals("") && guild_id != null && !guild_id.equals("")) {
                try {
                    SDK.setGuildId(guild_id);
                    SDK.exchangeCodes(code, SDK.state, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            authorizeWorkflow();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            endLogin();
                        }
                    });
                } catch(Exception e) {
                    e.printStackTrace();
                    this.authorizeWorkflow();
                }
            }
        } else {
            this.authorizeWorkflow();
        }
    }
}
