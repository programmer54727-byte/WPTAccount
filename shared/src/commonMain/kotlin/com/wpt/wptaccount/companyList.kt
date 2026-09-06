package com.wpt.wptaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import wptaccount.shared.generated.resources.Res
import wptaccount.shared.generated.resources.applogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyList(
    onCreateCompanyClick: () -> Unit,
    onEditCompanyClick: (Company) -> Unit,
    onCompanyClick: (Company) -> Unit,
    onLogout: () -> Unit
) {
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    
    var companyToDelete by rememberSaveable(stateSaver = CompanySaver) { mutableStateOf<Company?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            companies = supabase.from("companies")
                .select()
                .decodeList<Company>()
        } catch (e: Exception) {
            println("Load companies error: ${e.message}")
            error = "Failed to load companies. Please check your connection."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Company") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            if (companies.isNotEmpty()) {
                FloatingActionButton(onClick = onCreateCompanyClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create Company")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (companies.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.applogo),
                        contentDescription = "WPT Logo",
                        modifier = Modifier.size(150.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "No companies found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onCreateCompanyClick,
                        modifier = Modifier.widthIn(min = 200.dp)
                    ) {
                        Text(
                            text = "Create Your First Company",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Your Companies",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(companies) { company ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onCompanyClick(company) }
                        ) {
                            ListItem(
                                headlineContent = { Text(company.company_name) },
                                supportingContent = { 
                                    company.state?.let { state ->
                                        company.country?.let { country ->
                                            Text("$state, $country")
                                        }
                                    }
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { onEditCompanyClick(company) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Company", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { companyToDelete = company }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Company", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (companyToDelete != null) {
        AlertDialog(
            onDismissRequest = { companyToDelete = null },
            title = { Text("Delete Company") },
            text = { Text("Are you sure you want to delete '${companyToDelete!!.company_name}'? All data associated with this company will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                supabase.from("companies").delete {
                                    filter { eq("id", companyToDelete!!.id!!) }
                                }
                                companies = companies.filter { it.id != companyToDelete!!.id }
                            } catch (e: Exception) {
                                println("Delete company error: ${e.message}")
                            } finally {
                                companyToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { companyToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
