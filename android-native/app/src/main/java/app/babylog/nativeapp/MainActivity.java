package app.babylog.nativeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE_ULTRASOUND = 1201;
    private static final int PEACH = 0xFFFFF7E8;
    private static final int INK = 0xFF23312D;
    private static final int MUTED = 0xFF6F7F78;
    private static final int PRIMARY = 0xFFEF7D66;

    private BabyLogStore store;
    private LinearLayout root;
    private EditText examDateInput;
    private EditText gestationalAgeInput;
    private EditText bpdInput;
    private EditText hcInput;
    private EditText acInput;
    private EditText flInput;
    private EditText efwInput;
    private EditText noteInput;
    private ImageView photoPreview;
    private String currentPhotoPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new BabyLogStore(this);
        showHome();
    }

    private void showHome() {
        root = createRoot();
        root.addView(title("BabyLog 原生版", 28));
        root.addView(caption("单胎 / 本机模式 / Android APK"));
        root.addView(primaryButton("记录 B 超", view -> showUltrasoundForm()));
        root.addView(sectionTitle("最近 B 超记录"));

        List<BabyLogRecord> records = store.listRecordsNewestFirst();
        if (records.isEmpty()) {
            root.addView(empty("还没有本机记录，先拍一张 B 超单。"));
        } else {
            for (BabyLogRecord record : records) {
                root.addView(recordCard(record));
            }
        }

        setContentView(wrap(root));
    }

    private void showUltrasoundForm() {
        root = createRoot();
        root.addView(title("B 超记录", 26));
        root.addView(caption("先拍照或填指标，保存到本机。"));

        examDateInput = input("检查日期 yyyy-MM-dd", today());
        gestationalAgeInput = input("孕周，例如 28+3", "28+3");
        bpdInput = numberInput("双顶径 BPD mm");
        hcInput = numberInput("头围 HC mm");
        acInput = numberInput("腹围 AC mm");
        flInput = numberInput("股骨长 FL mm");
        efwInput = numberInput("估计胎重 EFW g");
        noteInput = input("备注，可先语音输入到键盘", "");

        root.addView(examDateInput);
        root.addView(gestationalAgeInput);
        root.addView(bpdInput);
        root.addView(hcInput);
        root.addView(acInput);
        root.addView(flInput);
        root.addView(efwInput);
        root.addView(noteInput);

        photoPreview = new ImageView(this);
        photoPreview.setAdjustViewBounds(true);
        photoPreview.setMaxHeight(dp(220));
        root.addView(photoPreview, matchWrap());

        root.addView(primaryButton("拍 B 超单", view -> launchCamera()));
        root.addView(primaryButton("保存记录", view -> saveUltrasound()));
        root.addView(secondaryButton("返回首页", view -> showHome()));

        setContentView(wrap(root));
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "没有找到系统相机", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(intent, REQUEST_CAPTURE_ULTRASOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE_ULTRASOUND || resultCode != RESULT_OK || data == null) {
            return;
        }

        Object bitmapData = data.getExtras() == null ? null : data.getExtras().get("data");
        if (!(bitmapData instanceof Bitmap)) {
            Toast.makeText(this, "相机未返回图片", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            currentPhotoPath = saveBitmap((Bitmap) bitmapData);
            photoPreview.setImageBitmap(BitmapFactory.decodeFile(currentPhotoPath));
            Toast.makeText(this, "照片已保存到本机", Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            Toast.makeText(this, "保存照片失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveUltrasound() {
        BabyLogRecord record = new BabyLogRecord(
                "ultrasound-" + UUID.randomUUID(),
                text(examDateInput),
                text(gestationalAgeInput),
                text(bpdInput),
                text(hcInput),
                text(acInput),
                text(flInput),
                text(efwInput),
                text(noteInput),
                currentPhotoPath,
                System.currentTimeMillis()
        );

        try {
            store.addRecord(record);
            Toast.makeText(this, "B 超已保存到本机", Toast.LENGTH_SHORT).show();
            showHome();
        } catch (JSONException error) {
            Toast.makeText(this, "保存失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private View recordCard(BabyLogRecord record) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        TextView date = label(record.examDate.isEmpty() ? "未填日期" : record.examDate, 16, INK);
        date.setTypeface(null, 1);
        card.addView(date);
        card.addView(label(RecordFormatter.ultrasoundSummary(record), 14, MUTED));
        if (!record.note.trim().isEmpty()) {
            card.addView(label(record.note, 14, INK));
        }
        if (!record.photoPath.isEmpty()) {
            ImageView image = new ImageView(this);
            image.setImageBitmap(BitmapFactory.decodeFile(record.photoPath));
            image.setAdjustViewBounds(true);
            image.setMaxHeight(dp(180));
            card.addView(image, matchWrap());
        }
        return card;
    }

    private String saveBitmap(Bitmap bitmap) throws IOException {
        File dir = new File(getFilesDir(), "ultrasound");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建图片目录");
        }
        File file = new File(dir, "scan-" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream output = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
        }
        return file.getAbsolutePath();
    }

    private LinearLayout createRoot() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(18), dp(18), dp(24));
        layout.setBackgroundColor(PEACH);
        return layout;
    }

    private ScrollView wrap(LinearLayout content) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(content);
        return scrollView;
    }

    private TextView title(String text, int sizeSp) {
        TextView view = label(text, sizeSp, INK);
        view.setTypeface(null, 1);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = title(text, 20);
        view.setPadding(0, dp(18), 0, dp(8));
        return view;
    }

    private TextView caption(String text) {
        TextView view = label(text, 14, MUTED);
        view.setPadding(0, dp(4), 0, dp(16));
        return view;
    }

    private TextView empty(String text) {
        TextView view = label(text, 15, MUTED);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(24), 0, dp(24));
        return view;
    }

    private TextView label(String text, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.18f);
        return view;
    }

    private EditText input(String hint, String value) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        editText.setSingleLine(false);
        editText.setTextColor(INK);
        editText.setHintTextColor(MUTED);
        editText.setBackgroundColor(0xFFFFFFFF);
        editText.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(10));
        editText.setLayoutParams(params);
        return editText;
    }

    private EditText numberInput(String hint) {
        EditText editText = input(hint, "");
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return editText;
    }

    private Button primaryButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(PRIMARY);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button secondaryButton(String text, View.OnClickListener listener) {
        Button button = primaryButton(text, listener);
        button.setTextColor(PRIMARY);
        button.setBackgroundColor(0xFFFFE3DA);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String text(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }
}
