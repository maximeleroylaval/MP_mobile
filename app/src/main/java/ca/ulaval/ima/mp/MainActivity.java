package ca.ulaval.ima.mp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import ca.ulaval.ima.mp.models.Channel;
import ca.ulaval.ima.mp.models.Guild;
import ca.ulaval.ima.mp.models.Message;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.gateway.voice.Opus;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static boolean debug = false;
    public Channel activeChannel = null;

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

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        SDK.getChannels(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("FAIL", "GET CHANNELS");
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            displayChannels(response, navigationView);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void sendMessage(View view) {
        if (activeChannel == null || activeChannel.type != Channel.TYPES.GUILD_TEXT) {
            SDK.displayMessage("Channel", "Please select a text channel before sending messages", null);
            return;
        }
        final EditText messageView = findViewById(R.id.edit_message);
        String message = messageView.getText().toString();
        SDK.postMessage(message, activeChannel, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SDK.displayMessage("MESSAGE", "Failed to send your message", null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                messageView.setText("");
            }
        });
    }

    protected void displayChannels(Response response, NavigationView navigationView) throws IOException {
        List<Channel> channels = Channel.sort(JSONHelper.asArray(Channel.class, response));
        Menu menu = navigationView.getMenu();
        for (Channel channel : channels) {
            if (channel.type == Channel.TYPES.CATEGORY) {
                Menu subMenu = menu.addSubMenu(channel.name);
                for (final Channel innerChannel : channels) {
                    if (channel.id.equals(innerChannel.parentId)) {
                        if (innerChannel.type == Channel.TYPES.GUILD_TEXT) {
                            MenuItem item = subMenu.add(innerChannel.name).setIcon(R.drawable.ic_textual_channel);
                            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    activeChannel = innerChannel;
                                    SDK.getMessages(innerChannel, new Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {
                                            Log.d("FAIL", "GET MESSAGES");
                                        }
                                        @Override
                                        public void onResponse(Call call, Response response) throws IOException {
                                            List<Message> messages = JSONHelper.asArray(Message.class, response);
                                            for (Message message : messages) {
                                                Log.d(message.id, message.content);
                                            }
                                        }
                                    });
                                    return false;
                                }
                            });
                        } else if (innerChannel.type == Channel.TYPES.GUILD_VOICE) {
                            MenuItem item = subMenu.add(innerChannel.name).setIcon(R.drawable.ic_speaker_channel);
                            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    activeChannel = innerChannel;
                                    Gateway.server.joinVoiceChannel(innerChannel);
                                    return false;
                                }
                            });
                        }
                    }
                }
            }
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String path = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            if (path != null && !path.equals("")) {
                File file = new File(path);
                Log.d("[RESULT]","Path:" + path);
                Gateway.voice.playFile(this, file);
            }
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

        if (id == R.id.action_play_file) {
            Intent intent = new Intent(this, FileManager.class);
//            intent.putExtra(FilePickerActivity.ARG_FILTER, Pattern.compile(".*\\.opus"));
            intent.putExtra(FilePickerActivity.ARG_CLOSEABLE, true);
            intent.putExtra(FilePickerActivity.ARG_TITLE, getString(R.string.choose_file));
            intent.putExtra(FilePickerActivity.ARG_START_PATH, FileManager.sdcard.getAbsolutePath());
            startActivityForResult(intent, 1);
            return true;
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
}
