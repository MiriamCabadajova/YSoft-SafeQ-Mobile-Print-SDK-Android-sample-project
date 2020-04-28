package com.ysoftsafeqmobileprintsampleapp.sdk

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

class Upload(
    private val uploadCallback: UploadCallback,
    private val serverName: String,
    private val myFilePaths: ArrayList<String>,
    private val uploadToken: String,
    private val deliveryEndpoint: String

) {
    interface UploadCallback {
        fun isUploadBeingProcessed(flag: Boolean)
        fun selectBtnIsVisible(flag: Boolean)
        fun showDialog(title: String, message: String)
    }

    val DELIVERY_ENDPOINT_EUI = "eui"
    val DELIVERY_ENDPOINT_MIG = "mig"
    val DELIVERY_ENDPOINT_CUPS = "cups"


    private var reUploadCandidatesPaths = arrayListOf<String>()
    private var uploadedJobsCounter = 0
    private lateinit var dialogMessageFailed: String
    private var failed = false
    private var jobsCountMig = myFilePaths.count()

    private var bwPrint = false
    private var duplexPrint = false

    private fun getUrl(suffix: String): String {

        if (deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
            return "https://" + serverName.split("://")[1] + suffix
        }

        return serverName + suffix
    }


    private fun get(url: String, callback: Callback): Call {
        val request = Request.Builder()
            .url(url)
            .build()
        val call = Login.client.newCall(request)
        call.enqueue(callback)
        return call
    }

    fun handleUpload() {
        uploadCallback.isUploadBeingProcessed(true)
        if (this.deliveryEndpoint == DELIVERY_ENDPOINT_EUI) {
            getUploadPage()
        } else if (this.deliveryEndpoint == DELIVERY_ENDPOINT_CUPS) {
            uploadToCups()
        } else {
            uploadToMig()
        }
    }

    //EUI Upload
    private fun getUploadPage() {
        val url: String = this.getUrl("upload-job")
        uploadCallback.isUploadBeingProcessed(true)
        reUploadCandidatesPaths = arrayListOf()

        get(url,
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (e.message == "java.security.cert.CertPathValidatorException: Trust anchor for " +
                        "certification path not found."
                    ) {
                        uploadCallback.showDialog(
                            "Upload Failed",
                            "You probably changed the permissions. Try rerunning the app."
                        )

                    } else {
                        uploadCallback.showDialog("Upload API failed", e.message.toString())
                    }
                    uploadCallback.selectBtnIsVisible(true)
                }

                override fun onResponse(call: Call, response: Response) {

                    val responseString: String? = response.body?.string()
                    val csrfPattern: Pattern = Pattern.compile("_csrf\".*")
                    val csrfMatcher: Matcher = csrfPattern.matcher(responseString)
                    if (csrfMatcher.find()) {
                        val uploadToken = responseString?.substring(
                            csrfMatcher.start() + 14,
                            csrfMatcher.end() - 2
                        )
                        Log.d("upload", "Download upload page success")

                        if (uploadToken != null) {
                            uploadCallback.selectBtnIsVisible(false)
                            uploadedJobsCounter = 0

                            while (myFilePaths.isNotEmpty()) {
                                //an item is always removed in uploadFile
                                uploadFile(uploadToken, 0, myFilePaths.size == 1)
                            }
                        }
                    } else {
                        uploadCallback.isUploadBeingProcessed(false)
                    }
                }
            }
        )

    }

    private fun uploadFile(token: String, indexOfFile: Int, isLastElement: Boolean) {
        val myFile = File(myFilePaths[indexOfFile])

        val uploadUrl: String = this.getUrl("upload-job")

        val MEDIA_TYPE_JPEG: MediaType? = "*/*".toMediaTypeOrNull()
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("importFile", myFile.name, myFile.readBytes().toRequestBody())
            .addFormDataPart("bw", bwPrint.toString())
            .addFormDataPart("duplex", duplexPrint.toString())
            .build()
        val request: Request = Request.Builder()
            .url(uploadUrl)
            .header("X-CSRF-TOKEN", token)
            .header("Content-Type", "multipart/form-data")
            .post(requestBody).build()

        try {
            val response: Response = Login.client.newCall(request).execute()

            val dialogTitle: String
            var dialogMessageSuccessful = "Successfully uploaded "

            if (response.isSuccessful) {
                uploadedJobsCounter++

            } else {
                failed = true
                dialogMessageFailed += "\n\t" + myFile.name
                reUploadCandidatesPaths.add(myFilePaths[indexOfFile])
            }

            //after the upload of last file, show dialog
            if (isLastElement) {

                var finalMessage = ""
                if (uploadedJobsCounter > 0) {
                    dialogTitle = "Upload Completed"
                    dialogMessageSuccessful += " $uploadedJobsCounter"

                    dialogMessageSuccessful += if (uploadedJobsCounter == 1) {
                        " file\n"
                    } else {
                        " files\n"
                    }

                    finalMessage += dialogMessageSuccessful
                } else {
                    dialogTitle = "Upload Failed"
                }

                if (failed) {
                    finalMessage += "\n" + dialogMessageFailed
                    uploadCallback.showDialog(dialogTitle, finalMessage)
                } else {
                    uploadCallback.showDialog(dialogTitle, finalMessage)
                }
                uploadCallback.isUploadBeingProcessed(false)

            }

        } catch (e: IOException) {
            uploadCallback.showDialog("Upload Failed", e.message.toString())
            uploadCallback.selectBtnIsVisible(true)
            uploadCallback.isUploadBeingProcessed(false)
        }

        myFilePaths.removeAt(indexOfFile)
    }

    //MIG upload
    private fun uploadToMig() {
        while (myFilePaths.isNotEmpty()) {
            uploadMigFile(0, myFilePaths.size == 1)
        }
    }

    private fun uploadMigFile(indexOfFile: Int, isLastElement: Boolean) {
        val myFile = File(myFilePaths[indexOfFile])

        val uploadUrl: String = this.getUrl("/ipp/print")
        val ippRequest = IppRequest(myFile.name)
        ippRequest.bwPrint = bwPrint
        ippRequest.printJobData = myFile.readBytes()

        if (duplexPrint) {
            ippRequest.sides = "two-sided-long-edge"
        }

        ippRequest.generateIppRequest()

        val requestBody = ippRequest.bytes.toRequestBody()

        val request: Request = Request.Builder()
            .url(uploadUrl)
            .header("Content-Type", "application/ipp")
            .header("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2")
            .header("Authorization", uploadToken)

            .post(requestBody).build()

        Login.client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val dialogTitle: String
                var finalMessage = ""

                // Upload failed - response code is not 200
                if (response.code != 200) {

                    dialogTitle = "Upload Failed"
                    finalMessage += "Server returned code: "
                    finalMessage += response.code

                    uploadCallback.showDialog(dialogTitle, finalMessage)
                    uploadCallback.selectBtnIsVisible(true)
                    return
                }

                val dialogMessageSuccessful = "Successfully uploaded $jobsCountMig file"
                if (jobsCountMig > 1) {
                    dialogMessageSuccessful + "s"
                }

                if (isLastElement) {
                    finalMessage = ""
                    dialogTitle = "Upload Completed"


                    finalMessage += dialogMessageSuccessful

                    if (failed) {
                        finalMessage += "\n" + dialogMessageFailed
                        uploadCallback.showDialog(dialogTitle, finalMessage)
                    } else {
                        uploadCallback.showDialog(dialogTitle, finalMessage)
                    }
                    uploadCallback.isUploadBeingProcessed(false)

                }

                uploadCallback.selectBtnIsVisible(true)

            }

            override fun onFailure(call: Call, e: IOException) {
                uploadCallback.selectBtnIsVisible(true)
                uploadCallback.isUploadBeingProcessed(false)
            }
        }
        )

        myFilePaths.removeAt(indexOfFile)
    }

    //CUPS Upload
    private fun uploadToCups() {
        while (myFilePaths.isNotEmpty()) {
            uploadCupsFile(0, myFilePaths.size == 1)
        }
    }

    private fun uploadCupsFile(indexOfFile: Int, isLastElement: Boolean) {
        val myFile = File(myFilePaths[indexOfFile])

        val uploadUrl: String = this.getUrl("")
        val ippRequest = IppRequest(myFile.name)
        ippRequest.bwPrint = bwPrint
        ippRequest.printJobData = myFile.readBytes()
        ippRequest.printerUri = uploadUrl

        if (duplexPrint) {
            ippRequest.sides = "two-sided-long-edge"
        }

        ippRequest.generateIppRequest()

        val requestBody = ippRequest.bytes.toRequestBody()

        val request: Request = Request.Builder()
            .url(uploadUrl)
            .header("Content-Type", "application/ipp")
            .header("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2")

            .post(requestBody).build()



        Login.client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val dialogTitle: String
                var finalMessage = ""

                if (response.code != 200) {

                    dialogTitle = "Upload Failed"
                    finalMessage += "Server returned code: "
                    finalMessage += response.code

                    uploadCallback.showDialog(dialogTitle, finalMessage)
                    uploadCallback.selectBtnIsVisible(true)
                    return
                }

                val dialogMessageSuccessful = "Successfully uploaded "
                if (isLastElement) {

                    var finalMessage = ""
                    dialogTitle = "Upload Completed"
                    finalMessage += dialogMessageSuccessful

                    if (failed) {
                        finalMessage += "\n" + dialogMessageFailed
                        uploadCallback.showDialog(dialogTitle, finalMessage)
                    } else {
                        uploadCallback.showDialog(dialogTitle, finalMessage)
                    }

                }
                uploadCallback.selectBtnIsVisible(true)

            }

            override fun onFailure(call: Call, e: IOException) {
                uploadCallback.selectBtnIsVisible(true)
            }
        }

        )

        myFilePaths.removeAt(indexOfFile)

    }
}