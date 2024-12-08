package com.simplemobiletools.musicplayer.new_architecture.shared.data

import com.simplemobiletools.musicplayer.new_architecture.shared.ui.views.MyRecyclerViewAdapter

interface ItemTouchHelperContract {
    fun onRowMoved(fromPosition: Int, toPosition: Int)

    fun onRowSelected(myViewHolder: MyRecyclerViewAdapter.ViewHolder?)

    fun onRowClear(myViewHolder: MyRecyclerViewAdapter.ViewHolder?)
}
