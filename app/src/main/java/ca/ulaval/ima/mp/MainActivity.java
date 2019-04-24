package ca.ulaval.ima.mp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import ca.ulaval.ima.mp.models.Channel;
import ca.ulaval.ima.mp.models.Guild;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.gateway.voice.Opus;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        FileConverterFragment.OnFileConverterFragmentInteractionListener,
        ChannelFragment.OnChannelFragmentInteractionListener {

    public static boolean debug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDK.Initialize(this);
        Opus.requestRecordPermission(this, this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Fragment fragment = ChannelFragment.newInstance();
        this.setNavigationFragment(fragment);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void playFile(File file) {
        if (Gateway.voice != null) {
            if (!Gateway.voice.playFile(file))
                SDK.displayMessage("Play error", "Could not send voice data to selected channel", null);
        } else
            SDK.displayMessage("Channel error", "Please select a voice channel",
                    null);
    }

    public void setFragment(Fragment fragment, @IdRes int id) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(id, fragment);
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.commit();
    }

    public void setNavigationFragment(Fragment fragment) {
        setFragment(fragment, R.id.navContainer);
    }

    public void setMainFragment(Fragment fragment) {
        setFragment(fragment, R.id.frameContainer);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FileManager.CODE.PLAY_FILE && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            this.playFile(new File(filePath));
        } else if (requestCode == FileManager.CODE.IMPORT_FILE && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            Fragment fragment = FileConverterFragment.newInstance(filePath);
            this.setMainFragment(fragment);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_bot) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_import_file) {
            Intent intent = new Intent(this, FileManager.class);
            intent.putExtra(FilePickerActivity.ARG_FILTER, Pattern.compile(".*\\.(wav|mp3|flac|ogg|oga|mogg|m4a|aiff|acc|3gp)"));
            intent.putExtra(FilePickerActivity.ARG_CLOSEABLE, true);
            intent.putExtra(FilePickerActivity.ARG_TITLE, getString(R.string.choose_file));
            intent.putExtra(FilePickerActivity.ARG_START_PATH, FileManager.sdcard.getAbsolutePath());
            startActivityForResult(intent, FileManager.CODE.IMPORT_FILE);
            return true;
        }

        if (id == R.id.action_play_file) {
            Intent intent = new Intent(this, FileManager.class);
            intent.putExtra(FilePickerActivity.ARG_FILTER, Pattern.compile(".*\\.(opus)"));
            intent.putExtra(FilePickerActivity.ARG_CLOSEABLE, true);
            intent.putExtra(FilePickerActivity.ARG_TITLE, getString(R.string.choose_file));
            intent.putExtra(FilePickerActivity.ARG_START_PATH, FileManager.importedDir.getAbsolutePath());
            startActivityForResult(intent, FileManager.CODE.PLAY_FILE);
        }

        if (id == R.id.action_stop_file) {
            Gateway.voice.stopPlaying();
            return true;
        }

        if (id == R.id.action_disconnect) {
            Gateway.voice.disconnect();
            return true;
        }

        if (id == R.id.action_list_members) {
            SDK.getGuildMembers(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    List<Guild.Member> members = JSONHelper.asArray(Guild.Member.class, response);
                    for (Guild.Member member : members) {
                        Log.d("MEMBER", member.nick != null ? member.nick : "null");
                    }
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onFileConversionSuccess(File file) {
        SDK.displayMessage("Conversion success", "File " + file.getAbsolutePath() +
                " is ready to be played", null);
    }

    public void onFileConversionFailure(String message) {
        SDK.displayMessage("Conversion error", message, null);
    }

    @Override
    public void onTextChannelClicked(Channel channel) {
        Fragment fragment = MessageFragment.newInstance(channel);
        setMainFragment(fragment);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawers();
    }

    @Override
    public void onVoiceChannelClicked(Channel channel) {
        Gateway.server.joinVoiceChannel(channel);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawers();
    }
}
