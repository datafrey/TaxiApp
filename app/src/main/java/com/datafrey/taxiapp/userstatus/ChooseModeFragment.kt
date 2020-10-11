package com.datafrey.taxiapp.userstatus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.drivermaps.DriverMapsActivity
import com.datafrey.taxiapp.passengermaps.PassengerMapsActivity
import com.datafrey.taxiapp.util.startActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_choose_mode.view.*

class ChooseModeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_choose_mode, container, false)

        root.passengerButton.setOnClickListener {
            activity?.startActivity<PassengerMapsActivity>()
        }

        root.driverButton.setOnClickListener {
            activity?.startActivity<DriverMapsActivity>()
        }

        root.signOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            root.findNavController()
                .navigate(R.id.action_chooseModeFragment_to_signInFragment)
        }

        return root
    }

}