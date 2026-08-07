package fabscreen.platform.base.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

import fabscreen.platform.base.Constants;
import fabscreen.platform.base.instantiation.IServiceIdentifier;

public class Preferences implements IPreferences, IServiceIdentifier {

    private Context context;

    public Preferences(IAppService appService) {
        this.context = appService.getAppContext();
    }

    public Helper getHelper() {
        return new Helper(this);
    }

    private void removeKey(String key) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().remove(key).apply();
    }

    public boolean getPref(String key, boolean defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, defValue);
    }

    public String getPref(String key, String defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getString(key, defValue);
    }

    public Set<String> getPref(String key, Set<String> defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getStringSet(key, defValue);
    }

    public float getPref(String key, float defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getFloat(key, defValue);
    }

    public int getPref(String key, int defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getInt(key, defValue);
    }

    @Override
    public long getPref(String key, long defValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        return sharedPref.getLong(key, defValue);
    }

    public void setPref(String key, boolean value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(key, value).apply();
    }

    public void setPref(String key, float value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putFloat(key, value).apply();
    }

    public void setPref(String key, int value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putInt(key, value).apply();
    }

    public void setPref(String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putString(key, value).apply();
    }

    public void setPref(String key, Set<String> value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putStringSet(key, value).apply();
    }

    @Override
    public void setPref(String key, long value) {
        SharedPreferences sharedPref = context.getSharedPreferences(Constants.SP_DEFAULT, Context.MODE_PRIVATE);
        sharedPref.edit().putLong(key, value).apply();
    }
}
