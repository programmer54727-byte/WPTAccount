package com.wpt.wptaccount

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
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
    val company_name: String,
    val mailing_name: String? = null,
    val address: String? = null,
    val state: String? = null,
    val country: String? = null,
    val pincode: String? = null,
    val telephone: String? = null,
    val mobile: String? = null,
    val fax: String? = null,
    val email: String? = null,
    val website: String? = null,
    val financial_year_beginning: String? = null,
    val books_beginning: String? = null,
    val tally_vault_password_enabled: String? = null,
    val control_user_access_enabled: String? = null,
    val base_currency_symbol: String? = null,
    val formal_name: String? = null,
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
    
    // Form States
    var showCreateDialog by remember { mutableStateOf(false) }
    var companyName by remember { mutableStateOf("") }
    var mailingName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var fax by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var financialYearStart by remember { mutableStateOf("2024-04-01") }
    var booksStart by remember { mutableStateOf("2024-04-01") }
    var tallyVaultEnabled by remember { mutableStateOf("No") }
    var controlAccessEnabled by remember { mutableStateOf("No") }
    var currencySymbol by remember { mutableStateOf("₹") }
    var formalName by remember { mutableStateOf("INR") }
    
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
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
                    if (companies.isNotEmpty()) {
                        selectedIndex = 0
                    }
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

    // Auto-focus the list to enable keyboard navigation
    LaunchedEffect(companies) {
        if (companies.isNotEmpty()) {
            focusRequester.requestFocus()
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Company") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mailingName, onValueChange = { mailingName = it }, label = { Text("Mailing Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pincode, onValueChange = { if (it.length <= 12) pincode = it }, label = { Text("Pincode") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = telephone, onValueChange = { it }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { if (it.length <= 15) mobile = it }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fax, onValueChange = { fax = it }, label = { Text("Fax") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = financialYearStart, onValueChange = { financialYearStart = it }, label = { Text("Financial Year Beginning (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = booksStart, onValueChange = { booksStart = it }, label = { Text("Books Beginning (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tally Vault Password:")
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = tallyVaultEnabled == "Yes", onClick = { tallyVaultEnabled = "Yes" })
                        Text("Yes")
                        RadioButton(selected = tallyVaultEnabled == "No", onClick = { tallyVaultEnabled = "No" })
                        Text("No")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Control User Access:")
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = controlAccessEnabled == "Yes", onClick = { controlAccessEnabled = "Yes" })
                        Text("Yes")
                        RadioButton(selected = controlAccessEnabled == "No", onClick = { controlAccessEnabled = "No" })
                        Text("No")
                    }

                    OutlinedTextField(value = currencySymbol, onValueChange = { currencySymbol = it }, label = { Text("Base Currency Symbol") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = formalName, onValueChange = { formalName = it }, label = { Text("Formal Name") }, modifier = Modifier.fillMaxWidth())
                }
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
                                            put("company_name", companyName)
                                            put("mailing_name", mailingName)
                                            put("address", address)
                                            put("state", state)
                                            put("country", country)
                                            put("pincode", pincode)
                                            put("telephone", telephone)
                                            put("mobile", mobile)
                                            put("fax", fax)
                                            put("email", email)
                                            put("website", website)
                                            put("financial_year_beginning", financialYearStart)
                                            put("books_beginning", booksStart)
                                            put("tally_vault_password_enabled", tallyVaultEnabled)
                                            put("control_user_access_enabled", controlAccessEnabled)
                                            put("base_currency_symbol", currencySymbol)
                                            put("formal_name", formalName)
                                            put("owner_id", user.id)
                                        }
                                    )
                                    showCreateDialog = false
                                    // Reset fields
                                    companyName = ""; mailingName = ""; address = ""; state = ""; country = ""
                                    pincode = ""; telephone = ""; mobile = ""; fax = ""; email = ""; website = ""
                                    fetchCompanies()
                                }
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isCreating = false
                            }
                        }
                    },
                    enabled = companyName.isNotBlank() && !isCreating
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
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text("Create Your First Company")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && companies.isNotEmpty()) {
                                when (keyEvent.key) {
                                    Key.DirectionDown -> {
                                        if (selectedIndex < companies.size - 1) {
                                            selectedIndex++
                                        }
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        if (selectedIndex > 0) {
                                            selectedIndex--
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Your Companies", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        itemsIndexed(companies) { index, company ->
                            val isSelected = index == selectedIndex
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) 
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                else null
                            ) {
                                ListItem(
                                    headlineContent = { 
                                        Text(
                                            text = company.company_name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent
                                    )
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
}
