package com.enchanting.keyboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#121418"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, pad);
        scrollView.addView(layout);

        // App Title
        TextView title = new TextView(this);
        title.setText("✨ SGA Enchanting Keyboard");
        title.setTextColor(Color.parseColor("#E0E6ED"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText("Клавиатура со столом зачарований Minecraft (SGA), рунами, глитчами и готикой.");
        subtitle.setTextColor(Color.parseColor("#8E99A8"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        int m = dp(8);
        subtitle.setPadding(0, m, 0, dp(20));
        layout.addView(subtitle);

        // Card: Step 1
        layout.addView(createStepButton("1. Включить клавиатуру в системе", "#3F69D6", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                startActivity(intent);
            }
        }));

        // Card: Step 2
        layout.addView(createStepButton("2. Выбрать SGA как активную", "#6C5CE7", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        }));

        // Test Area Header
        TextView testHeader = new TextView(this);
        testHeader.setText("Проверка работы:");
        testHeader.setTextColor(Color.parseColor("#CCD5E0"));
        testHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        testHeader.setTypeface(Typeface.DEFAULT_BOLD);
        testHeader.setPadding(0, dp(16), 0, dp(8));
        layout.addView(testHeader);

        // EditText for testing
        EditText testInput = new EditText(this);
        testInput.setHint("Нажми сюда и напиши: чего тебе надо");
        testInput.setHintTextColor(Color.parseColor("#5A6577"));
        testInput.setTextColor(Color.WHITE);
        testInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#1E222A"));
        inputBg.setCornerRadius(dp(8));
        inputBg.setStroke(dp(1), Color.parseColor("#384152"));
        testInput.setBackground(inputBg);
        int ip = dp(14);
        testInput.setPadding(ip, ip, ip, ip);
        layout.addView(testInput);

        // Quick Copy Card
        TextView sampleHeader = new TextView(this);
        sampleHeader.setText("Твой текст в SGA:");
        sampleHeader.setTextColor(Color.parseColor("#CCD5E0"));
        sampleHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        sampleHeader.setTypeface(Typeface.DEFAULT_BOLD);
        sampleHeader.setPadding(0, dp(24), 0, dp(8));
        layout.addView(sampleHeader);

        final String sgaSample = Translators.translateKey("чего тебе надо", Translators.Mode.SGA);
        TextView sampleText = new TextView(this);
        sampleText.setText(sgaSample + "\n(чего тебе надо)");
        sampleText.setTextColor(Color.parseColor("#54D2D2"));
        sampleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        GradientDrawable sampleBg = new GradientDrawable();
        sampleBg.setColor(Color.parseColor("#182A2E"));
        sampleBg.setCornerRadius(dp(8));
        sampleBg.setStroke(dp(1), Color.parseColor("#25575E"));
        sampleText.setBackground(sampleBg);
        sampleText.setPadding(ip, ip, ip, ip);
        layout.addView(sampleText);

        layout.addView(createStepButton("Скопировать SGA-текст", "#00B894", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("SGA", sgaSample));
                    Toast.makeText(MainActivity.this, "Скопировано в буфер обмена!", Toast.LENGTH_SHORT).show();
                }
            }
        }));

        addFeedbackSettings(layout, ip);
        addDecoderCard(layout, ip);

        setContentView(scrollView);
    }

    // ---- Haptics and sound settings ----

    private void addFeedbackSettings(LinearLayout layout, int ip) {
        final Prefs prefs = new Prefs(this);
        layout.addView(sectionHeader("Отклик клавиш"));

        final Feedback preview = new Feedback(layout, prefs);

        Switch hapticsSwitch = new Switch(this);
        hapticsSwitch.setText("Вибрация при нажатии");
        styleLabel(hapticsSwitch);
        hapticsSwitch.setChecked(prefs.hapticsOn());
        hapticsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                prefs.setHapticsOn(checked);
                if (checked) preview.key(Feedback.KeyType.LETTER);
            }
        });
        layout.addView(hapticsSwitch);

        layout.addView(smallLabel("Сила вибрации"));
        SeekBar strength = new SeekBar(this);
        strength.setMax(100);
        strength.setProgress(prefs.hapticStrength());
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) prefs.setHapticStrength(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) { }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                preview.key(Feedback.KeyType.LETTER);
            }
        });
        layout.addView(strength);

        Switch soundSwitch = new Switch(this);
        soundSwitch.setText("Звук клавиш");
        styleLabel(soundSwitch);
        soundSwitch.setPadding(0, dp(12), 0, 0);
        soundSwitch.setChecked(prefs.soundOn());
        soundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                prefs.setSoundOn(checked);
                if (checked) preview.key(Feedback.KeyType.LETTER);
            }
        });
        layout.addView(soundSwitch);

        layout.addView(smallLabel("Громкость звука"));
        SeekBar volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(prefs.soundVolume());
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) prefs.setSoundVolume(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) { }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                preview.key(Feedback.KeyType.LETTER);
            }
        });
        layout.addView(volume);
    }

    // ---- Decoder card ----

    private void addDecoderCard(LinearLayout layout, int ip) {
        final Prefs prefs = new Prefs(this);
        layout.addView(sectionHeader("Декодер"));

        final EditText input = new EditText(this);
        input.setHint("Вставь SGA / руны / готику / зальго / Ozkavosh / Ariral");
        input.setHintTextColor(Color.parseColor("#5A6577"));
        input.setTextColor(Color.WHITE);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP);
        GradientDrawable inBg = new GradientDrawable();
        inBg.setColor(Color.parseColor("#1E222A"));
        inBg.setCornerRadius(dp(8));
        inBg.setStroke(dp(1), Color.parseColor("#384152"));
        input.setBackground(inBg);
        input.setPadding(ip, ip, ip, ip);
        layout.addView(input);

        layout.addView(smallLabel("Шрифт / язык игры"));
        final Decoder.Script[] scripts = {
            Decoder.Script.AUTO, Decoder.Script.SGA, Decoder.Script.RUNES, Decoder.Script.GOTHIC,
            Decoder.Script.ZALGO, Decoder.Script.OZKAVOSH, Decoder.Script.ARIRAL
        };
        String[] scriptNames = new String[scripts.length];
        for (int i = 0; i < scripts.length; i++) scriptNames[i] = scripts[i].label;
        final Spinner scriptSpinner = darkSpinner(scriptNames);
        layout.addView(scriptSpinner);

        layout.addView(smallLabel("Читать как (Latin / Кириллица)"));
        final Decoder.Target[] targets = Decoder.Target.values();
        String[] targetNames = new String[targets.length];
        for (int i = 0; i < targets.length; i++) targetNames[i] = targets[i].label;
        final Spinner targetSpinner = darkSpinner(targetNames);
        targetSpinner.setSelection(prefs.decodeTarget().ordinal());
        layout.addView(targetSpinner);

        final TextView info = smallLabel("");
        layout.addView(info);

        final TextView output = new TextView(this);
        output.setTextColor(Color.parseColor("#54D2D2"));
        output.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        output.setTextIsSelectable(true);
        GradientDrawable outBg = new GradientDrawable();
        outBg.setColor(Color.parseColor("#182A2E"));
        outBg.setCornerRadius(dp(8));
        outBg.setStroke(dp(1), Color.parseColor("#25575E"));
        output.setBackground(outBg);
        output.setPadding(ip, ip, ip, ip);
        output.setMinHeight(dp(56));
        layout.addView(output);

        final Runnable update = new Runnable() {
            @Override
            public void run() {
                Decoder.Result r = Decoder.decodeDetailed(input.getText().toString(),
                    scripts[scriptSpinner.getSelectedItemPosition()],
                    targets[targetSpinner.getSelectedItemPosition()]);
                output.setText(r.text);
                info.setText(input.getText().length() == 0 ? ""
                    : "Определено: " + r.script.label
                        + (r.script == Decoder.Script.NONE ? "" : " · читаю как " + r.target.label));
            }
        };

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) { update.run(); }
            @Override
            public void afterTextChanged(Editable s) { }
        });
        scriptSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { update.run(); }
            @Override
            public void onNothingSelected(AdapterView<?> p) { }
        });
        targetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                prefs.setDecodeTarget(targets[pos]);
                update.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) { }
        });

        layout.addView(createStepButton("Скопировать результат", "#00B894", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("decoded", output.getText()));
                    Toast.makeText(MainActivity.this, "Скопировано в буфер обмена!", Toast.LENGTH_SHORT).show();
                }
            }
        }));
    }

    private Spinner darkSpinner(String[] items) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                tv.setPadding(dp(16), dp(12), dp(16), dp(12));
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        return sp;
    }

    private TextView sectionHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#CCD5E0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(28), 0, dp(8));
        return tv;
    }

    private TextView smallLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#8E99A8"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(0, dp(10), 0, dp(4));
        return tv;
    }

    private void styleLabel(TextView tv) {
        tv.setTextColor(Color.parseColor("#E0E6ED"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    }

    private Button createStepButton(String text, String colorHex, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(colorHex));
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        int pad = dp(12);
        btn.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, dp(6), 0, dp(10));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        return btn;
    }

    private int dp(float dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
