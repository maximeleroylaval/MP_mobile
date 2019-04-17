package ca.ulaval.ima.mp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.List;

import ca.ulaval.ima.mp.models.Channel;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.Guild;
import ca.ulaval.ima.mp.models.Message;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDK.Initialize(this);
        Gateway.Initialize();

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_bot) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
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
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
