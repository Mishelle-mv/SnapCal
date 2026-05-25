package com.example.snapcal.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.snapcal.R
import com.example.snapcal.data.repository.AuthRepository
import com.example.snapcal.data.repository.UserRepository
import com.example.snapcal.databinding.FragmentAuthBinding

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository(), UserRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                    binding.btnRegister.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.authFragment, true)
                        .build()
                    findNavController().navigate(R.id.exploreFragment, null, navOptions)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotBlank() && password.isNotBlank()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotBlank() && password.isNotBlank()) {
                viewModel.register(email, password)
            } else {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
