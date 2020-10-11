package com.datafrey.taxiapp.userstatus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.datafrey.taxiapp.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_user_status.*

class UserStatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_status)

        val authorizationCompleted = intent.getBooleanExtra(
            "authorizationCompleted", false
        )

        if (FirebaseAuth.getInstance().currentUser == null) {
            val navHostFragment = NavHostFragment as NavHostFragment
            val inflater = navHostFragment.navController.navInflater
            val graph = inflater.inflate(R.navigation.user_status_navigation)
            graph.startDestination = R.id.signInFragment

            navHostFragment.navController.graph = graph
        }
    }

}