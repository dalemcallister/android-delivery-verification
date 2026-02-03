package com.connexi.deliveryverification.ui.delivery

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connexi.deliveryverification.domain.model.LocationValidationStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DeliveryVerificationScreen(
    deliveryId: String,
    onBackClick: () -> Unit,
    onVerificationComplete: () -> Unit,
    viewModel: DeliveryVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(uiState.verificationComplete) {
        if (uiState.verificationComplete) {
            onVerificationComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Verification") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.delivery?.let { delivery ->
                // Facility Information
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = delivery.facilityName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text("Stop ${delivery.stopNumber}")
                        Text("Order: ${delivery.orderVolume}L | ${delivery.orderWeight}kg")
                    }
                }

                // GPS Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (uiState.locationValidation?.status) {
                            LocationValidationStatus.VALID -> MaterialTheme.colorScheme.primaryContainer
                            LocationValidationStatus.POOR_ACCURACY,
                            LocationValidationStatus.TOO_FAR_FROM_TARGET -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = uiState.locationValidation?.message ?: "Searching for GPS...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            uiState.currentLocation?.let { location ->
                                Text(
                                    text = "Accuracy: ${location.accuracy.toInt()}m",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Verification Form
                OutlinedTextField(
                    value = uiState.actualVolume,
                    onValueChange = { viewModel.onActualVolumeChange(it) },
                    label = { Text("Actual Volume (L)") },
                    placeholder = { Text(delivery.orderVolume.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = uiState.actualWeight,
                    onValueChange = { viewModel.onActualWeightChange(it) },
                    label = { Text("Actual Weight (kg)") },
                    placeholder = { Text(delivery.orderWeight.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = uiState.comments,
                    onValueChange = { viewModel.onCommentsChange(it) },
                    label = { Text("Comments (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Complete Button
                Button(
                    onClick = { viewModel.completeDelivery() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = uiState.canComplete && !uiState.isSubmitting
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Complete Delivery")
                    }
                }

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
