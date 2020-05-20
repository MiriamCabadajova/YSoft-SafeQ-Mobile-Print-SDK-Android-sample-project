package com.ysoftsafeqmobileprintsampleapp


import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ysoftsafeqmobileprintsampleapp.sdk.DELIVERY_ENDPOINT_MIG
import com.ysoftsafeqmobileprintsampleapp.sdk.Upload
import kotlinx.android.synthetic.main.activity_upload.*
import java.io.File

/**
 * Created by cabadajova on 16.4.2020.
 */

class UploadActivity : AppCompatActivity(), Upload.UploadCallback {

    private lateinit var serverUri: String
    private lateinit var deliveryEndpoint: String
    private var token = ""
    private var filePaths = arrayListOf<String>()
    private var printJobsArray = arrayListOf<PrintJob>()
    private var testFileNameCounter = 1

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun isUploadBeingProcessed(flag: Boolean) {
        runOnUiThread {
            if (flag) {
                add_file_btn.isEnabled = !flag
                upload_btn.isEnabled = flag
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        initializeRecyclerView()
        loadBundleInfo()

        add_file_btn.setOnClickListener {
            val filepath = createGenericFile()
            filePaths.add(filepath)

            val newPrintJob = PrintJob()
            newPrintJob.filePath = filepath
            newPrintJob.fileName = "test_file_$testFileNameCounter.txt"
            newPrintJob.uri = filepath.toUri()

            printJobsArray.add(newPrintJob)
            recycler_view.adapter?.notifyDataSetChanged()
            testFileNameCounter += 1
        }

        upload_btn.setOnClickListener {
            val uploadClass =
                Upload(this, serverUri, filePaths, token, deliveryEndpoint)
            uploadClass.handleUpload()

            clearRecyclerView()
        }
    }

    private fun loadBundleInfo() {
        val bundle = intent.extras
        this.serverUri = bundle?.getString("serverUri").toString()
        this.deliveryEndpoint = bundle?.getString("deliveryEndpoint").toString()
        if (this.deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
            this.token = bundle?.getString("token").toString()
        }
    }

    private fun initializeRecyclerView() {
        viewManager = LinearLayoutManager(this)
        viewAdapter = PrintJobAdapter(printJobsArray, this)
        recycler_view.adapter = viewAdapter
        recycler_view.layoutManager = viewManager
    }

    private fun clearRecyclerView() {
        printJobsArray.clear()
        recycler_view.adapter?.notifyDataSetChanged()
    }

    private fun createGenericFile(): String {
        val file = File(applicationContext.filesDir, "test_file_$testFileNameCounter.txt")

        file.printWriter().use { out ->
            out.println("Hello World")
        }
        return file.path
    }


}