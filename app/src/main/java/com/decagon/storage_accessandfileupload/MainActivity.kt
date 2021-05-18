package com.decagon.storage_accessandfileupload

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentResolverCompat.query
import androidx.core.content.ContextCompat
import com.decagon.storage_accessandfileupload.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

        // initialize variables
        private lateinit var binding: ActivityMainBinding
        private lateinit var picture: Unit
        private val TAG = "PermissionDemo"
        private lateinit var imageUri: Uri
        private lateinit var downloadLink: String

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // set binding
            binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
            setContentView(binding.root)

            // select image if permission is granted
            binding.btnSelectImageButton.setOnClickListener {
                if (isPermissionsAllowed()) {
                    selectImage()
                } else {
                    askForPermissions()
                }
            }

            // upload image
            binding.btnUploadButton.setOnClickListener {
                uploadImage(imageUri)
            }

            // download image
            binding.btnDownloadButton.setOnClickListener {
                downloadPhoto()
            }
        }

        // function to download image
        private fun downloadPhoto() {
            // set dispatchers
            GlobalScope.launch(context = Dispatchers.IO) {

                // save the reference in a variable
                val imageLink = URL(downloadLink)

                // open connection to the website
                val connection = imageLink.openConnection() as HttpURLConnection

                // initiate input stream
                connection.doInput = true
                connection.connect()

                // save data coming from internet
                val inputStream: InputStream? = connection.inputStream

                // decode input to bitmap
                val bitmap = BitmapFactory.decodeStream(inputStream)

                // return result to main thread
                launch(context = Main) {
                    binding.receivedImage.setImageBitmap(bitmap)
                }
            }
        }

        // check for permission
        @RequiresApi(Build.VERSION_CODES.M)
        fun isPermissionsAllowed(): Boolean {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        // ask for permission
        @RequiresApi(Build.VERSION_CODES.M)
        fun askForPermissions(): Boolean {
            if (!isPermissionsAllowed()) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this as Activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    showPermissionDeniedDialog()
                } else {
                    ActivityCompat.requestPermissions(
                        this as Activity,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_CODE
                    )
                }
                return false
            }
            return true
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
            when (requestCode) {
                REQUEST_CODE -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission is granted, you can perform your operation here
                        selectImage()
                    } else {
                        // permission is denied, you can ask for permission again, if you want
                        askForPermissions()
                    }
                    return
                }
            }
        }

        // show runtime permission dialogue
        private fun showPermissionDeniedDialog() {
            AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Permission is denied, Please allow permissions from App Settings.")
                .setPositiveButton(
                    "App Settings"
                ) { dialogInterface, i ->
                    // send to app settings if permission is denied permanently
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // function to select image
        private fun selectImage()
        {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE)
        }

        // if image is selected successfully
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
                // display picture in holder
                picture = binding.receivedImage.setImageURI(data?.data)
                // get image uri
                imageUri = data?.data!!
                Log.d(TAG, "Upload uri: $imageUri")
            }
        }

    // upload image
        private fun uploadImage(uri: Uri) {
            val parcelFileDescriptor = this.contentResolver
                .openFileDescriptor(uri, "r", null) ?: return

            // get file details
            val file = File(this.cacheDir, getFileName(uri, this.contentResolver))

            val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
            val outStream = FileOutputStream(file)
            inputStream.copyTo(outStream)


            val body = file.asRequestBody("image/jpg".toMediaTypeOrNull())

            // get image information to upload
            val image = MultipartBody.Part.createFormData("file", file.name, body)

            // initiate upload with retrofit
            ImageUploadService.RetrofitService.retrofit.uploadImage(image)
                .enqueue(object: Callback<PhotoFormat> {
                    override fun onResponse(call: Call<PhotoFormat>, response: Response<PhotoFormat>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MainActivity, "Upload Successful", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "output success: ${response.body().toString()}")

                            // get the download link
                            downloadLink = response.body()?.payload?.downloadUri?: String()

                            // clear the image holder
                            binding.receivedImage.setImageResource(0)
                        } else {
                            Toast.makeText(this@MainActivity, "Uplaod not Successful", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "output is $response")
                        }
                    }

                    override fun onFailure(call: Call<PhotoFormat>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "${t.message}", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "output is ${t.message}")
                    }
                }
            )
        }

        // get file name
        private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
            var name = ""
            val cursor = query(contentResolver, uri, null, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                name = cursor.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
            return name
        }

        companion object {
            private var REQUEST_CODE = 100
        }

}