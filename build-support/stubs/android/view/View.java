package android.view;
public class View {
  public interface OnClickListener { void onClick(View v); }
  public interface OnLongClickListener { boolean onLongClick(View v); }
  public View(android.content.Context c){}
  public void setPadding(int a,int b,int c,int d){}
  public void setOnClickListener(OnClickListener l){}
  public void setOnLongClickListener(OnLongClickListener l){}
  public void setFitsSystemWindows(boolean b){}
  public void setMinimumHeight(int h){}
  public void setBackground(android.graphics.drawable.Drawable d){}
  public void setAlpha(float a){}
  public void setLayoutParams(android.view.ViewGroup.LayoutParams p){}
  public void setContentDescription(CharSequence c){}
}
