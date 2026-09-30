package android.content;
public abstract class Context {
  public final <T> T getSystemService(Class<T> c){throw new RuntimeException();}
  public java.util.concurrent.Executor getMainExecutor(){throw new RuntimeException();}
  public String getPackageName(){throw new RuntimeException();}
  public abstract android.content.pm.PackageManager getPackageManager();
  public abstract android.content.res.Resources getResources();
  public abstract void startActivity(Intent i);
  public final int getColor(int id){return 0;}
}
