package com.example.detectionapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavBotActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view); // Use main_view.xml for the bottom navigation

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                replaceFragment(new MenuFragment());
            } else if (id == R.id.nav_logout) {
                showLogoutConfirmation();
            }
            return true;
        });

        // Set the default fragment
        replaceFragment(new MenuFragment());
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private void showLogoutConfirmation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
    
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout Confirmation")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Navigate back to MainActivity (login screen)
                finish();
            })
            .setNegativeButton("No", (dialog, which) -> {
                // Clear the selection of the "Logout" item
                bottomNav.getMenu().findItem(R.id.nav_logout).setChecked(false);
            })
            .setOnDismissListener(dialog -> {
                // Ensure no item is selected if the dialog is dismissed
                bottomNav.getMenu().findItem(R.id.nav_logout).setChecked(false);
            })
            .show();
    }
}