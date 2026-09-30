package android.app;
public class Activity extends android.content.Context {
  protected void onCreate(android.os.Bundle b){}
  protected void onResume(){}
  protected void onNewIntent(android.content.Intent i){}
  public android.content.Intent getIntent(){return null;}
  public void setIntent(android.content.Intent i){}
  public void setContentView(android.view.View v){}
  public void runOnUiThread(Runnable r){}
  public void setTitle(CharSequence t){}
  public void setTheme(int id){}
  public android.view.Window getWindow(){return null;}
  public android.content.pm.PackageManager getPackageManager(){return null;}
  public android.content.res.Resources getResources(){return null;}
  public void startActivity(android.content.Intent i){}
}
