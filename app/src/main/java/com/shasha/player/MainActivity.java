package com.shasha.player;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO = 21;
    private static final String PREFS = "shasha";
    private static final String RECENT_KEY = "recent";
    private static final int BACKGROUND = Color.rgb(15, 15, 29);
    private static final int PANEL = Color.rgb(25, 25, 44);
    private static final int ACCENT = Color.rgb(184, 167, 255);
    private static final int TEXT = Color.rgb(243, 240, 255);
    private static final int MUTED = Color.rgb(184, 181, 201);

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView status;
    private EditText link;
    private LinearLayout recentList;
    private Uri selected;
    private long savedPosition;
    private boolean savedPlayWhenReady = true;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        buildScreen();
        if (savedInstanceState != null) {
            String uri = savedInstanceState.getString("video_uri");
            savedPosition = savedInstanceState.getLong("position", 0);
            savedPlayWhenReady = savedInstanceState.getBoolean("playing", false);
            if (uri != null) selected = Uri.parse(uri);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        if (player == null) createPlayer();
        if (selected != null) load(selected, savedPosition, savedPlayWhenReady);
    }

    @Override protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (selected != null) {
            out.putString("video_uri", selected.toString());
            out.putLong("position", player != null ? player.getCurrentPosition() : savedPosition);
            out.putBoolean("playing", player != null && player.getPlayWhenReady());
        }
    }

    private void createPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) status.setText("الفيديو جاهز للمشاهدة.");
                if (state == Player.STATE_BUFFERING) status.setText("جارٍ التحميل...");
            }
            @Override public void onPlayerError(PlaybackException error) {
                status.setText(describeError(error));
            }
        });
    }

    private void releasePlayer() {
        if (player == null) return;
        savedPosition = player.getCurrentPosition();
        savedPlayWhenReady = player.getPlayWhenReady();
        player.release();
        player = null;
    }

    private void load(Uri uri, long position, boolean play) {
        if (player == null) createPlayer();
        selected = uri;
        status.setText("جارٍ تحميل الفيديو...");
        MediaItem.Builder item = new MediaItem.Builder().setUri(uri);
        String path = uri.getPath();
        if (path != null && path.toLowerCase(Locale.ROOT).endsWith(".m3u8")) {
            item.setMimeType(MimeTypes.APPLICATION_M3U8);
            status.setText("جارٍ تحميل بث HLS...");
        }
        player.setMediaItem(item.build());
        player.seekTo(position);
        player.setPlayWhenReady(play);
        player.prepare();
    }

    private void openVideo(Uri uri, boolean play) {
        savedPosition = 0;
        savedPlayWhenReady = play;
        load(uri, 0, play);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.RIGHT);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams marginParams(int width, int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(top);
        return params;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTextColor(primary ? BACKGROUND : TEXT);
        button.setBackground(shape(primary ? ACCENT : PANEL, 12));
        return button;
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(20), dp(24), dp(20), dp(30));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(text("شاشة  •  نموذج أولي", 18, ACCENT, true));
        root.addView(text("الفيلم يبدأ هنا.", 30, TEXT, true), marginParams(-1, -2, 22));
        root.addView(text("رابط مرخّص، شاشة واحدة، وكل التركيز على المشهد.", 15, MUTED, false), marginParams(-1, -2, 4));

        playerView = new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);
        root.addView(playerView, marginParams(-1, dp(220), 24));

        status = text("اختار فيديو تجريبي أو ملف أو رابط مباشر.", 14, MUTED, false);
        root.addView(status, marginParams(-1, -2, 12));

        Button demo = button("جرّب فيديو تجريبي", false);
        root.addView(demo, marginParams(-1, dp(52), 20));
        demo.setOnClickListener(v -> openVideo(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.demo), true));

        Button choose = button("اختار فيديو من جهازك", false);
        root.addView(choose, marginParams(-1, dp(52), 10));
        choose.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivityForResult(intent, PICK_VIDEO); }
            catch (android.content.ActivityNotFoundException e) { status.setText("لا يوجد تطبيق متاح لاختيار ملف فيديو."); }
        });

        root.addView(text("رابط فيديو مباشر", 17, TEXT, true), marginParams(-1, -2, 26));
        link = new EditText(this);
        link.setSingleLine(true);
        link.setTextDirection(View.TEXT_DIRECTION_LTR);
        link.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        link.setHint("https://example.com/video.mp4 أو .m3u8");
        link.setHintTextColor(MUTED);
        link.setTextColor(TEXT);
        link.setTextSize(15);
        link.setPadding(dp(15), 0, dp(15), 0);
        link.setBackground(shape(PANEL, 12));
        root.addView(link, marginParams(-1, dp(52), 10));

        Button play = button("تشغيل الرابط", true);
        root.addView(play, marginParams(-1, dp(52), 10));
        play.setOnClickListener(v -> playLink(link.getText().toString().trim()));

        root.addView(text("آخر الروابط", 17, TEXT, true), marginParams(-1, -2, 26));
        recentList = new LinearLayout(this);
        recentList.setOrientation(LinearLayout.VERTICAL);
        root.addView(recentList, marginParams(-1, -2, 10));
        refreshRecent();

        root.addView(text("استخدم روابط عندك حق تشغيلها. يدعم الرابط HLS (.m3u8) عبر المشغّل. روابط الصفحات والمحتوى المحمي بـ DRM غير مدعومة.", 13, MUTED, false), marginParams(-1, -2, 24));
    }

    private String describeError(PlaybackException error) {
        switch (error.errorCode) {
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT:
                return "مقدرتش أوصل للرابط. اتأكد إن النت شغال والرابط صح.";
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS:
                return "السيرفر رفض الطلب (خطأ في الرابط أو الصلاحيات). الرابط ممكن يكون منتهي أو محتاج تسجيل دخول.";
            case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND:
                return "الملف مش موجود. ممكن يكون اتحذف أو اتنقل من مكانه.";
            case PlaybackException.ERROR_CODE_IO_NO_PERMISSION:
                return "التطبيق مش عنده صلاحية يقرأ الملف ده. اختار الملف من جديد.";
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED:
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED:
                return "صيغة الفيديو مش مدعومة على الجهاز ده.";
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED:
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED:
                return "ملف الفيديو تالف أو الرابط مش بيرجع فيديو صالح.";
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
                return "الجهاز مش قادر يفك تشفير الفيديو ده.";
            case PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR:
            case PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED:
                return "الفيديو محمي بـ DRM، ومش مدعوم في النسخة دي.";
            default:
                return "حصل خطأ أثناء التشغيل. جرّب رابط تاني أو ملف تاني.";
        }
    }

    private void playLink(String value) {
        if (value.isEmpty()) {
            status.setText("اكتب رابط فيديو الأول.");
            return;
        }
        Uri uri = Uri.parse(value);
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            status.setText("الرابط لازم يبدأ بـ https:// عشان يكون آمن.");
            return;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            status.setText("اكتب رابط فيديو مباشر يبدأ بـ https://");
            return;
        }
        rememberRecent(value);
        openVideo(uri, true);
    }

    private List<String> recent() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        List<String> list = new ArrayList<>();
        for (String item : prefs.getString(RECENT_KEY, "").split("\n")) if (!item.isEmpty()) list.add(item);
        return list;
    }

    private void rememberRecent(String url) {
        List<String> list = recent();
        list.remove(url);
        list.add(0, url);
        while (list.size() > 5) list.remove(list.size() - 1);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(RECENT_KEY, TextUtils.join("\n", list)).apply();
        refreshRecent();
    }

    private void refreshRecent() {
        if (recentList == null) return;
        recentList.removeAllViews();
        List<String> list = recent();
        if (list.isEmpty()) {
            recentList.addView(text("ما فيش روابط محفوظة لسه.", 14, MUTED, false));
            return;
        }
        for (String url : list) {
            Button item = button(url, false);
            item.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            item.setSingleLine(true);
            item.setEllipsize(TextUtils.TruncateAt.END);
            recentList.addView(item, marginParams(-1, dp(48), 8));
            item.setOnClickListener(v -> {
                link.setText(url);
                playLink(url);
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Some providers only grant temporary access.
            }
            openVideo(uri, true);
        }
    }
}
