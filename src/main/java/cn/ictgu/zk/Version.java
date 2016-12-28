package cn.ictgu.zk;

/**
 * Version
 * Created by Silence on 2016/12/19.
 */
class Version {
  private final static String version = "AnySchedule-1.0.0";

  static String getVersion() {
    return version;
  }

  static boolean isCompatible(String dataVersion) {
    return version.compareTo(dataVersion) >= 0;
  }
}