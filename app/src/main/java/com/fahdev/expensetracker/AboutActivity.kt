package com.fahdev.expensetracker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fahdev.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {
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
                                        contentDescription = stringResource(id = R.string.back_button_desc)
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

@Composable
fun AboutScreenContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val packageName = context.packageName
    val versionName: String = try {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: "1.0"
    } catch (e: PackageManager.NameNotFoundException) {
        "1.0" // Fallback version
    }

    val contactUrl = stringResource(id = R.string.about_contact_url)
    val privacyUrl = stringResource(id = R.string.about_privacy_url)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppHeader(versionName = versionName)
            Spacer(modifier = Modifier.height(32.dp))
            InfoList(
                onContactClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contactUrl))
                    context.startActivity(intent)
                },
                onPrivacyClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                    context.startActivity(intent)
                },
                onRateClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                },
                onLicensesClick = {
                    // You can replace this with an intent to a proper Open Source Licenses screen
                    Toast.makeText(context, "Open source licenses screen not implemented yet.", Toast.LENGTH_SHORT).show()
                }
            )
        }
        CopyrightFooter()
    }
}

@Composable
fun AppHeader(versionName: String) {
    val appIcon: Painter = painterResource(id = R.mipmap.ic_launcher_foreground_foreground)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = appIcon,
            contentDescription = stringResource(id = R.string.app_name) + " Icon",
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.version_label, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.developed_by),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun InfoList(
    onContactClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onRateClick: () -> Unit,
    onLicensesClick: () -> Unit
) {
    Column {
        InfoListItem(
            icon = Icons.Default.Email,
            text = stringResource(R.string.contact_us),
            onClick = onContactClick
        )
        HorizontalDivider()
        InfoListItem(
            icon = Icons.Default.Shield,
            text = stringResource(R.string.privacy_policy),
            onClick = onPrivacyClick
        )
        HorizontalDivider()
        InfoListItem(
            icon = Icons.Default.Star,
            text = stringResource(R.string.rate_the_app),
            onClick = onRateClick
        )
        HorizontalDivider()
        InfoListItem(
            icon = Icons.Default.Code,
            text = stringResource(R.string.open_source_licenses),
            onClick = onLicensesClick
        )
    }
}

@Composable
fun InfoListItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Decorative
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CopyrightFooter() {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    Text(
        text = stringResource(id = R.string.copyright, currentYear),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    ExpenseTrackerTheme {
        AboutScreenContent()
    }
}