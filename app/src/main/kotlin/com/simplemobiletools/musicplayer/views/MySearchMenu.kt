package com.simplemobiletools.musicplayer.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.appbar.AppBarLayout
import com.simplemobiletools.commons.R
import com.simplemobiletools.musicplayer.activities.BaseSimpleActivity
import com.simplemobiletools.commons.databinding.MenuSearchBinding
import com.simplemobiletools.musicplayer.extensions.onTextChangeListener
import com.simplemobiletools.musicplayer.helpers.LOWER_ALPHA
import com.simplemobiletools.musicplayer.helpers.MEDIUM_ALPHA
import com.simplemobiletools.musicplayer.extensions.adjustAlpha
import com.simplemobiletools.musicplayer.extensions.applyColorFilter
import com.simplemobiletools.musicplayer.extensions.getContrastColor
import com.simplemobiletools.musicplayer.extensions.getProperBackgroundColor
import com.simplemobiletools.musicplayer.extensions.getProperPrimaryColor
import com.simplemobiletools.musicplayer.extensions.hideKeyboard
import com.simplemobiletools.musicplayer.extensions.removeBit
import com.simplemobiletools.musicplayer.extensions.showKeyboard

class MySearchMenu(context: Context, attrs: AttributeSet) : AppBarLayout(context, attrs) {
    var isSearchOpen = false
    var useArrowIcon = false
    var onSearchOpenListener: (() -> Unit)? = null
    var onSearchClosedListener: (() -> Unit)? = null
    var onSearchTextChangedListener: ((text: String) -> Unit)? = null
    var onNavigateBackClickListener: (() -> Unit)? = null

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
            binding.topToolbarSearch.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    openSearch()
                }
            }
        }

        binding.topToolbarSearch.onTextChangeListener { text ->
            onSearchTextChangedListener?.invoke(text)
        }
    }

    fun focusView() {
        binding.topToolbarSearch.requestFocus()
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

    fun getCurrentQuery() = binding.topToolbarSearch.text.toString()

    fun updateHintText(text: String) {
        binding.topToolbarSearch.hint = text
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

    fun toggleForceArrowBackIcon(useArrowBack: Boolean) {
        this.useArrowIcon = useArrowBack
        val (icon, accessibilityString) = if (useArrowBack) {
            Pair(R.drawable.ic_arrow_left_vector, R.string.back)
        } else {
            Pair(R.drawable.ic_search_vector, R.string.search)
        }

        binding.topToolbarSearchIcon.setImageResource(icon)
        binding.topToolbarSearchIcon.contentDescription = resources.getString(accessibilityString)
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
