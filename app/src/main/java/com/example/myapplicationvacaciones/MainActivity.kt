package com.example.myapplicationvacaciones

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.example.myapplicationvacaciones.db.Alojo
import com.example.myapplicationvacaciones.db.AppDataBase
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Pantalla {
    HOME,
    FORM,
    EDITAR,
    ALOJAMIENTO,
    CAMARA,
    UBICACION,
}

class AppVM : ViewModel() {
    val pantallaActual = mutableStateOf(Pantalla.HOME)
    var alojoParaEditar by mutableStateOf<Alojo?>(null)

    var permisosUbicacionOk: () -> Unit = {}
    var onPermisoCamaraOk: () -> Unit = {}

    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)

    fun setPantallaActual(pantalla: Pantalla) {
        pantallaActual.value = pantalla
    }

    fun editarAlojo(alojo: Alojo) {
        alojoParaEditar = alojo
        pantallaActual.value = Pantalla.EDITAR
    }

}

class MainActivity : ComponentActivity() {
    val appVM: AppVM by viewModels()
    val camaraVM: AppVM by viewModels()

    lateinit var cameraController: LifecycleCameraController

    val lanzadorPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[android.Manifest.permission.CAMERA] == true) {
            camaraVM.onPermisoCamaraOk()
        } else if (
            (it[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false) or
            (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
        ) {
            appVM.permisosUbicacionOk()
        } else {
            Log.v("LanzadorPermisos callback", "Se denegaron los permisos")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


        lifecycleScope.launch(Dispatchers.IO) {
            val alojoDao = AppDataBase.getInstance(this@MainActivity).alojoDao()
            val cantAlojamiento = alojoDao.contar()
            if (cantAlojamiento < 1) {
                alojoDao.insertar(
                    Alojo(
                        id = 0,
                        lugar = "Termas de Chillan",
                        imagen = "https://www.chileanski.com/fotos/hotel/hi/puerto_htch_ago2019-2058-1573843675.jpg",
                        lat_long = "-36.904551, -71.5478645",
                        orden = 1,
                        costo_alojamiento = 50000,
                        costo_traslado = 30000,
                        comentarios = "Hermoso"
                    )
                )
                alojoDao.insertar(
                    Alojo(
                        id = 0,
                        lugar = "Termas de Chillan",
                        imagen = "https://www.chileanski.com/fotos/hotel/hi/puerto_htch_ago2019-2058-1573843675.jpg",
                        lat_long = "-36.904551, -71.5478645",
                        orden = 1,
                        costo_alojamiento = 50000,
                        costo_traslado = 30000,
                        comentarios = "Hermoso"
                    )
                )
                alojoDao.insertar(
                    Alojo(
                        id = 0,
                        lugar = "Termas de Chillan",
                        imagen = "https://www.chileanski.com/fotos/hotel/hi/puerto_htch_ago2019-2058-1573843675.jpg",
                        lat_long = "-36.904551, -71.5478645",
                        orden = 1,
                        costo_alojamiento = 50000,
                        costo_traslado = 30000,
                        comentarios = "Hermoso"
                    )
                )
            }
        }

        setContent {
            AppUI(lanzadorPermisos, cameraController)
        }
    }
}

@Composable
fun AppUI(lanzadorPermisos: ActivityResultLauncher<Array<String>>, cameraController: LifecycleCameraController) {
    val appVM: AppVM = viewModel()
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    when (val pantallaActual = appVM.pantallaActual.value) {
        Pantalla.HOME -> {
            ListaAlojosUI(appVM)
        }
        Pantalla.FORM -> {
            NuevoAlojamientoUI {
                appVM.setPantallaActual(Pantalla.HOME)
            }
        }
        Pantalla.EDITAR -> {
            appVM.alojoParaEditar?.let { alojo ->
                EditarAlojamientoUI(alojo) {
                    appVM.setPantallaActual(Pantalla.HOME)
                }
            }
        }
        Pantalla.ALOJAMIENTO -> {
            appVM.alojoParaEditar?.let { alojo ->
                AlojamientoUI(
                    alojo = alojo,
                    onAtrasClick = {
                        appVM.setPantallaActual(Pantalla.HOME)
                    },
                    onDeleteClick = {
                        alcanceCorrutina.launch(Dispatchers.IO) {
                            try {
                                val dao = AppDataBase.getInstance(contexto).alojoDao()
                                dao.eliminar(alojo)
                                withContext(Dispatchers.Main) {
                                    appVM.setPantallaActual(Pantalla.HOME)
                                    dao.findAll()
                                }
                            } catch (e: Exception) {
                                Log.e("EliminarAlojamiento", "Error al eliminar alojamiento: ${e.message}", e)
                            }
                        }
                    },
                            onEditClick = {
                        appVM.editarAlojo(alojo)
                    }
                )
            }
        }
        Pantalla.CAMARA -> {
            PantallaCamaraUI(cameraController, "nombreDelLugar")
        }
        Pantalla.UBICACION -> {
            PantallaUbicacionUI(appVM, lanzadorPermisos)
        }

        else -> {}
    }
}

@Composable
fun ListaAlojosUI(appVM: AppVM) {
    val contexto = LocalContext.current
    val (alojos, setAlojos) = remember { mutableStateOf(emptyList<Alojo>()) }

    LaunchedEffect(alojos) {
        withContext(Dispatchers.IO) {
            val dao = AppDataBase.getInstance(contexto).alojoDao()
            val alojosFromDB = dao.findAll()
            setAlojos(alojosFromDB)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (alojos.isEmpty()) {
                item {
                    Text(
                        "No hay alojamientos disponibles",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(alojos) { alojo ->
                    AlojosItemUI(alojo, appVM) {
                        val dao = AppDataBase.getInstance(contexto).alojoDao()
                        val alojoActualizado = dao.findAll()
                        setAlojos(alojoActualizado)
                    }
                }
            }
        }

        Button(
            onClick = {
                appVM.setPantallaActual(Pantalla.FORM)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Agregar Alojamiento")
        }
    }
}

@Composable
fun AlojosItemUI(alojo: Alojo, appVM: AppVM, onSave: () -> Unit = {}) {
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 20.dp)
            .clickable {
                appVM.setPantallaActual(Pantalla.ALOJAMIENTO) // Cambiar a la pantalla "Alojamiento"
                appVM.alojoParaEditar = alojo // Guardar el alojamiento seleccionado
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberImagePainter(alojo.imagen),
            contentDescription = "Imagen: ${alojo.lugar}",
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
                .padding(end = 16.dp),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = alojo.lugar,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Costo Alojamiento: ${alojo.costo_alojamiento}",
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Costo Traslado: ${alojo.costo_traslado}",
                modifier = Modifier.fillMaxWidth()
            )

            if (alojo.id != 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            alcanceCorrutina.launch(Dispatchers.IO) {
                                val dao = AppDataBase
                                    .getInstance(contexto)
                                    .alojoDao()
                                dao.eliminar(alojo)
                            }
                        }
                ) {
                    if (alojo.id != 0) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Eliminar Producto",
                            modifier = Modifier.clickable {
                                alcanceCorrutina.launch(Dispatchers.IO) {
                                    val dao = AppDataBase.getInstance(contexto).alojoDao()
                                    dao.eliminar(alojo)
                                    onSave()
                                }
                            }
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Alojamiento",
                            modifier = Modifier
                                .clickable {
                                    appVM.editarAlojo(alojo)
                                }
                                .padding(end = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Ver Mapa",
                            modifier = Modifier
                                .clickable {
                                    // Llamar a PantallaUbicacionUI con las coordenadas del alojamiento
                                    val latitudYLongitud = alojo.lat_long.split(",")
                                    val latitudNumerica = latitudYLongitud.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                                    val longitudNumerica = latitudYLongitud.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                                    appVM.latitud.value = latitudNumerica
                                    appVM.longitud.value = longitudNumerica
                                    appVM.pantallaActual.value = Pantalla.UBICACION
                                }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoAlojamientoUI(onAgregarAlojo: () -> Unit) {
    var nuevoAlojo by remember {
        mutableStateOf(
            Alojo(
                id = 0,
                lugar = "",
                imagen = "",
                lat_long = "",
                orden = 0,
                costo_alojamiento = 0,
                costo_traslado = 0,
                comentarios = ""
            )
        )
    }
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 20.dp)
    ) {
        TextField(
            value = nuevoAlojo.lugar,
            onValueChange = { nuevoAlojo = nuevoAlojo.copy(lugar = it) },
            label = { Text("Lugar") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = nuevoAlojo.imagen,
            onValueChange = { nuevoAlojo = nuevoAlojo.copy(imagen = it) },
            label = { Text("URL de la Imagen") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = nuevoAlojo.lat_long.toString(),
            onValueChange = {
                try {
                    nuevoAlojo = nuevoAlojo.copy(lat_long = it.toString())
                } catch (e: NumberFormatException) {
                    nuevoAlojo = nuevoAlojo.copy(lat_long = "")
                }
            },
            label = { Text("Latitud, Longitud") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = nuevoAlojo.costo_alojamiento.toString(),
            onValueChange = {
                try {
                    nuevoAlojo = nuevoAlojo.copy(costo_alojamiento = it.toInt())
                } catch (e: NumberFormatException) {
                    nuevoAlojo = nuevoAlojo.copy(costo_alojamiento = 0)
                }
            },
            label = { Text("Costo Alojamiento") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = nuevoAlojo.costo_traslado.toString(),
            onValueChange = {
                try {
                    nuevoAlojo = nuevoAlojo.copy(costo_traslado = it.toInt())
                } catch (e: NumberFormatException) {
                    nuevoAlojo = nuevoAlojo.copy(costo_traslado = 0)
                }
            },
            label = { Text("Costo Traslado") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = nuevoAlojo.orden.toString(),
            onValueChange = {
                try {
                    nuevoAlojo = nuevoAlojo.copy(orden = it.toInt())
                } catch (e: NumberFormatException) {
                    nuevoAlojo = nuevoAlojo.copy(orden = 0)
                }
            },
            label = { Text("Orden") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = nuevoAlojo.comentarios,
            onValueChange = { nuevoAlojo = nuevoAlojo.copy(comentarios = it) },
            label = { Text("Comentarios") },
            modifier = Modifier
                .fillMaxWidth()
        )

        Button(
            onClick = {
                alcanceCorrutina.launch(Dispatchers.IO) {
                    val dao = AppDataBase.getInstance(contexto).alojoDao()
                    dao.insertar(nuevoAlojo)
                    onAgregarAlojo()
                    nuevoAlojo = Alojo(
                        id = 0,
                        lugar = "",
                        imagen = "",
                        lat_long = "",
                        orden = 0,
                        costo_alojamiento = 0,
                        costo_traslado = 0,
                        comentarios = ""
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Agregar Alojamiento")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarAlojamientoUI(alojo: Alojo, onEditarAlojo: () -> Unit) {
    var alojoEditado by remember { mutableStateOf(alojo) }
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 20.dp)
    ) {
        TextField(
            value = alojoEditado.lugar,
            onValueChange = { alojoEditado = alojoEditado.copy(lugar = it) },
            label = { Text("Lugar") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = alojoEditado.imagen,
            onValueChange = { alojoEditado = alojoEditado.copy(imagen = it) },
            label = { Text("URL de la Imagen") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = alojoEditado.lat_long.toString(),
            onValueChange = {
                try {
                    alojoEditado = alojoEditado.copy(lat_long = it.toString())
                } catch (e: NumberFormatException) {
                    alojoEditado = alojoEditado.copy(lat_long = "")
                }
            },
            label = { Text("Latitud") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = alojoEditado.costo_alojamiento.toString(),
            onValueChange = {
                try {
                    alojoEditado = alojoEditado.copy(costo_alojamiento = it.toInt())
                } catch (e: NumberFormatException) {
                    alojoEditado = alojoEditado.copy(costo_alojamiento = 0)
                }
            },
            label = { Text("Costo Alojamiento") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = alojoEditado.costo_traslado.toString(),
            onValueChange = {
                try {
                    alojoEditado = alojoEditado.copy(costo_traslado = it.toInt())
                } catch (e: NumberFormatException) {
                    alojoEditado = alojoEditado.copy(costo_traslado = 0)
                }
            },
            label = { Text("Costo Traslado") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = alojoEditado.orden.toString(),
            onValueChange = {
                try {
                    alojoEditado = alojoEditado.copy(orden = it.toInt())
                } catch (e: NumberFormatException) {
                    alojoEditado = alojoEditado.copy(orden = 0)
                }
            },
            label = { Text("Orden") },
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = alojoEditado.comentarios,
            onValueChange = { alojoEditado = alojoEditado.copy(comentarios = it) },
            label = { Text("Comentarios") },
            modifier = Modifier
                .fillMaxWidth()
        )

        Button(
            onClick = {
                alcanceCorrutina.launch(Dispatchers.IO) {
                    val dao = AppDataBase.getInstance(contexto).alojoDao()
                    dao.actualizar(alojoEditado)
                    onEditarAlojo()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Guardar Cambios")
        }
    }
}

@Composable
fun AlojamientoUI(alojo: Alojo, onAtrasClick: () -> Unit, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
    val appVM: AppVM = viewModel()
    val contexto = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio vertical entre elementos
    ) {
        item {
            Text(
                text = "${alojo.lugar}",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Image(
                painter = rememberImagePainter(alojo.imagen),
                contentDescription = "Imagen: ${alojo.lugar}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Altura deseada para la imagen
                    .padding(vertical = 16.dp),
                contentScale = ContentScale.Crop
            )
        }

        item {
            Text(
                text = "Costo X Noche: ${alojo.costo_alojamiento}",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Traslados: ${alojo.costo_traslado}",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Comentarios: ${alojo.comentarios}",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar Producto",
                    modifier = Modifier
                        .clickable {
                            onDeleteClick()
                        }
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar Alojamiento",
                    modifier = Modifier
                        .clickable {
                            onEditClick()
                        }
                        .padding(end = 8.dp)
                )
                Button(onClick = {
                    appVM.pantallaActual.value = Pantalla.CAMARA
                }) {
                    Text("Tomar foto")
                }
            }
        }


        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Altura deseada para el mapa
                    .padding(bottom = 66.dp)
                    .padding(top = 66.dp)// Agregar un espacio en la parte inferior (puedes ajustar el valor según tus necesidades)
            ) {
                val latitudYLongitud = alojo.lat_long.split(",") // Divide la cadena en latitud y longitud
                val latitudNumerica = latitudYLongitud.getOrNull(0)?.toDoubleOrNull() ?: 0.0 // Convierte la latitud en Double
                val longitudNumerica = latitudYLongitud.getOrNull(1)?.toDoubleOrNull() ?: 0.0 // Convierte la longitud en Double

                AndroidView(
                    factory = {
                        MapView(it).apply {
                            setTileSource(TileSourceFactory.MAPNIK)

                            org.osmdroid.config.Configuration.getInstance().userAgentValue =
                                contexto.packageName
                            controller.setZoom(15.0)
                        }
                    }, update = {
                        it.overlays.removeIf { true }
                        it.invalidate()

                        val geoPoint = GeoPoint(latitudNumerica, longitudNumerica)
                        it.controller.animateTo(geoPoint)

                        val marcador = Marker(it)
                        marcador.position = geoPoint
                        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        it.overlays.add(marcador)
                    }
                )
            }
        }

        item {
            Button(
                onClick = {
                    onAtrasClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Atrás")
            }
        }
    }
}


class FaltaPermisosException(mensaje:String): Exception(mensaje)

@Composable
fun PantallaCamaraUI(
    cameraController: LifecycleCameraController,
    nombreLugar: String // Agregar el nombre del lugar como parámetro
) {
    val appVM: AppVM = viewModel()
    val contexto = LocalContext.current

    // Agregar el código para solicitar permisos de cámara
    val lanzadorPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso de cámara concedido, puedes proceder a tomar la foto
            capturarFotografia(cameraController, contexto) { uri ->
                // Aquí puedes hacer algo con la URI de la foto, como guardarla en la base de datos
                // o mostrarla en tu aplicación

                // Después de tomar la foto, cambia la pantalla actual de vuelta a la pantalla anterior
                appVM.setPantallaActual(Pantalla.ALOJAMIENTO) // Cambia a la pantalla que desees
            }
        } else {
            // Permiso de cámara no concedido, puedes mostrar un mensaje de error o tomar otra acción
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PreviewView(it).apply {
                controller = cameraController
            }
        }
    )

    Button(onClick = {
        if (ContextCompat.checkSelfPermission(
                contexto, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            capturarFotografia(cameraController, contexto) { uri ->
                appVM.setPantallaActual(Pantalla.ALOJAMIENTO) // Cambia a la pantalla que desees
            }
        } else {
            lanzadorPermisos.launch(android.Manifest.permission.CAMERA)
        }
    }) {
        Text("Tomar Foto")
    }
}

fun generarNombreUnico(): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "IMG_$timeStamp"
}


fun capturarFotografia(
    cameraController: LifecycleCameraController,
    contexto: Context,
    onImagenGuardada: (uri: Uri) -> Unit
) {
    val directorioFotos = File(contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "nombreLugar")
    directorioFotos.mkdirs()

    val nombreFoto = generarNombreUnico() + ".jpg"
    val archivoFoto = File(directorioFotos, nombreFoto)

    val opciones = ImageCapture.OutputFileOptions.Builder(archivoFoto).build()
    cameraController.takePicture(
        opciones,
        ContextCompat.getMainExecutor(contexto),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let {
                    Log.v("etiqueta", "URI = $it")
                    onImagenGuardada(it)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("capturarFotografia::OnImageSavedCallback::onError", exception.message ?: "error")
            }
        }
    )
}

fun conseguirUbicacion(contexto: Context, onSuccess:(ubicacion: Location)-> Unit){
    try {
        val servicio = LocationServices.getFusedLocationProviderClient(contexto)
        val tarea = servicio.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
        tarea.addOnSuccessListener {
            onSuccess(it)
        }
    }catch (se:SecurityException){
        throw FaltaPermisosException("Sin Permisos de ubicacion")
    }

}


@Composable
fun PantallaUbicacionUI(appVM:AppVM, lanzadorPermisos:ActivityResultLauncher<Array<String>> ){
    val contexto = LocalContext.current


    Column (){
        Button(onClick = {
            appVM.permisosUbicacionOk = {
                conseguirUbicacion(contexto) {
                    appVM.latitud.value = it.latitude
                    appVM.longitud.value = it.longitude
                }
            }


            lanzadorPermisos.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

        }) {
            Text("Cordenadas de la Ubicacion")
        }
        Text("Lat: ${appVM.latitud.value} Long: ${appVM.longitud.value}")
        Button(onClick = {
            appVM.pantallaActual.value = Pantalla.FORM
        }) {
            Text("Regresar")
        }
        Spacer(Modifier.height(100.dp))



        AndroidView(
            factory = {
                MapView(it).apply  {
                    setTileSource(TileSourceFactory.MAPNIK)

                    org.osmdroid.config.Configuration.getInstance().userAgentValue =
                        contexto.packageName
                    controller.setZoom(15.0)
                }
            }, update = {
                it.overlays.removeIf{ true }
                it.invalidate()

                val geoPoint = GeoPoint(appVM.latitud.value, appVM.longitud.value)
                it.controller.animateTo(geoPoint)

                val marcador = Marker(it)
                marcador.position = geoPoint
                marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                it.overlays.add(marcador)
            }
        )
    }
}
