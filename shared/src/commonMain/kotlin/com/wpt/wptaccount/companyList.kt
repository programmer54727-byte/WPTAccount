package com.wpt.wptaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
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
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = { 
                    Text(
                        text = "Select Company",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        modifier = Modifier.padding(start = 8.dp)
                    ) 
                },
                actions = {
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFF7C4DFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7C4DFF)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (companies.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onCreateCompanyClick,
                    containerColor = Color(0xFF7C4DFF),
                    contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, 
                        contentDescription = "Create Company",
                        modifier = Modifier.size(28.dp)
                    )
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20),
                            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp, top = 8.dp)
                        )
                    }
                    items(companies) { company ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clickable { onCompanyClick(company) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left accent strip indicator matching screenshot
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(5.dp)
                                        .background(Color(0xFF7C4DFF), androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Violet tinted circular icon backing
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color(0xFF7C4DFF).copy(alpha = 0.08f), androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CorporateFare,
                                        contentDescription = null,
                                        tint = Color(0xFF7C4DFF),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Text details block
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = company.company_name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val locationText = buildString {
                                        company.state?.let { append(it) }
                                        if (company.country != null) {
                                            if (isNotEmpty()) append(", ")
                                            append(company.country)
                                        }
                                    }
                                    if (locationText.isNotEmpty()) {
                                        Text(
                                            text = locationText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF49454F)
                                        )
                                    }
                                }
                                
                                // Sleek actions area matching screenshot bounds
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    IconButton(
                                        onClick = { onEditCompanyClick(company) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit, 
                                            contentDescription = "Edit Company", 
                                            tint = Color(0xFF7C4DFF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Subtle thin vertical divider line
                                    Box(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .width(1.dp)
                                            .background(Color(0xFFE0E0E0))
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    IconButton(
                                        onClick = { companyToDelete = company }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete, 
                                            contentDescription = "Delete Company", 
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
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
