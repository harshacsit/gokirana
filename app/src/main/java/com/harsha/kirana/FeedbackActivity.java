package com.harsha.kirana;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;
import static com.harsha.kirana.AppTheme.*;
import static com.harsha.kirana.UiHelper.*;

public class FeedbackActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userName  = "";
    private String userPhone = "";
    private String userUid   = "";

    private int    selectedRating   = 0;
    private String selectedCategory = "";

    private TextView[] stars       = new TextView[5];
    private TextView[] categoryBtns;
    private EditText   etMessage;
    private Button     btnSubmit;
    private ProgressBar progress;
    private LinearLayout successCard;
    private ScrollView formScroll;

    private String s(String key) { return LangHelper.t(this, key); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setContentView(buildUI());
        loadUserInfo();
    }

    // ── Load user name + phone from Firestore ──────────────────────────────
    private void loadUserInfo() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String n = doc.getString("name");
                        String p = doc.getString("phone");
                        if (n != null) userName  = n;
                        if (p != null) userPhone = p;
                    }
                });
    }

    // ── Build full UI ──────────────────────────────────────────────────────
    private LinearLayout buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_PAGE);

        // ── Blue top bar ───────────────────────────────────────────────────
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setBackgroundColor(BG_TOPBAR);
        topBar.setPadding(dp(this,18), dp(this,48), dp(this,18), dp(this,20));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Back arrow
        TextView backBtn = new TextView(this);
        backBtn.setText("←");
        backBtn.setTextColor(0xFFFFFFFF);
        backBtn.setTextSize(22f);
        backBtn.setTypeface(Typeface.DEFAULT_BOLD);
        backBtn.setClickable(true); backBtn.setFocusable(true);
        backBtn.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bP.setMargins(0, 0, dp(this,14), 0);
        backBtn.setLayoutParams(bP);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView titleTv = new TextView(this);
        titleTv.setText(LangHelper.isTelugu(this) ? "అభిప్రాయం" : "Feedback");
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(TEXT_XL);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);

        TextView subTv = new TextView(this);
        subTv.setText(LangHelper.isTelugu(this)
                ? "మీ అభిప్రాయం మాకు చాలా విలువైనది"
                : "Your opinion helps us improve");
        subTv.setTextColor(0xCCFFFFFF);
        subTv.setTextSize(12f);

        textCol.addView(titleTv);
        textCol.addView(subTv);
        topRow.addView(backBtn);
        topRow.addView(textCol);
        topBar.addView(topRow);
        root.addView(topBar);

        // ── Scrollable form ────────────────────────────────────────────────
        formScroll = new ScrollView(this);
        formScroll.setBackgroundColor(BG_PAGE);
        formScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG_PAGE);
        content.setPadding(dp(this,16), dp(this,16), dp(this,16), dp(this,80));

        // ── Success card (hidden initially) ───────────────────────────────
        successCard = new LinearLayout(this);
        successCard.setOrientation(LinearLayout.VERTICAL);
        successCard.setGravity(Gravity.CENTER);
        successCard.setPadding(dp(this,24), dp(this,40), dp(this,24), dp(this,40));
        successCard.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 16, 1, this));
        successCard.setVisibility(View.GONE);
        LinearLayout.LayoutParams scP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scP.setMargins(0, 0, 0, dp(this,12));
        successCard.setLayoutParams(scP);

        TextView successIcon = new TextView(this);
        successIcon.setText("✅");
        successIcon.setTextSize(52f);
        successIcon.setGravity(Gravity.CENTER);

        TextView successTitle = new TextView(this);
        successTitle.setText(LangHelper.isTelugu(this)
                ? "ధన్యవాదాలు!"
                : "Thank you for your feedback!");
        successTitle.setTextColor(GREEN);
        successTitle.setTextSize(TEXT_LG);
        successTitle.setTypeface(Typeface.DEFAULT_BOLD);
        successTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stP.setMargins(0, dp(this,12), 0, dp(this,8));
        successTitle.setLayoutParams(stP);

        TextView successSub = new TextView(this);
        successSub.setText(LangHelper.isTelugu(this)
                ? "మీ అభిప్రాయం అందుకున్నాం. మేము దానిని సమీక్షిస్తాం."
                : "We received your feedback and will review it.");
        successSub.setTextColor(TEXT_MUTED);
        successSub.setTextSize(TEXT_MD);
        successSub.setGravity(Gravity.CENTER);

        Button btnNewFeedback = new Button(this);
        btnNewFeedback.setText(LangHelper.isTelugu(this) ? "మళ్ళీ పంపు" : "Send Another");
        btnNewFeedback.setTextColor(0xFFFFFFFF);
        btnNewFeedback.setTextSize(TEXT_MD);
        btnNewFeedback.setTypeface(Typeface.DEFAULT_BOLD);
        btnNewFeedback.setBackground(roundedBg(BLUE, 12, this));
        btnNewFeedback.setAllCaps(false);
        LinearLayout.LayoutParams nbP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(this,48));
        nbP.gravity = Gravity.CENTER_HORIZONTAL;
        nbP.setMargins(0, dp(this,20), 0, 0);
        btnNewFeedback.setLayoutParams(nbP);
        btnNewFeedback.setOnClickListener(v -> resetForm());

        successCard.addView(successIcon);
        successCard.addView(successTitle);
        successCard.addView(successSub);
        successCard.addView(btnNewFeedback);
        content.addView(successCard);

        // ── Star Rating card ───────────────────────────────────────────────
        LinearLayout ratingCard = whiteCard();

        TextView ratingTitle = cardLabel(LangHelper.isTelugu(this)
                ? "GoKirana ను ఎలా రేట్ చేస్తారు?" : "How would you rate GoKirana?");
        ratingCard.addView(ratingTitle);

        LinearLayout starsRow = new LinearLayout(this);
        starsRow.setOrientation(LinearLayout.HORIZONTAL);
        starsRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams srP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srP.setMargins(0, dp(this,8), 0, dp(this,8));
        starsRow.setLayoutParams(srP);

        for (int i = 0; i < 5; i++) {
            final int starNum = i + 1;
            stars[i] = new TextView(this);
            stars[i].setText("☆");
            stars[i].setTextSize(38f);
            stars[i].setTextColor(0xFFCCCCCC);
            stars[i].setGravity(Gravity.CENTER);
            stars[i].setClickable(true); stars[i].setFocusable(true);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            stars[i].setLayoutParams(sp);
            stars[i].setOnClickListener(v -> {
                AnimationHelper.pulse(stars[starNum - 1]);
                setRating(starNum);
            });
            starsRow.addView(stars[i]);
        }
        ratingCard.addView(starsRow);

        // Rating label
        TextView ratingLabel = new TextView(this);
        ratingLabel.setTag("rating_label");
        ratingLabel.setText(LangHelper.isTelugu(this) ? "నక్షత్రాన్ని నొక్కండి" : "Tap a star to rate");
        ratingLabel.setTextColor(TEXT_HINT);
        ratingLabel.setTextSize(TEXT_SM);
        ratingLabel.setGravity(Gravity.CENTER);
        ratingCard.addView(ratingLabel);
        content.addView(ratingCard);

        // ── Category card ──────────────────────────────────────────────────
        LinearLayout catCard = whiteCard();
        catCard.addView(cardLabel(LangHelper.isTelugu(this)
                ? "మీ అభిప్రాయం రకం:" : "Type of feedback:"));

        LinearLayout catRow1 = new LinearLayout(this);
        catRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cr1P = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cr1P.setMargins(0, dp(this,6), 0, dp(this,6));
        catRow1.setLayoutParams(cr1P);

        LinearLayout catRow2 = new LinearLayout(this);
        catRow2.setOrientation(LinearLayout.HORIZONTAL);

        String[] catLabels = LangHelper.isTelugu(this)
                ? new String[]{"🐛  బగ్",     "💡  సూచన",   "👍  అభినందన", "❓  ప్రశ్న"}
                : new String[]{"🐛  Bug",      "💡  Suggestion", "👍  Compliment", "❓  Question"};
        String[] catValues = {"Bug", "Suggestion", "Compliment", "Question"};

        categoryBtns = new TextView[catLabels.length];
        for (int i = 0; i < catLabels.length; i++) {
            final int idx = i;
            categoryBtns[i] = new TextView(this);
            categoryBtns[i].setText(catLabels[i]);
            categoryBtns[i].setTextSize(TEXT_SM);
            categoryBtns[i].setTypeface(Typeface.DEFAULT_BOLD);
            categoryBtns[i].setGravity(Gravity.CENTER);
            categoryBtns[i].setPadding(dp(this,10), dp(this,10), dp(this,10), dp(this,10));
            categoryBtns[i].setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 10, 1, this));
            categoryBtns[i].setTextColor(TEXT_MUTED);
            categoryBtns[i].setClickable(true); categoryBtns[i].setFocusable(true);
            LinearLayout.LayoutParams cp2 = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cp2.setMargins(0, 0, i % 2 == 0 ? dp(this,6) : 0, 0);
            categoryBtns[i].setLayoutParams(cp2);
            categoryBtns[i].setOnClickListener(v -> {
                AnimationHelper.pulse(categoryBtns[idx]);
                selectedCategory = catValues[idx];
                for (int j = 0; j < categoryBtns.length; j++) {
                    if (j == idx) {
                        categoryBtns[j].setBackground(roundedBg(BLUE_DIM, 10, this));
                        categoryBtns[j].setTextColor(BLUE);
                    } else {
                        categoryBtns[j].setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 10, 1, this));
                        categoryBtns[j].setTextColor(TEXT_MUTED);
                    }
                }
            });
            if (i < 2) catRow1.addView(categoryBtns[i]);
            else        catRow2.addView(categoryBtns[i]);
        }
        catCard.addView(catRow1);
        catCard.addView(catRow2);
        content.addView(catCard);

        // ── Message card ───────────────────────────────────────────────────
        LinearLayout msgCard = whiteCard();
        msgCard.addView(cardLabel(LangHelper.isTelugu(this)
                ? "మీ సందేశం:" : "Your message:"));

        etMessage = new EditText(this);
        etMessage.setHint(LangHelper.isTelugu(this)
                ? "మీ అభిప్రాయం ఇక్కడ రాయండి..."
                : "Write your feedback here...");
        etMessage.setHintTextColor(TEXT_HINT);
        etMessage.setTextColor(TEXT_WHITE);
        etMessage.setTextSize(TEXT_MD);
        etMessage.setTypeface(Typeface.DEFAULT);
        etMessage.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etMessage.setGravity(Gravity.TOP);
        etMessage.setMinLines(4);
        etMessage.setMaxLines(8);
        etMessage.setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 10, 1, this));
        etMessage.setPadding(dp(this,14), dp(this,14), dp(this,14), dp(this,14));
        etMessage.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        msgCard.addView(etMessage);
        content.addView(msgCard);

        // ── App Version info card ──────────────────────────────────────────
        LinearLayout infoCard = whiteCard();
        infoCard.setOrientation(LinearLayout.HORIZONTAL);
        infoCard.setGravity(Gravity.CENTER_VERTICAL);

        TextView infoIcon = new TextView(this);
        infoIcon.setText("ℹ️"); infoIcon.setTextSize(18f);
        LinearLayout.LayoutParams iiP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iiP.setMargins(0, 0, dp(this,10), 0);
        infoIcon.setLayoutParams(iiP);

        TextView infoTv = new TextView(this);
        infoTv.setText((LangHelper.isTelugu(this)
                ? "మీ పేరు, ఫోన్ నంబర్ మరియు అప్ వెర్షన్ స్వయంచాలకంగా పంపబడతాయి"
                : "Your name, phone and app version will be sent automatically")
                + "\nGoKirana v2.0 · Android");
        infoTv.setTextColor(TEXT_MUTED);
        infoTv.setTextSize(TEXT_XS);
        infoTv.setTypeface(Typeface.DEFAULT);
        infoTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        infoCard.addView(infoIcon);
        infoCard.addView(infoTv);
        content.addView(infoCard);

        // ── Submit button ──────────────────────────────────────────────────
        btnSubmit = new Button(this);
        btnSubmit.setText(LangHelper.isTelugu(this) ? "అభిప్రాయం పంపు" : "Submit Feedback");
        btnSubmit.setTextColor(0xFFFFFFFF);
        btnSubmit.setTextSize(TEXT_MD);
        btnSubmit.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmit.setBackground(roundedBg(BLUE, 14, this));
        btnSubmit.setAllCaps(false);
        LinearLayout.LayoutParams sbP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(this,56));
        sbP.setMargins(0, dp(this,8), 0, 0);
        btnSubmit.setLayoutParams(sbP);
        btnSubmit.setOnClickListener(v -> {
            AnimationHelper.pulse(btnSubmit);
            submitFeedback();
        });
        content.addView(btnSubmit);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        progress.getIndeterminateDrawable()
                .setColorFilter(BLUE, android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams pbP = new LinearLayout.LayoutParams(
                dp(this,40), dp(this,40));
        pbP.gravity = Gravity.CENTER_HORIZONTAL;
        pbP.setMargins(0, dp(this,10), 0, 0);
        progress.setLayoutParams(pbP);
        content.addView(progress);

        formScroll.addView(content);
        root.addView(formScroll);
        return root;
    }

    // ── Set star rating ────────────────────────────────────────────────────
    private void setRating(int rating) {
        selectedRating = rating;
        String[] labels = LangHelper.isTelugu(this)
                ? new String[]{"", "చాలా చెడ్డది", "చెడ్డది", "సరైనది", "మంచిది", "చాలా మంచిది"}
                : new String[]{"", "Very Bad", "Bad", "Okay", "Good", "Excellent"};

        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                stars[i].setText("★");
                stars[i].setTextColor(0xFFFFB300);   // filled gold star
            } else {
                stars[i].setText("☆");
                stars[i].setTextColor(0xFFCCCCCC);   // empty grey star
            }
            AnimationHelper.bounceIn(stars[i]);
        }

        // Update label under stars
        if (getWindow().getDecorView().findViewWithTag("rating_label") instanceof TextView) {
            ((TextView) getWindow().getDecorView().findViewWithTag("rating_label"))
                    .setText(rating + " ★  " + (rating < labels.length ? labels[rating] : ""));
        }
    }

    // ── Submit feedback to Firestore ───────────────────────────────────────
    private void submitFeedback() {
        String message = etMessage.getText().toString().trim();

        // Validation
        if (selectedRating == 0) {
            Toast.makeText(this,
                    LangHelper.isTelugu(this) ? "దయచేసి రేటింగ్ ఇవ్వండి" : "Please select a star rating",
                    Toast.LENGTH_SHORT).show();
            AnimationHelper.shake(stars[0]);
            return;
        }
        if (selectedCategory.isEmpty()) {
            Toast.makeText(this,
                    LangHelper.isTelugu(this) ? "అభిప్రాయం రకం ఎంచుకోండి" : "Please select a feedback type",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.isEmpty()) {
            Toast.makeText(this,
                    LangHelper.isTelugu(this) ? "సందేశం రాయండి" : "Please write your message",
                    Toast.LENGTH_SHORT).show();
            AnimationHelper.shake(etMessage);
            return;
        }

        setLoading(true);

        // Build feedback document
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("uid",         userUid);
        feedback.put("name",        userName.isEmpty() ? "Unknown" : userName);
        feedback.put("phone",       userPhone.isEmpty() ? "Unknown" : userPhone);
        feedback.put("rating",      selectedRating);
        feedback.put("category",    selectedCategory);
        feedback.put("message",     message);
        feedback.put("appVersion",  "2.0");
        feedback.put("platform",    "Android");
        feedback.put("language",    LangHelper.isTelugu(this) ? "Telugu" : "English");
        feedback.put("timestamp",   System.currentTimeMillis());
        feedback.put("date",        new SimpleDateFormat("dd/MM/yyyy HH:mm",
                Locale.getDefault()).format(new Date()));

        // Save to Firestore — feedback collection at root level
        db.collection("feedback").add(feedback)
                .addOnSuccessListener(ref -> {
                    setLoading(false);
                    showSuccess();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            LangHelper.isTelugu(this)
                                    ? "పంపడం విఫలమైంది. మళ్ళీ ప్రయత్నించండి."
                                    : "Failed to submit. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── Show success state ─────────────────────────────────────────────────
    private void showSuccess() {
        // Scroll to top and show success card
        formScroll.smoothScrollTo(0, 0);
        successCard.setVisibility(View.VISIBLE);
        AnimationHelper.slideUpFadeIn(successCard, 400, 0);

        // Reset form fields for next use
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            etMessage.setText("");
            selectedRating = 0;
            selectedCategory = "";
            for (TextView star : stars) { star.setText("☆"); star.setTextColor(0xFFCCCCCC); }
            if (categoryBtns != null) {
                for (TextView cat : categoryBtns) {
                    cat.setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 10, 1, this));
                    cat.setTextColor(TEXT_MUTED);
                }
            }
        }, 300);
    }

    // ── Reset form ─────────────────────────────────────────────────────────
    private void resetForm() {
        successCard.setVisibility(View.GONE);
        etMessage.setText("");
        selectedRating = 0;
        selectedCategory = "";
        for (TextView star : stars) { star.setText("☆"); star.setTextColor(0xFFCCCCCC); }
        if (categoryBtns != null) {
            for (TextView cat : categoryBtns) {
                cat.setBackground(roundedStroke(BG_FIELD, BLACK_BORDER, 10, 1, this));
                cat.setTextColor(TEXT_MUTED);
            }
        }
    }

    private void setLoading(boolean on) {
        btnSubmit.setEnabled(!on);
        btnSubmit.setText(on
                ? (LangHelper.isTelugu(this) ? "పంపుతున్నాం..." : "Submitting...")
                : (LangHelper.isTelugu(this) ? "అభిప్రాయం పంపు" : "Submit Feedback"));
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    // ── UI helpers ─────────────────────────────────────────────────────────
    private LinearLayout whiteCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(this,16), dp(this,14), dp(this,16), dp(this,14));
        card.setBackground(roundedStroke(BG_CARD, BLACK_BORDER, 14, 1, this));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(this,12));
        card.setLayoutParams(p);
        return card;
    }

    private TextView cardLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(TEXT_WHITE);
        tv.setTextSize(TEXT_MD);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(this,8));
        tv.setLayoutParams(p);
        return tv;
    }
}