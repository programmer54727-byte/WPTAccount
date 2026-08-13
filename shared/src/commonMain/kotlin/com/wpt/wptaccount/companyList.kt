package com.wpt.wptaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import org.jetbrains.compose.resources.painterResource
import wptaccount.shared.generated.resources.Res
import wptaccount.shared.generated.resources.applogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyList(
    onCreateCompanyClick: () -> Unit,
    onCompanyClick: (Company) -> Unit,
    onLogout: () -> Unit
) {
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
