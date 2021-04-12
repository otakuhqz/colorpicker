/*
 *  This file is part of ColorPickerX
 *  Copyright (c) 2021   Saul Henriquez
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.saggitt.colorpickerx;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.saggitt.colorpickerx.ColorUtils.dip2px;
import static com.saggitt.colorpickerx.ColorUtils.getDimensionDp;

public class ColorPickerTab implements CustomPickerSelector.OnColorChangedListener, TextWatcher {

    private OnChooseColorListener onChooseColorListener;
    private OnFastChooseColorListener onFastChooseColorListener;

    public interface OnButtonListener {
        void onClick(View v, int position, int color);
    }

    private ArrayList<ColorPal> colors;
    private ColorViewAdapter colorViewAdapter;
    private boolean fastChooser;
    private TypedArray ta;
    private final Context mContext;
    private int columns;
    private int marginLeft, marginRight, marginTop, marginBottom;
    private int tickColor;
    private int marginColorButtonLeft, marginColorButtonRight, marginColorButtonTop, marginColorButtonBottom;
    private int colorButtonWidth, colorButtonHeight;
    private int colorButtonDrawable;
    private final String negativeText;
    private final String positiveText;
    private boolean roundColorButton;
    private boolean dismiss;
    private boolean fullHeight;
    private WeakReference<CustomDialog> mDialog;
    private final RecyclerView recyclerView;
    private final LinearLayout buttons_layout;
    private int default_color;
    private final View dialogViewLayout;
    private boolean disableDefaultButtons;
    private final MaterialButton positiveButton;
    private final MaterialButton negativeButton;
    private final View tabPresets;
    private final View tabCustom;

    EditText hexEditText;
    PanelView newColorPanel;
    PanelView oldColorPanel;
    CustomPickerSelector colorPicker;
    private boolean fromEditText;
    boolean showAlphaSlider;
    @ColorInt
    int color;
    ViewPager viewPager;
    TabLayout tabLayout;

    public ColorPickerTab(Context context) {
        dialogViewLayout = LayoutInflater.from(context).inflate(R.layout.color_selector_tabbed, null, false);
        tabCustom = dialogViewLayout.findViewById(R.id.tab_custom);
        tabPresets = dialogViewLayout.findViewById(R.id.tab_presets);
        View[] views = {tabCustom, tabPresets};
        String[] titles = {context.getString(R.string.color_custom), context.getString(R.string.color_presets)};

        viewPager = dialogViewLayout.findViewById(R.id.color_pager);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new CustomPagerAdapter(views, titles));

        tabLayout = dialogViewLayout.findViewById(R.id.color_tabs);
        tabLayout.setupWithViewPager(viewPager);

        recyclerView = dialogViewLayout.findViewById(R.id.color_palette);
        buttons_layout = dialogViewLayout.findViewById(R.id.buttons_layout);
        positiveButton = dialogViewLayout.findViewById(R.id.positive);
        negativeButton = dialogViewLayout.findViewById(R.id.negative);

        mContext = context;
        this.dismiss = true;
        this.marginColorButtonLeft = this.marginColorButtonTop = this.marginColorButtonRight = this.marginColorButtonBottom = 5;
        this.negativeText = context.getString(R.string.colorpicker_dialog_cancel);
        this.positiveText = context.getString(R.string.colorpicker_dialog_ok);
        this.default_color = 0;
        this.columns = 5;

        hexEditText = dialogViewLayout.findViewById(R.id.cpx_hex);
        oldColorPanel = dialogViewLayout.findViewById(R.id.cpx_color_panel_current);
        newColorPanel = dialogViewLayout.findViewById(R.id.cpx_color_panel_new);
        colorPicker = dialogViewLayout.findViewById(R.id.cpx_color_picker_selector);

        color = Color.BLUE;
        colorPicker.setAlphaSliderVisible(showAlphaSlider);
        oldColorPanel.setColor(color);
        colorPicker.setColor(color, true);
        newColorPanel.setColor(color);
        setHex(color);
        if (!showAlphaSlider) {
            hexEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        }

        newColorPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newColorPanel.getColor() == color) {
                    onColorSelected(color);
                    //dismiss();
                }
            }
        });
        colorPicker.setOnColorChangedListener(this);
        hexEditText.addTextChangedListener(this);

        hexEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(hexEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (hexEditText.isFocused()) {
            int color = parseColorString(s.toString());
            if (color != colorPicker.getColor()) {
                fromEditText = true;
                colorPicker.setColor(color, true);
            }
        }
    }

    @Override
    public void onColorChanged(int newColor) {
        color = newColor;
        if (newColorPanel != null) {
            newColorPanel.setColor(newColor);
        }
        if (!fromEditText && hexEditText != null) {
            setHex(newColor);
            if (hexEditText.hasFocus()) {
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(hexEditText.getWindowToken(), 0);
                hexEditText.clearFocus();
            }
        }
        fromEditText = false;
    }

    private void onColorSelected(int color) {

    }

    private void setHex(int color) {
        if (showAlphaSlider) {
            hexEditText.setText(String.format("%08X", (color)));
        } else {
            hexEditText.setText(String.format("%06X", (0xFFFFFF & color)));
        }
    }

    private int parseColorString(String colorString) throws NumberFormatException {
        int a, r, g, b = 0;
        if (colorString.startsWith("#")) {
            colorString = colorString.substring(1);
        }
        if (colorString.length() == 0) {
            r = 0;
            a = 255;
            g = 0;
        } else if (colorString.length() <= 2) {
            a = 255;
            r = 0;
            b = Integer.parseInt(colorString, 16);
            g = 0;
        } else if (colorString.length() == 3) {
            a = 255;
            r = Integer.parseInt(colorString.substring(0, 1), 16);
            g = Integer.parseInt(colorString.substring(1, 2), 16);
            b = Integer.parseInt(colorString.substring(2, 3), 16);
        } else if (colorString.length() == 4) {
            a = 255;
            r = Integer.parseInt(colorString.substring(0, 2), 16);
            g = r;
            r = 0;
            b = Integer.parseInt(colorString.substring(2, 4), 16);
        } else if (colorString.length() == 5) {
            a = 255;
            r = Integer.parseInt(colorString.substring(0, 1), 16);
            g = Integer.parseInt(colorString.substring(1, 3), 16);
            b = Integer.parseInt(colorString.substring(3, 5), 16);
        } else if (colorString.length() == 6) {
            a = 255;
            r = Integer.parseInt(colorString.substring(0, 2), 16);
            g = Integer.parseInt(colorString.substring(2, 4), 16);
            b = Integer.parseInt(colorString.substring(4, 6), 16);
        } else if (colorString.length() == 7) {
            a = Integer.parseInt(colorString.substring(0, 1), 16);
            r = Integer.parseInt(colorString.substring(1, 3), 16);
            g = Integer.parseInt(colorString.substring(3, 5), 16);
            b = Integer.parseInt(colorString.substring(5, 7), 16);
        } else if (colorString.length() == 8) {
            a = Integer.parseInt(colorString.substring(0, 2), 16);
            r = Integer.parseInt(colorString.substring(2, 4), 16);
            g = Integer.parseInt(colorString.substring(4, 6), 16);
            b = Integer.parseInt(colorString.substring(6, 8), 16);
        } else {
            b = -1;
            g = -1;
            r = -1;
            a = -1;
        }
        return Color.argb(a, r, g, b);
    }

    public ColorPickerTab showAlpha(boolean showAlpha) {
        showAlphaSlider = showAlpha;
        colorPicker.setAlphaSliderVisible(showAlphaSlider);

        return this;
    }

    /**
     * Set buttons color using a resource array of colors example : check in library  res/values/colorpicker-array.xml
     *
     * @param resId Array resource
     * @return this
     */
    public ColorPickerTab setColors(int resId) {
        if (mContext == null)
            return this;

        ta = mContext.getResources().obtainTypedArray(resId);
        colors = new ArrayList<>();
        for (int i = 0; i < ta.length(); i++) {
            colors.add(new ColorPal(ta.getColor(i, 0), false));
        }
        return this;
    }

    /**
     * Set buttons from an arraylist of Hex values
     *
     * @param colorsHexList List of hex values of the colors
     * @return this
     */
    public ColorPickerTab setColors(ArrayList<String> colorsHexList) {
        colors = new ArrayList<>();
        for (int i = 0; i < colorsHexList.size(); i++) {
            colors.add(new ColorPal(Color.parseColor(colorsHexList.get(i)), false));
        }
        return this;
    }

    /**
     * Set buttons color  Example : Color.RED,Color.BLACK
     *
     * @param colorsList list of colors
     * @return this
     */
    public ColorPickerTab setColors(int... colorsList) {
        colors = new ArrayList<>();
        for (int aColorsList : colorsList) {
            colors.add(new ColorPal(aColorsList, false));
        }
        return this;
    }

    /**
     * Choose the color to be selected by default
     *
     * @param color int
     * @return this
     */
    public ColorPickerTab setDefaultColorButton(int color) {
        this.default_color = color;
        return this;
    }

    /**
     * Show the Material Dialog
     */
    public void show() {
        if (mContext == null)
            return;

        if (colors == null || colors.isEmpty())
            setColors();

        mDialog = new WeakReference<>(new CustomDialog(mContext, dialogViewLayout));

        GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext, columns);
        recyclerView.setLayoutManager(gridLayoutManager);
        if (fastChooser)
            colorViewAdapter = new ColorViewAdapter(colors, onFastChooseColorListener, mDialog);
        else
            colorViewAdapter = new ColorViewAdapter(colors);
        if (fullHeight) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            //lp.addRule(RelativeLayout.BELOW, titleView.getId());
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            recyclerView.setLayoutParams(lp);
        }

        recyclerView.setAdapter(colorViewAdapter);

        if (marginBottom != 0 || marginLeft != 0 || marginRight != 0 || marginTop != 0) {
            colorViewAdapter.setMargin(marginLeft, marginTop, marginRight, marginBottom);
        }
        if (tickColor != 0) {
            colorViewAdapter.setTickColor(tickColor);
        }
        if (marginColorButtonBottom != 0 || marginColorButtonLeft != 0 || marginColorButtonRight != 0 || marginColorButtonTop != 0) {
            colorViewAdapter.setColorButtonMargin(
                    dip2px(marginColorButtonLeft, mContext), dip2px(marginColorButtonTop, mContext),
                    dip2px(marginColorButtonRight, mContext), dip2px(marginColorButtonBottom, mContext));
        }
        if (colorButtonHeight != 0 || colorButtonWidth != 0) {
            colorViewAdapter.setColorButtonSize(dip2px(colorButtonWidth, mContext), dip2px(colorButtonHeight, mContext));
        }
        if (roundColorButton) {
            setColorButtonDrawable(R.drawable.round_button);
        }
        if (colorButtonDrawable != 0) {
            colorViewAdapter.setColorButtonDrawable(colorButtonDrawable);
        }

        if (default_color != 0) {
            colorViewAdapter.setDefaultColor(default_color);
        }

        if (disableDefaultButtons) {
            positiveButton.setVisibility(View.GONE);
            negativeButton.setVisibility(View.GONE);
        }

        positiveButton.setText(positiveText);
        negativeButton.setText(negativeText);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onChooseColorListener != null && !fastChooser)
                    if (viewPager.getCurrentItem() == 0) {
                        onChooseColorListener.onChooseColor(-1, newColorPanel.getColor());
                    } else {
                        onChooseColorListener.onChooseColor(colorViewAdapter.getColorPosition(), colorViewAdapter.getColorSelected());
                    }
                if (dismiss) {
                    dismissDialog();
                    if (onFastChooseColorListener != null) {
                        onFastChooseColorListener.onCancel();
                    }
                }
            }
        });
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dismiss)
                    dismissDialog();
                if (onChooseColorListener != null)
                    onChooseColorListener.onCancel();
            }
        });

        if (mDialog == null) {
            return;
        }

        Dialog dialog = mDialog.get();

        if (dialog != null) {
            dialog.show();
            //Keep mDialog open when rotate
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            //lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.width = (int) (mContext.getResources().getDisplayMetrics().widthPixels * 0.90);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

    }

    /**
     * Define the number of columns by default value= 3
     *
     * @param c Columns number
     * @return this
     */
    public ColorPickerTab setColumns(int c) {
        columns = c;
        return this;
    }

    /**
     * Set tick color
     *
     * @param color Color
     * @return this
     */
    public ColorPickerTab setColorButtonTickColor(int color) {
        this.tickColor = color;
        return this;
    }

    /**
     * Set a single drawable for all buttons example : you can define a different shape ( then round or square )
     *
     * @param drawable Resource
     * @return this
     */
    public ColorPickerTab setColorButtonDrawable(int drawable) {
        this.colorButtonDrawable = drawable;
        return this;
    }

    /**
     * Set the buttons size in DP
     *
     * @param width  width
     * @param height height
     * @return this
     */
    public ColorPickerTab setColorButtonSize(int width, int height) {
        this.colorButtonWidth = width;
        this.colorButtonHeight = height;
        return this;
    }

    /**
     * Set the Margin between the buttons in DP is 10
     *
     * @param left   left
     * @param top    top
     * @param right  right
     * @param bottom bottom
     * @return this
     */
    public ColorPickerTab setColorButtonMargin(int left, int top, int right, int bottom) {
        this.marginColorButtonLeft = left;
        this.marginColorButtonTop = top;
        this.marginColorButtonRight = right;
        this.marginColorButtonBottom = bottom;
        return this;
    }

    /**
     * Set round button
     *
     * @param roundButton true if you want a round button
     * @return this
     */
    public ColorPickerTab setRoundColorButton(boolean roundButton) {
        this.roundColorButton = roundButton;
        return this;
    }

    /**
     * set a fast listener ( it shows a mDialog without buttons and the event fires as soon you select a color )
     *
     * @param listener OnFastChooseColorListener
     * @return this
     */
    public ColorPickerTab setOnFastChooseColorListener(OnFastChooseColorListener listener) {
        this.fastChooser = true;
        buttons_layout.setVisibility(View.GONE);
        this.onFastChooseColorListener = listener;
        dismissDialog();
        return this;
    }

    /**
     * set a listener for the color picked
     *
     * @param listener OnChooseColorListener
     */
    public ColorPickerTab setOnChooseColorListener(OnChooseColorListener listener) {
        onChooseColorListener = listener;
        return this;
    }

    /**
     * Add a  Button
     *
     * @param text     title of button
     * @param button   button to be added
     * @param listener listener
     * @return this
     */
    public ColorPickerTab addListenerButton(String text, Button button, final OnButtonListener listener) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v, colorViewAdapter.getColorPosition(), colorViewAdapter.getColorSelected());
            }
        });
        button.setText(text);
        if (button.getParent() != null)
            buttons_layout.removeView(button);
        buttons_layout.addView(button);
        return this;
    }

    /**
     * add a new Button using default style
     *
     * @param text     title of button
     * @param listener OnButtonListener
     * @return this
     */
    public ColorPickerTab addListenerButton(String text, final OnButtonListener listener) {
        if (mContext == null)
            return this;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(dip2px(10, mContext), 0, 0, 0);
        Button button = new Button(mContext);
        button.setMinWidth(getDimensionDp(R.dimen.action_button_min_width, mContext));
        button.setMinimumWidth(getDimensionDp(R.dimen.action_button_min_width, mContext));
        button.setPadding(
                getDimensionDp(R.dimen.action_button_padding_horizontal, mContext) + dip2px(5, mContext), 0,
                getDimensionDp(R.dimen.action_button_padding_horizontal, mContext) + dip2px(5, mContext), 0);
        button.setBackgroundResource(R.drawable.button);
        button.setTextSize(getDimensionDp(R.dimen.action_button_text_size, mContext));
        button.setTextColor(ContextCompat.getColor(mContext, R.color.black_de));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v, colorViewAdapter.getColorPosition(), colorViewAdapter.getColorSelected());
            }
        });
        button.setText(text);
        if (button.getParent() != null)
            buttons_layout.removeView(button);

        buttons_layout.addView(button);
        button.setLayoutParams(params);
        return this;
    }

    /**
     * set if to dismiss the mDialog or not on button listener click, by default is set to true
     *
     * @param dismiss boolean
     * @return this
     */
    public ColorPickerTab setDismissOnButtonListenerClick(boolean dismiss) {
        this.dismiss = dismiss;
        return this;
    }

    /**
     * set Match_parent to RecyclerView
     *
     * @return this
     */
    public ColorPickerTab setDialogFullHeight() {
        this.fullHeight = true;
        return this;
    }

    /**
     * getmDialog if you need more options
     *
     * @return CustomDialog
     */
    public
    @Nullable
    CustomDialog getDialog() {
        if (mDialog == null)
            return null;
        return mDialog.get();
    }

    /**
     * getDialogViewLayout is the view inflated into the mDialog
     *
     * @return View
     */
    public View getDialogViewLayout() {
        return dialogViewLayout;
    }

    /**
     * getDialogBaseLayout which is the RelativeLayout that contains the RecyclerView
     *
     * @return RelativeLayout
     */
    public View getDialogBaseLayout() {
        return tabPresets;
    }

    /**
     * get the default PositiveButton
     *
     * @return Button
     */
    public Button getPositiveButton() {
        return positiveButton;
    }

    /**
     * get the default NegativeButton
     *
     * @return Button
     */
    public Button getNegativeButton() {
        return negativeButton;
    }

    /**
     * dismiss the mDialog
     */
    public void dismissDialog() {
        if (mDialog == null)
            return;

        Dialog dialog = mDialog.get();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * disables the postive and negative buttons
     *
     * @param disableDefaultButtons boolean
     * @return this
     */
    public ColorPickerTab disableDefaultButtons(boolean disableDefaultButtons) {
        this.disableDefaultButtons = disableDefaultButtons;
        return this;
    }

    /**
     * set padding to the title in DP
     *
     * @param left   dp
     * @param top    dp
     * @param right  dp
     * @param bottom dp
     * @return this
     */
    public ColorPickerTab setTitlePadding(int left, int top, int right, int bottom) {
        return this;
    }

    /**
     * Set default colors defined in colorpicker-array.xml of the library
     *
     * @return this
     */
    private ColorPickerTab setColors() {
        if (mContext == null)
            return this;

        ta = mContext.getResources().obtainTypedArray(R.array.default_colors);
        colors = new ArrayList<>();
        for (int i = 0; i < ta.length(); i++) {
            colors.add(new ColorPal(ta.getColor(i, 0), false));
        }
        return this;
    }

    private ColorPickerTab setMargin(int left, int top, int right, int bottom) {
        this.marginLeft = left;
        this.marginRight = right;
        this.marginTop = top;
        this.marginBottom = bottom;
        return this;
    }

}
