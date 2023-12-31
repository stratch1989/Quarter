// Generated by view binder compiler. Do not edit!
package com.example.quarter.android.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.quarter.android.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityMainBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final Button button0;

  @NonNull
  public final Button button1;

  @NonNull
  public final Button button2;

  @NonNull
  public final Button button3;

  @NonNull
  public final Button button4;

  @NonNull
  public final Button button5;

  @NonNull
  public final Button button6;

  @NonNull
  public final Button button7;

  @NonNull
  public final Button button8;

  @NonNull
  public final Button button9;

  @NonNull
  public final Button buttonDel;

  @NonNull
  public final Button buttonEnter;

  @NonNull
  public final Button buttonPoint;

  @NonNull
  public final TextView dayLimit;

  @NonNull
  public final TextView history;

  @NonNull
  public final LinearLayout linearLayout;

  @NonNull
  public final FrameLayout placeHolder;

  @NonNull
  public final TextView result;

  @NonNull
  public final TextView settings;

  @NonNull
  public final ConstraintLayout testet;

  @NonNull
  public final TextView textView2;

  @NonNull
  public final TextView value;

  private ActivityMainBinding(@NonNull ConstraintLayout rootView, @NonNull Button button0,
      @NonNull Button button1, @NonNull Button button2, @NonNull Button button3,
      @NonNull Button button4, @NonNull Button button5, @NonNull Button button6,
      @NonNull Button button7, @NonNull Button button8, @NonNull Button button9,
      @NonNull Button buttonDel, @NonNull Button buttonEnter, @NonNull Button buttonPoint,
      @NonNull TextView dayLimit, @NonNull TextView history, @NonNull LinearLayout linearLayout,
      @NonNull FrameLayout placeHolder, @NonNull TextView result, @NonNull TextView settings,
      @NonNull ConstraintLayout testet, @NonNull TextView textView2, @NonNull TextView value) {
    this.rootView = rootView;
    this.button0 = button0;
    this.button1 = button1;
    this.button2 = button2;
    this.button3 = button3;
    this.button4 = button4;
    this.button5 = button5;
    this.button6 = button6;
    this.button7 = button7;
    this.button8 = button8;
    this.button9 = button9;
    this.buttonDel = buttonDel;
    this.buttonEnter = buttonEnter;
    this.buttonPoint = buttonPoint;
    this.dayLimit = dayLimit;
    this.history = history;
    this.linearLayout = linearLayout;
    this.placeHolder = placeHolder;
    this.result = result;
    this.settings = settings;
    this.testet = testet;
    this.textView2 = textView2;
    this.value = value;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_main, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityMainBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.button0;
      Button button0 = ViewBindings.findChildViewById(rootView, id);
      if (button0 == null) {
        break missingId;
      }

      id = R.id.button1;
      Button button1 = ViewBindings.findChildViewById(rootView, id);
      if (button1 == null) {
        break missingId;
      }

      id = R.id.button2;
      Button button2 = ViewBindings.findChildViewById(rootView, id);
      if (button2 == null) {
        break missingId;
      }

      id = R.id.button3;
      Button button3 = ViewBindings.findChildViewById(rootView, id);
      if (button3 == null) {
        break missingId;
      }

      id = R.id.button4;
      Button button4 = ViewBindings.findChildViewById(rootView, id);
      if (button4 == null) {
        break missingId;
      }

      id = R.id.button5;
      Button button5 = ViewBindings.findChildViewById(rootView, id);
      if (button5 == null) {
        break missingId;
      }

      id = R.id.button6;
      Button button6 = ViewBindings.findChildViewById(rootView, id);
      if (button6 == null) {
        break missingId;
      }

      id = R.id.button7;
      Button button7 = ViewBindings.findChildViewById(rootView, id);
      if (button7 == null) {
        break missingId;
      }

      id = R.id.button8;
      Button button8 = ViewBindings.findChildViewById(rootView, id);
      if (button8 == null) {
        break missingId;
      }

      id = R.id.button9;
      Button button9 = ViewBindings.findChildViewById(rootView, id);
      if (button9 == null) {
        break missingId;
      }

      id = R.id.button_del;
      Button buttonDel = ViewBindings.findChildViewById(rootView, id);
      if (buttonDel == null) {
        break missingId;
      }

      id = R.id.button_enter;
      Button buttonEnter = ViewBindings.findChildViewById(rootView, id);
      if (buttonEnter == null) {
        break missingId;
      }

      id = R.id.button_point;
      Button buttonPoint = ViewBindings.findChildViewById(rootView, id);
      if (buttonPoint == null) {
        break missingId;
      }

      id = R.id.day_limit;
      TextView dayLimit = ViewBindings.findChildViewById(rootView, id);
      if (dayLimit == null) {
        break missingId;
      }

      id = R.id.history;
      TextView history = ViewBindings.findChildViewById(rootView, id);
      if (history == null) {
        break missingId;
      }

      id = R.id.linearLayout;
      LinearLayout linearLayout = ViewBindings.findChildViewById(rootView, id);
      if (linearLayout == null) {
        break missingId;
      }

      id = R.id.place_holder;
      FrameLayout placeHolder = ViewBindings.findChildViewById(rootView, id);
      if (placeHolder == null) {
        break missingId;
      }

      id = R.id.result;
      TextView result = ViewBindings.findChildViewById(rootView, id);
      if (result == null) {
        break missingId;
      }

      id = R.id.settings;
      TextView settings = ViewBindings.findChildViewById(rootView, id);
      if (settings == null) {
        break missingId;
      }

      ConstraintLayout testet = (ConstraintLayout) rootView;

      id = R.id.textView2;
      TextView textView2 = ViewBindings.findChildViewById(rootView, id);
      if (textView2 == null) {
        break missingId;
      }

      id = R.id.value;
      TextView value = ViewBindings.findChildViewById(rootView, id);
      if (value == null) {
        break missingId;
      }

      return new ActivityMainBinding((ConstraintLayout) rootView, button0, button1, button2,
          button3, button4, button5, button6, button7, button8, button9, buttonDel, buttonEnter,
          buttonPoint, dayLimit, history, linearLayout, placeHolder, result, settings, testet,
          textView2, value);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
