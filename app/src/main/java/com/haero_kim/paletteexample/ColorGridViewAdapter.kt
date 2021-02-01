package com.haero_kim.paletteexample

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class ColorGridViewAdapter(private val context: Context, private val imageArrayList: ArrayList<Int>) :
    BaseAdapter() {

    override fun getCount(): Int = imageArrayList.size

    override fun getItem(position: Int): Any = imageArrayList[position]

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView

        if (convertView == null) {
            imageView = ImageView(context)
            imageView.run {
                layoutParams = ViewGroup.LayoutParams(120, 90)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(2,2,2,2)
            }
        } else {
            imageView = convertView as ImageView
        }

        imageView.setBackgroundColor(imageArrayList[position])
        return imageView
    }
}