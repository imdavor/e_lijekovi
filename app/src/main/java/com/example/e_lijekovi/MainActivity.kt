package com.example.e_lijekovi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.e_lijekovi.data.Medication
import com.example.e_lijekovi.data.MedicationRepository
import com.example.e_lijekovi.ui.MedicationFormScreen
import com.example.e_lijekovi.ui.MedicationListScreen
import com.example.e_lijekovi.ui.theme.E_lijekoviTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            E_lijekoviTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Use a MutableState<Boolean> and access .value explicitly to avoid analyzer warnings
    val showModule1: MutableState<Boolean> = remember { mutableStateOf(false) }
    val showForm = remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Create repository lazily using LocalContext
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { MedicationRepository(context) }

    // list of medications loaded from repo
    val medsState = remember { mutableStateOf(repo.loadAll()) }

    // currently editing medication (null when creating)
    val medToEdit = remember { mutableStateOf<Medication?>(null) }

    // snackbar host state for confirmations
    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(id = R.string.menu),
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.home)) },
                    selected = !showModule1.value,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showModule1.value = false
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(id = R.string.home)) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = NavigationDrawerItemDefaults.colors()
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.module_1)) },
                    selected = showModule1.value,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showModule1.value = true
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = NavigationDrawerItemDefaults.colors()
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("E_lijekovi") },
                    navigationIcon = {
                        // Animated hamburger ↔ close icon that reflects drawer state
                        Crossfade(targetState = (drawerState.currentValue == DrawerValue.Open)) { isOpen ->
                            IconButton(onClick = {
                                scope.launch {
                                    if (isOpen) drawerState.close() else drawerState.open()
                                }
                            }) {
                                if (isOpen) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(id = R.string.close_navigation_drawer)
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Menu,
                                        contentDescription = stringResource(id = R.string.open_navigation_drawer)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            if (!showModule1.value) {
                Greeting(
                    name = "Android",
                    modifier = Modifier.padding(innerPadding),
                    onOpenModule = {
                        showModule1.value = true
                        // close drawer if somehow open
                        scope.launch { drawerState.close() }
                    }
                )
            } else {
                if (showForm.value) {
                    // Show medication form; pass initial if editing
                    MedicationFormScreen(onSave = { med ->
                        if (medToEdit.value != null) {
                            repo.update(med)
                            medsState.value = repo.loadAll()
                            medToEdit.value = null
                            scope.launch { snackbarHostState.showSnackbar("Lijek spremljen (uređeno)") }
                        } else {
                            repo.add(med)
                            medsState.value = repo.loadAll()
                            scope.launch { snackbarHostState.showSnackbar("Lijek spremljen") }
                        }
                        showForm.value = false
                    }, onCancel = {
                        medToEdit.value = null
                        showForm.value = false
                    }, modifier = Modifier.padding(innerPadding), initial = medToEdit.value)
                } else {
                    MedicationListScreen(
                        medications = medsState.value,
                        onAdd = {
                            medToEdit.value = null
                            showForm.value = true
                        },
                        onEdit = { med ->
                            medToEdit.value = med
                            showForm.value = true
                        },
                        onDelete = { id ->
                            repo.delete(id)
                            medsState.value = repo.loadAll()
                            scope.launch { snackbarHostState.showSnackbar("Lijek obrisan") }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, onOpenModule: () -> Unit = {}) {
    Column(modifier = modifier) {
        Text(
            text = "Hello $name!",
            modifier = Modifier
        )
        Button(onClick = onOpenModule) {
            Text("Open Module 1")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    E_lijekoviTheme {
        Greeting("Android")
    }
}