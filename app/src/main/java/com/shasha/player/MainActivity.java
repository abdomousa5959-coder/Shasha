package com.shasha.player;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.TextUtils;
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
    private static final String USER="family_f3821697fe";
    private static final String PASS="zqjG5wAQqyIaFjbduo3AGQgeDKq9yfWg";
    private static final int BG=Color.rgb(10,12,16), CARD=Color.rgb(24,29,36), GREEN=Color.rgb(43,215,139), FG=Color.WHITE, MUTED=Color.rgb(165,174,184);
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final List<Item> data=new ArrayList<>();
    private LinearLayout root,list,playerBox;
    private TextView status,count,heading;
    private EditText search;
    private PlayerView playerView;
    private ExoPlayer player;
    private String section="live";
    private boolean showingEpisodes=false;
    private static class Item {
        String id,name,categoryId,category,url,logo;
        Item(JSONObject j) {
            id=j.optString("stream_id",j.optString("series_id",j.optString("id")));
            name=j.optString("name",j.optString("title","بدون اسم"));
            categoryId=j.optString("category_id","");
            category=j.optString("category_name",categoryId);
            url=j.optString("direct_source","");
            logo=j.optString("stream_icon",j.optString("cover",""));
        }
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);screen();load("live");}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams params(int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h<0?h:dp(h));p.topMargin=dp(top);return p;}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(primary?BG:FG);b.setTextSize(14);b.setBackground(shape(primary?GREEN:CARD));return b;}
    private void screen(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(BG);
        root=new LinearLayout(this);root.setOrientation(1);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setPadding(dp(16),dp(20),dp(16),dp(28));sc.addView(root);setContentView(sc);
        root.addView(text("▶  شاشة IPTV",25,GREEN,true));
        root.addView(text("قنوات وأفلام ومسلسلات من لوحتك مباشرة",13,MUTED,false),params(30,2));
        playerBox=new LinearLayout(this);playerBox.setOrientation(1);playerBox.setBackground(shape(CARD));playerBox.setVisibility(View.GONE);root.addView(playerBox,params(-2,16));
        playerView=new PlayerView(this);playerView.setBackgroundColor(Color.BLACK);playerBox.addView(playerView,new LinearLayout.LayoutParams(-1,dp(220)));
        TextView close=text("إغلاق المشغل  ✕",14,GREEN,true);close.setPadding(dp(12),dp(12),dp(12),dp(12));playerBox.addView(close);close.setOnClickListener(v->stopPlayer());
        LinearLayout nav=new LinearLayout(this);root.addView(nav,params(50,18));
        addTab(nav,"القنوات","live");addTab(nav,"الأفلام","movies");addTab(nav,"المسلسلات","series");
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(FG);search.setHintTextColor(MUTED);search.setHint("ابحث في المحتوى");search.setPadding(dp(14),0,dp(14),0);search.setBackground(shape(CARD));root.addView(search,params(50,14));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){render();}public void afterTextChanged(android.text.Editable e){}});
        heading=text("القنوات المباشرة",19,FG,true);root.addView(heading,params(34,18));
        count=text("",12,MUTED,false);root.addView(count,params(24,0));
        status=text("جارٍ الاتصال باللوحة...",13,MUTED,false);root.addView(status,params(48,4));
        list=new LinearLayout(this);list.setOrientation(1);root.addView(list,params(-2,4));
    }
    private void addTab(LinearLayout nav,String label,String key){Button b=button(label,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(3),0,dp(3),0);nav.addView(b,p);b.setOnClickListener(v->load(key));}
    private String auth(){try{return "username="+URLEncoder.encode(USER,"UTF-8")+"&password="+URLEncoder.encode(PASS,"UTF-8");}catch(Exception e){return "";}}
    private JSONArray requestArray(String path)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Shasha-Android/2.0");
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();c.disconnect();String body=out.toString("UTF-8");if(code<200||code>=300)throw new IOException("HTTP "+code+" "+body);return new JSONArray(body);
    }
    private JSONObject requestObject(String path)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);in.close();c.disconnect();if(code<200||code>=300)throw new IOException("HTTP "+code);return new JSONObject(out.toString("UTF-8"));
    }
    private void load(String key){
        section=key;showingEpisodes=false;heading.setText(key.equals("live")?"القنوات المباشرة":key.equals("movies")?"الأفلام":"المسلسلات");status.setText("جارٍ تحميل المحتوى من اللوحة...");list.removeAllViews();
        io.execute(()->{try{
            String action=key.equals("live")?"get_live_streams":key.equals("movies")?"get_vod_streams":"get_series";
            String ca=key.equals("live")?"get_live_categories":key.equals("movies")?"get_vod_categories":"get_series_categories";
            JSONArray cats=requestArray("/player_api.php?"+auth()+"&action="+ca);Map<String,String> names=new HashMap<>();for(int i=0;i<cats.length();i++){JSONObject c=cats.getJSONObject(i);names.put(c.optString("category_id"),c.optString("category_name"));}
            JSONArray arr=requestArray("/player_api.php?"+auth()+"&action="+action);List<Item> result=new ArrayList<>();for(int i=0;i<arr.length();i++){Item x=new Item(arr.getJSONObject(i));x.category=names.getOrDefault(x.categoryId,x.category);result.add(x);}
            runOnUiThread(()->{data.clear();data.addAll(result);render();status.setText(result.isEmpty()?"لا يوجد محتوى نشط في هذا القسم.":"تم الاتصال باللوحة بنجاح.");});
        }catch(Exception e){runOnUiThread(()->status.setText("فشل الاتصال: "+friendly(e)));}});
    }
    private void render(){
        if(list==null)return;list.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);int shown=0;
        for(Item x:new ArrayList<>(data)){if(!q.isEmpty()&&!x.name.toLowerCase(Locale.ROOT).contains(q))continue;shown++;
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(13),dp(9),dp(13),dp(9));card.setBackground(shape(CARD));list.addView(card,params(-2,8));
            card.addView(text(x.name,16,FG,true));card.addView(text(x.category,12,MUTED,false));
            Button play=button(section.equals("series")&&!showingEpisodes?"عرض الحلقات":"تشغيل  ▶",true);card.addView(play,params(44,8));
            play.setOnClickListener(v->{if(section.equals("series")&&!showingEpisodes)episodes(x);else play(x);});
        }
        count.setText("العناصر: "+shown);if(shown==0)list.addView(text("لا توجد نتائج.",14,MUTED,false));
    }
    private void episodes(Item series){
        status.setText("جارٍ تحميل الحلقات...");
        io.execute(()->{try{
            JSONObject obj=requestObject("/player_api.php?"+auth()+"&action=get_series_info&series_id="+URLEncoder.encode(series.id,"UTF-8"));
            JSONObject seasons=obj.optJSONObject("episodes");List<Item> result=new ArrayList<>();if(seasons!=null){Iterator<String> it=seasons.keys();while(it.hasNext()){String season=it.next();JSONArray a=seasons.optJSONArray(season);if(a!=null)for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);Item ep=new Item(j);ep.id=j.optString("id");ep.name="الموسم "+season+" - الحلقة "+j.optString("episode_num")+"  "+j.optString("title");result.add(ep);}}}
            runOnUiThread(()->{data.clear();data.addAll(result);showingEpisodes=true;heading.setText(series.name+" - الحلقات");render();status.setText("اختر الحلقة للمشاهدة.");});
        }catch(Exception e){runOnUiThread(()->status.setText("تعذّر تحميل الحلقات: "+friendly(e)));}});
    }
    private void play(Item x){
        String url=x.url;if(url.isEmpty()){String kind=section.equals("live")?"channel":section.equals("movies")?"movie":"episode";url=BASE+"/play.php?kind="+kind+"&id="+x.id+"&"+auth();}
        if(!url.startsWith("https://")&&!url.startsWith("http://")){status.setText("رابط البث غير صالح.");return;}
        try{if(player==null){player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);player.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int s){if(s==Player.STATE_BUFFERING)status.setText("جارٍ تحميل البث...");if(s==Player.STATE_READY)status.setText("يعمل الآن.");}@Override public void onPlayerError(PlaybackException e){status.setText("خطأ تشغيل: "+e.getMessage());}});}
            playerBox.setVisibility(View.VISIBLE);MediaItem.Builder item=new MediaItem.Builder().setUri(Uri.parse(url));String path=Uri.parse(url).getPath();if(path!=null&&path.toLowerCase(Locale.ROOT).endsWith(".m3u8"))item.setMimeType(MimeTypes.APPLICATION_M3U8);player.setMediaItem(item.build());player.prepare();player.play();status.setText("جارٍ بدء التشغيل...");
        }catch(Exception e){status.setText("تعذّر تشغيل المحتوى: "+e.getMessage());}
    }
    private void stopPlayer(){if(player!=null){player.stop();player.release();player=null;}playerBox.setVisibility(View.GONE);}
    private String friendly(Exception e){String m=e.getMessage()==null?"خطأ غير معروف":e.getMessage();if(m.contains("UnknownHost")||m.contains("resolve"))return"النطاق غير متاح أو لا يوجد اتصال إنترنت";if(m.contains("401"))return"بيانات تشغيل اللوحة غير صحيحة";if(m.contains("403"))return"الاستضافة رفضت الاتصال (403)";return m;}
    @Override protected void onDestroy(){stopPlayer();io.shutdownNow();super.onDestroy();}
}