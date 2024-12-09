package ca.hojat.smart.musicplayer.shared.ui.dialogs

import android.app.Activity
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.databinding.DialogPropertiesBinding
import ca.hojat.smart.musicplayer.databinding.ItemPropertyBinding
import ca.hojat.smart.musicplayer.shared.extensions.copyToClipboard
import ca.hojat.smart.musicplayer.shared.extensions.getProperTextColor
import ca.hojat.smart.musicplayer.shared.extensions.showLocationOnMap
import ca.hojat.smart.musicplayer.shared.extensions.value

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
