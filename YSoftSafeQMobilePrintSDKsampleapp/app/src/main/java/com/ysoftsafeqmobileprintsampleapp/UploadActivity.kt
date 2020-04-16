package com.ysoftsafeqmobileprintsampleapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ysoftsafeqmobileprintsampleapp.sdk.Upload
import kotlinx.android.synthetic.main.activity_upload.*
import java.io.File
import java.net.URI

class UploadActivity : AppCompatActivity() {

    val DELIVERY_ENDPOINT_EUI = "eui"
    val DELIVERY_ENDPOINT_MIG = "mig"

    lateinit var serverUri: String
    lateinit var deliveryEndpoint: String
    lateinit var token: String
    var filePaths = arrayListOf<String>()
    private var printJobsArray = arrayListOf<PrintJob>()

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val uploadCallback = object : Upload.UploadCallback {
        override fun isUploadBeingProcessed(flag: Boolean) {
            runOnUiThread {
                if (flag) {
                    add_file_btn.isEnabled = !flag
                    upload_btn.isEnabled = flag
                } else {
                    add_file_btn.isEnabled = !flag
                }
            }


        }

        override fun selectBtnIsVisible(flag: Boolean) {
            // hide optional select files button
        }

        override fun showDialog(title: String, message: String) {
            this@UploadActivity.runOnUiThread {
                val alertDialogBuilder = AlertDialog.Builder(this@UploadActivity)
                alertDialogBuilder.setTitle(title)
                alertDialogBuilder.setMessage(message)
                alertDialogBuilder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }

                val alert = alertDialogBuilder.create()
                alert.show()

            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        createGenericFile()

        viewManager = LinearLayoutManager(this)
        viewAdapter =  PrintJobAdapter(printJobsArray, this)

        recycler_view.adapter = viewAdapter
        recycler_view.layoutManager = viewManager

        val bundle = intent.extras

        this.serverUri = bundle?.getString("serverUri").toString()
        this.deliveryEndpoint = bundle?.getString("deliveryEndpoint").toString()
        if (this.deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
            this.token = bundle?.getString("token").toString()
        }

        add_file_btn.setOnClickListener {
            val filepath = createGenericFile()
            filePaths.add(filepath)

            val newPrintJob = PrintJob()
            newPrintJob.filePath = filepath
            newPrintJob.fileName = "test.txt"
            newPrintJob.uri = filepath.toUri()

            printJobsArray.add(newPrintJob)
            recycler_view.adapter?.notifyDataSetChanged()
        }

        upload_btn.setOnClickListener {
            val uploadClass =
                Upload(uploadCallback, serverUri, filePaths, token, deliveryEndpoint)
            uploadClass.handleUpload()

            clearRecyclerView()
        }
    }

    private fun clearRecyclerView() {
        printJobsArray.clear()
        recycler_view.adapter?.notifyDataSetChanged()
    }

    private fun createGenericFile(): String {
        val file = File(applicationContext.filesDir, "test.txt")
        file.printWriter().use { out ->
            out.println("Hello World")
        }
        return file.path
    }


}