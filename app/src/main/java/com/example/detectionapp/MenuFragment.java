package com.example.detectionapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        // Navigate to QR Scanner
        LinearLayout boxQR = view.findViewById(R.id.boxQR);
        boxQR.setOnClickListener(v -> navigateToFragment(new QRScannerFragment()));

        // Navigate to 3D Scanner
        LinearLayout box3D = view.findViewById(R.id.box3D);
        box3D.setOnClickListener(v -> navigateToFragment(new ThreeDObjectFragment()));

        return view;
    }

    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }
}