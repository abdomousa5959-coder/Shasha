package com.shasha.player;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import android.text.InputType;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final String BASE="https://x-men.wuaze.com";
    private static final int BG=Color.rgb(10,12,16),CARD=Color.rgb(24,29,36),GREEN=Color.rgb(43,215,139),FG=Color.WHITE,MUTED=Color.rgb(165,174,184);
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final List<Item> data=new ArrayList<>(), allEpisodes=new ArrayList<>();
    private LinearLayout root,list,playerBox;
    private TextView status,count,heading;
    private EditText search;
    private PlayerView playerView;
    private ExoPlayer player;
    private String section="live",username="aaa",csrf="";
    private boolean showingEpisodes=false,loggedIn=false;
    private static class Item {
        String id,seriesId,kind,name,category,seriesName,url,logo;
        int season,episode;
        Item(JSONObject j){
            id=j.optString("id");seriesId=j.optString("series_id");kind=j.optString("kind");
            name=j.optString("name","بدون اسم");category=j.optString("category","عام");
            seriesName=j.optString("series_name",name);url=j.optString("url");logo=j.optString("logo");
            season=j.optInt("season",0);episode=j.optInt("episode",0);
        }
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);CookieHandler.setDefault(new CookieManager(null,CookiePolicy.ACCEPT_ALL));getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);screen();showLogin();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams params(int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h<0?h:dp(h));p.topMargin=dp(top);return p;}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(primary?BG:FG);b.setTextSize(14);b.setBackground(shape(primary?GREEN:CARD));return b;}
    private void screen(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(BG);
        root=new LinearLayout(this);root.setOrientation(1);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(dp(16),dp(20),dp(16),dp(28));sc.addView(root);setContentView(sc);
        root.addView(text("▶  شاشة IPTV",25,GREEN,true));
        root.addView(text("قنوات وأفلام ومسلسلات من لوحة عائلتنا مباشرة",13,MUTED,false),params(30,2));
        playerBox=new LinearLayout(this);playerBox.setOrientation(1);playerBox.setBackground(shape(CARD));playerBox.setVisibility(View.GONE);root.addView(playerBox,params(-2,16));
        playerView=new PlayerView(this);playerView.setBackgroundColor(Color.BLACK);playerBox.addView(playerView,new LinearLayout.LayoutParams(-1,dp(220)));
        TextView close=text("إغلاق المشغل  ✕",14,GREEN,true);close.setPadding(dp(12),dp(12),dp(12),dp(12));playerBox.addView(close);close.setOnClickListener(v->stopPlayer());
        LinearLayout nav=new LinearLayout(this);root.addView(nav,params(50,18));
        addTab(nav,"القنوات","live");addTab(nav,"الأفلام","movies");addTab(nav,"المسلسلات","series");
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(FG);search.setHintTextColor(MUTED);search.setHint("ابحث في المحتوى");search.setPadding(dp(14),0,dp(14),0);search.setBackground(shape(CARD));root.addView(search,params(50,14));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){render();}public void afterTextChanged(android.text.Editable e){}});
        heading=text("القنوات المباشرة",19,FG,true);root.addView(heading,params(34,18));
        count=text("",12,MUTED,false);root.addView(count,params(24,0));
        status=text("سجّل الدخول لاستيراد المحتوى...",13,MUTED,false);root.addView(status,params(52,4));
        list=new LinearLayout(this);list.setOrientation(1);root.addView(list,params(-2,4));
    }
    private void addTab(LinearLayout nav,String label,String key){Button b=button(label,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(3),0,dp(3),0);nav.addView(b,p);b.setOnClickListener(v->{if(loggedIn)load(key);});}
    private String readResponse(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();
        if(in!=null){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);in.close();}
        String body=out.toString("UTF-8").trim();String type=c.getContentType()==null?"":c.getContentType().toLowerCase(Locale.ROOT);
        if(code<200||code>=300)throw new IOException("HTTP "+code+": "+body.substring(0,Math.min(160,body.length())));
        if(body.startsWith("<")||type.contains("text/html"))throw new IOException("الاستضافة أعادت HTML بدل JSON. تحقق من ملفات api.php وPHP على الاستضافة.");
        if(body.isEmpty())throw new IOException("استجابة فارغة من السيرفر.");
        return body;
    }
    private HttpURLConnection connection(String path)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();c.setConnectTimeout(20000);c.setReadTimeout(45000);c.setInstanceFollowRedirects(true);
        c.setRequestProperty("Accept","application/json, text/plain, */*");
        c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36");
        c.setRequestProperty("Referer",BASE+"/");c.setRequestProperty("Accept-Language","ar-EG,ar;q=0.9,en;q=0.8");return c;
    }
    private JSONObject getObject(String path)throws Exception{HttpURLConnection c=connection(path);try{return new JSONObject(readResponse(c));}finally{c.disconnect();}}
    private JSONObject postObject(String path,JSONObject body)throws Exception{
        HttpURLConnection c=connection(path);try{c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");if(!csrf.isEmpty())c.setRequestProperty("X-CSRF-Token",csrf);
            byte[] bytes=body.toString().getBytes("UTF-8");try(OutputStream o=c.getOutputStream()){o.write(bytes);}return new JSONObject(readResponse(c));
        }finally{c.disconnect();}
    }
    private void showLogin(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(8),dp(4),dp(8),dp(4));
        EditText user=new EditText(this);user.setSingleLine(true);user.setText(username);user.setHint("اسم مستخدم اللوحة");user.setTextColor(FG);user.setHintTextColor(MUTED);box.addView(user);
        EditText pass=new EditText(this);pass.setSingleLine(true);pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);pass.setHint("كلمة مرور اللوحة");pass.setTextColor(FG);pass.setHintTextColor(MUTED);box.addView(pass);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("تسجيل الدخول إلى اللوحة").setMessage("استخدم بيانات مدير لوحة عائلتنا، وليس بيانات Xtream.").setView(box).setCancelable(false).setNegativeButton("إغلاق التطبيق",(d,w)->finish()).setPositiveButton("اتصال واستيراد",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(btn->{username=user.getText().toString().trim();String password=pass.getText().toString();if(username.isEmpty()||password.isEmpty()){status.setText("أدخل اسم المستخدم وكلمة المرور.");return;}dialog.dismiss();login(username,password);}));dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void login(String user,String password){
        status.setText("جارٍ الاتصال بلوحة الموقع وتسجيل الدخول...");
        io.execute(()->{try{
            JSONObject me=getObject("/api.php?action=me");csrf=me.optString("csrf");
            JSONObject body=new JSONObject();body.put("username",user);body.put("password",password);
            JSONObject result=postObject("/api.php?action=login",body);csrf=result.optString("csrf",csrf);
            if(!result.optBoolean("authenticated",false))throw new IOException("تعذّر توثيق جلسة المدير.");
            loggedIn=true;runOnUiThread(()->{status.setText("تم تسجيل الدخول. جارٍ استيراد المحتوى من السيرفر...");load("live");});
        }catch(Exception e){runOnUiThread(()->{status.setText("فشل الاتصال: "+friendly(e));showLogin();});}});
    }
    private void load(String key){
        section=key;showingEpisodes=false;heading.setText(key.equals("live")?"القنوات المباشرة":key.equals("movies")?"الأفلام":"المسلسلات");status.setText("جارٍ استيراد المحتوى من السيرفر...");list.removeAllViews();
        io.execute(()->{try{
            String kind=key.equals("live")?"channel":key.equals("movies")?"movie":"episode";
            JSONObject response=getObject("/api.php?action=items&kind="+kind);JSONArray arr=response.optJSONArray("items");if(arr==null)throw new IOException("رد السيرفر لا يحتوي على items.");
            List<Item> result=new ArrayList<>();for(int i=0;i<arr.length();i++)result.add(new Item(arr.getJSONObject(i)));
            if(key.equals("series")){allEpisodes.clear();allEpisodes.addAll(result);LinkedHashMap<String,Item> groups=new LinkedHashMap<>();for(Item ep:result)if(!groups.containsKey(ep.seriesId)){Item s=new Item(new JSONObject());s.id=ep.seriesId;s.seriesId=ep.seriesId;s.name=ep.seriesName;s.category=ep.category;s.kind="series";groups.put(ep.seriesId,s);}result=new ArrayList<>(groups.values());}
            List<Item> finalResult=result;
            runOnUiThread(()->{data.clear();data.addAll(finalResult);render();status.setText(finalResult.isEmpty()?"السيرفر متصل، لكن لا يوجد محتوى في هذا القسم.":"تم استيراد "+finalResult.size()+" عنصر من السيرفر بنجاح.");});
        }catch(Exception e){runOnUiThread(()->status.setText("فشل استيراد المحتوى: "+friendly(e)));}});
    }
    private void render(){
        if(list==null)return;list.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);int shown=0;
        for(Item x:new ArrayList<>(data)){if(!q.isEmpty()&&!x.name.toLowerCase(Locale.ROOT).contains(q))continue;shown++;
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(13),dp(9),dp(13),dp(9));card.setBackground(shape(CARD));list.addView(card,params(-2,8));
            card.addView(text(x.name,16,FG,true));card.addView(text(x.category,12,MUTED,false));
            Button action=button(section.equals("series")&&!showingEpisodes?"عرض الحلقات":"تشغيل  ▶",true);card.addView(action,params(44,8));
            action.setOnClickListener(v->{if(section.equals("series")&&!showingEpisodes)episodes(x);else play(x);});
        }
        count.setText("العناصر: "+shown);if(shown==0)list.addView(text("لا توجد نتائج.",14,MUTED,false));
    }
    private void episodes(Item series){
        data.clear();for(Item ep:allEpisodes)if(ep.seriesId.equals(series.seriesId))data.add(ep);
        showingEpisodes=true;heading.setText(series.name+" - الحلقات");render();status.setText("الحلقات المستوردة: "+data.size());
    }
    private void play(Item x){
        String url=x.url;if(url==null||url.isEmpty()){status.setText("لا يوجد رابط تشغيل لهذا العنصر في اللوحة.");return;}
        if(!url.startsWith("https://")&&!url.startsWith("http://")){status.setText("رابط البث غير صالح.");return;}
        try{if(player==null){player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);player.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int s){if(s==Player.STATE_BUFFERING)status.setText("جارٍ تحميل البث...");if(s==Player.STATE_READY)status.setText("يعمل الآن.");}@Override public void onPlayerError(PlaybackException e){status.setText("خطأ تشغيل: "+e.getMessage());}});}
            playerBox.setVisibility(View.VISIBLE);MediaItem.Builder item=new MediaItem.Builder().setUri(Uri.parse(url));String path=Uri.parse(url).getPath();if(path!=null&&path.toLowerCase(Locale.ROOT).endsWith(".m3u8"))item.setMimeType(MimeTypes.APPLICATION_M3U8);player.setMediaItem(item.build());player.prepare();player.play();status.setText("جارٍ بدء التشغيل...");
        }catch(Exception e){status.setText("تعذّر تشغيل المحتوى: "+e.getMessage());}
    }
    private void stopPlayer(){if(player!=null){player.stop();player.release();player=null;}playerBox.setVisibility(View.GONE);}
    private String friendly(Exception e){String m=e.getMessage()==null?"خطأ غير معروف":e.getMessage();if(m.contains("UnknownHost")||m.contains("resolve"))return"النطاق غير متاح أو لا يوجد اتصال إنترنت.";if(m.contains("401"))return"بيانات مدير اللوحة غير صحيحة.";if(m.contains("403"))return"السيرفر رفض الطلب. حدّث اللوحة أو تحقق من الجلسة.";return m;}
    @Override protected void onDestroy(){stopPlayer();io.shutdownNow();super.onDestroy();}
}