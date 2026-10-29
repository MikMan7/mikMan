package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.AppLanguage
import com.example.data.model.NavigationTab
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthManager
import com.example.ui.auth.SignInScreen
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RoutersScreen
import com.example.ui.screens.UsersScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardLight
import com.example.ui.theme.SuccessGreen
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("MainActivity", "FirebaseApp initialized in onCreate")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize FirebaseApp in onCreate: ${e.message}", e)
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun RootAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val auth = remember {
        try {
            Firebase.auth
        } catch (e: Exception) {
            Log.w("RootAppContent", "Firebase Auth not available yet: ${e.message}")
            null
        }
    }

    var currentUser by remember {
        mutableStateOf<FirebaseUser?>(
            try {
                auth?.currentUser
            } catch (e: Exception) {
                null
            }
        )
    }

    var isPreviewMode by remember { mutableStateOf(false) }

    val language by viewModel.appLanguage.collectAsState()
    val isArabic = language == AppLanguage.ARABIC

    // Listen to Auth State safely
    DisposableEffect(auth) {
        if (auth == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            currentUser = try {
                fbAuth.currentUser
            } catch (e: Exception) {
                null
            }
        }
        try {
            auth.addAuthStateListener(listener)
        } catch (e: Exception) {
            Log.w("RootAppContent", "Could not add auth state listener: ${e.message}")
        }
        onDispose {
            try {
                auth.removeAuthStateListener(listener)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Attempt silent auto-sign in on initial launch if auth is available
    LaunchedEffect(auth) {
        if (auth != null && currentUser == null && !isPreviewMode) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = try {
                        auth.currentUser
                    } catch (e: Exception) {
                        null
                    }
                },
                onUnauthenticated = { /* Keep on SignInScreen */ },
                scope = scope
            )
        }
    }

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        if (currentUser == null && !isPreviewMode) {
            SignInScreen(
                isArabic = isArabic,
                onAuthSuccess = {
                    currentUser = try {
                        auth?.currentUser
                    } catch (e: Exception) {
                        null
                    }
                },
                onEnterPreviewMode = {
                    isPreviewMode = true
                }
            )
        } else {
            MikroTrackApp(
                viewModel = viewModel,
                currentUser = currentUser,
                isPreviewMode = isPreviewMode,
                onSignOut = {
                    if (isPreviewMode) {
                        isPreviewMode = false
                    } else {
                        AuthManager.signOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = { currentUser = null },
                            scope = scope
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MikroTrackApp(
    viewModel: MainViewModel,
    currentUser: FirebaseUser?,
    isPreviewMode: Boolean = false,
    onSignOut: () -> Unit
) {
    val language by viewModel.appLanguage.collectAsState()
    val isArabic = language == AppLanguage.ARABIC
    val currentTab by viewModel.currentTab.collectAsState()
    val unresolvedCount by viewModel.unresolvedAlertCount.collectAsState()

    // Handle back button to return to Dashboard if on a sub-screen
    BackHandler(enabled = currentTab != NavigationTab.DASHBOARD) {
        viewModel.selectTab(NavigationTab.DASHBOARD)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MikroTrack",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPreviewMode) {
                                if (isArabic) "معاينة" else "Preview"
                            } else {
                                if (isArabic) "سحابي" else "Cloud"
                            },
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Language switcher
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("language_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyCardLight)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "EN" else "عربي",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Sign Out action
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = if (isArabic) "تسجيل الخروج" else "Sign Out",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NavyCard
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavyCard,
                contentColor = CyanAccent,
                tonalElevation = 8.dp
            ) {
                val tabs = NavigationTab.values()
                tabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    val icon = when (tab) {
                        NavigationTab.DASHBOARD -> Icons.Default.Dashboard
                        NavigationTab.USERS -> Icons.Default.People
                        NavigationTab.ALERTS -> Icons.Default.Notifications
                        NavigationTab.REPORTS -> Icons.Default.Assessment
                        NavigationTab.ROUTERS -> Icons.Default.Router
                    }

                    val title = if (isArabic) tab.titleAr else tab.titleEn

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            if (tab == NavigationTab.ALERTS && unresolvedCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = AlertRed,
                                            contentColor = Color.White
                                        ) {
                                            Text("$unresolvedCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(imageVector = icon, contentDescription = title)
                                }
                            } else {
                                Icon(imageVector = icon, contentDescription = title)
                            }
                        },
                        label = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                NavigationTab.USERS -> UsersScreen(viewModel = viewModel)
                NavigationTab.ALERTS -> AlertsScreen(viewModel = viewModel)
                NavigationTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                NavigationTab.ROUTERS -> RoutersScreen(viewModel = viewModel)
            }
        }
    }
}
