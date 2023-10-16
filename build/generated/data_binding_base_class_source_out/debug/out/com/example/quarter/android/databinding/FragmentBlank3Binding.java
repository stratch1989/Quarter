// Generated by view binder compiler. Do not edit!
package com.example.quarter.android.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.quarter.android.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentBlank3Binding implements ViewBinding {
  @NonNull
  private final FrameLayout rootView;

  @NonNull
  public final FrameLayout clickableBackground;

  @NonNull
  public final FrameLayout frameForMetrics;

  @NonNull
  public final RecyclerView recyclerView;

  @NonNull
  public final ImageView save;

  @NonNull
  public final TextView textView;

  private FragmentBlank3Binding(@NonNull FrameLayout rootView,
      @NonNull FrameLayout clickableBackground, @NonNull FrameLayout frameForMetrics,
      @NonNull RecyclerView recyclerView, @NonNull ImageView save, @NonNull TextView textView) {
    this.rootView = rootView;
    this.clickableBackground = clickableBackground;
    this.frameForMetrics = frameForMetrics;
    this.recyclerView = recyclerView;
    this.save = save;
    this.textView = textView;
  }

  @Override
  @NonNull
  public FrameLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentBlank3Binding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentBlank3Binding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_blank3, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentBlank3Binding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      FrameLayout clickableBackground = (FrameLayout) rootView;

      id = R.id.frameForMetrics;
      FrameLayout frameForMetrics = ViewBindings.findChildViewById(rootView, id);
      if (frameForMetrics == null) {
        break missingId;
      }

      id = R.id.recyclerView;
      RecyclerView recyclerView = ViewBindings.findChildViewById(rootView, id);
      if (recyclerView == null) {
        break missingId;
      }

      id = R.id.save;
      ImageView save = ViewBindings.findChildViewById(rootView, id);
      if (save == null) {
        break missingId;
      }

      id = R.id.textView;
      TextView textView = ViewBindings.findChildViewById(rootView, id);
      if (textView == null) {
        break missingId;
      }

      return new FragmentBlank3Binding((FrameLayout) rootView, clickableBackground, frameForMetrics,
          recyclerView, save, textView);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}