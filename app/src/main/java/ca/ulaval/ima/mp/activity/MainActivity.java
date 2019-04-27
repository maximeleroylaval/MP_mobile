package ca.ulaval.ima.mp.activity;

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
import android.view.Menu;
import android.view.MenuItem;

import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.util.regex.Pattern;

import ca.ulaval.ima.mp.R;
import ca.ulaval.ima.mp.fragment.ChannelFragment;
import ca.ulaval.ima.mp.fragment.FileConverterFragment;
import ca.ulaval.ima.mp.fragment.MemberFragment;
import ca.ulaval.ima.mp.fragment.MessageFragment;
import ca.ulaval.ima.mp.fragment.SearchFragment;
import ca.ulaval.ima.mp.fragment.SoundFragment;
import ca.ulaval.ima.mp.gateway.Gateway;
import ca.ulaval.ima.mp.gateway.voice.Opus;
import ca.ulaval.ima.mp.sdk.SDK;
import ca.ulaval.ima.mp.sdk.models.Channel;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ChannelFragment.OnChannelFragmentInteractionListener,
        FileConverterFragment.OnFileConverterFragmentInteractionListener,
        SoundFragment.OnSoundFragmentInteractionListener,
        SearchFragment.Listener {

    public static boolean debug = false;

    public SoundFragment soundFragment = null;
    public MessageFragment messageFragment = null;
    public FileConverterFragment convertFragment = null;
    public SearchFragment searchFragment = null;

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

        NavigationView navigationView = findViewById(R.id.nav_left_view);
        navigationView.setNavigationItemSelectedListener(this);

        NavigationView navigationRightView = findViewById(R.id.nav_right_view);
        navigationRightView.setNavigationItemSelectedListener(this);

        this.setLeftNavigationFragment(ChannelFragment.newInstance());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            Fragment myFragment = getSupportFragmentManager().findFragmentByTag(FileConverterFragment.class.getName());
            if (myFragment != null && myFragment.isVisible() && soundFragment != null) {
                setMainFragment(soundFragment);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void removeMainFragment(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.commit();
    }

    public void setFragment(Fragment fragment, @IdRes int id) {
        if (fragment == null) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(id, fragment, fragment.getClass().getName());
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void setLeftNavigationFragment(Fragment fragment) { setFragment(fragment, R.id.navLeftContainer); }

    public void setRightNavigationFragment(Fragment fragment) { setFragment(fragment, R.id.navRightContainer); }

    public void setMainFragment(Fragment fragment) {
        setFragment(fragment, R.id.frameContainer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_bot) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.list_members) {
            this.setRightNavigationFragment(MemberFragment.newInstance());
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.END);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FileManager.CODE.PLAY_FILE && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            if (soundFragment != null) {
                soundFragment.setActiveFile(new File(filePath));
            }
        } else if (requestCode == FileManager.CODE.IMPORT_FILE && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            if (convertFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(convertFragment).commit();
            }
            convertFragment = FileConverterFragment.newInstance(filePath);
            setMainFragment(convertFragment);
        }
    }

    public void onFileConversionSuccess(File file) {
        if (convertFragment != null) {
            getSupportFragmentManager().popBackStack();
            removeMainFragment(convertFragment);
            convertFragment = null;
            if (soundFragment != null) {
                setMainFragment(soundFragment);
            }
        }
        SDK.displayMessage("Conversion success", "File " + file.getAbsolutePath() +
                " is ready to be played", null);
    }

    public void onFileConversionFailure(String message) {
        SDK.displayMessage("Conversion error", message, null);
    }

    public void onPlayFile() {
        Intent intent = new Intent(this, FileManager.class);
        intent.putExtra(FilePickerActivity.ARG_FILTER, Pattern.compile(".*\\.(opus)"));
        intent.putExtra(FilePickerActivity.ARG_CLOSEABLE, true);
        intent.putExtra(FilePickerActivity.ARG_TITLE, getString(R.string.choose_file));
        intent.putExtra(FilePickerActivity.ARG_START_PATH, FileManager.importedDir.getAbsolutePath());
        startActivityForResult(intent, FileManager.CODE.PLAY_FILE);
    }

    public void onImportFile() {
        if (convertFragment != null) {
            setMainFragment(convertFragment);
        } else {
            Intent intent = new Intent(this, FileManager.class);
            intent.putExtra(FilePickerActivity.ARG_FILTER, Pattern.compile(".*\\.(wav|mp3|flac|ogg|oga|mogg|m4a|aiff|acc|3gp)"));
            intent.putExtra(FilePickerActivity.ARG_CLOSEABLE, true);
            intent.putExtra(FilePickerActivity.ARG_TITLE, getString(R.string.choose_file));
            intent.putExtra(FilePickerActivity.ARG_START_PATH, FileManager.sdcard.getAbsolutePath());
            startActivityForResult(intent, FileManager.CODE.IMPORT_FILE);
        }
    }

    @Override
    public void onSearch() {
        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance(this);
        }
        this.setFragment(searchFragment, R.id.frameContainer);
    }

    @Override
    public void onVoiceDisconnect(boolean fromDestroy) {
        Gateway.voice.stopPlaying();
        Gateway.server.leaveVoiceChannel();
        if (!fromDestroy) {
            removeMainFragment(soundFragment);
            soundFragment = null;
        }
    }

    @Override
    public void onFirstTextChannelLoaded(Channel channel) {
        onTextChannelClicked(channel);
    }

    @Override
    public void onTextChannelClicked(Channel channel) {
        if (messageFragment == null) {
            messageFragment = MessageFragment.newInstance(channel);
        } else {
            messageFragment.setActiveChannel(channel);
        }
        setMainFragment(messageFragment);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawers();
    }

    @Override
    public void onVoiceChannelClicked(Channel channel) {
        if (soundFragment == null) {
            soundFragment = SoundFragment.newInstance(channel);
        } else {
            soundFragment.setActiveChannel(channel);
        }
        setMainFragment(soundFragment);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawers();
    }

    @Override
    public void onSearch(String search) {
        Intent intent = new Intent(this, FileManager.class);
        intent.putExtra(FilePickerActivity.ARG_FILTER, Pattern.compile(search + ".*\\.(opus)"));
        intent.putExtra(FilePickerActivity.ARG_CLOSEABLE, true);
        intent.putExtra(FilePickerActivity.ARG_TITLE, getString(R.string.choose_file));
        intent.putExtra(FilePickerActivity.ARG_START_PATH, FileManager.importedDir.getAbsolutePath());
        startActivityForResult(intent, FileManager.CODE.PLAY_FILE);

    }
}
