package com.ysoftsafeqmobileprintsampleapp

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recyclerview_item.view.*

class PrintJobAdapter(
    private val printJobs: ArrayList<PrintJob>,
    private val context: Context
) : RecyclerView.Adapter<PrintJobAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        val uploadBtn = (context as Activity).findViewById<Button>(R.id.upload_btn)
        uploadBtn.isEnabled = printJobs.size > 0
        return printJobs.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.recyclerview_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.recyclerViewItemType.text = printJobs[position].fileName
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recyclerViewItemType = view.recycler_view_item_filename!!

    }

}