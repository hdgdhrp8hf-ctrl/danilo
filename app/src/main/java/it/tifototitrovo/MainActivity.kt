package it.tifototitrovo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling

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
            .getClient()
            .process(image)
            .addOnSuccessListener { result ->

                labels = result
                    .filter { it.confidence >= 0.35f }
                    .take(10)
                    .map {
                        "${it.text} — ${(it.confidence * 100).toInt()}%"
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

    val camera =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->

            if (bitmap != null) {
                analyze(bitmap)
            }
        }

    val permission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                camera.launch(null)
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
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(18.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                item {

                    Text(
                        "📸 Fai solo la foto",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        "L'AI prova a riconoscere automaticamente gli oggetti.",
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

                                camera.launch(null)

                            } else {

                                permission.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text("FOTOGRAFA")
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
                                "🤖 Oggetti riconosciuti",
                                style =
                                    MaterialTheme.typography.titleLarge
                            )

                            Spacer(
                                Modifier.height(8.dp)
                            )

                            labels.forEach { label ->

                                Text(
                                    "• $label"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
