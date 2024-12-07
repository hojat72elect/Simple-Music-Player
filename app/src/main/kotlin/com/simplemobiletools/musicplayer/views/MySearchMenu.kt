package com.simplemobiletools.musicplayer.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.appbar.AppBarLayout
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.new_architecture.shared.BaseSimpleActivity
import com.simplemobiletools.musicplayer.databinding.MenuSearchBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.onTextChangeListener
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.LOWER_ALPHA
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.MEDIUM_ALPHA
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.adjustAlpha
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.applyColorFilter
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getContrastColor
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getProperBackgroundColor
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getProperPrimaryColor
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.hideKeyboard
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.removeBit
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.showKeyboard

class MySearchMenu(context: Context, attrs: AttributeSet) : AppBarLayout(context, attrs) {
    var isSearchOpen = false
    private var useArrowIcon = false
    private var onSearchOpenListener: (() -> Unit)? = null
    var onSearchClosedListener: (() -> Unit)? = null
    var onSearchTextChangedListener: ((text: String) -> Unit)? = null
    private var onNavigateBackClickListener: (() -> Unit)? = null

    val binding = MenuSearchBinding.inflate(LayoutInflater.from(context), this, true)

    fun getToolbar() = binding.topToolbar

    fun setupMenu() {
        binding.topToolbarSearchIcon.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else if (useArrowIcon && onNavigateBackClickListener != null) {
                onNavigateBackClickListener!!()
            } else {
                binding.topToolbarSearch.requestFocus()
                (context as? Activity)?.showKeyboard(binding.topToolbarSearch)
            }
        }

        post {
            binding.topToolbarSearch.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    openSearch()
                }
            }
        }

        binding.topToolbarSearch.onTextChangeListener { text ->
            onSearchTextChangedListener?.invoke(text)
        }
    }

    private fun openSearch() {
        isSearchOpen = true
        onSearchOpenListener?.invoke()
        binding.topToolbarSearchIcon.setImageResource(R.drawable.ic_arrow_left_vector)
        binding.topToolbarSearchIcon.contentDescription = resources.getString(R.string.back)
    }

    fun closeSearch() {
        isSearchOpen = false
        onSearchClosedListener?.invoke()
        binding.topToolbarSearch.setText("")
        if (!useArrowIcon) {
            binding.topToolbarSearchIcon.setImageResource(R.drawable.ic_search_vector)
            binding.topToolbarSearchIcon.contentDescription = resources.getString(R.string.search)
        }
        (context as? Activity)?.hideKeyboard()
    }



    fun toggleHideOnScroll(hideOnScroll: Boolean) {
        val params = binding.topAppBarLayout.layoutParams as LayoutParams
        if (hideOnScroll) {
            params.scrollFlags =
                LayoutParams.SCROLL_FLAG_SCROLL or LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else {
            params.scrollFlags =
                params.scrollFlags.removeBit(LayoutParams.SCROLL_FLAG_SCROLL or LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
        }
    }



    fun updateColors() {
        val backgroundColor = context.getProperBackgroundColor()
        val contrastColor = backgroundColor.getContrastColor()

        setBackgroundColor(backgroundColor)
        binding.topAppBarLayout.setBackgroundColor(backgroundColor)
        binding.topToolbarSearchIcon.applyColorFilter(contrastColor)
        binding.topToolbarHolder.background?.applyColorFilter(
            context.getProperPrimaryColor().adjustAlpha(LOWER_ALPHA)
        )
        binding.topToolbarSearch.setTextColor(contrastColor)
        binding.topToolbarSearch.setHintTextColor(contrastColor.adjustAlpha(MEDIUM_ALPHA))
        (context as? BaseSimpleActivity)?.updateTopBarColors(binding.topToolbar, backgroundColor)
    }
}
