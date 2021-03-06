package ca.ulaval.ima.mp.sdk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import ca.ulaval.ima.mp.sdk.models.Channel;
import ca.ulaval.ima.mp.gateway.Gateway;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class SDK {

    private static String scheme = "https";
    private static String host = "discordapp.com";
    private static String version = "api";

    private static String clientId = "408378466220638218";
    private static String clientSecret = "9H30Ge-eVQNkXwskPVy8HrAQFPsQ9Wk6";

    private static String token;
    private static String refreshToken;

    private static String scope = "bot";
    private static String guild_id = "";
    public static String botToken = "NDA4Mzc4NDY2MjIwNjM4MjE4.XLS-pw.dbU0HB20CFITP-MSE9VRarbNDtg";

    public static String cdn = "https://cdn.discordapp.com/";
    public static String main = "https://discordapp.com/";

    public static String state = "0986545678";

    public static Context mainContext = null;
    public static OkHttpClient client = null;
    public static Gateway gateway = null;

    public static void setGuildId(String guild_id) {
        if (guild_id != null && !guild_id.equals("")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SDK.mainContext);
            prefs.edit().putString("guild_id", guild_id).apply();
            SDK.guild_id = guild_id;
        }
    }

    private static void setToken(String token, String refreshToken) {
        SDK.token = token;
        SDK.refreshToken = refreshToken;
    }

    private static void addAuthorization(Request.Builder builder) {
        builder.addHeader("Authorization", "Bot " + SDK.botToken);
    }

    private static String BuildURL(String target) {
        return SDK.scheme + "://" + SDK.host + "/" + version + "/" + target;
    }

    private static void loadUserPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SDK.mainContext);
        String guildId = prefs.getString("guild_id", null);
        if (guildId != null) {
            SDK.guild_id = guildId;
        }
    }

    public static void Initialize(Context mainContext) {
        SDK.mainContext = mainContext;
        loadUserPreferences();
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .addInterceptor(logInterceptor)
                .build();

        gateway = new Gateway();
    }

    public static void displayMessage(Context context, String title, String message, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .show();
    }

    public static void displayMessage(String title, String message, DialogInterface.OnClickListener listener) {
        SDK.displayMessage(mainContext, title, message, listener);
    }

    public static String getLoginURL()  {
        HttpUrl url = new HttpUrl.Builder()
                .scheme(SDK.scheme)
                .host(SDK.host)
                .addPathSegment(SDK.version)
                .addPathSegment("oauth2")
                .addPathSegment("authorize")
                .addQueryParameter("client_id", SDK.clientId)
                .addQueryParameter("client_secret", SDK.clientSecret)
                .addQueryParameter("response_type", "code")
                .addQueryParameter("state", SDK.state)
                .addQueryParameter("scope", SDK.scope)
                .build();
        return url.toString();
    }

    public static String getBotLoginURL()  {
        Integer permissions = 0x00000001 | 0x00000002 | 0x00000004 | 0x00000008 | 0x00000010 | 0x00000020 | 0x00000040 | 0x00000080 | 0x00000400 | 0x00000800 | 0x00001000 | 0x00002000 | 0x00004000 | 0x00008000 | 0x00010000 | 0x00020000 | 0x00040000 | 0x00100000 | 0x00200000 | 0x00400000 | 0x00800000 | 0x01000000 | 0x02000000 | 0x00000100 | 0x04000000 | 0x08000000 | 0x10000000 | 0x20000000 | 0x40000000;
        HttpUrl url = new HttpUrl.Builder()
                .scheme(SDK.scheme)
                .host(SDK.host)
                .addPathSegment(SDK.version)
                .addPathSegment("oauth2")
                .addPathSegment("authorize")
                .addQueryParameter("client_id", SDK.clientId)
                .addQueryParameter("scope", SDK.scope)
                .addQueryParameter("permissions", permissions.toString())
                .addQueryParameter("response_type", "code")
                .build();
        return url.toString();
    }

    public static void exchangeCodes(String code, String state, final Callback mycall) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("client_id", SDK.clientId)
                .add("client_secret", SDK.clientSecret)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("state", state)
                .build();

        Request request = new Request.Builder()
                .url(SDK.BuildURL("oauth2/token"))
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mycall.onFailure(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject res = new JSONObject(response.body().string());
                    SDK.setToken(res.getString("access_token"), res.getString("refresh_token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mycall.onResponse(call, response);
            }
        });
    }

    public static void getGatewayBot(Callback call) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(SDK.BuildURL("gateway/bot"));
        SDK.addAuthorization(requestBuilder);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(call);
    }

    public static void getChannels(Callback call) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(SDK.BuildURL("guilds/" + SDK.guild_id + "/channels"));
        SDK.addAuthorization(requestBuilder);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(call);
    }

    public static void getMessages(Channel channel, Callback call) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(SDK.BuildURL("channels/" + channel.id + "/messages"));
        SDK.addAuthorization(requestBuilder);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(call);
    }

    public static void postMessage(String message, Channel channel, Callback call) {
        RequestBody body = new FormBody.Builder()
                .add("content", message)
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(SDK.BuildURL("channels/" + channel.id + "/messages"))
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded");

        SDK.addAuthorization(requestBuilder);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(call);
    }

    public static void getGuildMembers(Callback call) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(SDK.BuildURL("guilds/" + SDK.guild_id + "/members"));
        SDK.addAuthorization(requestBuilder);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(call);
    }

    public static void getUserById(Integer id, Callback call) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(SDK.BuildURL("/users/" + id));
        SDK.addAuthorization(requestBuilder);
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(call);
    }
}