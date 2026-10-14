package jp.aburipan.everylauncher.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AppPickerDialog {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int GRID_VERTICAL_SPACING_DP = UiDimensions.SPACING_12_DP;
    private static final int GRID_HORIZONTAL_SPACING_DP = UiDimensions.SPACING_8_DP;
    private static final int GRID_PADDING_DP = UiDimensions.SPACING_24_DP;
    private static final int GRID_COLUMN_COUNT = 6;
    private static final int CELL_VERTICAL_PADDING_DP = UiDimensions.SPACING_8_DP;
    private static final int APP_ICON_SIZE_DP = 56;
    private static final int LABEL_TOP_MARGIN_DP = UiDimensions.SPACING_6_DP;
    private static final int LABEL_MAX_LINES = 1;
    private static final int DIALOG_CORNER_RADIUS_DP = 32;
    private static final int PACKAGE_QUERY_FLAGS = 0;
    private static final int DIALOG_BACKGROUND_COLOR = Color.argb(245, 18, 22, 40);
    private static final double DIALOG_WIDTH_RATIO = 0.88d;
    private static final double DIALOG_HEIGHT_RATIO = 0.85d;

    private AppPickerDialog() { }

    public static void show(Activity activity) {
        PackageManager packageManager = activity.getPackageManager();
        List<ResolveInfo> apps = new ArrayList<>(packageManager.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PACKAGE_QUERY_FLAGS));
        apps.removeIf(app -> activity.getPackageName().equals(app.activityInfo.packageName));
        apps.sort(Comparator.comparing(app -> app.loadLabel(packageManager)
                .toString().toLowerCase(Locale.ROOT)));

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        GridView grid = new GridView(activity);
        grid.setNumColumns(GRID_COLUMN_COUNT);
        grid.setVerticalSpacing(dp(activity, GRID_VERTICAL_SPACING_DP));
        grid.setHorizontalSpacing(dp(activity, GRID_HORIZONTAL_SPACING_DP));
        int gridPadding = dp(activity, GRID_PADDING_DP);
        grid.setPadding(gridPadding, gridPadding, gridPadding, gridPadding);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setBackground(rounded(activity, DIALOG_BACKGROUND_COLOR, DIALOG_CORNER_RADIUS_DP));
        grid.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return apps.size();
            }

            @Override
            public Object getItem(int position) {
                return apps.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View recycledView, ViewGroup parent) {
                ResolveInfo app = apps.get(position);
                LinearLayout cell = new LinearLayout(activity);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(
                        0,
                        dp(activity, CELL_VERTICAL_PADDING_DP),
                        0,
                        dp(activity, CELL_VERTICAL_PADDING_DP)
                );

                ImageView icon = new ImageView(activity);
                icon.setImageDrawable(app.loadIcon(packageManager));
                cell.addView(icon, new LinearLayout.LayoutParams(
                        dp(activity, APP_ICON_SIZE_DP), dp(activity, APP_ICON_SIZE_DP)));

                TextView label = new TextView(activity);
                label.setText(app.loadLabel(packageManager).toString());
                label.setTextSize(UiTextSizes.SIZE_13_SP);
                label.setTextColor(Color.WHITE);
                label.setTypeface(android.graphics.Typeface.create(
                        Fonts.SANS_SERIF_LIGHT, android.graphics.Typeface.NORMAL));
                label.setMaxLines(LABEL_MAX_LINES);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(MATCH, WRAP);
                labelParams.topMargin = dp(activity, LABEL_TOP_MARGIN_DP);
                cell.addView(label, labelParams);
                return cell;
            }
        });
        grid.setOnItemClickListener((parent, view, position, id) -> {
            String packageName = apps.get(position).activityInfo.packageName;
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                activity.startActivity(launchIntent);
                dialog.dismiss();
            }
        });

        dialog.setContentView(grid);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (activity.getResources().getDisplayMetrics().widthPixels * DIALOG_WIDTH_RATIO),
                    (int) (activity.getResources().getDisplayMetrics().heightPixels * DIALOG_HEIGHT_RATIO));
        }
    }

    private static GradientDrawable rounded(Activity activity, int color, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(activity, radiusDp));
        return background;
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }
}
