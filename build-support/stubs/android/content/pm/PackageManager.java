package android.content.pm;
public abstract class PackageManager {
  public static class NameNotFoundException extends Exception {}
  public abstract java.util.List<ApplicationInfo> getInstalledApplications(int flags);
  public InstallSourceInfo getInstallSourceInfo(String p) throws NameNotFoundException {return null;}
  public abstract CharSequence getApplicationLabel(ApplicationInfo ai);
  public abstract android.graphics.drawable.Drawable getApplicationIcon(ApplicationInfo ai);
  public abstract android.content.Intent getLaunchIntentForPackage(String p);
}
