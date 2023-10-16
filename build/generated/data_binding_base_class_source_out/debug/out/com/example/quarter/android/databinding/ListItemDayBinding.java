// Generated by view binder compiler. Do not edit!
package com.example.quarter.android.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.quarter.android.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ListItemDayBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final FrameLayout dayFrame;

  @NonNull
  public final TextView dayTextView;

  private ListItemDayBinding(@NonNull LinearLayout rootView, @NonNull FrameLayout dayFrame,
      @NonNull TextView dayTextView) {
    this.rootView = rootView;
    this.dayFrame = dayFrame;
    this.dayTextView = dayTextView;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ListItemDayBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ListItemDayBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.list_item_day, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ListItemDayBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.dayFrame;
      FrameLayout dayFrame = ViewBindings.findChildViewById(rootView, id);
      if (dayFrame == null) {
        break missingId;
      }

      id = R.id.dayTextView;
      TextView dayTextView = ViewBindings.findChildViewById(rootView, id);
      if (dayTextView == null) {
        break missingId;
      }

      return new ListItemDayBinding((LinearLayout) rootView, dayFrame, dayTextView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}