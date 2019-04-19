package ca.ulaval.ima.mp;

import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Response;

public class JSONHelper {

    public static <T,W> List<T> asArrayWithConstructor(Class<T> myClass, Class<W> myParameter, JSONArray array) {
        List<T> list = new ArrayList<>();
        try {
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    W parameter;
                    if (myParameter.equals(JSONObject.class)) {
                        parameter = (W)array.getJSONObject(i);
                    } else {
                        parameter = myParameter.getConstructor(myParameter).newInstance(array.get(i));
                    }
                    T obj = myClass.getConstructor(myParameter).newInstance(parameter);
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

    public static <T,W> List<T> asArrayWithConstructor(Class<T> myClass, Class<W> myParameter, Response response) throws IOException {
        JSONArray array = null;
        try {
            if (response != null && response.body() != null)
                array = new JSONArray(response.body().string());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return asArrayWithConstructor(myClass, myParameter, array);
    }

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
        return asArrayWithConstructor(myClass, JSONObject.class, array);
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

    public static byte[] getByteArray(JSONObject obj, String field) {
        if (isSet(obj, field)) {
            try {
                JSONArray array = obj.getJSONArray(field);
                if (array != null) {
                    byte[] bytes = new byte[array.length()];
                    for (int i = 0; i < array.length(); i++) {
                        bytes[i] = (byte)array.getInt(i);
                    }
                    Log.d("BYTES", Arrays.toString(bytes));
                    return bytes;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
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

    public static JSONObject getJSONObject(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getJSONObject(Response response) throws IOException {
        if (response != null && response.body() != null) {
            return getJSONObject(response.body().string());
        }
        return null;
    }

    public static <T> String asJSONString(T obj) {
        String jsonInString = null;
        if (obj != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                jsonInString = mapper.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return jsonInString;
    }

    public static <T> JSONObject asJSONObject(T obj) {
        try {
            String jsonObj = asJSONString(obj);
            if (jsonObj != null)
                return new JSONObject(jsonObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] toPrimitiveByteArray(Byte[] oBytes)
    {
        byte[] bytes = new byte[oBytes.length];

        for(int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }

        return bytes;
    }
}