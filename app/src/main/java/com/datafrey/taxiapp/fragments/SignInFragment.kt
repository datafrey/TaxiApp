package com.datafrey.taxiapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.databinding.FragmentSignInBinding
import com.datafrey.taxiapp.util.data
import com.datafrey.taxiapp.util.toast
import com.datafrey.taxiapp.viewmodelfactories.SignInViewModelFactory
import com.datafrey.taxiapp.viewmodels.SignInViewModel
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.android.synthetic.main.fragment_sign_in.view.*

class SignInFragment : Fragment() {

    private lateinit var viewModel: SignInViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_sign_in, container, false)

        val binding = FragmentSignInBinding.bind(root)

        viewModel = ViewModelProvider(
            requireActivity(),
            SignInViewModelFactory(requireActivity().application)
        ).get(SignInViewModel::class.java)

        binding.let {
            it.viewModel = viewModel
            it.lifecycleOwner = viewLifecycleOwner
            it.executePendingBindings()
        }

        viewModel.signInSuccessful.observe(viewLifecycleOwner, { success ->
            uiReactionToAuthorizationAttempt(root, success)
        })

        root.signInSignUpButton.setOnClickListener { onSignInSignUpButtonClick() }

        return root
    }

    private fun uiReactionToAuthorizationAttempt(root: View, authorizationSuccessful: Boolean?) {
        authorizationSuccessful?.let {
            if (it) {
                root.findNavController()
                    .navigate(R.id.action_signInFragment_to_chooseModeFragment)
            } else {
                activity?.toast("Authentication error!")
            }
            viewModel.uiReactedToAuthorizationAttempt()
        }
    }

    private fun onSignInSignUpButtonClick() {
        val email = textInputEmail.editText!!.data
        val password = textInputPassword.editText!!.data
        val confirmPassword = textInputConfirmPassword.editText!!.data
        val name = textInputName.editText!!.data
        viewModel.authorize(email, password, confirmPassword, name)
    }
}