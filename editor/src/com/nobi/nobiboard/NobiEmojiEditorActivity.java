package com.nobi.nobiboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Phase 3 editor for per-letter long-press popup emojis.
 * All UI is built programmatically (no XML, no resources).
 * Framework android.* only. Persists to SharedPreferences
 * "nobi_longpress_emoji", key "emoji_A".."emoji_Z", value
 * "center,up,down,left,right"; a cleared slot is U+200B.
 */
public class NobiEmojiEditorActivity extends Activity {

    private static final String PREFS_NAME = "nobi_longpress_emoji";
    private static final String DIAG_PREFS_NAME = "nobi_diag";
    private static final String BLANK = "\u200B";

    private static final int COLOR_BG = 0xFF212121;
    private static final int COLOR_TEXT = 0xFFEEEEEE;
    private static final int COLOR_GREY = 0xFFAAAAAA;
    private static final int COLOR_ACCENT = 0xFF4DB6AC;
    private static final int COLOR_SELECTED_KEY = 0xFF2E7D6F;
    private static final int COLOR_KEY = 0xFF3A3A3A;
    private static final int COLOR_POPUP_BG = 0xFF3C474C;
    private static final int COLOR_POPUP_STROKE = 0xFF9AA0A6;
    private static final int COLOR_ROW_BG = 0xFF2E2E2E;

    // Canonical slot order: center, up, down, left, right.
    private static final String[] SLOT_NAMES = {
        "Center", "Up", "Down", "Left", "Right"
    };
    // Popup grid (row, col) per slot: center (1,1), up (0,1),
    // down (2,1), left (1,0), right (1,2).
    private static final int[][] SLOT_GRID_POS = {
        { 1, 1 }, { 0, 1 }, { 2, 1 }, { 1, 0 }, { 1, 2 }
    };

    private static final String[] KEY_ROWS = {
        "qwertyuiop", "asdfghjkl", "zxcvbnm"
    };

    private SharedPreferences prefs;
    private int selectedLetter = 0; // index into Defaults.LETTERS, default 'A'

    private Button[] keyButtons = new Button[26];
    private TextView[] popupCells = new TextView[5];
    private TextView[] slotEmojiViews = new TextView[5];
    private TextView[] slotHintViews = new TextView[5];
    private TextView diagView;

