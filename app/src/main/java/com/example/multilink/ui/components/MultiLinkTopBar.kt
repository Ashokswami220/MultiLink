package com.example.multilink.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.multilink.R
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiLinkTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    profileColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    titleAlpha: Float = 1f,
    elevation: Dp = dimensionResource(id = R.dimen.elevation_top_bar),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val myPhotoUrl = auth.currentUser?.photoUrl?.toString()

    TopAppBar(
        modifier = modifier.shadow(elevation = elevation),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.alpha(titleAlpha)
            )
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                if (myPhotoUrl != null) {
                    AsyncImage(
                        model = myPhotoUrl,
                        contentDescription = stringResource(id = R.string.cd_profile),
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.icon_profile))
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(id = R.string.cd_profile),
                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_profile)),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            actionIconContentColor = profileColor
        ),
        windowInsets = windowInsets
    )
}
