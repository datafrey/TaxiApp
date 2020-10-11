package com.datafrey.taxiapp.splashscreen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.userstatus.UserStatusActivity
import com.datafrey.taxiapp.util.startActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val authorizationCompleted = FirebaseAuth.getInstance().currentUser != null
        startActivity<UserStatusActivity> {
            putExtra("authorizationCompleted", authorizationCompleted)
        }

        finish()
    }

}