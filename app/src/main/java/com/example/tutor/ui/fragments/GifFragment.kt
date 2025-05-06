package com.example.tutor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.tutor.R
import com.example.tutor.databinding.FragmentGifBinding

class GifFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_gif, container, false)

        val gifImageView: ImageView = view.findViewById(R.id.ivGifBackground)

        Glide.with(this)
            .asGif()
            .load(R.drawable.login_background)
            .into(gifImageView)

        return view
    }
}