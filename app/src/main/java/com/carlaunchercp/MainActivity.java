package com.carlaunchercp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Car-optimised home screen: speed, radio, two music shortcuts, phone, navigation and favourites. */
public class MainActivity extends Activity implements Speedometer.Listener {

    private Prefs prefs;
    private ThemePalette theme;
    private Speedometer speedometer;
    private boolean landscape;

    private SpeedGaugeView gauge;
    private TextView clockView;
    private TextView dateView;
    private EqualizerBarsView clockBars;
    private TextView gpsLabel;
    private View gpsDot;
    private TextView maxView;
    private GridLayout favoritesGrid;
    private ImageView playPauseIcon;
    private FrameLayout playPauseButton;
    private boolean mediaPlaying = true;
    private TileView radioTile;
    private TileView musicTile;
    private TileView music2Tile;
    private TileView navTile;

    private float maxSpeed = 0f;
    private boolean hasFix = false;
    private boolean permissionMissing = false;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 10_000L);
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase, new Prefs(newBase).language()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        theme = ThemePalette.byId(prefs.themeId());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        speedometer = new Speedometer(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        speedometer.start();
        refreshFavorites();
        refreshTileSubtitles();
        updateGpsChip();
        clockTick.run();
        applyImmersive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        speedometer.stop();
        clockHandler.removeCallbacks(clockTick);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    @Override
    public void onBackPressed() {
        // Home screen: back does nothing, as on every car launcher.
    }

    private void applyImmersive() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // ------------------------------------------------------------------ layout

    private void buildUi() {
        landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        int tileCell = tileCellSize(landscape);

        FrameLayout rootFrame = new FrameLayout(this);
        CarBackgroundView backdrop = new CarBackgroundView(this);
        backdrop.setTheme(theme);
        rootFrame.addView(backdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 22), Ui.dp(this, 0), Ui.dp(this, 22), Ui.dp(this, 14));
        content.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        LinearLayout.LayoutParams speedLp = landscape
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.36f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 240));
        body.addView(buildSpeedCard(), speedLp);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.addView(buildTiles(landscape, tileCell), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                landscape ? tileCell : ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams favoritesLp = landscape
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        favoritesLp.topMargin = Ui.dp(this, 10);
        right.addView(buildFavoritesCard(landscape), favoritesLp);

        LinearLayout.LayoutParams rightLp = landscape
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.64f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (landscape) rightLp.leftMargin = Ui.dp(this, 18);
        body.addView(right, rightLp);

        LinearLayout.LayoutParams bodyLp = landscape
                ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = Ui.dp(this, 10);
        content.addView(body, bodyLp);

        View contentRoot = content;
        if (!landscape) {
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.addView(content, new ScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            contentRoot = scroll;
        }
        rootFrame.addView(contentRoot, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(rootFrame);

        content.setAlpha(0f);
        content.setTranslationY(Ui.dp(this, 10));
        content.animate().alpha(1f).translationY(0f).setDuration(320).start();
        updateClock();
        updateGpsChip();
    }

    /** Side of a square tile: a touch smaller than before, to leave room for the lower motion strip. */
    private int tileCellSize(boolean landscape) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int gap = Ui.dp(this, landscape ? 12 : 10);
        if (landscape) {
            int rightWidth = Math.round((screenWidth - Ui.dp(this, 44) - Ui.dp(this, 18)) * 0.64f);
            int cell = (rightWidth - gap * 4) / 5;
            return Math.max(Ui.dp(this, 100), Math.min(cell, Ui.dp(this, 142)));
        }
        int cell = (screenWidth - Ui.dp(this, 44) - gap) / 2;
        return Math.max(Ui.dp(this, 110), cell);
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        clockView = Ui.text(this, "--:--", 54f, 0xFFF7F9FC, Ui.displayBold(this));
        clockView.setLetterSpacing(0.01f);

        dateView = Ui.text(this, "", 19f, 0xFF9AA4B3, Ui.medium(this));
        dateView.setLetterSpacing(0.04f);

        clockBars = new EqualizerBarsView(this);
        clockBars.setAccent(theme.accent);
        clockBars.setRunning(true);
        LinearLayout.LayoutParams barsLp = new LinearLayout.LayoutParams(
                Ui.dp(this, 32), Ui.dp(this, 26));
        barsLp.leftMargin = Ui.dp(this, 14);

        LinearLayout clockBlock = new LinearLayout(this);
        clockBlock.setOrientation(LinearLayout.VERTICAL);
        if (landscape) {
            LinearLayout clockRow = new LinearLayout(this);
            clockRow.setOrientation(LinearLayout.HORIZONTAL);
            clockRow.setGravity(Gravity.CENTER_VERTICAL);
            clockRow.addView(clockView);
            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateLp.leftMargin = Ui.dp(this, 16);
            clockRow.addView(dateView, dateLp);
            clockRow.addView(clockBars, barsLp);
            clockBlock.addView(clockRow);
        } else {
            clockBlock.addView(clockView);
            LinearLayout dateRow = new LinearLayout(this);
            dateRow.setOrientation(LinearLayout.HORIZONTAL);
            dateRow.setGravity(Gravity.CENTER_VERTICAL);
            dateRow.addView(dateView);
            dateRow.addView(clockBars, barsLp);
            LinearLayout.LayoutParams dateRowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateRowLp.topMargin = Ui.dp(this, 2);
            clockBlock.addView(dateRow, dateRowLp);
        }
        header.addView(clockBlock);

        // Transport keys live in the free space beside the clock, and drop below it
        // on screens too narrow to keep everything on one line.
        boolean inlineTransport = landscape
                && getResources().getConfiguration().screenWidthDp >= 780;
        View transport = buildTransport();
        if (inlineTransport) {
            FrameLayout transportSlot = new FrameLayout(this);
            transportSlot.addView(transport, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER));
            header.addView(transportSlot, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            View spacer = new View(this);
            header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        }

        LinearLayout gpsChip = new LinearLayout(this);
        gpsChip.setOrientation(LinearLayout.HORIZONTAL);
        gpsChip.setGravity(Gravity.CENTER_VERTICAL);
        gpsChip.setPadding(Ui.dp(this, 14), Ui.dp(this, 9), Ui.dp(this, 16), Ui.dp(this, 9));
        gpsChip.setBackground(Ui.shape(0x0DFFFFFF, 999f, this, 0x16FFFFFF, 1f));
        gpsChip.setClickable(true);
        gpsChip.setOnClickListener(v -> handleGpsChipClick());

        gpsDot = new View(this);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(Ui.dp(this, 9), Ui.dp(this, 9));
        dotLp.rightMargin = Ui.dp(this, 9);
        gpsDot.setBackground(Ui.oval(0xFF8A93A2, this, 0, 0));
        gpsChip.addView(gpsDot, dotLp);

        gpsLabel = Ui.text(this, getString(R.string.gps), 16f, 0xFFB9C2D0, Ui.display(this));
        gpsLabel.setLetterSpacing(0.12f);
        gpsChip.addView(gpsLabel);

        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipLp.rightMargin = Ui.dp(this, 12);
        header.addView(gpsChip, chipLp);

        View themeButton = roundButton(R.drawable.ic_palette, 0, v ->
                ThemePanel.show(this, theme, palette -> {
                    prefs.setThemeId(palette.id);
                    recreate();
                }));
        LinearLayout.LayoutParams themeLp = new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54));
        themeLp.rightMargin = Ui.dp(this, 10);
        header.addView(themeButton, themeLp);

        View appsButton = roundButton(R.drawable.ic_apps_grid, 0, v ->
                AllAppsDialog.show(this, theme, getString(R.string.app_picker_title)));
        header.addView(appsButton, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));

        // Thin accent streaks rising behind the header, back a touch so the clock still reads first.
        int headerZone = Ui.dp(this, inlineTransport ? 96 : 124);
        FrameLayout headerZoneBox = new FrameLayout(this);
        SmokeView headerSmoke = new SmokeView(this);
        headerSmoke.setTheme(theme);
        headerZoneBox.addView(headerSmoke, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, headerZone, Gravity.TOP));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!inlineTransport) {
            LinearLayout transportRow = new LinearLayout(this);
            transportRow.setOrientation(LinearLayout.HORIZONTAL);
            transportRow.setGravity(Gravity.CENTER);
            transportRow.addView(transport);
            LinearLayout.LayoutParams transportLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            transportLp.topMargin = Ui.dp(this, 2);
            column.addView(transportRow, transportLp);
        }
        headerZoneBox.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));
        FrameLayout headerWrap = new FrameLayout(this);
        headerWrap.addView(headerZoneBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        headerWrap.setAlpha(0.92f);

        // Same motion lower on the screen, so the body feels like one surface, not a cut-out.
        LinearLayout bodyStrip = new LinearLayout(this);
        bodyStrip.setOrientation(LinearLayout.HORIZONTAL);
        bodyStrip.setGravity(Gravity.TOP);
        SmokeView bodySmoke = new SmokeView(this);
        bodySmoke.setTheme(theme);
        bodySmoke.setAnchorBottom(true);
        int bodyStripHeight = Ui.dp(this, landscape ? 64 : 84);
        bodyStrip.addView(bodySmoke, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bodyStripHeight, Gravity.TOP));
        bodyStrip.setAlpha(0.9f);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.addView(headerWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        outer.addView(bodyStrip, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bodyStripHeight));
        return outer;
    }

    /** Back, play/pause and next: keys for whichever app owns the media session. */
    private View buildTransport() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(mediaButton(R.drawable.ic_media_prev, R.string.media_previous,
                MediaKeys.PREVIOUS, false, 0));
        row.addView(mediaButton(R.drawable.ic_media_pause, R.string.media_pause,
                MediaKeys.PLAY_PAUSE, true, 8));
        row.addView(mediaButton(R.drawable.ic_media_next, R.string.media_next,
                MediaKeys.NEXT, false, 8));
        return row;
    }

    private FrameLayout mediaButton(int iconRes, int descriptionRes, final int keyCode,
                                    boolean primary, int leftMarginDp) {
        int size = Ui.dp(this, primary ? 52 : 44);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(primary
                ? Ui.card(this, Ui.mix(theme.accent, 0xFFFFFFFF, 0.22f), Ui.darken(theme.accent, 0.12f),
                999f, 0x40FFFFFF, theme.withAlpha(0x66))
                : Ui.card(this, 0x16FFFFFF, 0x0AFFFFFF, 999f, 0x1AFFFFFF, theme.withAlpha(0x40)));
        ImageView icon = Ui.icon(this, iconRes, primary ? 26f : 22f, 0xFFFFFFFF);
        int iconSize = Ui.dp(this, primary ? 26 : 22);
        box.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
        box.setContentDescription(getString(descriptionRes));
        box.setClickable(true);
        box.setFocusable(true);
        box.setOnClickListener(v -> {
            MediaKeys.send(this, keyCode);
            if (primary) togglePlayPause();
        });
        Ui.pressScale(box);
        if (primary) {
            playPauseIcon = icon;
            playPauseButton = box;
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.leftMargin = Ui.dp(this, leftMarginDp);
        box.setLayoutParams(lp);
        return box;
    }

    /** Flips the glyph after a press; the player itself stays the source of truth. */
    private void togglePlayPause() {
        mediaPlaying = !mediaPlaying;
        if (playPauseIcon != null) {
            playPauseIcon.setImageResource(mediaPlaying
                    ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
        }
        if (playPauseButton != null) {
            playPauseButton.setContentDescription(getString(mediaPlaying
                    ? R.string.media_pause : R.string.media_play));
        }
    }

    private View roundButton(int iconRes, int tint, View.OnClickListener listener) {
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.card(this, 0x14FFFFFF, 0x0AFFFFFF, 999f, 0x16FFFFFF,
                theme.withAlpha(0x40)));
        box.addView(Ui.icon(this, iconRes, 28f, tint),
                new FrameLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28), Gravity.CENTER));
        box.setClickable(true);
        box.setOnClickListener(listener);
        Ui.pressScale(box);
        return box;
    }

    private View buildSpeedCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 18);
        card.setPadding(pad, pad, pad, Ui.dp(this, 8));
        card.setBackground(Ui.card(this, 0xFF14181F, 0xFF0D1015, 26f, 0x1FFFFFFF, 0));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = Ui.text(this, getString(R.string.speed_label), 15f, 0xFF9AA3B2,
                Ui.displayBold(this));
        label.setLetterSpacing(0.18f);
        head.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        maxView = Ui.text(this, getString(R.string.speed_max, 0), 15f, theme.accent,
                Ui.displayBold(this));
        maxView.setLetterSpacing(0.1f);
        head.addView(maxView);

        card.addView(head);

        gauge = new SpeedGaugeView(this);
        gauge.setTheme(theme);
        card.addView(gauge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return card;
    }

    private View buildTiles(boolean landscape, int cell) {
        radioTile = makeTile(R.drawable.ic_radio, R.string.tile_radio, radioSubtitle(), true);
        radioTile.setOnClickListener(v -> openRadio());
        radioTile.setOnLongClickListener(v -> {
            chooseRadioApp();
            return true;
        });

        musicTile = makeTile(R.drawable.ic_bluetooth, R.string.tile_music, musicSubtitle(), false);
        musicTile.setOnClickListener(v -> openMusic());
        musicTile.setOnLongClickListener(v -> {
            chooseMusicApp();
            return true;
        });

        music2Tile = makeTile(R.drawable.ic_music, R.string.tile_music_2, music2Subtitle(), false);
        music2Tile.setOnClickListener(v -> openMusic2());
        music2Tile.setOnLongClickListener(v -> {
            chooseMusic2App();
            return true;
        });

        TileView phoneTile = makeTile(R.drawable.ic_phone, R.string.tile_phone,
                getString(R.string.tile_phone_sub), false);
        phoneTile.setOnClickListener(v -> LauncherActions.openDialer(this));

        navTile = makeTile(R.drawable.ic_navigation, R.string.tile_nav, navSubtitle(), false);
        navTile.setOnClickListener(v -> openNavigation());
        navTile.setOnLongClickListener(v -> {
            chooseNavApp();
            return true;
        });

        if (landscape) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            TileView[] tiles = {radioTile, musicTile, music2Tile, phoneTile, navTile};
            for (int i = 0; i < tiles.length; i++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, cell, 1f);
                if (i > 0) lp.leftMargin = Ui.dp(this, 12);
                row.addView(tiles[i], lp);
            }
            return row;
        }

        LinearLayout stack = new LinearLayout(this);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.addView(tilePair(radioTile, musicTile, cell, 0));
        stack.addView(tilePair(music2Tile, phoneTile, cell, Ui.dp(this, 10)));
        stack.addView(tilePair(navTile, null, cell, Ui.dp(this, 10)));
        return stack;
    }

    private TileView makeTile(int iconRes, int titleRes, CharSequence subtitle, boolean primary) {
        TileView tile = new TileView(this, theme, primary);
        tile.setData(iconRes, getString(titleRes), subtitle);
        return tile;
    }

    private View tilePair(TileView left, TileView right, int cell, int topMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(left, new LinearLayout.LayoutParams(0, cell, 1f));
        LinearLayout.LayoutParams secondLp = new LinearLayout.LayoutParams(0, cell, 1f);
        secondLp.leftMargin = Ui.dp(this, 10);
        row.addView(right != null ? right : new View(this), secondLp);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, cell);
        rowLp.topMargin = topMargin;
        row.setLayoutParams(rowLp);
        return row;
    }

    private View buildFavoritesCard(boolean landscape) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        card.setPadding(pad, pad, pad, Ui.dp(this, 12));
        card.setBackground(Ui.card(this, 0xFF12161D, 0xFF0C0F14, 26f, 0x17FFFFFF, 0));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = Ui.text(this, getString(R.string.favorites_title), 15f, 0xFF9AA3B2,
                Ui.displayBold(this));
        label.setLetterSpacing(0.18f);
        head.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView edit = Ui.text(this, getString(R.string.favorites_edit), 18f, theme.accent,
                Ui.display(this));
        edit.setPadding(Ui.dp(this, 10), Ui.dp(this, 2), 0, Ui.dp(this, 2));
        edit.setClickable(true);
        edit.setOnClickListener(v -> addFavorite());
        head.addView(edit);

        card.addView(head);

                    favoritesGrid = new GridLayout(this);
            favoritesGrid.setColumnCount(landscape ? 3 : 5);
            LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    landscape ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT,
                    landscape ? 1f : 0f);
            gridLp.topMargin = Ui.dp(this, 6);
            card.addView(favoritesGrid, gridLp);
        return card;
    }

    // ------------------------------------------------------------- favourites

    private void refreshFavorites() {
        if (favoritesGrid == null) return;
        favoritesGrid.removeAllViews();
        List<String> favorites = prefs.favorites();
        boolean showAdd = favorites.size() < Prefs.MAX_FAVORITES;
        int cells = favorites.size() + (showAdd ? 1 : 0);
        int columns = landscape ? 3 : 6;

        for (String flattened : favorites) {
            favoritesGrid.addView(landscape ? wideAppCell(flattened) : appColumnCell(flattened),
                    gridCellParams());
        }
        if (showAdd) {
            favoritesGrid.addView(addCell(), gridCellParams());
        }
        int remainder = cells % columns;
        if (remainder != 0) {
            for (int i = 0; i < columns - remainder; i++) {
                favoritesGrid.addView(new View(this), gridCellParams());
            }
        }
    }

    private GridLayout.LayoutParams gridCellParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = landscape ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        if (landscape) lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
        return lp;
    }

    /** Landscape cell: icon and name on one line, like a dashboard shortcut. */
    private View wideAppCell(final String flattened) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        cell.setBackground(Ui.card(this, 0x0FFFFFFF, 0x08FFFFFF, 16f, 0x16FFFFFF,
                theme.withAlpha(0x2A)));

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            icon.setImageDrawable(getPackageManager()
                    .getApplicationIcon(packageOf(flattened)));
        } catch (Exception e) {
            icon.setImageResource(R.drawable.ic_apps_grid);
        }
        cell.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 34), Ui.dp(this, 34)));

        TextView label = Ui.text(this, LauncherActions.appLabel(this, flattened), 19f,
                0xFFDCE3EE, Ui.medium(this));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelLp.leftMargin = Ui.dp(this, 12);
        cell.addView(label, labelLp);

        cell.setClickable(true);
        cell.setOnClickListener(v -> LauncherActions.launchComponent(this, flattened));
        cell.setOnLongClickListener(v -> {
            showFavoriteMenu(flattened);
            return true;
        });
        Ui.pressScale(cell);
        return cell;
    }

    /** Portrait cell: icon above the name. */
    private View appColumnCell(final String flattened) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);

        int boxSize = Ui.dp(this, 64);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.shape(0x12FFFFFF, 16f, this, 0x1AFFFFFF, 1f));
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            icon.setImageDrawable(getPackageManager().getApplicationIcon(packageOf(flattened)));
        } catch (Exception e) {
            icon.setImageResource(R.drawable.ic_apps_grid);
        }
        box.addView(icon, new FrameLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38), Gravity.CENTER));
        cell.addView(box, new LinearLayout.LayoutParams(boxSize, boxSize));

        TextView label = Ui.text(this, LauncherActions.appLabel(this, flattened), 14.5f,
                0xFFAEB6C4, Ui.medium(this));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = Ui.dp(this, 6);
        cell.addView(label, labelLp);

        cell.setClickable(true);
        cell.setOnClickListener(v -> LauncherActions.launchComponent(this, flattened));
        cell.setOnLongClickListener(v -> {
            showFavoriteMenu(flattened);
            return true;
        });
        Ui.pressScale(cell);
        return cell;
    }

    private View addCell() {
        String add = getString(R.string.favorites_add);
        if (landscape) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            cell.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
            cell.setBackground(Ui.card(this, theme.withAlpha(0x1A), theme.withAlpha(0x0F), 16f,
                    theme.withAlpha(0x4D), theme.withAlpha(0x30)));
            cell.addView(Ui.icon(this, R.drawable.ic_add, 26f, theme.accent),
                    new LinearLayout.LayoutParams(Ui.dp(this, 26), Ui.dp(this, 26)));
            TextView label = Ui.text(this, add, 19f, theme.withAlpha(0xE6), Ui.medium(this));
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.leftMargin = Ui.dp(this, 12);
            cell.addView(label, labelLp);
            cell.setClickable(true);
            cell.setOnClickListener(v -> addFavorite());
            Ui.pressScale(cell);
            return cell;
        }

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);

        int boxSize = Ui.dp(this, 64);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.shape(theme.withAlpha(0x1A), 16f, this, theme.withAlpha(0x59), 1f));
        box.addView(Ui.icon(this, R.drawable.ic_add, 28f, theme.accent),
                new FrameLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28), Gravity.CENTER));
        cell.addView(box, new LinearLayout.LayoutParams(boxSize, boxSize));

        TextView label = Ui.text(this, add, 14.5f, theme.withAlpha(0xCC), Ui.medium(this));
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = Ui.dp(this, 6);
        cell.addView(label, labelLp);

        cell.setClickable(true);
        cell.setOnClickListener(v -> addFavorite());
        Ui.pressScale(cell);
        return cell;
    }

    private void addFavorite() {
        AppPickerDialog.show(this, theme, getString(R.string.add_favorite_title), component -> {
            prefs.addFavorite(component.flattenToString());
            refreshFavorites();
        });
    }

    private void showFavoriteMenu(final String flattened) {
        final int index = prefs.favorites().indexOf(flattened);
        new AlertDialog.Builder(this)
                .setTitle(LauncherActions.appLabel(this, flattened))
                .setItems(new String[]{getString(R.string.replace_app), getString(R.string.remove_favorite)},
                        (dialog, which) -> {
                            if (which == 0) {
                                AppPickerDialog.show(this, theme, getString(R.string.choose_app), component -> {
                                    prefs.replaceFavorite(index, component.flattenToString());
                                    refreshFavorites();
                                });
                            } else {
                                prefs.removeFavorite(index);
                                refreshFavorites();
                            }
                        })
                .show();
    }

    private String packageOf(String flattened) {
        ComponentName component = ComponentName.unflattenFromString(flattened == null ? "" : flattened);
        return component == null ? "" : component.getPackageName();
    }

    // ------------------------------------------------------------------ actions

    private String radioSubtitle() {
        String target = prefs.radioTarget();
        if (target == null) return getString(R.string.tile_radio_sub);
        String label = LauncherActions.appLabel(this, target);
        return label.isEmpty() ? getString(R.string.tile_radio_sub) : label;
    }

    private String musicSubtitle() {
        String target = prefs.musicTarget();
        if (target == null) return getString(R.string.tile_music_sub);
        String label = LauncherActions.appLabel(this, target);
        return label.isEmpty() ? getString(R.string.tile_music_sub) : label;
    }

    private String music2Subtitle() {
        String target = prefs.musicTarget2();
        if (target == null) return getString(R.string.tile_music_2_sub);
        String label = LauncherActions.appLabel(this, target);
        return label.isEmpty() ? getString(R.string.tile_music_2_sub) : label;
    }

    private String navSubtitle() {
        String target = prefs.navTarget();
        if (target == null) return getString(R.string.tile_nav_sub);
        String label = LauncherActions.appLabel(this, target);
        return label.isEmpty() ? getString(R.string.tile_nav_sub) : label;
    }

    private void refreshTileSubtitles() {
        if (radioTile != null) radioTile.setSubtitle(radioSubtitle());
        if (musicTile != null) musicTile.setSubtitle(musicSubtitle());
        if (music2Tile != null) music2Tile.setSubtitle(music2Subtitle());
        if (navTile != null) navTile.setSubtitle(navSubtitle());
    }

    private void openRadio() {
        if (LauncherActions.openRadio(this, prefs)) {
            radioTile.setSubtitle(radioSubtitle());
            return;
        }
        chooseRadioApp();
    }

    private void chooseRadioApp() {
        AppPickerDialog.show(this, theme, getString(R.string.choose_radio), component -> {
            prefs.setRadioTarget(component.flattenToString());
            radioTile.setSubtitle(radioSubtitle());
        });
    }

    private void openMusic() {
        if (LauncherActions.openMusic(this, prefs)) return;
        chooseMusicApp();
    }

    private void chooseMusicApp() {
        AppPickerDialog.show(this, theme, getString(R.string.choose_music), component -> {
            prefs.setMusicTarget(component.flattenToString());
            musicTile.setSubtitle(musicSubtitle());
            LauncherActions.launchComponent(this, component.flattenToString());
        });
    }

    private void openMusic2() {
        String saved = prefs.musicTarget2();
        if (saved != null && LauncherActions.launchComponent(this, saved)) return;
        chooseMusic2App();
    }

    private void chooseMusic2App() {
        AppPickerDialog.show(this, theme, getString(R.string.choose_music_2), component -> {
            prefs.setMusicTarget2(component.flattenToString());
            music2Tile.setSubtitle(music2Subtitle());
            LauncherActions.launchComponent(this, component.flattenToString());
        });
    }

    private void openNavigation() {
        if (LauncherActions.openNavigation(this, prefs)) {
            navTile.setSubtitle(navSubtitle());
            return;
        }
        chooseNavApp();
    }

    private void chooseNavApp() {
        AppPickerDialog.show(this, theme, getString(R.string.choose_nav), component -> {
            prefs.setNavTarget(component.flattenToString());
            navTile.setSubtitle(navSubtitle());
            LauncherActions.launchComponent(this, component.flattenToString());
        });
    }

    // ---------------------------------------------------------------------- GPS

    private void updateGpsChip() {
        if (gpsLabel == null) return;
        if (permissionMissing) {
            gpsLabel.setText(R.string.gps_off);
            gpsLabel.setTextColor(0xFF97A0AE);
            gpsDot.setBackground(Ui.oval(0xFF6D7685, this, 0, 0));
        } else if (hasFix) {
            gpsLabel.setText(R.string.gps_on);
            gpsLabel.setTextColor(0xFFE7EDF6);
            gpsDot.setBackground(Ui.oval(theme.accent, this, 0, 0));
        } else {
            gpsLabel.setText(R.string.gps_search);
            gpsLabel.setTextColor(0xFF97A0AE);
            gpsDot.setBackground(Ui.oval(0xFFB9A76B, this, 0, 0));
        }
    }

    private void handleGpsChipClick() {
        if (permissionMissing) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    @Override
    public void onSpeed(final float kmh, final boolean fix) {
        runOnUiThread(() -> {
            gauge.setHasFix(fix);
            gauge.setValue(fix ? kmh : 0f);
            if (fix != hasFix) {
                hasFix = fix;
                updateGpsChip();
            }
            if (fix && kmh > maxSpeed) {
                maxSpeed = kmh;
                maxView.setText(getString(R.string.speed_max, Math.round(maxSpeed)));
            }
        });
    }

    @Override
    public void onGpsPermissionMissing() {
        runOnUiThread(() -> {
            permissionMissing = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED;
            gauge.setHasFix(false);
            gauge.setDimmed(permissionMissing);
            updateGpsChip();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 41) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        permissionMissing = !granted;
        gauge.setDimmed(!granted);
        updateGpsChip();
        if (granted) speedometer.start();
    }

    // -------------------------------------------------------------------- clock

    private void updateClock() {
        Date now = new Date();
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        clockView.setText(new SimpleDateFormat("HH:mm", locale).format(now));
        dateView.setText(new SimpleDateFormat("EEEE d MMMM", locale).format(now));
    }
}
