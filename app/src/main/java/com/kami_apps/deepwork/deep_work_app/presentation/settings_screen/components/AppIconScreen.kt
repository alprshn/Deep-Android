package com.kami_apps.deepwork.deep_work_app.presentation.settings_screen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Text
import com.kami_apps.deepwork.deep_work_app.domain.data.AppIcon
import com.kami_apps.deepwork.deep_work_app.presentation.settings_screen.SettingsViewModel

@Composable
fun AppIconScreen(
    navController: NavHostController? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadAppIcons()
    }

    // Handle success/error messages
    LaunchedEffect(state.iconChangeSuccess, state.iconError) {
        when {
            state.iconChangeSuccess -> {
                snackbarHostState.showSnackbar("App icon changed successfully!")
                viewModel.clearIconMessages()
            }

            state.iconError != null -> {
                snackbarHostState.showSnackbar("Error: ${state.iconError}")
                viewModel.clearIconMessages()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController?.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "App Icon",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Content
            when {
                state.isIconLoading && state.availableIcons.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Loading icons...",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                state.iconError != null && state.availableIcons.isEmpty() -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Error loading icons: ${state.iconError}",
                            color = Color.Red,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    // Icons grid
                    Column(
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            "Choose your app icon",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.availableIcons) { icon ->
                                AppIconItem(
                                    icon = icon,
                                    isSelected = icon.isSelected,
                                    isLoading = state.isIconLoading,
                                    onClick = {
                                        if (!state.isIconLoading && !icon.isSelected) {
                                            viewModel.changeAppIcon(icon.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF2C2C2E),
                contentColor = Color.White
            )
        }
    }
}

@Composable

fun AppIconItem(
    icon: AppIcon,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val borderColor = if (isSelected) Color(0xFF0A84FF) else Color.Transparent
    val backgroundColor =
        if (isSelected) Color(0xFF0A84FF).copy(alpha = 0.1f) else Color(0xFF1D1A1F)

    // Convert mipmap resource to bitmap
    val iconBitmap = remember(icon.iconRes) {
        try {
            ContextCompat.getDrawable(context, icon.iconRes)?.toBitmap()?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = !isLoading) { onClick() }
    ) {
        iconBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = icon.name,
                                 modifier = Modifier
                     .size(80.dp)
                     .clip(RoundedCornerShape(12.dp))
                     .border(
                         width = if (isSelected) 2.dp else 0.dp,
                         color = borderColor,
                         shape = RoundedCornerShape(12.dp)
                     )
            )
                     } ?: run {
                 // Fallback in case bitmap conversion fails
                 Box(
                     modifier = Modifier
                         .size(50.dp)
                         .clip(RoundedCornerShape(12.dp))
                         .background(Color.Gray, RoundedCornerShape(12.dp))
                         .border(
                             width = if (isSelected) 2.dp else 0.dp,
                             color = borderColor,
                             shape = RoundedCornerShape(12.dp)
                         ),
                     contentAlignment = Alignment.Center
                 ) {
                     Text(
                         text = icon.name.first().toString(),
                         color = Color.Gray,
                         fontSize = 24.sp,
                         fontWeight = FontWeight.Bold
                     )
                 }
             }
        Text(
            text = icon.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun AppIconItemPreview() {
    Column(
        modifier = Modifier
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "App Icon States",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selected state
            AppIconItem(
                icon = AppIcon(
                    id = "selected",
                    name = "Selected",
                    activityAlias = "test.alias",
                    iconRes = com.kami_apps.deepwork.R.mipmap.ic_launcher,
                    isSelected = true
                ),
                isSelected = true,
                isLoading = false,
                onClick = {}
            )

            // Unselected state
            AppIconItem(
                icon = AppIcon(
                    id = "unselected",
                    name = "Normal",
                    activityAlias = "test.alias",
                    iconRes = com.kami_apps.deepwork.R.mipmap.ic_launcher,
                    isSelected = false
                ),
                isSelected = false,
                isLoading = false,
                onClick = {}
            )

            // Loading state
            AppIconItem(
                icon = AppIcon(
                    id = "loading",
                    name = "Loading",
                    activityAlias = "test.alias",
                    iconRes = com.kami_apps.deepwork.R.mipmap.ic_launcher,
                    isSelected = false
                ),
                isSelected = false,
                isLoading = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun AppIconGridPreview() {
    val mockIcons = listOf(
        AppIcon(
            id = "original",
            name = "Original",
            activityAlias = "test.alias.original",
            iconRes = com.kami_apps.deepwork.R.mipmap.ic_launcher,
            isSelected = true
        ),
        AppIcon(
            id = "blue",
            name = "Blue",
            activityAlias = "test.alias.blue",
            iconRes = com.kami_apps.deepwork.R.mipmap.ic_launcher,
            isSelected = false
        ),
        AppIcon(
            id = "white",
            name = "White",
            activityAlias = "test.alias.white",
            iconRes = com.kami_apps.deepwork.R.mipmap.ic_launcher,
            isSelected = false
        )
    )

    Column(
        modifier = Modifier
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            "Choose your app icon",
            color = Color.Gray,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(mockIcons) { icon ->
                AppIconItem(
                    icon = icon,
                    isSelected = icon.isSelected,
                    isLoading = false,
                    onClick = {}
                )
            }
        }
    }
}