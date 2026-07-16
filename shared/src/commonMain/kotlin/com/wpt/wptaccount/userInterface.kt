package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class Company(
    val id: String,
    val name: String,
    val owner_id: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboard(
    onLogout: () -> Unit = {}
) {
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCompanyName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    fun fetchCompanies() {
        scope.launch {
            isLoading = true
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    companies = supabase.from("companies")
                        .select()
                        .decodeList<Company>()
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchCompanies()
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Company") },
            text = {
                OutlinedTextField(
                    value = newCompanyName,
                    onValueChange = { newCompanyName = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isCreating = true
                            try {
                                val user = supabase.auth.currentUserOrNull()
                                if (user != null) {
                                    supabase.from("companies").insert(
                                        buildJsonObject {
                                            put("name", newCompanyName)
                                            put("owner_id", user.id)
                                        }
                                    )
                                    showCreateDialog = false
                                    newCompanyName = ""
                                    fetchCompanies()
                                }
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isCreating = false
                            }
                        }
                    },
                    enabled = newCompanyName.isNotBlank() && !isCreating
                ) {
                    Text(if (isCreating) "Creating..." else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WPT Dashboard") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text("Error: $error", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else if (companies.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No companies found.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text("Create Your First Company")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Your Companies", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(companies) { company ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(company.name) },
                                supportingContent = { Text("ID: ${company.id}") }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create New Company")
                        }
                    }
                }
            }
        }
    }
}