    // Emoji picker state.
    private int pickerSlot = 0;
    private int pickerCategory = -1; // -1 = All
    private String pickerQuery = "";
    private AlertDialog pickerDialog;
    private EmojiGridAdapter pickerAdapter;
    private List<String> pickerEmojis = new ArrayList<String>();
    private Button[] pickerCategoryButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Long-press emojis");
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        // 1. Header.
        TextView title = new TextView(this);
        title.setText("Long-press emojis");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_TEXT);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tap a key, then tap a slot to choose its emoji.");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setTextColor(COLOR_GREY);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(16);
        root.addView(subtitle, subLp);

        // 2. Keyboard preview (3 rows).
        for (int r = 0; r < KEY_ROWS.length; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            String letters = KEY_ROWS[r];
            for (int c = 0; c < letters.length(); c++) {
                final int letterIdx = Defaults.LETTERS.indexOf(letters.charAt(c));
                Button keyButton = new Button(this);
                keyButton.setTextColor(COLOR_TEXT);
                keyButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                keyButton.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(
                        0, dp(48), 1.0f);
                klp.setMargins(dp(2), dp(2), dp(2), dp(2));
                keyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedLetter = letterIdx;
                        refreshAll();
                    }
                });
                row.addView(keyButton, klp);
                keyButtons[letterIdx] = keyButton;
            }
            root.addView(row);
        }

        // 3. Popup preview: centered 84dp square, 3x3 grid.
        LinearLayout previewWrap = new LinearLayout(this);
        previewWrap.setOrientation(LinearLayout.HORIZONTAL);
        previewWrap.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapLp.topMargin = dp(16);
        wrapLp.bottomMargin = dp(16);
        root.addView(previewWrap, wrapLp);

        FrameLayout popupFrame = new FrameLayout(this);
        GradientDrawable popupBg = new GradientDrawable();
        popupBg.setShape(GradientDrawable.OVAL);
        popupBg.setColor(COLOR_POPUP_BG);
        popupBg.setStroke(dp(1), COLOR_POPUP_STROKE);
        popupFrame.setBackground(popupBg);
        previewWrap.addView(popupFrame,
                new LinearLayout.LayoutParams(dp(84), dp(84)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setRowCount(3);
        popupFrame.addView(grid,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        for (int s = 0; s < 5; s++) {
            int row = SLOT_GRID_POS[s][0];
            int col = SLOT_GRID_POS[s][1];
            TextView cell = new TextView(this);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            cell.setTextColor(COLOR_TEXT);
            if (s == 0) {
                // Center cell: green circle behind the emoji.
                GradientDrawable centerBg = new GradientDrawable();
                centerBg.setShape(GradientDrawable.OVAL);
                centerBg.setColor(COLOR_ACCENT);
                centerBg.setSize(dp(40), dp(40));
                cell.setBackground(centerBg);
            }
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams(
                    GridLayout.spec(row), GridLayout.spec(col));
            glp.width = dp(28);
            glp.height = dp(28);
            grid.addView(cell, glp);
            popupCells[s] = cell;
        }

        // 4. Slots list: 5 rows.
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            LinearLayout slotRow = new LinearLayout(this);
            slotRow.setOrientation(LinearLayout.HORIZONTAL);
            slotRow.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dp(6));
            rowBg.setColor(COLOR_ROW_BG);
            slotRow.setBackground(rowBg);
            slotRow.setPadding(dp(12), dp(12), dp(12), dp(12));
            slotRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPicker(slot);
                }
            });

            TextView label = new TextView(this);
            label.setText(SLOT_NAMES[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            label.setTextColor(COLOR_TEXT);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            slotRow.addView(label, labelLp);

            TextView emojiView = new TextView(this);
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            emojiView.setTextColor(COLOR_TEXT);
            emojiView.setGravity(Gravity.CENTER);
            slotRow.addView(emojiView);
            slotEmojiViews[i] = emojiView;

            TextView hint = new TextView(this);
            hint.setText("tap to change");
            hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            hint.setTextColor(COLOR_GREY);
            LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            hintLp.leftMargin = dp(8);
            slotRow.addView(hint, hintLp);
            slotHintViews[i] = hint;

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            root.addView(slotRow, rowLp);
        }

        // 5. Bottom bar: Reset this key / Reset all.
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        barLp.topMargin = dp(8);
        root.addView(bottomBar, barLp);

        Button resetKey = new Button(this);
        resetKey.setText("Reset this key");
        resetKey.setTextColor(COLOR_TEXT);
        resetKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetConfirm(false);
            }
        });
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        resetLp.rightMargin = dp(4);
        bottomBar.addView(resetKey, resetLp);

        Button resetAll = new Button(this);
        resetAll.setText("Reset all");
        resetAll.setTextColor(COLOR_TEXT);
        resetAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetConfirm(true);
            }
        });
        LinearLayout.LayoutParams resetAllLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        resetAllLp.leftMargin = dp(4);
        bottomBar.addView(resetAll, resetAllLp);

        // 6. Diagnostics: what the keyboard's def-parse hook recorded.
        // The hook writes to SharedPreferences "nobi_diag" whenever the
        // keyboard loads (last_run) and whenever key A is checked (last_a).
        // This section shows those records so a failed customization can be
        // diagnosed without a Toast or a logcat.
        TextView diagTitle = new TextView(this);
        diagTitle.setText("Diagnostics");
        diagTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        diagTitle.setTypeface(Typeface.DEFAULT_BOLD);
        diagTitle.setTextColor(COLOR_TEXT);
        LinearLayout.LayoutParams diagTitleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        diagTitleLp.topMargin = dp(20);
        diagTitleLp.bottomMargin = dp(4);
        root.addView(diagTitle, diagTitleLp);

        TextView diagHint = new TextView(this);
        diagHint.setText("Tip: after customizing, force-stop the keyboard once, open it, then come back here.");
        diagHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        diagHint.setTextColor(COLOR_GREY);
        LinearLayout.LayoutParams diagHintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        diagHintLp.bottomMargin = dp(8);
        root.addView(diagHint, diagHintLp);

        diagView = new TextView(this);
        diagView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        diagView.setTextColor(COLOR_TEXT);
        GradientDrawable diagBg = new GradientDrawable();
        diagBg.setCornerRadius(dp(6));
        diagBg.setColor(COLOR_ROW_BG);
        diagView.setBackground(diagBg);
        diagView.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(diagView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        refreshAll();
        refreshDiag();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDiag();
    }

    // ---------- prefs ----------

    private String prefKey(int letterIdx) {
        char letter = Character.toUpperCase(Defaults.LETTERS.charAt(letterIdx));
        return "emoji_" + letter;
    }

    private boolean isCustomized(int letterIdx) {
        return prefs.getString(prefKey(letterIdx), null) != null;
    }

    /** Current 5 emojis for a key: custom from prefs, else Defaults. */
    private String[] getCurrentEmojis(int letterIdx) {
        String saved = prefs.getString(prefKey(letterIdx), null);
        if (saved == null) {
            String[] copy = new String[5];
            System.arraycopy(Defaults.DEFAULTS[letterIdx], 0, copy, 0, 5);
            return copy;
        }
        String[] parts = saved.split(",", -1);
        String[] out = new String[5];
        for (int i = 0; i < 5; i++) {
            if (i < parts.length) {
                out[i] = parts[i];
            } else {
                out[i] = Defaults.DEFAULTS[letterIdx][i];
            }
        }
        return out;
    }

    private void saveSlot(int letterIdx, int slot, String emoji) {
        String[] current = getCurrentEmojis(letterIdx);
        current[slot] = emoji;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(current[i]);
        }
        prefs.edit().putString(prefKey(letterIdx), sb.toString()).apply();
    }

    // ---------- refresh ----------

    private void refreshAll() {
        refreshKeyButtons();
        refreshPopup();
        refreshSlots();
    }

    /** Show the def-parse diagnostic records written by the Lpud.m hook. */
    private void refreshDiag() {
        if (diagView == null) {
            return;
        }
        SharedPreferences diag = getSharedPreferences(DIAG_PREFS_NAME, MODE_PRIVATE);
        String lastRun = diag.getString("last_run", null);
        String lastA = diag.getString("last_a", null);
        StringBuilder sb = new StringBuilder();
        sb.append("Last keyboard load:\n");
        sb.append(lastRun == null ? "No record yet." : lastRun);
        sb.append("\n\nLast A-key check:\n");
        sb.append(lastA == null ? "No record yet." : lastA);
        if (lastRun == null && lastA == null) {
            sb.append("\n\nNo diagnostic record yet - the keyboard def has not been parsed since this version was installed. Force-stop the keyboard once and open it, then come back here.");
        }
        diagView.setText(sb.toString());
    }

    private void refreshKeyButtons() {
        for (int i = 0; i < 26; i++) {
            Button b = keyButtons[i];
            if (b == null) {
                continue;
            }
            char letter = Character.toUpperCase(Defaults.LETTERS.charAt(i));
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(String.valueOf(letter));
            if (isCustomized(i)) {
                int dotStart = ssb.length();
                ssb.append("\n\u25CF");
                ssb.setSpan(new ForegroundColorSpan(COLOR_ACCENT),
                        dotStart + 1, ssb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            b.setText(ssb);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(4));
            bg.setColor(i == selectedLetter ? COLOR_SELECTED_KEY : COLOR_KEY);
            b.setBackground(bg);
        }
    }

    private void refreshPopup() {
        String[] current = getCurrentEmojis(selectedLetter);
        for (int i = 0; i < 5; i++) {
            popupCells[i].setText(BLANK.equals(current[i]) ? "" : current[i]);
        }
    }

    private void refreshSlots() {
        String[] current = getCurrentEmojis(selectedLetter);
        for (int i = 0; i < 5; i++) {
            boolean blank = BLANK.equals(current[i]);
            slotEmojiViews[i].setText(blank ? "" : current[i]);
            slotHintViews[i].setText(
                    blank ? "blank — tap to change" : "tap to change");
        }
    }

    // ---------- reset ----------

    private void showResetConfirm(final boolean all) {
        String message;
        if (all) {
            message = "Reset all keys to defaults?";
        } else {
            char letter = Character.toUpperCase(
                    Defaults.LETTERS.charAt(selectedLetter));
            message = "Reset '" + letter + "' to defaults?";
        }
        new AlertDialog.Builder(this)
                .setTitle("Reset")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = prefs.edit();
                        if (all) {
                            for (int i = 0; i < 26; i++) {
                                editor.remove(prefKey(i));
                            }
                        } else {
                            editor.remove(prefKey(selectedLetter));
                        }
                        editor.apply();
                        refreshAll();
                        Toast.makeText(NobiEmojiEditorActivity.this,
                                "Reset to defaults",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------- emoji picker ----------

    private void showPicker(final int slot) {
        pickerSlot = slot;
        pickerCategory = -1; // All
        pickerQuery = "";

        LinearLayout dialogRoot = new LinearLayout(this);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setBackgroundColor(COLOR_BG);
        int pad = dp(16);
        dialogRoot.setPadding(pad, pad, pad, pad);

        // Search row: EditText + Clear button.
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        dialogRoot.addView(searchRow);

        EditText searchBox = new EditText(this);
        searchBox.setHint("Search emojis");
        searchBox.setSingleLine(true);
        searchBox.setTextColor(COLOR_TEXT);
        searchBox.setHintTextColor(COLOR_GREY);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                    int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                    int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                pickerQuery = s.toString().trim().toLowerCase(Locale.US);
                rebuildPickerList();
            }
        });
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        searchRow.addView(searchBox, searchLp);

        Button clearButton = new Button(this);
        clearButton.setText("Clear");
        clearButton.setTextColor(COLOR_TEXT);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSlot(selectedLetter, pickerSlot, BLANK);
                if (pickerDialog != null) {
                    pickerDialog.dismiss();
                }
                refreshAll();
            }
        });
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        clearLp.leftMargin = dp(8);
        searchRow.addView(clearButton, clearLp);

        // Category row: horizontal scroll of buttons (All + categories).
        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        LinearLayout.LayoutParams catScrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        catScrollLp.topMargin = dp(12);
        catScrollLp.bottomMargin = dp(8);
        dialogRoot.addView(categoryScroll, catScrollLp);

        LinearLayout categoryRow = new LinearLayout(this);
        categoryRow.setOrientation(LinearLayout.HORIZONTAL);
        categoryScroll.addView(categoryRow);

        pickerCategoryButtons = new Button[EmojiData.CATEGORIES.length + 1];
        for (int i = 0; i < pickerCategoryButtons.length; i++) {
            final int catIndex = i - 1; // -1 = All
            Button catButton = new Button(this);
            catButton.setText(i == 0 ? "All" : EmojiData.CATEGORIES[i - 1]);
            catButton.setTextColor(COLOR_TEXT);
            catButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickerCategory = catIndex;
                    refreshPickerCategoryStyles();
                    rebuildPickerList();
                }
            });
            LinearLayout.LayoutParams catLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            catLp.rightMargin = dp(4);
            categoryRow.addView(catButton, catLp);
            pickerCategoryButtons[i] = catButton;
        }
        refreshPickerCategoryStyles();

        // Emoji grid: 6 columns.
        pickerAdapter = new EmojiGridAdapter();
        GridView grid = new GridView(this);
        grid.setNumColumns(6);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(dp(4));
        grid.setVerticalSpacing(dp(4));
        grid.setAdapter(pickerAdapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                String emoji = (String) pickerAdapter.getItem(position);
                saveSlot(selectedLetter, pickerSlot, emoji);
                if (pickerDialog != null) {
                    pickerDialog.dismiss();
                }
                refreshAll();
            }
        });
        dialogRoot.addView(grid,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));

        rebuildPickerList();

        pickerDialog = new AlertDialog.Builder(this)
                .setTitle("Choose emoji")
                .setView(dialogRoot)
                .setNegativeButton("Cancel", null)
                .create();
        pickerDialog.show();
    }

    private void refreshPickerCategoryStyles() {
        if (pickerCategoryButtons == null) {
            return;
        }
        for (int i = 0; i < pickerCategoryButtons.length; i++) {
            boolean selected = (i == 0)
                    ? pickerCategory == -1
                    : pickerCategory == i - 1;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(4));
            bg.setColor(selected ? COLOR_SELECTED_KEY : COLOR_KEY);
            pickerCategoryButtons[i].setBackground(bg);
        }
    }

    private void rebuildPickerList() {
        pickerEmojis.clear();
        String query = pickerQuery;
        for (int cat = 0; cat < EmojiData.EMOJIS.length; cat++) {
            String catName = EmojiData.CATEGORIES[cat].toLowerCase(Locale.US);
            if (query.length() == 0) {
                // Empty query: show the selected category (or All).
                if (pickerCategory != -1 && pickerCategory != cat) {
                    continue;
                }
                for (int e = 0; e < EmojiData.EMOJIS[cat].length; e++) {
                    pickerEmojis.add(emojiOf(EmojiData.EMOJIS[cat][e]));
                }
            } else {
                // Search across all categories: keywords or category name.
                for (int e = 0; e < EmojiData.EMOJIS[cat].length; e++) {
                    String entry = EmojiData.EMOJIS[cat][e];
                    String keywords = keywordsOf(entry).toLowerCase(Locale.US);
                    if (keywords.contains(query) || catName.contains(query)) {
                        pickerEmojis.add(emojiOf(entry));
                    }
                }
            }
        }
        if (pickerAdapter != null) {
            pickerAdapter.notifyDataSetChanged();
        }
    }

    private static String emojiOf(String entry) {
        int bar = entry.indexOf('|');
        return bar < 0 ? entry : entry.substring(0, bar);
    }

    private static String keywordsOf(String entry) {
        int bar = entry.indexOf('|');
        return bar < 0 ? "" : entry.substring(bar + 1);
    }

    private class EmojiGridAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return pickerEmojis.size();
        }

        @Override
        public Object getItem(int position) {
            return pickerEmojis.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView cell;
            if (convertView instanceof TextView) {
                cell = (TextView) convertView;
            } else {
                cell = new TextView(NobiEmojiEditorActivity.this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
                cell.setPadding(dp(4), dp(4), dp(4), dp(4));
            }
            cell.setText(pickerEmojis.get(position));
            return cell;
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }
}
