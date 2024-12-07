package com.simplemobiletools.musicplayer.dialogs

import android.app.Activity
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.databinding.DialogPropertiesBinding
import com.simplemobiletools.musicplayer.databinding.ItemPropertyBinding
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.copyToClipboard
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.getProperTextColor
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.showLocationOnMap
import com.simplemobiletools.musicplayer.new_architecture.shared.extensions.value

abstract class BasePropertiesDialog(activity: Activity) {
    private val mInflater: LayoutInflater = LayoutInflater.from(activity)
    protected val mPropertyView: ViewGroup
    private val mResources: Resources = activity.resources
    protected val mActivity: Activity = activity
    protected val mDialogView: DialogPropertiesBinding =
        DialogPropertiesBinding.inflate(mInflater, null, false)

    init {
        mPropertyView = mDialogView.propertiesHolder
    }

    protected fun addProperty(labelId: Int, value: String?, viewId: Int = 0) {
        if (value == null) {
            return
        }

        ItemPropertyBinding.inflate(mInflater, null, false).apply {
            propertyValue.setTextColor(mActivity.getProperTextColor())
            propertyLabel.setTextColor(mActivity.getProperTextColor())

            propertyLabel.text = mResources.getString(labelId)
            propertyValue.text = value
            mPropertyView.findViewById<LinearLayout>(R.id.properties_holder).addView(root)

            root.setOnLongClickListener {
                mActivity.copyToClipboard(propertyValue.value)
                true
            }

            if (labelId == R.string.gps_coordinates) {
                root.setOnClickListener {
                    mActivity.showLocationOnMap(value)
                }
            }

            if (viewId != 0) {
                root.id = viewId
            }
        }
    }
}
