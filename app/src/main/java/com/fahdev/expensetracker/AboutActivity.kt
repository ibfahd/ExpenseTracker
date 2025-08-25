package com.fahdev.expensetracker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

private const val TAG = "AboutActivity"

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.about)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back_button_desc)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AboutScreenContent(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AboutScreenContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appInfo = remember { getAppInfo(context) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppHeader(
                versionName = appInfo.versionName,
                versionCode = appInfo.versionCode
            )
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                InfoList(
                    onContactClick = { handleContactClick(context) },
                    onPrivacyClick = { handlePrivacyClick(context) },
                    onRateClick = { handleRateClick(context, appInfo.packageName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        CopyrightFooter()
    }
}

@Composable
fun AppHeader(versionName: String, versionCode: Long) {
    val appIcon: Painter = painterResource(id = R.mipmap.ic_launcher_foreground_foreground)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = appIcon,
            contentDescription = stringResource(R.string.app_name) + " Icon",
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.version_label, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.build_label, versionCode),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.developed_by),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InfoList(
    onContactClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onRateClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        InfoListItem(
            icon = Icons.Default.Email,
            text = stringResource(R.string.contact_us),
            contentDescription = stringResource(R.string.contact_us_desc),
            onClick = onContactClick
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        InfoListItem(
            icon = Icons.Default.Shield,
            text = stringResource(R.string.privacy_policy),
            contentDescription = stringResource(R.string.privacy_policy_desc),
            onClick = onPrivacyClick
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        InfoListItem(
            icon = Icons.Default.Star,
            text = stringResource(R.string.rate_the_app),
            contentDescription = stringResource(R.string.rate_the_app_desc),
            onClick = onRateClick
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun InfoListItem(
    icon: ImageVector,
    text: String,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Decorative, described by row
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CopyrightFooter() {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    Text(
        text = stringResource(R.string.copyright, currentYear),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

// Data class for app information
data class AppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long
)

// Utility functions
@RequiresApi(Build.VERSION_CODES.P)
private fun getAppInfo(context: Context): AppInfo {
    val packageName = context.packageName
    return try {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        AppInfo(
            packageName = packageName,
            versionName = packageInfo.versionName ?: "1.0",
            versionCode = packageInfo.longVersionCode
        )
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e(TAG, "Package not found", e)
        AppInfo(
            packageName = packageName,
            versionName = "1.0",
            versionCode = 1L
        )
    }
}

private fun handleContactClick(context: Context) {
    try {
        val contactUrl = context.getString(R.string.about_contact_url)
        val intent = Intent(Intent.ACTION_VIEW, contactUrl.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open contact URL", e)
        showErrorToast(context, R.string.error_opening_contact)
    }
}

private fun handlePrivacyClick(context: Context) {
    try {
        val privacyUrl = context.getString(R.string.about_privacy_url)
        val intent = Intent(Intent.ACTION_VIEW, privacyUrl.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open privacy URL", e)
        showErrorToast(context, R.string.error_opening_privacy)
    }
}

private fun handleRateClick(context: Context, packageName: String) {
    try {
        // Try to open Play Store app first
        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            "market://details?id=$packageName".toUri()
        )
        context.startActivity(playStoreIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            // Fallback to browser
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Play Store", e)
            showErrorToast(context, R.string.error_opening_play_store)
        }
    }
}


private fun showErrorToast(context: Context, messageResId: Int) {
    Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_SHORT).show()
}

@RequiresApi(Build.VERSION_CODES.P)
@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    ExpenseTrackerTheme {
        AboutScreenContent()
    }
}