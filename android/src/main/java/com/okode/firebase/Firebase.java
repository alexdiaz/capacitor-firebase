package com.okode.firebase;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

@NativePlugin(
    permissions = {
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.WAKE_LOCK
    }
)
public class Firebase extends Plugin {

    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void load() {
        super.load();
        firebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
    }

    // Firebase Analytics

    @PluginMethod()
    public void logEvent(PluginCall call) {
        final String eventName = call.getString("name", null);
        final JSONObject params = call.getData().optJSONObject("parameters");

        if (eventName == null) {
            call.reject("key 'name' does not exist");
            return;
        }

        if (params == null) {
            call.reject("key 'parameters' does not exist");
            return;
        }

        // Preparing event bundle
        Bundle bundle = new Bundle();
        try {
            Iterator<String> keys = params.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = params.get(key);

                if (value instanceof String) {
                    bundle.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    bundle.putInt(key, (Integer) value);
                } else if (value instanceof Double) {
                    bundle.putDouble(key, (Double) value);
                } else if (value instanceof Long) {
                    bundle.putLong(key, (Long) value);
                } else {
                    call.reject("Value for key " + key + " not one of (String, Integer, Double, Long)");
                }
            }
        } catch (JSONException e) {
            call.reject(e.getLocalizedMessage(), e);
        }

        firebaseAnalytics.logEvent(eventName, bundle);
        call.success();
    }


    @PluginMethod()
    public void setUserProperty(PluginCall call) throws JSONException {
        final String name = call.getString("name");
        final String value = call.getString("value");
        if (name != null && value != null) {
            firebaseAnalytics.setUserProperty(name, value);
            call.success();
        } else {
            call.reject("key 'name' or 'value' does not exist");
        }
    }

    @PluginMethod()
    public void setUserId(PluginCall call) {
        final String userId = call.getString("userId");
        if (userId != null) {
            firebaseAnalytics.setUserId(userId);
            call.success();
        } else {
            call.reject("key 'userId' does not exist");
        }
    }

    @PluginMethod()
    public void setScreenName(PluginCall call) {
        final String value = call.getString("screenName");
        final String overrideName = call.getString("screenClassOverride", null);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                firebaseAnalytics.setCurrentScreen(getActivity(), value, overrideName);
            }
        });
        call.success();
    }

    // Firebase Remote Config

    @PluginMethod()
    public void activateFetched(final PluginCall call) {
        FirebaseRemoteConfig.getInstance().activate()
            .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                @Override
                public void onSuccess(Boolean activated) {
                    final JSObject res = new JSObject();
                    res.put("activated", activated);
                    call.success(res);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    call.error("Error activating fetched remote config");
                }
            });
    }

    @PluginMethod()
    public void fetch(final PluginCall call) {
        FirebaseRemoteConfig.getInstance().fetch()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    call.success();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    call.error("Error fetching remote config");
                }
            });
    }

    @PluginMethod()
    public void getRemoteConfigValue(PluginCall call) {
        final String key = call.getString("key");
        if (key != null) {
            FirebaseRemoteConfigValue configValue = FirebaseRemoteConfig.getInstance().getValue(key);
            final JSObject res = new JSObject();
            res.put("value", configValue.asString());
            call.success(res);
        } else {
            call.reject("You must pass 'key'");
        }
    }

}