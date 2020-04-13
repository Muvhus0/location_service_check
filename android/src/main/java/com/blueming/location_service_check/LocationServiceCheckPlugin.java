package com.blueming.location_service_check;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** LocationServiceCheckPlugin */
public class LocationServiceCheckPlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  private LocationManager locationManager;

  private ContentResolver contentResolver;

  @SuppressLint("StaticFieldLeak")
  private static Context aContext;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "location_service_check");
    channel.setMethodCallHandler(this);
    aContext = flutterPluginBinding.getApplicationContext();
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "location_service_check");
    channel.setMethodCallHandler(new LocationServiceCheckPlugin());
    aContext = registrar.context();
  }

  private void init() {
    if (locationManager == null) {
      locationManager = (LocationManager) aContext.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    }
    if (contentResolver == null) {
      contentResolver = aContext.getApplicationContext().getContentResolver();
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    init();
    if ("getPlatformVersion".equals(call.method)) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if ("checkLocationIsOpen".equals(call.method)) {
      checkLocationIsOpen(result);
    } else if ("openSetting".equals(call.method)) {
      openSetting();
    } else if ("getLocation".equals(call.method)) {
      getLocation();
    } else {
      result.notImplemented();
    }
  }

  /**
   * 检查定位服务是否开启
   */
  private void checkLocationIsOpen(Result result) {
    Map<String, Object> map = new HashMap<>();
    if (isLocationEnabled()) {
      map.put("success", true);
    } else {
      map.put("success", false);
    }
    result.success(map);
  }

  /**
   * 返回定位服务开启状态
   */
  private boolean isLocationEnabled() {
    int locationMode;
    String locationProviders;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      try {
        locationMode = Settings.Secure.getInt(contentResolver,Settings.Secure.LOCATION_MODE);
      } catch (Settings.SettingNotFoundException e) {
        e.printStackTrace();
        return false;
      }
      return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    } else {
      locationProviders = Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
      return !TextUtils.isEmpty(locationProviders);
    }
  }

  /**
   * 打开定位服务设置页
   */
  private void openSetting() {
    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
    aContext.startActivity(intent);
  }

  @SuppressWarnings("uncheckWarning")
  private void getLocation() {
    Location location = getMyLocation();
    HashMap map = new HashMap();
    if (location != null) {
      map.put("latitude", location.getLatitude());
      map.put("longitude", location.getLongitude());
      channel.invokeMethod("receiveLocation", map);
    } else {
      map.put("error", "error");
      channel.invokeMethod("receiveLocation", map);
    }
  }

  private String provider;

  private Location getMyLocation() {
    //获取定位服务
    LocationManager locationManager = (LocationManager) aContext.getSystemService(Context.LOCATION_SERVICE);
    //获取当前可用的位置控制器
    List<String> list = locationManager.getProviders(true);

    if (list.contains(LocationManager.GPS_PROVIDER)) {
//            GPS位置控制器
      provider = LocationManager.GPS_PROVIDER;//GPS定位
    } else if (list.contains(LocationManager.NETWORK_PROVIDER)) {
//            网络位置控制器
      provider = LocationManager.NETWORK_PROVIDER;//网络定位
    }

    if (provider != null) {
      if (ActivityCompat.checkSelfPermission(aContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
              && ActivityCompat.checkSelfPermission(aContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return null;
      }

      return locationManager.getLastKnownLocation(provider);
    }


    return null;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
