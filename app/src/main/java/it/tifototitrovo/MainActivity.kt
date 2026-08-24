package it.tifototitrovo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TifotoTiTrovo()
        }
    }
}

@Composable
fun TifotoTiTrovo() {

    val context = LocalContext.current

    var labels by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var busy by remember {
        mutableStateOf(false)
    }

    fun analyze(bitmap: Bitmap) {

        labels = emptyList()
        busy = true

        val image = InputImage.fromBitmap(bitmap, 0)

        ImageLabeling
            .getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { result ->

                labels = result
                    .filter { it.confidence >= 0.35f }
                    .take(10)
                    .map { label ->
                        "${label.text} — ${(label.confidence * 100).toInt()}%"
                    }

                busy = false
            }
            .addOnFailureListener { error ->

                labels = listOf(
                    "Errore: ${error.message ?: "analisi non riuscita"}"
                )

                busy = false
            }
    }

    val camera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == ComponentActivity.RESULT_OK) {

            val bitmap =
                result.data
                    ?.extras
                    ?.get("data") as? Bitmap

            if (bitmap != null) {
                analyze(bitmap)
            }
        }
    }

    val permission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {

            val intent =
                Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            camera.launch(intent)
        }
    }

    MaterialTheme {

        Scaffold(
            topBar = {

                TopAppBar(
                    title = {
                        Text("TifotoTiTrovo")
                    }
                )
            }
        ) { paddingValues ->

            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(18.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                item {

                    Text(
                        text = "📸 Fai solo la foto",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text =
                            "Non scrivere il nome. Fai solo la foto.",
                        fontSize = 14.sp
                    )
                }

                item {

                    Button(
                        onClick = {

                            if (
                                context.checkSelfPermission(
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {

                                val intent =
                                    Intent(
                                        MediaStore.ACTION_IMAGE_CAPTURE
                                    )

                                camera.launch(intent)

                            } else {

                                permission.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text("📸 FOTOGRAFA")
                    }
                }

                item {

                    if (busy) {

                        CircularProgressIndicator()
                    }

                    if (labels.isNotEmpty()) {

                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    "🤖 Oggetti riconosciuti",

                                style =
                                    MaterialTheme.typography
                                        .titleLarge
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            labels.forEach { label ->

                                Text("• $label")
                            }
                        }
                    }
                }
            }
        }
    }
}
