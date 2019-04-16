package ca.ulaval.ima.mp.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Response;

public class JSONHelper {

    public static <T> List<T> asArray(Class<T> myClass, Response response) throws IOException {
        JSONArray array = null;
        try {
            if (response != null && response.body() != null)
            array = new JSONArray(response.body().string());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return asArray(myClass, array);
    }

    public static <T> List<T> asArray(Class<T> myClass, JSONArray array) {
        List<T> list = new ArrayList<>();
        try {
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    T obj = myClass.getConstructor(JSONObject.class).newInstance(array.getJSONObject(i));
                    list.add(obj);
                }
                return list;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.d("CLASS NAME", myClass.getName());
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean isSet(JSONObject obj, String field) {
        return obj != null && obj.has(field) && !obj.isNull(field);
    }

    public static Boolean getBoolean(JSONObject obj, String field) {
        if (isSet(obj, field)) {
            try {
                return obj.getBoolean(field);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Integer getInteger(JSONObject obj, String field) {
        if (isSet(obj, field)) {
            try {
                return obj.getInt(field);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getString(JSONObject obj, String field) {
        if (isSet(obj, field)) {
            try {
                return obj.getString(field);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Timestamp getTimestamp(JSONObject obj, String field) {
        if (isSet(obj, field)) {
            try {
                String strTimestamp = obj.getString(field);
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.CANADA_FRENCH);
                Date date = formatter.parse(strTimestamp);
                return new Timestamp(date.getTime());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static JSONArray getJSONArray(JSONObject obj, String field) {
        if (isSet(obj, field)) {
            try {
                return obj.getJSONArray(field);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static JSONObject getJSONObject(JSONObject obj, String field) {
        if (isSet(obj, field)) {
            try {
                return obj.getJSONObject(field);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}