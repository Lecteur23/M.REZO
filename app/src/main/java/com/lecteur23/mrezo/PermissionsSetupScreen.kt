package com.lecteur23.mrezo

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun PermissionsSetupScreen(
    phonePermissionsGranted: Boolean,
    overlayPermissionGranted: Boolean,
    ussdAccessibilityEnabled: Boolean,
    onRequestPhonePermissions: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(homeBackground())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RoundedAppLogo(size = 96)
        Spacer(modifier = Modifier.height(22.dp))
        Text("Autorisations M-REZO", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            "Activez ces acces avant d'ouvrir l'espace de travail.",
            color = TextSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 22.dp)
        )

        PermissionLine(
            title = "Telephone et SIM",
            subtitle = "Lancer les codes USSD et lire les SIM.",
            granted = phonePermissionsGranted,
            onClick = onRequestPhonePermissions
        )
        PermissionLine(
            title = "Affichage par-dessus",
            subtitle = "Afficher le chargement M-REZO pendant l'USSD.",
            granted = overlayPermissionGranted,
            onClick = onOpenOverlaySettings
        )
        PermissionLine(
            title = "Service M-REZO USSD",
            subtitle = "Repondre automatiquement aux menus USSD.",
            granted = ussdAccessibilityEnabled,
            onClick = onOpenAccessibilitySettings
        )

        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
        ) {
            Text("Verifier les autorisations", color = BrandBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionLine(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(enabled = !granted, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (granted) Success.copy(alpha = 0.16f) else Panel)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (granted) Success else BrandBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Security,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Text(if (granted) "OK" else "Activer", color = if (granted) Success else Color(0xFFFFCC00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
